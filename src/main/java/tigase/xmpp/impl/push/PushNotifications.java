/*
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

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.eventbus.HandleEvent;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.Iq;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.amp.db.MsgRepository;
import tigase.server.xmppsession.SessionManager;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.OfflineMessages;
import tigase.xmpp.impl.annotation.DiscoFeatures;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static tigase.xmpp.impl.push.PushNotifications.XMLNS;

/**
 * Created by andrzej on 30.12.2016.
 */
@Bean(name = PushNotifications.ID, parent = SessionManager.class, active = true, exportable = true)
@Id(PushNotifications.ID)
@DiscoFeatures({PushNotifications.ID})
@Handles({@Handle(path = {Iq.ELEM_NAME, "enable"}, xmlns = XMLNS),
		  @Handle(path = {Iq.ELEM_NAME, "disable"}, xmlns = XMLNS),
		  @Handle(path = {Message.ELEM_NAME}, xmlns = Message.CLIENT_XMLNS)})
public class PushNotifications
		extends AbstractPushNotifications
		implements XMPPProcessorIfc, OfflineMessages.Notifier, RegistrarBean {

	private static final Logger log = Logger.getLogger(PushNotifications.class.getCanonicalName());

	private Element[] discoFeatures = new Element[0];

	@Inject
	private ArrayList<PushNotificationsAware> awares = new ArrayList<>();
	@Inject
	private ArrayList<PushNotificationsExtension> triggers = new ArrayList<>();
	@Inject(nullAllowed = true)
	private ArrayList<PushNotificationsFilter> filters = new ArrayList<>();
	@ConfigField(desc = "Send offline messages retrieved notification", alias = "send-offline-messages-retrieved-notification")
	private boolean sendOfflineMessagesRetrievedNotification = true;
	@ConfigField(desc = "Send account removal notification", alias = "send-account-removal-notification")
	private boolean sendAccountRemovalNotification = false;

	@Override
	public Element[] supDiscoFeatures(XMPPResourceConnection session) {
		return discoFeatures;
	}

	public void setAwares(ArrayList<PushNotificationsAware> awares) {
		this.awares = awares;
	}

	public void setFilter(ArrayList<PushNotificationsFilter> filters) {
		this.filters = Optional.ofNullable(filters).orElseGet(ArrayList::new);
		refreshDiscoFeatures();
	}

	public void setTriggers(ArrayList<PushNotificationsExtension> triggers) {
		this.triggers = triggers;
		refreshDiscoFeatures();
	}

	protected void refreshDiscoFeatures() {
		this.discoFeatures = Stream.concat(Arrays.stream(super.supDiscoFeatures(null)),
										   awares.stream().map(PushNotificationsAware::getDiscoFeatures).flatMap(Arrays::stream)).toArray(Element[]::new);
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository nonAuthUserRepository,
						Queue<Packet> results, Map<String, Object> map) throws XMPPException {
		try {
			if (packet.getElemName() == Message.ELEM_NAME) {
				processMessage(packet, session, results::offer);
				return;
			} else {
				super.process(packet, session, nonAuthUserRepository, results, map);
			}
		} catch (NotAuthorizedException ex) {
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet, "Session is not authorized", true));
		} catch (TigaseDBException ex) {
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet, null, true));
		}
	}

	@Override
	protected void processMessage(Packet packet, XMPPResourceConnection session, Consumer<Packet> consumer)
			throws NotAuthorizedException, TigaseDBException {
		super.processMessage(packet, session, consumer);

		if (session == null || !session.isAuthorized() || !shouldSendNotification(packet, session.getBareJID(), session)) {
			return;
		}
		sendPushNotification(session, PushNotificationCause.STANZA, packet, consumer);
	}

	@Override
	public void notifyNewOfflineMessage(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
										Map<String, Object> map) {
		if (packet.getElemName() != tigase.server.Message.ELEM_NAME) {
			return;
		}

		if (!shouldSendNotification(packet, packet.getStanzaTo().getBareJID(), session)) {
			return;
		}

		try {
			sendPushNotification(session, PushNotificationCause.MESSAGES_FETCHED, packet, results::offer);
		} catch (UserNotFoundException ex) {
			log.log(Level.FINEST, "Could not send push notification for message " + packet, ex);
		} catch (TigaseDBException ex) {
			log.log(Level.WARNING, "Could not send push notification for message " + packet, ex);
		}
	}

	@Override
	public void notifyOfflineMessagesRetrieved(XMPPResourceConnection session, Queue<Packet> results) {
		try {
			BareJID userJid = session.getBareJID();
			Map<String, Element> pushServices = getPushServices(userJid);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Offline messages retrieved push notifications for JID: {0}, pushServices: {1}",
				        new Object[]{userJid, pushServices});
			}
			if (pushServices.isEmpty()) {
				return;
			}

			notifyOfflineMessagesRetrieved(userJid, pushServices.values(), results::offer);
		} catch (UserNotFoundException | NotAuthorizedException ex) {
			log.log(Level.FINEST, "Could not send push notification about offline message retrieval by " + session, ex);
		} catch (TigaseDBException ex) {
			log.log(Level.WARNING, "Could not send push notification about offline message retrieval by " + session,
					ex);
		}
	}

	@Override
	public void register(Kernel kernel) {
		
	}

	@Override
	public void unregister(Kernel kernel) {

	}

	@HandleEvent(filter = HandleEvent.Type.local, sync = true)
	public void onUserRemoved(UserRepository.UserBeforeRemovedEvent event) {
		try {
			if (!sendAccountRemovalNotification) {
				return;
			}
			Map<String,Element> pushServices = getPushServices(event.getJid());
			if (pushServices.isEmpty()) {
				return;
			}
			
			sendPushNotification(event.getJid(), pushServices.values(), null, PushNotificationCause.ACCOUNT_REMOVED,
								 null, Map.of(), consumer -> {
					});
		} catch (Throwable ex) {
			log.log(Level.WARNING, "Could not get push services for " + event.getJid(), ex);
		}
	}
	
	@Override
	protected Element createSettingsElement(JID jid, String node, Element enableElem, Element optionsForm) {
		Element settingsEl = super.createSettingsElement(jid, node, enableElem, optionsForm);
		String name = enableElem.getAttributeStaticStr("name");
		if (name != null && !name.isBlank()) {
			settingsEl.setAttribute("name", name);
		}
		for (PushNotificationsAware trigger : awares) {
			trigger.processEnableElement(enableElem, settingsEl);
		}
		return settingsEl;
	}

	protected void notifyOfflineMessagesRetrieved(BareJID userJid, Collection<Element> pushServices, Consumer<Packet> packetConsumer) {
		if (!sendOfflineMessagesRetrievedNotification) {
			return;
		}
		Map<Enum, Long> map = new HashMap<>();
		map.put(MsgRepository.MSG_TYPES.message, 0l);
		sendPushNotification(userJid, pushServices, null, PushNotificationCause.MESSAGES_FETCHED, null, map,
							 packetConsumer);
	}

	@Override
	protected Element prepareNotificationPayload(Element pushServiceSettings, PushNotificationCause cause, Packet packet, long msgCount) {
		Element notification = super.prepareNotificationPayload(pushServiceSettings, cause, packet, msgCount);
		for (PushNotificationsExtension trigger : triggers) {
			trigger.prepareNotificationPayload(pushServiceSettings, cause, packet, msgCount, notification);
		}
		return notification;
	}

	@Override
	protected boolean isSendingNotificationAllowed(BareJID userJid, XMPPResourceConnection session, Element pushServiceSettings, Packet packet) {
		if (!super.isSendingNotificationAllowed(userJid, session, pushServiceSettings, packet)) {
			return false;
		}

		for (PushNotificationsFilter filter : filters) {
			if (!filter.isSendingNotificationAllowed(userJid, session, pushServiceSettings, packet)) {
				return false;
			}
		}
		return true;
	}

	// move to filter
	protected boolean shouldSendNotification(Packet packet, BareJID userJid, XMPPResourceConnection session) {
		if (session == null && packet.getElemName() == Message.ELEM_NAME && packet.getElemChild("body") != null) {
			return true;
		}

		for (PushNotificationsExtension trigger : triggers) {
			try {
				if (trigger.shouldSendNotification(packet, userJid, session)) {
					return true;
				}
			} catch (XMPPException ex) {
				log.log(Level.FINER, "exception while checking if trigger " + trigger.getClass().getCanonicalName() +
						" should be fired", ex);
			}
		}

		return false;
	}

	protected static abstract class AbstractAdhocCommand implements AdHocCommand {

		private final String node;
		private final String name;

		@Inject
		private SessionManager component;
		@Inject
		private AbstractPushNotifications pushNotifications;

		protected AbstractAdhocCommand(String node, String name) {
			this.node = node;
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getNode() {
			return node;
		}

		@Override
		public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
			try {
				final Element data = request.getCommand().getChild("x", "jabber:x:data");

				if (request.isAction("cancel")) {
					response.cancelSession();
				} else {
					if (data == null) {
						response.getElements().add(prepareForm(request, response).getElement());
						response.startSession();
					} else {
						Form form = new Form(data);
						if (form.isType("submit")) {
							Form responseForm = submitForm(request, response, form);
							if (responseForm != null) {
								response.getElements().add(responseForm.getElement());
							}
						}
					}
				}
			} catch (AdHocCommandException ex) {
				throw ex;
			} catch (Exception e) {
				log.log(Level.FINE, "Exception during execution of adhoc command " + getNode(), e);
				throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR, e.getMessage());
			}
		}

		protected abstract Form prepareForm(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException;
		protected abstract Form submitForm(AdhHocRequest request, AdHocResponse response, Form form)
				throws AdHocCommandException;

		protected boolean isEmpty(String input) {
			return input == null || input.isBlank();
		}

		protected String assertNotEmpty(String input, String message) throws AdHocCommandException {
			if (isEmpty(input)) {
				throw new AdHocCommandException(Authorization.BAD_REQUEST, message);
			}
			return input.trim();
		}

		@Override
		public boolean isAllowedFor(JID jid) {
			return component.isAdmin(jid);
		}

		public SessionManager getComponent() {
			return component;
		}

		public AbstractPushNotifications getPushNotifications() {
			return pushNotifications;
		}
	}

	@Bean(name = "push-list-devices", parent = SessionManager.class, active = true)
	public static class ListDevicesAdhocCommand extends AbstractAdhocCommand {

		public ListDevicesAdhocCommand() {
			super("push-list-devices", "List push devices");
		}

		@Override
		protected Form prepareForm(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
			Form form = new Form("form", "Unregister device", "Fill out and submit this form to list enabled devices with push notifications");
			form.addField(Field.fieldJidSingle("userJid", "", "Account JID"));
			return form;
		}

		@Override
		protected Form submitForm(AdhHocRequest request, AdHocResponse response, Form form)
				throws AdHocCommandException {
			Form result = new Form("result", "List of push devices", null);
			try {
				BareJID accountJid = BareJID.bareJIDInstance(
						assertNotEmpty(form.getAsString("userJid"), "Account JID is required!"));
				Map<String, Element> pushServices = getPushNotifications().getPushServices(accountJid);
				String[] deviceIds = pushServices.keySet().stream().sorted().toArray(String[]::new);
				result.addField(Field.fieldTextMulti("deviceIds", deviceIds, "List of devices"));
				return result;
			} catch (TigaseStringprepException|TigaseDBException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Bean(name = "push-unregister-device", parent = SessionManager.class, active = true)
	public static class DisableDeviceAdHocCommand
			extends AbstractAdhocCommand {


		public DisableDeviceAdHocCommand() {
			super("push-disable-device", "Disable push notifications");
		}

		protected Form prepareForm(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
			try {
				return prepareForm(null);
			} catch (TigaseDBException ex) {
				throw new RuntimeException(ex);
			}
		}

		protected Form submitForm(AdhHocRequest request, AdHocResponse response, Form form)
				throws AdHocCommandException {
			try {
				BareJID accountJid = BareJID.bareJIDInstance(assertNotEmpty(form.getAsString("userJid"), "Account JID is required!"));
				String key = form.getAsString("deviceId");
				if (isEmpty(key)) {
					return prepareForm(accountJid);
				}
				int idx = key.indexOf('/');
				if (idx < 0) {
					throw new RuntimeException("Invalid device ID: " + key);
				}
				JID jid = JID.jidInstance(key.substring(0, idx));
				String node = key.substring(idx + 1);
				getPushNotifications().disableNotifications(null, accountJid, jid, node, getComponent()::addOutPacket);
				return null;
			} catch (TigaseStringprepException|TigaseDBException|NotAuthorizedException e) {
				throw new RuntimeException(e);
			}
		}

		protected Form prepareForm(BareJID accountJid) throws TigaseDBException {
			Form form = new Form("form", "Unregister device", "Fill out and submit this form to disable sending push notifications to selected device");
			form.addField(Field.fieldJidSingle("userJid", accountJid == null ? "" : accountJid.toString(), "Account JID"));
			if (accountJid != null) {
				Map<String, Element> pushServices = getPushNotifications().getPushServices(accountJid);
				List<Map.Entry<String,Element>> entries = pushServices.entrySet().stream().sorted(
						Map.Entry.comparingByKey()).toList();
				form.addField(Field.fieldListSingle("deviceId", "", "Device", entries.stream()
						.map(Map.Entry::getValue)
						.map(settings -> Optional.ofNullable(settings.getAttributeStaticStr("name"))
								.orElseGet(() -> settings.getAttributeStaticStr("jid") + " / " +
										settings.getAttributeStaticStr("node")))
						.toArray(String[]::new), entries.stream().map(Map.Entry::getKey).toArray(String[]::new)));
			}
			return form;
		}
		
	}

}
