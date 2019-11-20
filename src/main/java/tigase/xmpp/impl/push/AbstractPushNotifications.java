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

import tigase.db.*;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.*;
import tigase.server.amp.db.MsgRepository;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.*;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;
import tigase.xmpp.impl.annotation.DiscoFeatures;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.push.AbstractPushNotifications.XMLNS;

@DiscoFeatures({PushNotifications.ID})
@Handles({@Handle(path = {Iq.ELEM_NAME, "enable"}, xmlns = XMLNS),
		  @Handle(path = {Iq.ELEM_NAME, "disable"}, xmlns = XMLNS),
		  @Handle(path = {Message.ELEM_NAME, "pubsub"}, xmlns = "http://jabber.org/protocol/pubsub")
})
public class AbstractPushNotifications
		extends AnnotatedXMPPProcessor
		implements XMPPProcessorIfc {

	public static final String XMLNS = "urn:xmpp:push:0";
	public static final String ID = XMLNS;
	private static final Logger log = Logger.getLogger(AbstractPushNotifications.class.getCanonicalName());
	private static final String JABBER_X_DATA_XMLNS = "jabber:x:data";
	private static final String SUMMARY_XMLNS = XMLNS + ":summary";
	private static final SimpleParser parser = SingletonFactory.getParserInstance();

	@ConfigField(desc = "Send notifications with body", alias = "with-body")
	protected boolean withBody = true;
	@ConfigField(desc = "Send notifications with sender", alias = "with-sender")
	protected boolean withSender = true;
	@ConfigField(desc = "Max notification timeout", alias = "max-timeout")
	protected Duration maxTimeout = Duration.ofMinutes(6);

	@Inject
	private MsgRepositoryIfc msgRepository;

	@Inject
	private UserRepository userRepository;

	@Inject
	private PacketWriterWithTimeout writer;
	
	protected boolean shouldDisablePush(Authorization error) {
		if (error == null) {
			return false;
		}
		switch (error) {
			case REMOTE_SERVER_TIMEOUT:
			case SERVICE_UNAVAILABLE:
			case INTERNAL_SERVER_ERROR:
			case BAD_REQUEST:
				return false;
			default:
				// we need to handle possible error
				return true;
		}
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository nonAuthUserRepository,
						Queue<Packet> results, Map<String, Object> map) throws XMPPException {
		try {
			if (packet.getElemName() == Message.ELEM_NAME) {
				processMessage(packet, session, results::offer);
				return;
			}

			if (session == null || !session.getConnectionId().equals(packet.getPacketFrom())) {
				results.offer(Authorization.FORBIDDEN.getResponseMessage(packet, null, true));
				return;
			}

			Element actionEl = packet.getElement().findChild(element -> element.getXMLNS() == XMLNS);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Processing PUSH registration, jid: {0}, actionEl: {1}",
						new Object[]{session.getjid(), actionEl});
			}

			if (actionEl != null) {
				String jidStr = actionEl.getAttributeStaticStr("jid");
				if (jidStr == null) {
					throw new TigaseStringprepException("JID is NULL!");
				}
				JID jid = JID.jidInstance(actionEl.getAttributeStaticStr("jid"));
				String node = actionEl.getAttributeStaticStr("node");
				switch (actionEl.getName()) {
					case "enable":
						enableNotifications(session, jid, node, actionEl, actionEl.findChild(
								element -> element.getXMLNS() == JABBER_X_DATA_XMLNS && element.getName() == "x"));
						break;
					case "disable":
						disableNotifications(session, jid, node);
						break;
					default:
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, null, true));
						return;
				}
				results.offer(packet.okResult((Element) null, 0));
			}
		} catch (NotAuthorizedException ex) {
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet, "Session is not authorized", true));
		} catch (TigaseDBException ex) {
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet, null, true));
		} catch (TigaseStringprepException ex) {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
																	   "Attribute 'jid' is not valid JID of Push Notifications service",
																	   true));
		}
	}

	protected void processMessage(Packet packet, XMPPResourceConnection session,
								  Consumer<Packet> results) throws NotAuthorizedException, TigaseDBException {
		Element pubsubEl = packet.getElemChild("pubsub", "http://jabber.org/protocol/pubsub");
		if (pubsubEl != null) {
			String node = pubsubEl.getAttributeStaticStr("node");
			if (node != null) {
				Element affiliationEl = pubsubEl.getChild("affiliation");
				if (affiliationEl != null) {
					String userJid = affiliationEl.getAttributeStaticStr("jid");
					if ("none".equals(affiliationEl.getAttributeStaticStr("affiliation"))) {
						if (userJid != null) {
							userRepository.removeData(BareJID.bareJIDInstanceNS(userJid), ID,
													  packet.getStanzaFrom().toString() + "/" + node);
						}
					}
				}
			}
		}
	}

	protected void enableNotifications(XMPPResourceConnection session, JID jid, String node, Element enableElem,
									   Element optionsForm) throws NotAuthorizedException, TigaseDBException {
		Element settings = createSettingsElement(jid, node, enableElem, optionsForm);

		enableNotifications(session, jid, node, settings);
	}

	protected Element createSettingsElement(JID jid, String node, Element enableElem, Element optionsForm) {
		Element settings = new Element("settings", new String[]{"jid", "node"},
									   new String[]{jid.toString(), node.toString()});
		if (optionsForm != null) {
			settings.addChild(optionsForm);
		}
		return settings;
	}

	protected void enableNotifications(XMPPResourceConnection session, JID jid, String node, Element settings)
			throws NotAuthorizedException, TigaseDBException {
		String key = jid.toString() + "/" + node;
		session.setData(ID, key, settings.toString());
		Map<String, Element> pushServices = getPushServices(session);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Enabled push notifications for JID: {0}, node: {1}, settings: {2}",
					new Object[]{jid, node, settings.toString()});
		}

		pushServices.put(key, settings);
	}

	protected void disableNotifications(XMPPResourceConnection session, JID jid, String node)
			throws NotAuthorizedException, TigaseDBException {
		Map<String, Element> pushServices = getPushServices(session);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Disabled push notifications for JID: {0}, node: {1}, pushServices: {2}",
					new Object[]{jid, node, pushServices});
		}

		if (pushServices != null) {
			if (node != null) {
				String key = jid.toString() + "/" + node;
				pushServices.remove(key);
				session.removeData(ID, key);
			} else {
				String prefix = jid.toString() + "/";
				List<String> removed = new ArrayList<>();
				pushServices.keySet().removeIf(key -> {
					if (key.startsWith(prefix)) {
						removed.add(key);
						return true;
					}
					return false;
				});
				for (String key : removed) {
					session.removeData(ID, key);
				}
			}
		}
	}

	protected Map<String, Element> getPushServices(XMPPResourceConnection session) {
		return (Map<String, Element>) session.computeCommonSessionDataIfAbsent(ID, (key) -> {
			Map<String, Element> pushServices = new ConcurrentHashMap<String, Element>();
			try {
				pushServices.putAll(getPushServices(session.getBareJID()));
			} catch (NotAuthorizedException | UserNotFoundException ex) {
				log.log(Level.FINEST, "Could not load push services for session " + session, ex);
			} catch (TigaseDBException ex) {
				log.log(Level.WARNING, "Could not load push services for session " + session, ex);
			}

			return pushServices;
		});
	}

	protected Element prepareNotificationPayload(Element pushServiceSettings, Packet packet, long msgCount) {
		Element notification = new Element("notification", new String[]{"xmlns"}, new String[]{XMLNS});

		Element x = new Element("x", new String[]{"xmlns"}, new String[]{"jabber:x:data"});
		notification.addChild(x);

		DataForm.addFieldValue(notification, "FORM_TYPE", SUMMARY_XMLNS);
		DataForm.addFieldValue(notification, "message-count", String.valueOf(msgCount));
		if (packet != null) {
			if (withSender) {
				DataForm.addFieldValue(notification, "last-message-sender", packet.getStanzaFrom().toString());
			}
			if (withBody) {
				DataForm.addFieldValue(notification, "last-message-body",
									   packet.getElemCDataStaticStr(tigase.server.Message.MESSAGE_BODY_PATH));
			}
			if (withSender && packet.getElemName() == Message.ELEM_NAME && packet.getType() == StanzaType.groupchat) {
				Element groupchat = new Element("groupchat");
				groupchat.setXMLNS("http://tigase.org/protocol/muc#offline");
				groupchat.addChild(new Element("nickname", packet.getStanzaFrom().getResource()));
				notification.addChild(groupchat);
			}
		}
		return notification;
	}

	protected void sendPushNotification(BareJID userJid, Collection<Element> pushServices,
										XMPPResourceConnection session, Packet packet, Map<Enum, Long> notificationData) {
		pushServices.forEach(settings -> {
			try {
				if (packet != null && !isSendingNotificationAllowed(userJid, session, settings, packet)) {
					return;
				}
				final Element notification = prepareNotificationPayload(settings, packet, notificationData.getOrDefault(
						MsgRepository.MSG_TYPES.message, 0l));
				JID pushService = JID.jidInstance(settings.getAttributeStaticStr("jid"));
				String pushNode = settings.getAttributeStaticStr("node");
				Element publishOptionsForm = settings.findChild(
						element -> element.getXMLNS() == JABBER_X_DATA_XMLNS && element.getName() == "x");

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Push notifications for JID: {0}, notification: {1}, pushServices: {2}",
							new Object[]{userJid, notification, pushService});
				}

				sendPushNotification(userJid, notification, pushService, pushNode, publishOptionsForm);
			} catch (Exception ex) {
				log.log(Level.FINE, "Could not publish notification for " + userJid + " to " +
						settings.getAttributeStaticStr("jid") + " at " + settings.getAttributeStaticStr("node"));
			}
		});
	}

	protected Map<String, Element> getPushServices(BareJID userJid) throws TigaseDBException {
		return userRepository.getDataMap(userJid, ID, this::parseElement);
	}

	protected void sendPushNotification(XMPPResourceConnection session, Packet packet)
			throws TigaseDBException {
		final BareJID userJid = packet.getStanzaTo().getBareJID();
		Map<String, Element> pushServices = (session != null && session.isAuthorized())
											? getPushServices(session)
											: getPushServices(userJid);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Sending push notifications for JID: {0}, packet: {1}, pushServices: {2}",
					new Object[]{userJid, packet, pushServices});
		}
		if (pushServices.isEmpty()) {
			return;
		}

		Map<Enum, Long> typesCount = msgRepository.getMessagesCount(packet.getStanzaTo());

		sendPushNotification(userJid, pushServices.values(), session, packet, typesCount);
	}

	protected boolean isSendingNotificationAllowed(BareJID userJid, XMPPResourceConnection session,
												   Element pushServiceSettings, Packet packet) {
		return true;
	}

	private void sendPushNotification(BareJID userJid, Element notification, JID pushService, String pushNode,
									  Element publishOptionsForm) {
		Element iq = new Element("iq", new String[]{"xmlns", "type"},
								 new String[]{Packet.CLIENT_XMLNS, StanzaType.set.name()});

		Element pubsub = new Element("pubsub", new String[]{"xmlns"},
									 new String[]{"http://jabber.org/protocol/pubsub"});
		iq.addChild(pubsub);
		Element publish = new Element("publish", new String[]{"node"}, new String[]{pushNode});
		pubsub.addChild(publish);
		Element item = new Element("item");
		publish.addChild(item);

		item.addChild(notification);

		if (publishOptionsForm != null) {
			Element publishOptions = new Element("publish-options");
			publishOptions.addChild(publishOptionsForm);
			pubsub.addChild(publishOptions);
		}

		writer.addOutPacketWithTimeout(new Iq(iq, JID.jidInstanceNS(null, userJid.getDomain(), null), pushService),
									   maxTimeout, result -> {
					if (result == null) {
						if (log.isLoggable(Level.FINER)) {
							log.log(Level.FINER, "push notification delivery to " + pushService + " from " + userJid +
									" timed out!");
						}
						return;
					}
					if (!shouldDisablePush(Authorization.getByCondition(result.getErrorCondition()))) {
						return;
					}
					try {
						userRepository.removeData(userJid, ID, pushService + "/" + pushNode);
					} catch (TigaseDBException ex) {
						log.log(Level.FINEST,
								"could not disable push for " + userJid + " on " + pushService + "/" + pushNode, ex);
					}
				});
	}

	private Element parseElement(String data) {
		DomBuilderHandler domHandler = new DomBuilderHandler();
		parser.parse(domHandler, data.toCharArray(), 0, data.length());

		Queue<Element> elems = domHandler.getParsedElements();

		return (elems == null) ? null : elems.poll();
	}

}
