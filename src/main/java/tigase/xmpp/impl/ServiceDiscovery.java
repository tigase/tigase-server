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

import tigase.component.modules.impl.AdHocCommandModule;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.db.services.AccountExpirationService;
import tigase.disco.XMPPServiceCollector;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.util.datetime.TimestampHelper;
import tigase.vhosts.VHostManagerIfc;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.jid.JID;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.component.modules.impl.DiscoveryModule.DISCO_ITEMS_XMLNS;

/**
 * Implementation of JEP-030.
 * <br>
 * Created: Mon Mar 27 20:45:36 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Bean(name = ServiceDiscovery.ID, parent = SessionManager.class, active = true)
public class ServiceDiscovery
		extends XMPPProcessorAbstract {

	protected static final String ID = "disco";
	private static final Logger log = Logger.getLogger(ServiceDiscovery.class.getName());
	private static final String[][] ELEMENTS = {Iq.IQ_QUERY_PATH, Iq.IQ_QUERY_PATH, Iq.IQ_QUERY_PATH};
	private static final String[] XMLNSS = {XMPPServiceCollector.INFO_XMLNS, XMPPServiceCollector.ITEMS_XMLNS,
											Command.XMLNS};
	private static final Element[] DISCO_FEATURES = {
			new Element("feature", new String[]{"var"}, new String[]{XMPPServiceCollector.INFO_XMLNS}),
			new Element("feature", new String[]{"var"}, new String[]{XMPPServiceCollector.ITEMS_XMLNS})};
	private static final TimestampHelper timestampHelper = new TimestampHelper();

	@Inject(nullAllowed = true)
	private AccountServiceProvider serviceProvider;

	@Inject(nullAllowed = true)
	private SessionManager sessionManager;
	@Inject
	private VHostManagerIfc vhostManager;
	@Inject
	private AdHocCommandModule adHocCommandModule;
	@Inject
	private UserRepository userRepository;
	
	@Override
	public String id() {
		return ID;
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {

		JID to = packet.getStanzaTo();
		if (to == null) {
			if (session == null || !session.isAuthorized()) {
				log.log(Level.FINEST, "got <iq/> packet with no ''from'' attribute = {0}", packet);
				return;
			}
			to = JID.jidInstance(session.getBareJID());
		}

		if (to != null) {
			JID from = packet.getStanzaFrom();
			if (from == null) {
				if (session == null || !session.isAuthorized()) {
					log.log(Level.FINEST, "got <iq/> packet with no ''from'' attribute = {0}", packet);
					return;
				}
				from = session.getjid();
			}

			if (packet.getType() == StanzaType.get && to.getLocalpart() != null && to.getResource() == null &&
					sessionManager.isLocalDomain(to.getDomain())) {
				Element query = packet.getElement()
						.findChild(el -> el.getName() == "query" &&
								el.getXMLNS() == "http://jabber.org/protocol/disco#info");
				if (!isLocalComponent(to)) {
					if (serviceProvider != null) {
						if (query != null && query.getAttributeStaticStr("node") == null) {
							// custom handling..
							Packet result = packet.copyElementOnly();
							if (result.getStanzaTo() == null) {
								result.initVars(from, to);
							}
							result.setPacketTo(serviceProvider.getServiceProviderComponentJid());
							result.setPacketFrom(sessionManager.getComponentId());
							results.offer(result);
						} else {
							// this packet is to local user (offline or not but to bare JID) - just forward to service provider component
							Element itemsQuery = packet.getElement().findChild(el -> el.getName() == "query" && el.getXMLNS() == "http://jabber.org/protocol/disco#items");
							if (itemsQuery != null && "http://jabber.org/protocol/commands".equals(itemsQuery.getAttributeStaticStr("node".intern()))) {
								Element resultQuery = new Element("query", new String[]{Packet.XMLNS_ATT}, new String[]{DISCO_ITEMS_XMLNS});
								adHocCommandModule.addCommandListItemsElements("http://jabber.org/protocol/commands", to, from, resultQuery::addChild);
								Packet result = packet.okResult(resultQuery, 0);
								results.offer(result);
								return;
							}
							Packet result = packet.copyElementOnly();
							if (packet.getStanzaTo() == null && session != null) {
								result.initVars(packet.getStanzaFrom() != null ? packet.getStanzaFrom() : session.getJID(),
												to);
							}
							if (packet.getStanzaFrom() == null) {
								// at this point we should already have "from" attribute set
								// if we do not have it, then we should drop this packet
								if (log.isLoggable(Level.FINEST)) {
									log.log(Level.FINEST,
											"received <iq/> packet to forward to service provider component without ''from'' attribute, dropping packet = {0}",
											packet);
								}
								return;
							}
							result.setPacketFrom(packet.getFrom());
							result.setPacketTo(serviceProvider.getServiceProviderComponentJid());

							results.offer(result);
						}
					} else if (query != null && query.getAttributeStaticStr("node") == null) {
						query = new Element("query");
						query.setXMLNS("http://jabber.org/protocol/disco#info");
						Packet result = packet.okResult(query, 0);
						addAccountFeatures(result, from.getBareJID().equals(to.getBareJID()));
						results.offer(result);
					} else {
						super.process(packet, session, repo, results, settings);
					}
				} else {
					super.process(packet, session, repo, results, settings);
				}
			} else if ((packet.getType() == StanzaType.result || packet.getType() == StanzaType.error) &&
					from.getLocalpart() != null && from.getResource() == null &&
					sessionManager.isLocalDomain(from.getDomain())) {
				if (serviceProvider != null && packet.getPacketFrom() != null &&
						packet.getPacketFrom().equals(serviceProvider.getServiceProviderComponentJid())) {
					if (session != null && session.isUserId(to.getBareJID())) {
						Packet result = packet.copyElementOnly();
						result.setPacketFrom(packet.getFrom());
						result.setPacketTo(session.getConnectionId());
						addAccountFeatures(result, from.getBareJID().equals(to.getBareJID()));
						results.offer(result);
					} else {
						if (sessionManager.isLocalDomain(to.getDomain())) {
							return;
						} else {
							Packet result = packet.copyElementOnly();
							result.setPacketFrom(packet.getFrom());
							result.setPacketTo(null);
							addAccountFeatures(result, false);
							results.offer(result);
						}
					}
				} else {
					super.process(packet, session, repo, results, settings);
				}
			} else {
				super.process(packet, session, repo, results, settings);
			}
		} else {
			super.process(packet, session, repo, results, settings);
		}
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
											  NonAuthUserRepository repo, Queue<Packet> results,
											  Map<String, Object> settings) throws PacketErrorTypeException {

		// Handled elsewhere (in MessageRouter)
		if (packet.getStanzaTo() != null) {
			log.log(Level.FINEST, "forwarding packet to MR" + packet.toString());
			results.offer(packet.copyElementOnly());
		}
	}

	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo, Queue<Packet> results,
										 Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getElemName() == Iq.ELEM_NAME && (packet.getType().equals(StanzaType.error) || packet.getType().equals(StanzaType.result))) {
			// https://xmpp.org/rfcs/rfc6120.html#stanzas-semantics-iq
			// An entity that receives a stanza of type "result" or "error" MUST NOT respond to the stanza
			// by sending a further IQ response of type "result" or "error"; however, the requesting entity
			// MAY send another request (e.g., an IQ of type "set" to provide obligatory information discovered
			// through a get/result pair).
			return;
		} else {
			results.offer(Authorization.RECIPIENT_UNAVAILABLE.getResponseMessage(packet,
																				 "The target is unavailable at this time.",
																				 true));
		}
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
										   Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {

		// Handled elsewhere (in MessageRouter)
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

	private boolean isLocalComponent(JID jid) {
		return vhostManager.isLocalDomainOrComponent(jid.getLocalpart() + "." + jid.getDomain());
	}

	private void addAccountFeatures(Packet result, boolean isOwner) {
		if (result.getType() != StanzaType.result) {
			return;
		}
		Element query = result.getElement().getChild("query");
		if (query != null) {
			if (isOwner) {
				List<Element> features = query.getChildren();
				query.setChildren(Collections.emptyList());
				query.addChild(new Element("identity", new String[]{"category", "type"},
										   new String[]{"account", "registered"}));
				if (features != null) {
					features.forEach(query::addChild);
				}
				sessionManager.getDiscoFeatures(result.getStanzaFrom()).forEach(query::addChild);
				try {
					Optional.ofNullable(userRepository.getData(result.getStanzaFrom().getBareJID(),
															   AccountExpirationService.ACCOUNT_EXPIRATION_DATE))
							.map(LocalDate::parse)
							.ifPresent(expirationDate -> {
								Element x = new DataForm.Builder(Command.DataType.result).withFields(builder -> {
									builder.withField(DataForm.FieldType.Hidden, "FORM_TYPE", field -> {
										field.setValue("tigase:disco:account:0");
									})
											.withField(DataForm.FieldType.TextSingle, "expiration-date", field -> field.setValue(expirationDate.toString()));
								}).build();
								query.addChild(x);
							});
				} catch (TigaseDBException e) {
					log.log(Level.SEVERE, "Failed to retrieve account " + result.getStanzaFrom() + " expiration date", e);
				}
			}
		}
	}

	public interface AccountServiceProvider {

		JID getServiceProviderComponentJid();

	}
}    // ServiceDiscovery
