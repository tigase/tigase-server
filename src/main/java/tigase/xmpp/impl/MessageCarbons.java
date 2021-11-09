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
package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Iq;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.server.xmppsession.UserPresenceChangedEvent;
import tigase.server.xmppsession.UserSessionEvent;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * MessageCarbons class implements XEP-0280 Message Carbons protocol extension.
 *
 * @author andrzej
 */
@Bean(name = MessageCarbons.ID, parent = SessionManager.class, active = true)
public class MessageCarbons
		extends XMPPProcessor
		implements XMPPProcessorIfc, XMPPPacketFilterIfc {

	public static final String XMLNS = "urn:xmpp:carbons:2";
	protected static final String ID = "message-carbons";
	private static final Logger log = Logger.getLogger(MessageCarbons.class.getCanonicalName());
	private static final String[][] ELEMENTS = {{"message"}, {"iq", "enable"}, {"iq", "disable"}};
	private static final String[] XMLNSS = {XMPPProcessor.CLIENT_XMLNS, XMLNS, XMLNS};

	private static final Element[] DISCO_FEATURES = {new Element("feature", new String[]{"var"}, new String[]{XMLNS})};
	private static final Element[] DISCO_FEATURES_WITH_RULES = {
			new Element("feature", new String[]{"var"}, new String[]{XMLNS}),
			new Element("feature", new String[]{"var"}, new String[]{"urn:xmpp:carbons:rules:0"})};

	private static final String ENABLED_KEY = XMLNS + "-enabled";

	private static final String ENABLED_RESOURCES_KEY = XMLNS + "-resources";

	private static final String ENABLE_ELEM_NAME = "enable";
	private static final String DISABLE_ELEM_NAME = "disable";

	private static final String[] MESSAGE_HINTS_NO_COPY = {Message.ELEM_NAME, "no-copy"};
	private static final String MESSAGE_HINTS_XMLNS = "urn:xmpp:hints";

	private static final Function<String, Object> RESOURCES_MAP_FACTORY = (k) -> {
		return new ConcurrentHashMap<JID, Boolean>();
	};

	private static final ElementMatcher[] DEF_MSG_CARBON_PATHS = {
			new ElementMatcher(new String[]{Message.ELEM_NAME, "body"}, null, true),
			// handling recipts, markers and states..
			new ElementMatcher(new String[]{Message.ELEM_NAME, "received"}, "urn:xmpp:receipts", true),
			new ElementMatcher(new String[]{Message.ELEM_NAME}, true, "urn:xmpp:chat-markers:0", null, true),
			new ElementMatcher(new String[]{Message.ELEM_NAME}, true, "http://jabber.org/protocol/chatstates", null, true),
			// handling MUC invitations..
			new ElementMatcher(new String[]{Message.ELEM_NAME, "x"},"jabber:x:conference", true),
			new ElementMatcher(new String[]{Message.ELEM_NAME, "x", "invite"}, null, true)
	};

	private final EventBus eventBus = EventBusFactory.getInstance();
	@Inject
	private MessageDeliveryLogic messageProcessor;

	@ConfigField(desc = "Send carbons of messages of type normal with matching paths", alias = "msg-carbons-paths")
	private ElementMatcher[] msgCarbonPaths = DEF_MSG_CARBON_PATHS;

	private boolean usesDefCarbonRules = true;

	/**
	 * Returns true if session is enabled for receiving carbon copy messages
	 */
	private static boolean isEnabled(XMPPResourceConnection session) throws NotAuthorizedException {
		Boolean value = (Boolean) session.getSessionData(ENABLED_KEY);
		return (value != null && value);
	}

	@Inject(nullAllowed = true)
	private SessionManager.MessageArchive messageArchive;

	/**
	 * Prepare packet which is carbon copy of message passed as argument
	 */
	private static Packet prepareCarbonCopy(Packet packet, JID srcJid, JID jid,
											String type) { //throws NoConnectionIdException {
		Packet msgClone = Message.getMessage(srcJid, jid, packet.getType(), null, null, null, packet.getStanzaId());

		//msgClone.setPacketTo(session.getConnectionId(jid));

		Element received = new Element(type);
		received.setXMLNS(XMLNS);
		msgClone.getElement().addChild(received);

		Element forwarded = new Element("forwarded");
		forwarded.setXMLNS("urn:xmpp:forward:0");
		received.addChild(forwarded);

		forwarded.addChild(packet.getElement().clone());

		return msgClone;

	}

	@Override
	public String id() {
		return ID;
	}

	public String[] getMsgCarbonPaths() {
		String[] result = new String[msgCarbonPaths.length];
		for (int i = 0; i < msgCarbonPaths.length; i++) {
			result[i] = msgCarbonPaths[i].toString();
		}
		return result;
	}

	public void setMsgCarbonPaths(String[] matcherStrs) {
		List<ElementMatcher> matchers = new ArrayList<>();
		for (String matcherStr : matcherStrs) {
			ElementMatcher matcher = ElementMatcher.create(matcherStr);
			if (matcher != null) {
				matchers.add(matcher);
			}
		}
		msgCarbonPaths = matchers.toArray(new ElementMatcher[0]);
		usesDefCarbonRules = Arrays.equals(
				Arrays.stream(DEF_MSG_CARBON_PATHS).map(matcher -> matcher.toString()).toArray(String[]::new),
				matcherStrs);
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {

		if (session == null) {
			return;
		}

		if (!session.isAuthorized()) {
			try {
				results.offer(
						session.getAuthState().getResponseMessage(packet, "Session is not yet authorized.", false));
			} catch (PacketErrorTypeException ex) {
				log.log(Level.FINEST, "ignoring packet from not authorized session");
			}
			return;
		}

		if (packet.getElemName() == Iq.ELEM_NAME) {

			boolean enable = packet.getElement().getChild(ENABLE_ELEM_NAME, XMLNS) != null;
			boolean disable = packet.getElement().getChild(DISABLE_ELEM_NAME, XMLNS) != null;

			// we can only enable or disable but we cannot do both
			if ((enable && disable) || (!enable && !disable)) {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, null, false));
			} else {
				setEnabled(session, enable);
				// send result of operation
				results.offer(packet.okResult((Element) null, 0));
			}
		} else if (packet.getElemName() == Message.ELEM_NAME
//				&& packet.getType() == StanzaType.chat
				&& packet.getStanzaTo() != null) {

			// ignoring if message packet was resent from c2s for redelivery
			if (C2SDeliveryErrorProcessor.isDeliveryError(packet)) {
				return;
			}

			Map<JID, Boolean> resources = (Map<JID, Boolean>) session.getCommonSessionData(ENABLED_RESOURCES_KEY);
			if (resources == null || resources.isEmpty()) {
				// no session has enabled message carbons
				return;
			}

			// if this is error delivering forked message we should not fork it
			// but we need to fork only messsages with type chat so no need to check it
			//if (isErrorDeliveringForkedMessage(packet, session))
			//	return;

			// it is better to carbon copy all messages except from errors..
			// this way all devices will be kept in sync
			if (shouldSendCarbons(packet, session)) {

				// if this is error delivering forked message we should not fork it
				// but we need to fork only messsages with type chat so no need to check it
				//if (isErrorDeliveringForkedMessage(packet, session))
				//	return;
				if (packet.getElement().getChild("received", XMLNS) != null ||
						packet.getElement().getChild("sent", XMLNS) != null) {
					return;
				}

				// support for XEP-0334 Message Processing Hints
				if (packet.getAttributeStaticStr(MESSAGE_HINTS_NO_COPY, "xmlns") == MESSAGE_HINTS_XMLNS) {
					return;
				}

				// if this is private message then do not send carbon copy
				Element privateEl = packet.getElement().getChild("private", XMLNS);

				if (privateEl != null) {
					// TODO: is it enought to just remove this element?
					packet.getElement().removeChild(privateEl);
					return;
				}

				String type = session.isUserId(packet.getStanzaTo().getBareJID()) ? "received" : "sent";
				JID srcJid = JID.jidInstance(session.getBareJID());
				// collections of jid to which message will be delivered by default so we need to skip them
				Set<JID> skipForkingTo = prepareSkipForkingToList(packet, session,resources);
				
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER,
							"Sending message carbon copy, packet: {0}, resources {1}, skipForkingTo: {2}, session: {3}",
							new Object[]{packet, resources, skipForkingTo, session});
				}

				List<JID> copyTo = resources.entrySet()
						.stream()
						.filter(Map.Entry::getValue)
						.map(Map.Entry::getKey)
						.filter(jid -> !skipForkingTo.contains(jid))
						.collect(Collectors.toList());

				if (!copyTo.isEmpty()) {
					Packet clone = packet.copyElementOnly();
					clone.setStableId(packet.getStableId());
					if (messageArchive != null) {
						messageArchive.addStableId(clone, session);
					}
					for (JID jid : copyTo) {
						Packet msgClone = prepareCarbonCopy(clone, srcJid, jid, type);
						results.offer(msgClone);
					}
				}
			}
		}
	}

	protected Set<JID> prepareSkipForkingToList(Packet packet, XMPPResourceConnection session, Map<JID, Boolean> resources)
			throws NotAuthorizedException {
		if (session.isUserId(packet.getStanzaTo().getBareJID()) && packet.getStanzaTo().getResource() == null) {
			// message is cloned to all resources by Message.java, it violates RFC6121
			// while it should be copied only to resources with non negative priority!!
			// until it is not solved there is no need to fork messages

			// as we started to respect connection priority we need to implement proper
			// forking of messages sent to bare jid
			// we need to fork this message
			Set<JID> skipForkingTo = messageProcessor.getJIDsForMessageDelivery(session);

			// we should skip forking to JID with enabled message carbons if jid is not from local node
			for (JID jid : resources.keySet()) {
				if (session.getParentSession().getResourceForJID(jid) == null) {
					skipForkingTo.add(jid);
				}
			}
			return skipForkingTo;
		} else {
			return Collections.singleton(session.getJID());
		}
	}

	@Override
	public Element[] supDiscoFeatures(XMPPResourceConnection session) {
		if (usesDefCarbonRules) {
			return DISCO_FEATURES_WITH_RULES;
		} else {
			return DISCO_FEATURES;
		}
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	@Override
	public void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
					   Queue<Packet> results) {
		if ((session == null) || !session.isAuthorized() || (results == null) || (results.size() == 0) ||
				packet == null) {
			return;
		}

		if (packet.getElemName() != Message.ELEM_NAME) {
			return;
		}

		for (Iterator<Packet> it = results.iterator(); it.hasNext(); ) {
			Packet res = it.next();

			if (res.getElemName() != Message.ELEM_NAME) {
				continue;
			}

			// if it is error during delivering forked message then drop it
			if (isErrorDeliveringForkedMessage(packet, session)) {
				it.remove();
			}

			Element messageEl = res.getElement();

			Element privateEl = messageEl.getChild("private", XMLNS);

			if (privateEl != null) {
				// TODO: is it enought to just remove this element?
				messageEl.removeChild(privateEl);
			}
		}
	}

	/**
	 * Method responsible for handing <code>MessageCarbonsStateChangedEvent</code> and marking JIDs of user as
	 * MessageCarbons enabled/disabled.
	 *
	 */
	@HandleEvent
	protected void stateChanged(MessageCarbonsStateChangedEvent event) {
		XMPPSession session = event.getSession();
		ConcurrentHashMap<JID, Boolean> resources = (ConcurrentHashMap<JID, Boolean>) session.computeCommonSessionDataIfAbsent(
				ENABLED_RESOURCES_KEY, RESOURCES_MAP_FACTORY);

		for (JID jid : event.getEnabledJids()) {
			resources.put(jid, true);
		}
		for (JID jid : event.getDisabledJids()) {
			resources.put(jid, false);
		}
	}

	/**
	 * Method handles <code>UserPresenceChangedEvent</code> and synchronizes state of JIDs for MessageCarbons for
	 * particular user.
	 */
	@HandleEvent
	protected void presenceUpdate(UserPresenceChangedEvent presenceEvent) throws NotAuthorizedException {
		XMPPSession session = presenceEvent.getSession();
		Packet packet = presenceEvent.getPresence();
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "session = {0} processing presence = {1}", new Object[]{session, packet.toString()});
		}

		ConcurrentHashMap<JID, Boolean> resources = (ConcurrentHashMap<JID, Boolean>) session.computeCommonSessionDataIfAbsent(
				ENABLED_RESOURCES_KEY, RESOURCES_MAP_FACTORY);

		StanzaType type = packet.getType();
		if (type == null || type == StanzaType.available) {
			if (resources.putIfAbsent(packet.getStanzaFrom(), false) != null) {
				return;
			}

			// if this is first time we see presence from this resource, 
			// notify this user about every other connected resources
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "session = {0} adding resource = {1} to list of available resources",
						new Object[]{session, packet.getStanzaFrom()});
			}

			MessageCarbonsStateChangedEvent event = new MessageCarbonsStateChangedEvent(
					packet.getStanzaFrom().copyWithoutResource(), packet.getStanzaFrom(), session);

			for (XMPPResourceConnection res : session.getActiveResources()) {
				if (res.isAuthorized()) {
					event.add(res.getJID(), isEnabled(res));
				}
			}
			eventBus.fire(event);

		} else if (type == StanzaType.unavailable) {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "session = {0} removing resource = {1} from list of available resources",
						new Object[]{session, packet.getStanzaFrom()});
			}
			resources.remove(packet.getStanzaFrom());
		}

	}

	protected boolean shouldSendCarbons(Packet packet, XMPPResourceConnection session) {
		if (packet.getElemChild("private", "urn:xmpp:carbons:2") != null) {
			return false;
		}
		if (packet.getElemChild("no-copy", "urn:xmpp:hints") != null) {
			return false;
		}

		if (packet.getType() == StanzaType.chat) {
			JID to = packet.getStanzaTo();
			if (to != null && packet.getElemChild("x", "http://jabber.org/protocol/muc#user") != null && session.isAuthorized()) {
				try {
					if (session.isUserId(to.getBareJID())) {
						return false;
					}
				} catch (NotAuthorizedException ex) {
					// we can ignore this..
				}
			}
			return true;
		}

		if (packet.getType() == null || packet.getType() == StanzaType.normal) {
			for (ElementMatcher matcher : msgCarbonPaths) {
				if (matcher.matches(packet)) {
					return matcher.getValue();
				}
			}
		}
		return false;
	}

	/**
	 * Add/Remove session JID to set of JIDs with enabled carbon copy protocol
	 */
	private void setEnabled(XMPPResourceConnection session, boolean value) throws NotAuthorizedException {
		session.putSessionData(ENABLED_KEY, value);

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "session = {0}" + " enabling " + XMLNS, session);
		}

		MessageCarbonsStateChangedEvent event = new MessageCarbonsStateChangedEvent(session.getJID(), session.getJID()
				.copyWithoutResource(), session.getParentSession());
		event.add(session.getJID(), value);
		eventBus.fire(event);
	}

	/**
	 * Method returns true if
	 *
	 */
	private boolean isErrorDeliveringForkedMessage(Packet packet, XMPPResourceConnection session) {

		if (!session.isAuthorized() || packet.getStanzaTo() == null) {
			return false;
		}

		try {
			// check if it is error from delivering forked message
			if (packet.getType() == StanzaType.error && packet.getStanzaTo().getResource() == null &&
					session.isUserId(packet.getStanzaTo().getBareJID())) {

				Element messageEl = packet.getElement();

				// it will be error if it contains sent element
				Element sentEl = messageEl.getChild("sent", XMLNS);
				if (sentEl != null) {
					return true;
				}

				// it will also be error if it contains received element
				Element receivedEl = messageEl.getChild("received", XMLNS);
				if (receivedEl != null) {
					return true;
				}
			}
		} catch (NotAuthorizedException ex) {
			// should not happens
		}

		return false;
	}

	/**
	 * Event class responsible for notification other sessions of same user that message carbons state was changed.
	 */
	public static class MessageCarbonsStateChangedEvent
			extends UserSessionEvent {

		private HashSet<String> disabled = null;
		private HashSet<String> enabled = null;

		/**
		 * Empty constructor to be able to serialize/deserialize event
		 */
		public MessageCarbonsStateChangedEvent() {
			super();
		}

		public MessageCarbonsStateChangedEvent(JID sender, JID userJid, XMPPSession session) {
			super(sender, userJid, session);
		}

		public void add(JID jid, Boolean value) {
			if (value == null) {
				value = false;
			}

			HashSet<String> tmp = null;
			if (value) {
				if (enabled == null) {
					enabled = new HashSet<>();
				}
				tmp = enabled;
			} else {
				if (disabled == null) {
					disabled = new HashSet<>();
				}
				tmp = disabled;
			}
			tmp.add(jid.getResource());
		}

		public Set<JID> getEnabledJids() {
			HashSet<JID> result = new HashSet<>();
			if (this.enabled != null) {
				for (String res : this.enabled) {
					result.add(JID.jidInstanceNS(getUserJid().getBareJID(), res));
				}
			}
			return result;
		}

		public Set<JID> getDisabledJids() {
			HashSet<JID> result = new HashSet<>();
			if (this.disabled != null) {
				for (String res : this.disabled) {
					result.add(JID.jidInstanceNS(getUserJid().getBareJID(), res));
				}
			}
			return result;
		}

	}
}
