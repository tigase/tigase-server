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

import tigase.db.AuthRepository;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.*;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.server.xmppsession.SessionManager;
import tigase.server.xmppsession.SessionManagerHandler;
import tigase.server.xmppsession.UserConnectedEvent;
import tigase.util.cache.LRUConcurrentCache;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.impl.roster.RosterFlat;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.Privacy.*;

/**
 * Describe class JabberIqPrivacy here.
 * <br>
 * Created: Mon Oct  9 18:18:11 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = JabberIqPrivacy.ID, parent = SessionManager.class, active = true)
public class JabberIqPrivacy
		extends XMPPProcessor
		implements XMPPProcessorIfc, XMPPPreprocessorIfc, XMPPPacketFilterIfc, RegistrarBean {

	protected static final String ACTIVE_EL_NAME = "active";
	protected static final Element BLOCKED_ELEM = new Element("blocked", new String[]{"xmlns"},
															  new String[]{"urn:xmpp:blocking:errors"});
	protected static final String DEFAULT_EL_NAME = "default";
	protected static final String[][] ELEMENTS = {Iq.IQ_QUERY_PATH};
	protected static final String LIST_EL_NAME = "list";
	protected static final String ERROR_EL_NAME = "error";
	protected static final String PRESENCE_EL_NAME = "presence";
	protected static final String PRESENCE_IN_EL_NAME = "presence-in";
	protected static final String PRESENCE_OUT_EL_NAME = "presence-out";
	protected static final String XMLNS = "jabber:iq:privacy";
	protected static final String ID = XMLNS;
	protected static final String[] XMLNSS = {XMLNS};
	protected static final Element[] DISCO_FEATURES = {
			new Element("feature", new String[]{"var"}, new String[]{XMLNS})};
	protected static final Comparator<Element> compar = new Comparator<Element>() {
		@Override
		public int compare(Element el1, Element el2) {
			String or1 = el1.getAttributeStaticStr(ORDER);
			String or2 = el2.getAttributeStaticStr(ORDER);
			if (or1.length() != or2.length()) {
				return Integer.compare(or1.length(), or2.length());
			}

			return or1.compareTo(or2);
		}
	};
	protected static Logger log = Logger.getLogger(JabberIqPrivacy.class.getName());
	protected static RosterAbstract roster_util = RosterFactory.getRosterImplementation(true);
	@Inject(nullAllowed = true)
	protected PrivacyListOfflineCache cache;

	public static Authorization validateList(final XMPPResourceConnection session, final List<Element> items) {
		Authorization result = null;

		try {
			HashSet<Integer> orderSet = new HashSet<Integer>();

			// creating set of all known groups in roster
			HashSet<String> groups = new HashSet<String>();

			if (session != null) {
				JID[] jids = roster_util.getBuddies(session);

				if (jids != null) {
					for (JID jid : jids) {
						String[] buddyGroups = roster_util.getBuddyGroups(session, jid);

						if (buddyGroups != null) {
							for (String group : buddyGroups) {
								groups.add(group);
							}
						}
					}
				}
			}
			for (Element item : items) {
				ITEM_TYPE type = ITEM_TYPE.all;

				if (item.getAttributeStaticStr(TYPE) != null) {
					type = ITEM_TYPE.valueOf(item.getAttributeStaticStr(TYPE));
				}    // end of if (item.getAttribute(TYPE) != null)

				String value = item.getAttributeStaticStr(VALUE);

				switch (type) {
					case jid:

						// if jid is not valid it will throw exception
						JID.jidInstance(value);

						break;

					case group:
						boolean matched = groups.contains(value);

						if (!matched) {
							result = Authorization.ITEM_NOT_FOUND;
						}

						break;

					case subscription:

						// if subscription is not valid it will throw exception
						ITEM_SUBSCRIPTIONS.valueOf(value);

						break;

					case all:
					default:
						break;
				}
				if (result != null) {
					break;
				}

				// if action is not valid it will throw exception
				ITEM_ACTION.valueOf(item.getAttributeStaticStr(ACTION));

				// checking unique order attribute value
				Integer order = Integer.parseInt(item.getAttributeStaticStr(ORDER));

				if ((order == null) || (order < 0) || !orderSet.add(order)) {
					result = Authorization.BAD_REQUEST;
				}
				if (result != null) {
					break;
				}
			}
		} catch (Exception ex) {

			// if we get exception list is not valid
			result = Authorization.BAD_REQUEST;
		}

		return result;
	}

	@Override
	public void register(Kernel kernel) {

	}

	@Override
	public void unregister(Kernel kernel) {

	}

	@Override
	public void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
					   Queue<Packet> results) {
		if ((session == null) || !session.isAuthorized() || (results == null) || (results.size() == 0)) {
			return;
		}
		Queue<Packet> errors = null;
		for (Iterator<Packet> it = results.iterator(); it.hasNext(); ) {
			Packet res = it.next();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Checking outbound packet: {0}", res);
			}

			// Always allow presence unavailable to go, privacy lists packets and
			// all other which are allowed by privacy rules
			if ((res.getType() == StanzaType.unavailable) || res.isXMLNSStaticStr(Iq.IQ_QUERY_PATH, XMLNS) ||
					allowed(res, session)) {
				continue;
			}
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Packet not allowed to go, removing: {0}", res);
			}
			it.remove();

			// support for sending error responses if packet is blocked
			Packet error = prepareError(res, session);
			if (error != null) {
				if (errors == null) {
					errors = new ArrayDeque<Packet>();
				}
				errors.offer(error);
			}
		}
		if (errors != null) {
			results.addAll(errors);
		}
	}

	@Override
	public String id() {
		return ID;
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * <code>preProcess</code> method checks only incoming stanzas so it doesn't check for presence-out at all.
	 */
	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
							  Queue<Packet> results, Map<String, Object> settings) {
		if (packet.isXMLNSStaticStr(Iq.IQ_QUERY_PATH, XMLNS)) {
			return false;
		}    // end of if (session == null)

		boolean sendError = session != null && session.isAuthorized();

		boolean allowed = allowed(packet, session);
		if (!allowed && sendError) {
			Packet error = prepareError(packet, session);
			if (error != null) {
				results.offer(error);
			}
		}
		return !allowed;
	}

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session, final NonAuthUserRepository repo,
						final Queue<Packet> results, final Map<String, Object> settings) throws XMPPException {
		if (session == null) {
			return;
		}    // end of if (session == null)
		if (session.isAnonymous()) {
			return;
		}
		try {
			StanzaType type = packet.getType();

			if (type == StanzaType.get) {
				processGetRequest(packet, session, results);

			} else if (type == StanzaType.set) {
				processSetRequest(packet, session, results);

			} else if (type != StanzaType.result && type != StanzaType.error) {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Request type is incorrect", false));
			}
		} catch (NotAuthorizedException e) {
			log.log(Level.FINEST, "Received privacy request but user session is not authorized yet: {0}", packet);
			results.offer(
					Authorization.NOT_AUTHORIZED.getResponseMessage(packet, "You must authorize session first.", true));
		} catch (TigaseDBException e) {
			log.log(Level.WARNING, "Database problem, please contact admin: {0}", e);
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
																				 "Database access problem, please contact administrator.",
																				 true));
		}
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	protected boolean allowed(Packet packet, JID connId, BareJID userJid, PrivacyList privacyList) {
		if (allowedByDefault(packet, connId, userJid)) {
			return true;
		}

		if (privacyList != null) {
			JID jid = packet.getStanzaFrom();
			boolean packetIn = true;

			if ((jid == null) || userJid.equals(jid.getBareJID())) {
				jid = packet.getStanzaTo();
				packetIn = false;
			}

			PrivacyList.Item.Type type = null;
			if (packetIn) {
				switch (packet.getElemName()) {
					case Presence.ELEM_NAME:
						type = PrivacyList.Item.Type.presenceIn;
						break;
					case Message.ELEM_NAME:
						type = PrivacyList.Item.Type.message;
						break;
					case Iq.ELEM_NAME:
						type = PrivacyList.Item.Type.iq;
						break;
					default:
						break;
				}
			} else {
				if (packet.getElemName() == Presence.ELEM_NAME) {
					type = PrivacyList.Item.Type.presenceOut;
				}
			}

			if (type != null) {
				return privacyList.isAllowed(jid, type);
			}
		}

		return true;
	}

	protected boolean allowed(Packet packet, XMPPResourceConnection session) {
		if (session != null && session.isAuthorized()) {
			try {
				PrivacyList list = Privacy.getActiveList(session);
				if (list == null) {
					list = Privacy.getDefaultList(session);
				}
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Using privcy list: {0}", list);
				}
				if (list != null) {
					return allowed(packet, session.getConnectionId(), session.getBareJID(), list);
				}
			} catch (NoConnectionIdException e) {

				// Always allow, this is server dummy session
			} catch (NotAuthorizedException e) {
//    results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
//        "You must authorize session first.", true));
			} catch (TigaseDBException e) {
				log.log(Level.WARNING, "Database problem, please notify the admin. {0}", e);
			}
		} else if (cache != null) {
			JID to = packet.getStanzaTo();
			if (to != null) {
				PrivacyList list = cache.getPrivacyList(to.getBareJID());
				if (list != null) {
					return allowed(packet, null, to.getBareJID(), list);
				}
			}
		}
		return true;
	}

	protected boolean allowedByDefault(Packet packet, JID connId, BareJID userJid) {
		// If this is a preprocessing phase, always allow all packets to
		// make it possible for the client to communicate with the server.
		if (connId != null && connId.equals(packet.getPacketFrom())) {
			return true;
		}

		// allow packets without from attribute and packets with from attribute same as domain name
		if ((packet.getStanzaFrom() == null) || ((packet.getStanzaFrom().getLocalpart() == null) &&
				userJid.getDomain().equals(packet.getStanzaFrom().getDomain()))) {
			return true;
		}

		// allow packets without to attribute and packets with to attribute same as domain name
		if ((packet.getStanzaTo() == null) || ((packet.getStanzaTo().getLocalpart() == null) &&
				userJid.getDomain().equals(packet.getStanzaTo().getDomain()))) {
			return true;
		}

		// Always allow packets sent between sessions of same user and
		// packets sent from user to his bare jid and results of this
		// packets
		if (packet.getStanzaFrom() != null && packet.getStanzaTo() != null &&
				packet.getStanzaFrom().getBareJID().equals(packet.getStanzaTo().getBareJID())) {
			return true;
		}

		if (packet.getType() == StanzaType.error && packet.getStanzaTo() != null &&
				userJid.equals(packet.getStanzaTo().getBareJID())) {
			// this may be error sent back to sender of blocked request
			// or even to user who sent packet to blocked user
			Element error = packet.getElement().findChild(e -> e.getName() == ERROR_EL_NAME);
			if (error != null &&
					error.findChild(e -> e.getName() == Authorization.NOT_ACCEPTABLE.getCondition()) != null &&
					error.findChild(
							e -> e.getName() == BLOCKED_ELEM.getName() && e.getXMLNS() == BLOCKED_ELEM.getXMLNS()) !=
							null) {
				return true;
			}
		}

		return false;
	}

	protected Packet prepareError(Packet packet, XMPPResourceConnection session) {
		if (packet.getType() != StanzaType.error) {
			try {
				Packet error = null;
				switch (packet.getElemName()) {
					case "presence":
						break;
					case "message":
						if (packet.getStanzaFrom() == null || session.isUserId(packet.getStanzaFrom().getBareJID())) {
							// we need this here to make sure that this error will be sent to user only once
							// as there might be many outgoing messages as result of sending message to single
							// user - ie. messages sent to message archive, etc.
							if (packet.getPacketTo() != null) {
								return null;
							}

							error = Authorization.NOT_ACCEPTABLE.getResponseMessage(packet, null, true);
							error.getElement().getChild("error").addChild(BLOCKED_ELEM.clone());
						} else {
							error = Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, null, true);
						}
						return error;
					case "iq":
						if (packet.getType() == StanzaType.get || packet.getType() == StanzaType.set) {
							if (packet.getStanzaFrom() == null ||
									session.isUserId(packet.getStanzaFrom().getBareJID())) {
								error = Authorization.NOT_ACCEPTABLE.getResponseMessage(packet, null, true);
								error.getElement().getChild("error").addChild(BLOCKED_ELEM.clone());
							} else {
								error = Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, null, true);
							}
						}
						return error;
				}
			} catch (NotAuthorizedException ex) {
				log.log(Level.FINEST,
						"Packet droped due to privacy list rules. Error could not be generated properly " +
								"as session is not authorized yet, should not happen.", ex);
			} catch (PacketErrorTypeException ex) {
				log.log(Level.FINER, "Packet dropped due to privacy list rules. Packet is error type already: {0}",
						packet.toStringSecure());
			}
		}
		return null;
	}

	protected void processGetRequest(final Packet packet, final XMPPResourceConnection session,
									 final Queue<Packet> results)
			throws NotAuthorizedException, XMPPException, TigaseDBException {
		List<Element> children = packet.getElemChildrenStaticStr(Iq.IQ_QUERY_PATH);

		if ((children == null) || (children.size() == 0)) {
			String[] lists = Privacy.getLists(session);

			if (lists != null) {
				StringBuilder sblists = new StringBuilder(100);

				for (String list : lists) {
					sblists.append("<list name=\"").append(list).append("\"/>");
				}

				String list = Privacy.getDefaultListName(session);

				if (list != null) {
					sblists.append("<default name=\"").append(list).append("\"/>");
				}    // end of if (defList != null)
				list = Privacy.getActiveListName(session);
				if (list != null) {
					sblists.append("<active name=\"").append(list).append("\"/>");
				}    // end of if (defList != null)
				results.offer(packet.okResult(sblists.toString(), 1));
			} else {
				results.offer(packet.okResult((String) null, 1));
			}      // end of if (buddies != null) else
		} else {
			if (children.size() > 1) {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
																		   "You can retrieve only one list at a time.",
																		   true));
			} else {
				Element eList = Privacy.getList(session, children.get(0).getAttributeStaticStr("name"));

				if (eList != null) {
					results.offer(packet.okResult(eList, 1));
				} else {
					results.offer(
							Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "Requested list not found.", true));
				}    // end of if (eList != null) else
			}      // end of else
		}        // end of else
	}

	protected void processSetRequest(final Packet packet, final XMPPResourceConnection session,
									 final Queue<Packet> results)
			throws NotAuthorizedException, XMPPException, TigaseDBException {
		List<Element> children = packet.getElemChildrenStaticStr(Iq.IQ_QUERY_PATH);

		if ((children != null) && (children.size() == 1)) {
			Element child = children.get(0);

			if (child.getName() == LIST_EL_NAME) {

				// Broken privacy implementation sends list without name set
				// instead of sending BAD_REQUEST error I can just assign
				// 'default' name here.
				String name = child.getAttributeStaticStr(NAME);

				if ((name == null) || (name.length() == 0)) {
					child.setAttribute(NAME, "default");
				}    // end of if (name == null || name.length() == 0)

				List<Element> items = child.getChildren();

				if ((items == null) || items.isEmpty()) {
					// if the list is in use then forbid changes
					boolean inUse = session.getCommonSessionData(PRIVACY_LIST_LOADED) != null &&
							session.getCommonSessionData(DEFAULT) != null;

					if (!inUse) {
						for (XMPPResourceConnection activeSession : session.getActiveSessions()) {
							if (activeSession.equals(session)) {
								// don't apply to the current session
								continue;
							}
							inUse |= name.equals(Privacy.getActiveListName(activeSession));
						}
					}
					if (inUse) {
						results.offer(Authorization.CONFLICT.getResponseMessage(packet,
																				"Can not modify list while being in use by other session",
																				true));
					} else {
						Privacy.removeList(session, child);
						results.offer(packet.okResult((String) null, 0));
					}
				} else {
					Authorization error = validateList(session, items);

					if (error == null) {
						Privacy.addList(session, child);

						// updating active list if it's name matches name of updated list
						for (XMPPResourceConnection activeSession : session.getActiveSessions()) {
							if (name.equals(Privacy.getActiveListName(activeSession))) {
								Privacy.setActiveList(activeSession, name);
							}
						}
						// update default list
						if (name.equals(Privacy.getDefaultListName(session))) {
							Privacy.setDefaultList(session, child);
						}
						results.offer(packet.okResult((String) null, 0));
					} else {
						results.offer(error.getResponseMessage(packet, null, true));
					}
				}
			}      // end of if (child.getName().equals("list))
			if (child.getName() == DEFAULT_EL_NAME) {

				// User selects a different default list
				String listName = child.getAttributeStaticStr(NAME);
				Element list = Privacy.getList(session, listName);

				if ((listName != null) && (list == null)) {
					results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
																				  "Selected list was not found on the server",
																				  true));
				} else {
					// This is either declining of default list use or setting a new default list
					Privacy.setDefaultList(session, list);
					results.offer(packet.okResult((String) null, 0));
				}
			}      // end of if (child.getName().equals("list))
			if (child.getName() == ACTIVE_EL_NAME) {

				// User selects a different active list
				String listName = child.getAttributeStaticStr(NAME);
				Element list = Privacy.getList(session, listName);

				if ((listName != null) && (list == null)) {
					results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
																				  "Selected list was not found on the server",
																				  true));
				} else {

					// This is either declining of active list use or setting a new active list
					Privacy.setActiveList(session, child.getAttributeStaticStr(NAME));
					results.offer(packet.okResult((String) null, 0));
				}
			}    // end of if (child.getName().equals("list))
		} else {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
																	   "Only 1 element is allowed in privacy set request.",
																	   true));
		}    // end of else
	}

	protected enum ITEM_ACTION {
		allow,
		deny
	}

	protected enum ITEM_SUBSCRIPTIONS {
		both,
		to,
		from,
		none
	}

	protected enum ITEM_TYPE {
		jid,
		group,
		subscription,
		all
	}

	public static class OfflineResourceConnection
			extends XMPPResourceConnection {

		/**
		 * Creates a new <code>XMPPResourceConnection</code> instance.
		 */
		public OfflineResourceConnection(JID connectionId, UserRepository rep, AuthRepository authRepo,
										 SessionManagerHandler loginHandler) {
			super(connectionId, rep, authRepo, loginHandler);
		}

		@Override
		public boolean isAuthorized() {
			return true;
		}
	}

	@Bean(name = "privacyListOfflineCache", parent = JabberIqPrivacy.class, active = false)
	public static class PrivacyListOfflineCache
			implements SessionManagerHandler, Initializable, UnregisterAware {

		@Inject
		private AuthRepository authRepository;
		@ConfigField(desc = "Cache size", alias = "size")
		private int cacheSize = 10000;
		private LRUConcurrentCache<BareJID, PrivacyList> cache = new LRUConcurrentCache<>(cacheSize);
		private JID compId = JID.jidInstanceNS("privacy-sessman", DNSResolverFactory.getInstance().getDefaultHost());
		@Inject
		private EventBus eventBus;
		private JID offlineConnectionId = JID.jidInstanceNS("offline-connection",
															DNSResolverFactory.getInstance().getDefaultHost());
		@Inject
		private UserRepository userRepository;

		public void clear() {
			cache.clear();
		}

		@Override
		public JID getComponentId() {
			return compId;
		}

		@Override
		public void handleLogin(BareJID userId, XMPPResourceConnection conn) {
		}

		@Override
		public void handleDomainChange(String domain, XMPPResourceConnection conn) {

		}

		@Override
		public void handleLogout(BareJID userId, XMPPResourceConnection conn) {

		}

		@Override
		public void handlePresenceSet(XMPPResourceConnection conn) {

		}

		@Override
		public void handleResourceBind(XMPPResourceConnection conn) {

		}

		@Override
		public boolean isLocalDomain(String domain, boolean includeComponents) {
			return false;
		}

		@Override
		public void initialize() {
			eventBus.registerAll(this);
		}

		@Override
		public void beforeUnregister() {
			eventBus.unregisterAll(this);
		}

		public void setCacheSize(int cacheSize) {
			this.cacheSize = cacheSize;
			if (cache.limit() != cacheSize) {
				cache = new LRUConcurrentCache<>(cacheSize);
			}
		}

		@HandleEvent
		protected void userConnected(UserConnectedEvent event) {
			cache.remove(event.getUserJid().getBareJID());
		}

		protected PrivacyList getPrivacyList(BareJID userJID) {
			if (!cache.containsKey(userJID)) {
				try {
					PrivacyList list = this.loadList(userJID);
					cache.put(userJID, list);
				} catch (NotAuthorizedException | TigaseDBException ex) {
					return null;
				}
			}
			return cache.get(userJID);
		}

		protected PrivacyList loadList(BareJID userJID) throws NotAuthorizedException, TigaseDBException {
			XMPPResourceConnection session = createXMPPResourceConnection(userJID);
			if (roster_util instanceof RosterFlat) {
				Element listEl = Privacy.getDefaultListElement(session);
				if (listEl == null) {
					return PrivacyList.ALLOW_ALL;
				}
				final Map<BareJID, RosterElement> roster = ((RosterFlat) roster_util).loadUserRoster(session);
				return PrivacyList.create(roster, listEl);
			} else {
				return Privacy.getDefaultList(session);
			}
		}

		private XMPPResourceConnection createXMPPResourceConnection(BareJID userJid) {
			try {
				XMPPResourceConnection session = new OfflineResourceConnection(offlineConnectionId, userRepository,
																			   authRepository, this);
				VHostItem vhost = new VHostItem();
				vhost.setVHost(userJid.getDomain());
				session.setDomain(vhost);
				session.authorizeJID(userJid, false);
				XMPPSession parentSession = new XMPPSession(userJid.getLocalpart());
				session.setParentSession(parentSession);
				return session;
			} catch (TigaseStringprepException ex) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "creation of temporary session for offline user " + userJid + " failed", ex);
				}
				return null;
			}
		}

	}
}    // JabberIqPrivacy

