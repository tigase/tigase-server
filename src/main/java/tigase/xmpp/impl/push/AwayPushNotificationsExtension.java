/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.xmpp.impl.push;

import tigase.cluster.strategy.ClusteringStrategyIfc;
import tigase.cluster.strategy.ConnectionRecordIfc;
import tigase.component.PacketWriter;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.server.xmppsession.SessionManagerHandler;
import tigase.server.xmppsession.UserPresenceChangedEvent;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tigase.xmpp.impl.push.AbstractPushNotifications.ID;

@Bean(name = "away", parent = PushNotifications.class, active = false)
public class AwayPushNotificationsExtension implements PushNotificationsExtension, Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(AwayPushNotificationsExtension.class.getCanonicalName());

	private static final String PRESENCE_PREV_KEY = ID + "#presence-prev";

	private static final Element[] DISCO_FEATURES = new Element[]{
			new Element("feature", new String[]{"var"}, new String[]{"tigase:push:away:0"})};

	@Inject(nullAllowed = true)
	private ClusteringStrategyIfc clusteringStrategy;
	@Inject
	private EventBus eventBus;
	@Inject
	private SessionManagerHandler sessionManagerHandler;
	@Inject
	private PacketWriter packetWriter;
	@Inject
	private PushNotifications pushNotifications;

	@Override
	public Element[] getDiscoFeatures() {
		return DISCO_FEATURES;
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@HandleEvent(filter = HandleEvent.Type.local)
	public void presenceChanged(UserPresenceChangedEvent event) {
		if (event.getPresence().getType() == StanzaType.unavailable) {
			return;
		}

		XMPPResourceConnection conn = event.getSession().getResourceForJID(event.getPresence().getStanzaFrom());
		if (conn == null) {
			return;
		}

		Packet oldPresence = (Packet) conn.getSessionData(PRESENCE_PREV_KEY);
		conn.putSessionData(PRESENCE_PREV_KEY, event.getPresence());
		if (oldPresence != null) {
			String show = event.getPresence().getElemCDataStaticStr(Presence.PRESENCE_SHOW_PATH);
			if (show != null && !"chat".equals(show)) {
				return;
			}
			String oldShow = event.getPresence().getElemCDataStaticStr(Presence.PRESENCE_SHOW_PATH);
			if (oldShow == null || "chat".equals(oldShow)) {
				return;
			}
		}

		// client changed presence to "online"/"chat" from "away", "xa", "dnd", "offline"
		// so we need to send notification
		Collection<Element> services = getPushServicesForAwayNotifications(conn);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Sending push notifications for JID: {0}, oldPresence: {1}, services: {2}",
					new Object[]{conn.getjid(), oldPresence, services});
		}

		if (services.isEmpty()) {
			return;
		}

		try {
			pushNotifications.notifyOfflineMessagesRetrieved(conn.getBareJID(), services, packetWriter::write);
		} catch (NotAuthorizedException ex) {
			log.log(Level.FINEST, "Connection {0} not yet authorized, ignoring..", conn);
		}
	}


	@Override
	public void processEnableElement(Element enableEl, Element settingsEl) {
		String away = enableEl.getAttributeStaticStr("away");
		if ("true".equals(away)) {
			settingsEl.addAttribute("away", "true");
		}
	}

	@Override
	public void setPushNotifications(PushNotifications pushNotifications) {
		this.pushNotifications = pushNotifications;
	}

	@Override
	public boolean shouldSendNotification(Packet packet, BareJID userJid, XMPPResourceConnection session)
			throws XMPPException {
		if (packet.getElemName() != Message.ELEM_NAME || packet.getType() == StanzaType.groupchat) {
			return false;
		}
		if (session == null || !session.isAuthorized()) {
			return false;
		}
		if (packet.getStanzaTo() == null || !session.isUserId(packet.getStanzaTo().getBareJID())) {
			return false;
		}

		Element body = packet.getElement().findChild(Message.MESSAGE_BODY_PATH);
		if (body == null) {
			return false;
		}

		Collection<Element> services = getPushServicesForAwayNotifications(session);
		if (services.isEmpty()) {
			return false;
		}

		boolean hasOnlineResource = session.getActiveSessions()
				.stream()
				.filter(conn -> conn.getPriority() >= 0)
				.map(conn -> conn.getPresence())
				.filter(presence -> presence != null)
				.filter(presence -> presence.getCDataStaticStr(Presence.PRESENCE_SHOW_PATH) == null)
				.findAny()
				.isPresent();

		if (hasOnlineResource) {
			return false;
		}

		if (clusteringStrategy != null) {
			Set<ConnectionRecordIfc> connections = clusteringStrategy.getConnectionRecords(packet.getStanzaTo().getBareJID());
			if (connections != null) {
				if (connections.stream().filter(rec -> {
					try {
						Method m = rec.getClass().getMethod("getLastPresence");
						Element presence = (Element) m.invoke(rec);
						if (presence == null) {
							return false;
						}
						return presence.getCDataStaticStr(Presence.PRESENCE_SHOW_PATH) == null;
					} catch (Throwable ex) {
						return false;
					}
				}).findAny().isPresent()) {
					return false;
				}
				Optional<JID> notificationSender = Stream.concat(connections.stream().map(rec -> rec.getNode()), Stream.of(sessionManagerHandler.getComponentId()))
						.distinct()
						.sorted()
						.findFirst();
				if (notificationSender.isPresent() &&
						!notificationSender.filter(jid -> !sessionManagerHandler.getComponentId().equals(jid)).isPresent()) {
					return false;
				}
			}
		} else {
			if (session.getActiveSessions()
					.stream()
					.map(conn -> conn.getPresence())
					.filter(Objects::nonNull)
					.filter(presence -> presence.getCDataStaticStr(Presence.PRESENCE_SHOW_PATH) == null)
					.findAny()
					.isPresent()) {
				return false;
			}
		}

		return true;
	}

	protected Collection<Element> getPushServicesForAwayNotifications(XMPPResourceConnection session) {
		Map<String, Element> serviceSettings = pushNotifications.getPushServices(session);
		if (serviceSettings.isEmpty()) {
			return Collections.EMPTY_LIST;
		}

		return serviceSettings.values()
				.stream()
				.filter(el -> el.getAttribute("away") != null)
				.collect(Collectors.toList());
	}
}
