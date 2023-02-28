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
import tigase.db.UserNotFoundException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.util.dns.DNSResolverFactory;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.jid.JID;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.server.amp.AmpFeatureIfc.*;

/**
 * Created: Apr 29, 2010 5:00:25 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
@Bean(name = MessageAmp.ID, parent = SessionManager.class, active = true, exportable = true)
public class MessageAmp
		extends XMPPProcessor
		implements XMPPPacketFilterIfc, XMPPPostprocessorIfc, XMPPPreprocessorIfc, XMPPProcessorIfc, RegistrarBean {

	protected static final String ID = "amp";
	private static final String AMP_JID_PROP_KEY = "amp-jid";
	private static final String STATUS_ATTRIBUTE_NAME = "status";
	private static final String[][] ELEMENTS = {{"message"}, {"presence"}, {"iq", "msgoffline"}, {"iq", "fin"}, {"iq", "fin"}};
	private static final Logger log = Logger.getLogger(MessageAmp.class.getName());
	private static final String XMLNS = "http://jabber.org/protocol/amp";
	private static final String[] XMLNSS = {"jabber:client", "jabber:client", "msgoffline", "urn:xmpp:mam:2", "urn:xmpp:mam:1"};
	private static final Element[] DISCO_FEATURES_WITH_OFFLINE = {
			new Element("feature", new String[]{"var"}, new String[]{XMLNS}),
			new Element("feature", new String[]{"var"}, new String[]{"msgoffline"})};
	private static final Element[] DISCO_FEATURES_WITHOUT_OFFLINE = new Element[]{
			new Element("feature", new String[]{"var"}, new String[]{XMLNS})};
	private static String defHost = DNSResolverFactory.getInstance().getDefaultHost();

//	private static final String STATUS_ATTRIBUTE_NAME = "status";

	@ConfigField(desc = "AMP component JID", alias = AMP_JID_PROP_KEY)
	private JID ampJID = JID.jidInstanceNS("amp@" + defHost);
	@Inject
	private MessageDeliveryLogic messageProcessor;
	@Inject
	private MsgRepositoryIfc msg_repo = null;
	@Inject(nullAllowed = true)
	private OfflineMessages offlineProcessor;
	@ConfigField(desc = "", alias = "quota-exceeded")
	private QuotaRule quotaExceededRule = QuotaRule.error;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
					   Queue<Packet> results) {
		C2SDeliveryErrorProcessor.filter(packet, session, repo, results, ampJID);
	}

	@Override
	public void postProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
							Queue<Packet> results, Map<String, Object> settings) {
		if ((offlineProcessor != null) && (session == null || (packet.getElemName() == Message.ELEM_NAME &&
				!messageProcessor.hasConnectionForMessageDelivery(session)))) {
			if (packet.getElemName() == tigase.server.Message.ELEM_NAME && packet.getStanzaTo() != null &&
					packet.getStanzaTo().getResource() != null) {
				return;
			}

			Element amp = packet.getElement().getChild("amp");

			if ((amp == null) || (amp.getXMLNS() != XMLNS)
//					 "Individual action definitions MAY provide their own requirements." regarding
//						"status" attribute requirement!!! applies to "alert" and "notify"
//					|| (amp.getAttributeStaticStr(STATUS_ATTRIBUTE_NAME) != null)
					) {
				try {
					if (session != null && packet.getStanzaTo() != null &&
							(packet.getStanzaTo().getLocalpart() == null ||
									!session.isUserId(packet.getStanzaTo().getBareJID()))) {
						return;
					}

					Authorization saveResult = offlineProcessor.savePacketForOffLineUser(packet, msg_repo, repo);
					Packet result = null;

					offlineProcessor.notifyNewOfflineMessage(packet, session, results, settings);

					switch (saveResult) {
						case SERVICE_UNAVAILABLE:
							switch (quotaExceededRule) {
								case error:
									result = saveResult.getResponseMessage(packet, "Offline messages queue is full",
																		   true);
									break;
								case drop:
									break;
							}
							break;
						default:
							break;
					}
					if (result != null) {
						results.offer(result);
					}
				} catch (UserNotFoundException ex) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("UserNotFoundException at trying to save packet for off-line user." + packet);
					}
				} catch (NotAuthorizedException ex) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "NotAuthorizedException when checking if message is to this " +
										"user at trying to save packet for off-line user, {0}, {1}",
								new Object[]{packet, session});
					}
				} catch (PacketErrorTypeException ex) {
					log.log(Level.FINE,
							"Could not sent error to packet sent to offline user which storage to offline " +
									"store failed. Packet is error type already: {0}", packet.toStringSecure());
				}
			}
		}
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
							  Queue<Packet> results, Map<String, Object> settings) {
		boolean processed = C2SDeliveryErrorProcessor.preProcess(packet, session, repo, results, settings,
																 messageProcessor);
		if (messageProcessor.preProcessFilter(packet, session)) {
			return true;
		}
		if (processed && packet.getPacketFrom() != null &&
				packet.getPacketFrom().getLocalpart().equals(ampJID.getLocalpart())) {
			processed = false;
		}
		if (processed) {
			packet.processedBy(ID);
		} else if (packet.getElemName() == Message.ELEM_NAME) {
			Element amp = packet.getElement().getChild("amp", XMLNS);
			if (amp == null
//					 "Individual action definitions MAY provide their own requirements." regarding
//						"status" attribute requirement!!! applies to "alert" and "notify"
					|| (amp.getAttributeStaticStr(STATUS_ATTRIBUTE_NAME) != null) ||
					ampJID.equals(packet.getPacketFrom())) {
				return false;
			}

			try {
				if (session == null) {
					Packet result = packet.copyElementOnly();
					result.setPacketTo(ampJID);
					result.setStableId(packet.getStableId());
					results.offer(result);
					result.getElement().addAttribute(OFFLINE, "1");
					packet.processedBy(ID);

					return true;
				}
				if (session.isUserId(packet.getStanzaTo().getBareJID()) && session.getjid() != null &&
						session.getjid().equals(packet.getStanzaTo())) {
					Packet result = packet.copyElementOnly();
					result.setStableId(packet.getStableId());
					result.setPacketTo(ampJID);
					if (packet.getStanzaTo().getResource() != null) {
						result.getElement().addAttribute(TO_RES, session.getResource());
					}
					results.offer(result);
					boolean offline = !messageProcessor.hasConnectionForMessageDelivery(session);
					if (offline) {
						result.getElement().addAttribute(OFFLINE, "1");
					}
					packet.processedBy(ID);
					return true;
//				} else {
					// this needs to be handled in process() method so we need to allow packet 
					// to be processed in this method
//					JID connectionId = session.getConnectionId();
//
//					if (connectionId.equals(packet.getPacketFrom())) {
//						result.getElement().addAttribute(FROM_CONN_ID, connectionId.toString());
//					}
				}
				return false;
			} catch (XMPPException ex) {
				log.log(Level.SEVERE, "this should not happen", ex);
			}
		}
		return processed;
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
		switch (packet.getElemName()) {
			case "presence":
				if ((offlineProcessor != null) && offlineProcessor.loadOfflineMessages(packet, session)) {
					try {
						Queue<Packet> packets = offlineProcessor.restorePacketForOffLineUser(session, msg_repo);

						if (packets != null) {
							if (log.isLoggable(Level.FINER)) {
								log.finer("Sending off-line messages: " + packets.size());
							}
							results.addAll(packets);

							if (!packets.isEmpty()) {
								offlineProcessor.notifyOfflineMessagesRetrieved(session, results);
							}
						}    // end of if (packets != null)
					} catch (UserNotFoundException e) {
						log.log(Level.CONFIG, "Something wrong, DB problem, cannot load offline messages. " + e);
					}      // end of try-catch

					// notify AMP component that user is online now
					if (packet.getStanzaTo() == null) {
						Packet notification = packet.copyElementOnly();
						notification.initVars(session.getJID(), ampJID);
						results.offer(notification);
					}
				}
				break;
			case "message":
				Element amp = packet.getElement().getChild("amp", XMLNS);

				if ((amp == null)
//					 "Individual action definitions MAY provide their own requirements." regarding
//						"status" attribute requirement!!! applies to "alert" and "notify"
						|| (amp.getAttributeStaticStr(STATUS_ATTRIBUTE_NAME) != null) ||
						(packet.getPacketFrom() != null &&
								ampJID.getLocalpart().equals(packet.getPacketFrom().getLocalpart()))) {
					messageProcessor.handleDelivery(packet, session, repo, results, settings);
				} else {
					// when packet from user with AMP is sent we need to forward it to AMP
					// for processing but we need to do this here and not in preProcess method
					// as in other case other processors would not receive this packet at all!
					JID connectionId = session.getConnectionId();
					Packet result = packet.copyElementOnly();
					if (connectionId.equals(packet.getPacketFrom())) {
						//if (!session.isUserId(packet.getStanzaTo().getBareJID()))
						result.getElement().addAttribute(FROM_CONN_ID, connectionId.toString());
						if (null != session.getBareJID()) {
							result.getElement().addAttribute(SESSION_JID, session.getJID().toString());
						}
					}
					result.setPacketTo(ampJID);
					results.offer(result);
				}
				break;
			case "iq":
				// if there is no session do not try to deliver this stanza, instead use offline processor
				if (session != null && packet.getElemChild("fin") != null) {
					messageProcessor.handleDelivery(packet, session, repo, results, settings);
				} else {
					if (offlineProcessor != null) {
						offlineProcessor.processIq(packet, session, repo, results);
					} else {
						results.offer(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, ID, true));
					}
				}
				break;
		}
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return offlineProcessor == null ? DISCO_FEATURES_WITHOUT_OFFLINE : DISCO_FEATURES_WITH_OFFLINE;
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
	public void register(Kernel kernel) {
		kernel.registerBean(OfflineMessages.class).setActive(true).exec();
	}

	@Override
	public void unregister(Kernel kernel) {

	}
	
	private enum QuotaRule {
		error,
		drop;

		public static QuotaRule valueof(String name) {
			try {
				if (name != null) {
					return QuotaRule.valueOf(name);
				}
			} catch (IllegalArgumentException ex) {
			}
			return QuotaRule.error;
		}
	}
}
