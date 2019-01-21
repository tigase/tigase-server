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
package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;
import tigase.xmpp.jid.BareJID;

import java.io.Serializable;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.MotdProcessor.ID;

/**
 * Created by andrzej on 10.12.2016.
 */
@Id(ID)
@Handles(@Handle(path = {Presence.ELEM_NAME}, xmlns = Presence.CLIENT_XMLNS))
@Bean(name = ID, parent = SessionManager.class, active = false)
public class MotdProcessor
		extends AnnotatedXMPPProcessor
		implements XMPPProcessorIfc, Initializable, UnregisterAware {

	protected static final String ID = "motd";
	private static final Logger log = Logger.getLogger(MotdProcessor.class.getCanonicalName());
	private static final long HOURS_24 = 24 * 60 * 60 * 1000;

	private static final String[] PRESENCE_PRIORITY_PATH = {"presence", "priority"};
	private static final BareJID smJid = BareJID.bareJIDInstanceNS("sess-man");
	@Inject
	private EventBus eventBus;
	private String motd = null;
	private Long motdTimestamp = null;
	@Inject
	private UserRepository userRepository;

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@Override
	public void initialize() {
		try {
			motd = userRepository.getData(smJid, ID, "message");
			String stamp = userRepository.getData(smJid, ID, "timestamp");
			motdTimestamp = stamp == null ? null : Long.parseLong(stamp);
		} catch (UserNotFoundException ex) {
			log.log(Level.FINEST, "MotD has never been set - nothing to load");
			try {
				userRepository.addUser(smJid);
			} catch (TigaseDBException ex1) {
				log.log(Level.WARNING, "failed to create user '" + smJid + "' for SessionManager", ex1);
			}
		} catch (TigaseDBException ex) {
			log.log(Level.WARNING, "failed to read current MOTD from user repository", ex);
		}
		eventBus.registerAll(this);
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {

		if (session == null) {
			return;
		}

		if (packet.getStanzaTo() != null) {
			return;
		}

		if (session.getSessionData(ID) != null) {
			return;
		}

		String priority = packet.getElemCDataStaticStr(PRESENCE_PRIORITY_PATH);
		if (priority != null && Integer.parseInt(priority) < 0) {
			return;
		}

		session.putSessionData(ID, ID);

		if (motd == null || motdTimestamp == null) {
			return;
		}

		long deliveryTimestamp = getLastDeliveryTime(session);
		if (deliveryTimestamp > motdTimestamp && (deliveryTimestamp + HOURS_24) > System.currentTimeMillis()) {
			return;
		}

		setLastDeliveryTime(session);

		Element messageEl = new Element("message");
		messageEl.setXMLNS(Message.CLIENT_XMLNS);
		messageEl.addChild(new Element("body", motd));

		Message message = new Message(messageEl, session.getDomainAsJID(), session.getJID());
		message.setPacketTo(session.getConnectionId());

		results.add(message);
	}

	@HandleEvent
	public void onMotdChanged(MotdUpdatedEvent event) {
		this.motd = event.getMessage();
		this.motdTimestamp = event.getTimestmap();
	}

	public String getMotd() {
		return motd;
	}

	public void setMotd(String motd) throws TigaseDBException {
		Long timestamp = motd == null ? null : System.currentTimeMillis();
		if (motd != null) {
			userRepository.setData(smJid, ID, "message", motd);
			userRepository.setData(smJid, ID, "timestamp", String.valueOf(timestamp));
		} else {
			userRepository.removeData(smJid, ID, "message");
			userRepository.removeData(smJid, ID, "timestamp");
		}
		eventBus.fire(new MotdUpdatedEvent(motd, timestamp));
	}

	private long getLastDeliveryTime(XMPPResourceConnection session) {
		try {
			String str = session.getData(ID, "last-delivery", null);
			if (str == null) {
				return 0;
			}

			return Long.parseLong(str);
		} catch (NotAuthorizedException | TigaseDBException ex) {
			log.log(Level.FINEST, session.toString() + ", could not retrieve last delivery timestamp", ex);
			return 0;
		}
	}

	private void setLastDeliveryTime(XMPPResourceConnection session) {
		try {
			String stamp = String.valueOf(System.currentTimeMillis());
			session.setData(ID, "last-delivery", stamp);
		} catch (NotAuthorizedException | TigaseDBException ex) {
			log.log(Level.FINEST, session.toString() + ", could not update last delivery timestamp", ex);
		}
	}

	public static class MotdUpdatedEvent
			implements Serializable {

		private String message;
		private Long timestamp;

		public MotdUpdatedEvent() {
		}

		public MotdUpdatedEvent(String msg, Long timestamp) {
			this.message = msg;
			this.timestamp = timestamp;
		}

		public String getMessage() {
			return message;
		}

		public Long getTimestmap() {
			return timestamp;
		}

	}
}
