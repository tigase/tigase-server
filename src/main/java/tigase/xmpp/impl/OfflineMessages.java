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

import tigase.db.MsgRepositoryIfc;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.amp.db.MsgRepository;
import tigase.server.xmppsession.SessionManager;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.*;
import tigase.xmpp.jid.JID;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.server.Message.ELEM_NAME;

/**
 * OfflineMessages plugin implementation which follows <a href="http://xmpp.org/extensions/xep-0160.html">XEP-0160: Best
 * Practices for Handling Offline Messages</a> specification. Responsible for storing messages send to offline users -
 * either as a standalone plugin or as a processor for other plugins (e.g. AMP). Is registered to handle packets of type
 * {@code <presence>}.
 * <br>
 * Created: Mon Oct 16 13:28:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Bean(name = OfflineMessages.ID, parent = SessionManager.class, active = false)
public class OfflineMessages
		extends XMPPProcessor
		implements XMPPPostprocessorIfc, XMPPProcessorIfc {

	/**
	 * Field holds an array for element paths for which the plugin offers message saving capabilities. In case of {@code
	 * msgoffline} plugin it is <em>presence</em> stanza
	 */
	public static final String[] MESSAGE_EVENT_PATH = {ELEM_NAME, "event"};
	/**
	 * Field holds an array for element paths for which the plugin offers processing capabilities. In case of {@code
	 * msgoffline} plugin it is <em>presence</em> stanza
	 */
	public static final String[] MESSAGE_HEADER_PATH = {ELEM_NAME, "header"};
	public static final String[] MESSAGE_HINTS_NO_STORE = {ELEM_NAME, "no-store"};
	public static final String MESSAGE_HINTS_XMLNS = "urn:xmpp:hints";
	public static final String[] MESSAGE_RECEIVED_PATH = {ELEM_NAME, "received"};
	public static final String MESSAGE_RECEIVED_XMLNS = "urn:xmpp:receipts";
	public static final String[] PUBSUB_NODE_PATH = {Message.ELEM_NAME, "event", "items"};
	public static final String PUBSUB_NODE_KEY = "node";
	/**
	 * Field holds default client namespace for stanzas. In case of {@code msgoffline} plugin it is
	 * <em>jabber:client</em>
	 */
	protected static final String XMLNS = "jabber:client";
	/**
	 * Field holds identification string for the plugin. In case of {@code msgoffline} plugin it is <em>msgoffline</em>
	 */
	protected static final String ID = "msgoffline";
	private static final Logger log = Logger.getLogger(OfflineMessages.class.getName());
	/**
	 * Field holds an array for element paths for which the plugin offers processing capabilities. In case of {@code
	 * msgoffline} plugin it is <em>presence</em> stanza
	 */
	private static final String[][] ELEMENTS = {{PresenceAbstract.PRESENCE_ELEMENT_NAME}, {"iq", "msgoffline"}};
	/**
	 * Field holds an array of name-spaces for stanzas which can be processed by this plugin. In case of {@code
	 * msgoffline} plugin it is <em>jabber:client</em>
	 */
	private static final String[] XMLNSS = {XMLNS, ID};
	/**
	 * Field holds an array of XML Elements with service discovery features which have to be returned to the client
	 * uppon request. In case of {@code msgoffline} plugin it is the same as plugin name - <em>msgoffline</em>
	 */
	private static final Element[] DISCO_FEATURES = {
			new Element("feature", new String[]{"var"}, new String[]{"msgoffline"})};
	private static final String MSG_OFFLINE_STORAGE_PATHS = "msg-store-offline-paths";
	private static final String MSG_REPO_CLASS_KEY = "msg-repo-class";
	private static final String MSG_PUBSUB_JID = "msg-pubsub-jid";
	private static final String MSG_PUBSUB_NODE = "msg-pubsub-node";
	private static final String MSG_PUBSUB_PUBLISHER = "msg-pubsub-publisher";
	/** Field holds the default hostname of the machine. */
	private static String defHost = DNSResolverFactory.getInstance().getDefaultHost();

	/**
	 * Field holds class for formatting and parsing dates in a locale-sensitive manner
	 */
	private final SimpleDateFormat formatter;
	@Inject
	private MessageDeliveryLogic message;
	@Inject(nullAllowed = true)
	private SessionManager.MessageArchive messageArchive;
	@ConfigField(desc = "Offline message implementation repository class", alias = MSG_REPO_CLASS_KEY)
	private String msgRepoCls = null;
	@Inject(nullAllowed = true)
	private List<Notifier> notifiers;
	@ConfigField(desc = "Store offline messages with mathing paths", alias = MSG_OFFLINE_STORAGE_PATHS)
	private ElementMatcher[] offlineStorageMatchers = new ElementMatcher[0];

	{
		this.formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		this.formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public String id() {
		return ID;
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * OfflineMessages postprocessor simply calls {@code savePacketForOffLineUser} method to store packet to offline
	 * repository.
	 */
	@Override
	public void postProcess(final Packet packet, final XMPPResourceConnection conn, final NonAuthUserRepository repo,
							final Queue<Packet> queue, Map<String, Object> settings) {
		if (conn == null ||
				(packet.getElemName() == Message.ELEM_NAME && !message.hasConnectionForMessageDelivery(conn))) {
			try {
				if (packet.getElemName() == tigase.server.Message.ELEM_NAME && packet.getStanzaTo() != null &&
						packet.getStanzaTo().getResource() != null) {
					return;
				}

				if (conn != null && packet.getStanzaTo() != null && !conn.isUserId(packet.getStanzaTo().getBareJID())) {
					return;
				}
				OfflineMsgRepositoryIfc msg_repo = getMsgRepoImpl(repo, conn);

				Authorization saveResult = savePacketForOffLineUser(packet, msg_repo, repo);
				Packet result = null;
				notifyNewOfflineMessage(packet, conn, queue, settings);

				switch (saveResult) {
					case SERVICE_UNAVAILABLE:
						result = saveResult.getResponseMessage(packet, "Offline messages queue is full", true);
						break;
					default:
						break;
				}
				if (result != null) {
					queue.offer(result);
				}
			} catch (UserNotFoundException e) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("UserNotFoundException at trying to save packet for off-line user." + packet);
				}
			} catch (TigaseDBException ex) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Could not save packet for offline user, returning error " + packet, ex);
				}
				try {
					queue.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet, null, true));
				} catch (PacketErrorTypeException e) {
					log.finest("Could not sent error for unsaved packet for offline user " + packet);
				}
			} catch (NotAuthorizedException ex) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "NotAuthorizedException when checking if message is to this " +
							"user at trying to save packet for off-line user, {0}, {1}", new Object[]{packet, conn});
				}
			} catch (PacketErrorTypeException ex) {
				log.log(Level.FINE, "Could not sent error to packet sent to offline user which storage to offline " +
						"store failed. Packet is error type already: {0}", packet.toStringSecure());
			}    // end of try-catch
		}      // end of if (conn == null)
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * {@code OfflineMessages} processor is triggered by {@code <presence>} stanza. Upon receiving it plugin tries to
	 * load messages from repository and, if the result is not empty, sends them to the user
	 */
	@Override
	public void process(final Packet packet, final XMPPResourceConnection conn, final NonAuthUserRepository repo,
						final Queue<Packet> results, final Map<String, Object> settings) throws NotAuthorizedException {
		switch (packet.getElemName()) {
			case tigase.server.Presence.ELEM_NAME:
				if (loadOfflineMessages(packet, conn)) {
					try {
						OfflineMsgRepositoryIfc msg_repo = getMsgRepoImpl(repo, conn);
						Queue<Packet> packets = restorePacketForOffLineUser(conn, msg_repo);

						if (packets != null) {
							if (log.isLoggable(Level.FINER)) {
								log.finer("Sending off-line messages: " + packets.size());
							}
							results.addAll(packets);

							if (!packets.isEmpty()) {
								notifyOfflineMessagesRetrieved(conn, results);
							}
						}    // end of if (packets != null)
					} catch (TigaseDBException e) {
						log.log(Level.CONFIG, "Something wrong, DB problem, cannot load offline messages. " + e);
					}      // end of try-catch
				}
				break;
			case Iq.ELEM_NAME:
				processIq(packet, conn, repo, results);
				break;
		}
	}

	public void processIq(Packet packet, XMPPResourceConnection conn, NonAuthUserRepository repo, Queue<Packet> results)
			throws NotAuthorizedException {
		try {
			if (conn != null && packet.getFrom().equals(conn.getConnectionId())) {
				Element msgoffline = packet.getElement().getChild("msgoffline");
				String limitStr = null;
				switch (packet.getType()) {
					case set:
						limitStr = msgoffline.getAttributeStaticStr("limit");
						Long limit = null;
						if (limitStr != null) {
							if ("none".equals(limitStr) || "false".equals(limitStr)) {
								limit = (long) -1;
							} else {
								limit = Long.parseLong(limitStr);
							}
						}
						if (limit == null) {
							results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
																					   "Value of limit attribute is incorrect",
																					   false));
							break;
						} else {
							if (limit >= 0) {
								conn.setPublicData(MsgRepository.OFFLINE_MSGS_KEY, MsgRepository.MSGS_STORE_LIMIT_KEY,
												   limitStr);
							} else {
								conn.removePublicData(MsgRepository.OFFLINE_MSGS_KEY,
													  MsgRepository.MSGS_STORE_LIMIT_KEY);
							}
						}
						// following get case will return result for set as well
					case get:
						if (limitStr == null) {
							limitStr = conn.getPublicData(MsgRepository.OFFLINE_MSGS_KEY,
														  MsgRepository.MSGS_STORE_LIMIT_KEY, null);
						}
						if (limitStr == null) {
							limitStr = "false";
						}
						msgoffline = new Element("msgoffline", new String[]{"xmlns", "limit"},
												 new String[]{"msgoffline", limitStr});
						results.offer(packet.okResult(msgoffline, 0));
						break;
					default:
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Request type is incorrect",
																				   false));
						break;
				}
			} else {
				results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
																			  "You are not authorized to access this private storage.",
																			  false));
			}
		} catch (TigaseDBException ex) {
		} catch (NoConnectionIdException ex) {
		} catch (PacketErrorTypeException ex) {

		}
	}

	/**
	 * Method restores all messages from repository for the JID of the current session. All retrieved elements are then
	 * instantiated as {@code Packet} objects added to {@code LinkedList} collection and, if possible, sorted by
	 * timestamp.
	 *
	 * @param conn user session which keeps all the user session data and also gives an access to the user's repository
	 * data.
	 * @param repo an implementation of {@link MsgRepositoryIfc} interface
	 *
	 * @return a {@link Queue} of {@link Packet} objects based on all stored payloads for the JID of the current
	 * session.
	 *
	 */
	public Queue<Packet> restorePacketForOffLineUser(XMPPResourceConnection conn,
													 tigase.db.OfflineMsgRepositoryIfc repo)
			throws UserNotFoundException, NotAuthorizedException, TigaseDBException {
		Queue<Element> elems = repo.loadMessagesToJID(conn, true);

		if (elems != null) {
			LinkedList<Packet> pacs = new LinkedList<Packet>();
			Element elem = null;

			while ((elem = elems.poll()) != null) {
				try {
					Packet p = Packet.packetInstance(elem);
					if (p.getElemName() == Iq.ELEM_NAME) {
						p.initVars(p.getStanzaFrom(), conn.getJID());
					}
					pacs.offer(p);
				} catch (TigaseStringprepException ex) {
					log.warning("Packet addressing problem, stringprep failed: " + elem);
				}
			}    // end of while (elem = elems.poll() != null)
			try {
				Collections.sort(pacs, new StampComparator());
			} catch (NullPointerException e) {
				try {
					log.warning("Can not sort off line messages: " + pacs + ",\n" + e);
				} catch (Exception exc) {
					log.log(Level.WARNING, "Can not print log message.", exc);
				}
			}

			return pacs;
		}

		return null;
	}

	/**
	 * Method stores messages to offline repository with the following rules applied, i.e. saves only: <ul> <li> message
	 * stanza with either nonempty {@code <body>}, {@code <event>} or {@code <header>} child element and only messages
	 * of type normal, chat.</li> <li> presence stanza of type subscribe, subscribed, unsubscribe and unsubscribed.</li>
	 * </ul> <br> Processed messages are stamped with the {@code delay} element and appropriate timestamp. <br>
	 *
	 * @param packet a {@link Packet} object containing packet that should be verified and saved
	 * @param repo a {@link MsgRepositoryIfc} repository handler responsible for storing messages
	 *
	 * @return {@code true} if the packet was correctly saved to repository, {@code false} otherwise.
	 *
	 */
	public Authorization savePacketForOffLineUser(Packet packet, tigase.db.OfflineMsgRepositoryIfc repo,
												  NonAuthUserRepository userRepo) throws UserNotFoundException, TigaseDBException {
		StanzaType type = packet.getType();

		// save only:
		// message stanza with either {@code <body>} or {@code <event>} child element and only of type normal, chat
		// presence stanza of type subscribe, subscribed, unsubscribe and unsubscribed
		if (isAllowedForOfflineStorage(packet)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Storing packet for offline user: {0}", packet);
			}

			Packet pac = packet.copyElementOnly();
			pac.setStableId(packet.getStableId());
			if (messageArchive != null) {
				messageArchive.addStableId(pac, null);
			}

			C2SDeliveryErrorProcessor.filterErrorElement(pac.getElement());

			String stamp = null;
			synchronized (formatter) {
				stamp = formatter.format(new Date());
			}


			String from = pac.getStanzaTo().getDomain();
			Element x = new Element("delay", "Offline Storage - " + defHost, new String[]{"from", "stamp", "xmlns"},
									new String[]{from, stamp, "urn:xmpp:delay"});

			pac.getElement().addChild(x);
			pac.processedBy(ID);


			if (repo.storeMessage(pac.getStanzaFrom(), pac.getStanzaTo(), null, pac.getElement(), userRepo)) {
				return Authorization.AUTHORIZED;
			} else {
				return Authorization.SERVICE_UNAVAILABLE;
			}
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Packet for offline user not suitable for storing: {0}", packet);
			}
		}

		return Authorization.FEATURE_NOT_IMPLEMENTED;
	}

	public String[] getOfflineStorageMatchers() {
		String[] result = new String[offlineStorageMatchers.length];
		for (int i = 0; i < offlineStorageMatchers.length; i++) {
			result[i] = offlineStorageMatchers[i].toString();
		}
		return result;
	}

	public void setOfflineStorageMatchers(String[] matcherStrs) {
		List<ElementMatcher> matchers = new ArrayList<>();
		for (String matcherStr : matcherStrs) {
			ElementMatcher matcher = ElementMatcher.create(matcherStr);
			if (matcher != null) {
				matchers.add(matcher);
			}
		}
		offlineStorageMatchers = matchers.toArray(new ElementMatcher[0]);
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

	/**
	 * Method allows obtaining instance of {@link MsgRepositoryIfc} interface implementation.
	 *
	 * @param conn user session which keeps all the user session data and also gives an access to the user's repository
	 * data.
	 * @param repo an implementation of {@link MsgRepositoryIfc} interface
	 *
	 * @return instance of {@link MsgRepositoryIfc} interface implementation.
	 */
	protected OfflineMsgRepositoryIfc getMsgRepoImpl(NonAuthUserRepository repo, XMPPResourceConnection conn) {
		if (msgRepoCls == null) {
			return new MsgRepositoryImpl(repo, conn);
		} else {
			try {
				OfflineMsgRepositoryIfc msgRepo = (OfflineMsgRepositoryIfc) ModulesManagerImpl.getInstance()
						.forName(msgRepoCls)
						.newInstance();
				msgRepo.init(repo, conn);
				return msgRepo;
			} catch (Exception ex) {
				return null;
			}
		}
	}

	/**
	 * Method determines whether packet sent to offline user should be stored in offline storage or not
	 */
	protected boolean isAllowedForOfflineStorage(Packet pac) {
		// custom element matchers override default values so let's check
		// this matchers at first
		for (ElementMatcher matcher : offlineStorageMatchers) {
			if (matcher.matches(pac)) {
				return matcher.getValue();
			}
		}

		return isAllowedForOfflineStorageDefaults(pac);
	}

	protected boolean isAllowedForOfflineStorageDefaults(Packet pac) {
		StanzaType type = pac.getType();
		switch (pac.getElemName()) {
			case "message":
				if (type == null || type == StanzaType.normal || type == StanzaType.chat) {
					// support for XEP-0334 Message Processing Hints					
					if (pac.getAttributeStaticStr(MESSAGE_HINTS_NO_STORE, "xmlns") == MESSAGE_HINTS_XMLNS) {
						return false;
					}
					if (pac.getElemCDataStaticStr(tigase.server.Message.MESSAGE_BODY_PATH) != null) {
						return true;
					}
					if (pac.getElemChildrenStaticStr(MESSAGE_EVENT_PATH) != null) {
						return true;
					}
					if (pac.getElemChildrenStaticStr(MESSAGE_HEADER_PATH) != null) {
						return true;
					}
					if (pac.getElement().getXMLNSStaticStr(MESSAGE_RECEIVED_PATH) == MESSAGE_RECEIVED_XMLNS) {
						return true;
					}
				}
				break;
			case "presence":
				if ((type == StanzaType.subscribe) || (type == StanzaType.subscribed) ||
						(type == StanzaType.unsubscribe) || (type == StanzaType.unsubscribed)) {
					return true;
				}
				break;
			default:
				break;
		}

		return false;
	}

	/**
	 * Method determines whether offline messages should be loaded - the process should be run only once per user
	 * session and only for available/null presence with priority greater than 0.
	 *
	 * @param packet a {@link Packet} object containing packet that should be verified and saved
	 * @param conn user session which keeps all the user session data and also gives an access to the user's repository
	 * data.
	 *
	 * @return {@code true} if the messages should be loaded, {@code false} otherwise.
	 */
	protected boolean loadOfflineMessages(Packet packet, XMPPResourceConnection conn) {

		// If the user session is null or the user is anonymous just
		// ignore it.
		if ((conn == null) || conn.isAnonymous()) {
			return false;
		}    // end of if (session == null)

		// Try to restore the offline messages only once for the user session
		if (conn.getSessionData(ID) != null) {
			return false;
		}

		// make sure this is broadcast presence as only in this case we should sent offline messages
		if (packet.getStanzaTo() != null) {
			return false;
		}

		// if we are using XEP-0013: Flexible offline messages retrieval then we skip loading
		if (conn.getCommonSessionData(FlexibleOfflineMessageRetrieval.FLEXIBLE_OFFLINE_XMLNS) != null) {
			return false;
		}

		StanzaType type = packet.getType();

		if ((type == null) || (type == StanzaType.available)) {

			// Should we send off-line messages now?
			// Let's try to do it here and maybe later I find better place.
			String priority_str = packet.getElemCDataStaticStr(tigase.server.Presence.PRESENCE_PRIORITY_PATH);
			int priority = 0;

			if (priority_str != null) {
				try {
					priority = Integer.decode(priority_str);
				} catch (NumberFormatException e) {
					priority = 0;
				}    // end of try-catch
			}      // end of if (priority != null)
			if (priority >= 0) {
				if (conn.getPresence() == null) {
					conn.setPriority(priority);
				}
				conn.putSessionData(ID, ID);

				return true;
			}      // end of if (priority >= 0)
		}        // end of if (type == null || type == StanzaType.available)

		return false;
	}

	protected void notifyNewOfflineMessage(Packet packet, XMPPResourceConnection conn, Queue<Packet> queue,
										   Map<String, Object> settings) {
		if (notifiers != null) {
			notifiers.forEach(notifier -> notifier.notifyNewOfflineMessage(packet, conn, queue, settings));
		}
	}

	protected void notifyOfflineMessagesRetrieved(XMPPResourceConnection conn, Queue<Packet> queue) {
		if (notifiers != null) {
			notifiers.forEach(notifier -> notifier.notifyOfflineMessagesRetrieved(conn, queue));
		}
	}

	public interface Notifier {

		void notifyNewOfflineMessage(Packet packet, XMPPResourceConnection conn, Queue<Packet> queue,
									 Map<String, Object> settings);

		default void notifyOfflineMessagesRetrieved(XMPPResourceConnection session, Queue<Packet> results) {
			// default implementation does nothing...
		}

	}

	public static interface OfflineMsgRepositoryIfc
			extends tigase.db.OfflineMsgRepositoryIfc {

		void init(NonAuthUserRepository repo, XMPPResourceConnection conn);

	}

	@Bean(name = "msg-offline-pubsub-publisher-notifier", parent = SessionManager.class, active = false, exportable = true)
	public static class PubSubPublisherNotifier
			implements Notifier {

		@ConfigField(desc = "PubSub offline message publisher", alias = MSG_PUBSUB_PUBLISHER)
		private String defaultPublisher;
		@ConfigField(desc = "PubSub component JID", alias = MSG_PUBSUB_JID)
		private String pubSubJID;
		@ConfigField(desc = "PubSub node for offline messages", alias = MSG_PUBSUB_NODE)
		private String pubSubNode;

		public void notifyNewOfflineMessage(final Packet packet, final XMPPResourceConnection conn,
											final Queue<Packet> queue, Map<String, Object> settings) {
			if (pubSubJID == null || pubSubNode == null) {
				return;
			}
			final StanzaType type = packet.getType();
			if ((packet.getElemName().equals("message") &&
					((packet.getElemCDataStaticStr(tigase.server.Message.MESSAGE_BODY_PATH) != null) ||
							(packet.getElemChildrenStaticStr(MESSAGE_EVENT_PATH) != null) ||
							(packet.getElemChildrenStaticStr(MESSAGE_HEADER_PATH) != null)) &&
					((type == null) || (type == StanzaType.normal) || (type == StanzaType.chat))) ||
					(packet.getElemName().equals("presence") &&
							((type == StanzaType.subscribe) || (type == StanzaType.subscribed) ||
									(type == StanzaType.unsubscribe) || (type == StanzaType.unsubscribed)))) {
				String tmpNode = packet.getElement().getAttributeStaticStr(PUBSUB_NODE_PATH, PUBSUB_NODE_KEY);
				if (tmpNode != null && tmpNode.equals(this.pubSubNode)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Publishing skipped to prevent loops: {0}", packet);
					}
					return;
				}

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Publishing packet in pubsub: {0}", packet);
				}
				try {
					if (defaultPublisher != null) {
						Element iq = new Element("iq", new String[]{"type", "id", "to", "from"},
												 new String[]{"set", "" + System.nanoTime(), pubSubJID,
															  defaultPublisher});
						Element pubsub = new Element("pubsub", new String[]{"xmlns"},
													 new String[]{"http://jabber.org/protocol/pubsub"});
						iq.addChild(pubsub);
						Element publish = new Element("publish", new String[]{"node"}, new String[]{this.pubSubNode});
						pubsub.addChild(publish);
						Element item = new Element("item");
						publish.addChild(item);

						item.addChild(packet.getElement());

						Packet out = Packet.packetInstance(iq);
						out.setXMLNS(Packet.CLIENT_XMLNS);
						queue.add(out);
					} else if (log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING,
								"Cannot publish message in PubSub, because cannot determine publisher. Please define default publisher JID.",
								packet);
					}
				} catch (Exception e) {
					log.log(Level.WARNING, "Problem during publish packet in pubsub", e);
				}
			}
		}

	}

	/**
	 * {@link Comparator} interface implementation for the purpose of sorting Elements retrieved from the repository by
	 * the timestamp stored in {@code delay} element.
	 */
	public static class StampComparator
			implements Comparator<Packet> {

		@Override
		public int compare(Packet p1, Packet p2) {
			// Try XEP-0203 - the new XEP...
			Element stamp_el1 = p1.getElement().getChild("delay", "urn:xmpp:delay");
			Element stamp_el2 = p2.getElement().getChild("delay", "urn:xmpp:delay");
			boolean isStamp1New = (stamp_el1 != null);
			boolean isStamp2New = (stamp_el2 != null);

			// if both entries are stamped with XEP-0203 then just compare stamps
			if (isStamp1New && isStamp2New) {
				String stamp1 = stamp_el1.getAttributeStaticStr("stamp");
				String stamp2 = stamp_el2.getAttributeStaticStr("stamp");
				return stamp1.compareTo(stamp2);
			}

			// retrieve XEP-0091 if there is no XEP-0203 stamp
			if (!isStamp1New) {
				// XEP-0091 support - the old one...
				stamp_el1 = p1.getElement().getChild("x", "jabber:x:delay");
			}
			if (!isStamp2New) {
				// XEP-0091 support - the old one...
				stamp_el2 = p2.getElement().getChild("x", "jabber:x:delay");
			}

			// retrive stamps
			String stamp1 = stamp_el1 == null ? "" : stamp_el1.getAttributeStaticStr("stamp");
			String stamp2 = stamp_el2 == null ? "" : stamp_el2.getAttributeStaticStr("stamp");

			// convert XEP-0203 stamp to XEP-0091 stamp, simple removal of '-' should work
			if (isStamp1New) {
				stamp1 = stamp1.replace("-", "");
			} else if (isStamp2New) {
				stamp2 = stamp2.replace("-", "");
			}

			return stamp1.compareTo(stamp2);
		}
	}

	/**
	 * Implementation of {@code MsgRepositoryIfc} interface providing basic support for storing and loading of Elements
	 * from repository.
	 */
	private class MsgRepositoryImpl
			implements OfflineMsgRepositoryIfc {

		/** Field holds user session which keeps all the user session data and also
		 * gives an access to the user's repository data. */
		//private XMPPResourceConnection conn = null;
		/**
		 * Field holds a reference to user session which keeps all the user session data and also gives an access to the
		 * user's repository data.
		 */
		private SimpleParser parser = SingletonFactory.getParserInstance();
		/**
		 * Field holds reference to an implementation of {@link MsgRepositoryIfc} interface
		 */
		private NonAuthUserRepository repo = null;

		/**
		 * Constructs {@code MsgRepositoryImpl} object referencing user session and having handle to user repository.
		 *
		 * @param repo an implementation of {@link MsgRepositoryIfc} interface
		 * @param conn user session which keeps all the user session data and also gives an access to the user's
		 * repository data.
		 */
		private MsgRepositoryImpl(NonAuthUserRepository repo, XMPPResourceConnection conn) {
			init(repo, conn);
		}

		@Override
		public void init(NonAuthUserRepository repo, XMPPResourceConnection conn) {
			this.repo = repo;
			//this.conn = conn;
		}

		@Override
		@Deprecated
		public void initRepository(String conn_str, Map<String, String> map) {
			// nothing to do here as we base on UserRepository which is already initialized
		}

		@Override
		public Element getMessageExpired(long time, boolean delete) {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public Queue<Element> loadMessagesToJID(XMPPResourceConnection session, boolean delete)
				throws UserNotFoundException {
			try {
				DomBuilderHandler domHandler = new DomBuilderHandler();
				String[] msgs = session.getOfflineDataList(ID, "messages");

				if ((msgs != null) && (msgs.length > 0)) {
					session.removeOfflineData(ID, "messages");

					StringBuilder sb = new StringBuilder();

					for (String msg : msgs) {
						sb.append(msg);
					}

					char[] data = sb.toString().toCharArray();

					parser.parse(domHandler, data, 0, data.length);

					return domHandler.getParsedElements();
				}    // end of while (elem = elems.poll() != null)
			} catch (NotAuthorizedException ex) {
				log.log(Level.CONFIG, "User not authrized to retrieve offline messages, " +
								 "this happens quite often on some installations where there" +
								 " are a very short living client connections. They can " + "disconnect at any time. " +
								 ex);
			} catch (TigaseDBException ex) {
				log.warning("Error accessing database for offline message: " + ex);
			}

			return null;
		}

		@Override
		public boolean storeMessage(JID from, JID to, Date expired, Element msg, NonAuthUserRepository userRepo)
				throws UserNotFoundException {
			repo.addOfflineDataList(to.getBareJID(), ID, "messages", new String[]{msg.toString()});
			return true;
		}
	}

}    // OfflineMessages
