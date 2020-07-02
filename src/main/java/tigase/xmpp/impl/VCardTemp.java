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

import tigase.component.exceptions.RepositoryException;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.Priority;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.*;
import tigase.xmpp.impl.annotation.DiscoFeatures;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.db.NonAuthUserRepository.PUBLIC_DATA_NODE;
import static tigase.xmpp.impl.VCardTemp.*;

/**
 * Describe class VCardTemp here.
 * <br>
 * Created: Thu Oct 19 23:37:23 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
@Id(XMLNS)
@Handles({@Handle(path = {Iq.ELEM_NAME, vCard}, xmlns = XMLNS), @Handle(path = {Iq.ELEM_NAME, VCARD}, xmlns = XMLNS)})
@DiscoFeatures({XMLNS})
@Bean(name = VCardTemp.ID, parent = SessionManager.class, active = true)
public class VCardTemp
		extends VCardXMPPProcessorAbstract implements PresenceState.ExtendedPresenceProcessorIfc {

	public static final String VCARD_KEY = "vCard";
	// VCARD element is added to support old vCard protocol where element
	// name was all upper cases. Now the plugin should catch both cases.
	protected static final String vCard = "vCard";
	protected static final String VCARD = "VCARD";
	protected static final String XMLNS = "vcard-temp";
	protected static final String ID = XMLNS;
	private static final SimpleParser parser = SingletonFactory.getParserInstance();
	/**
	 * Private logger for class instances.
	 */
	private static Logger log = Logger.getLogger(VCardTemp.class.getName());

	@Inject
	private UserRepository userRepository;

	public void processFromUserOutPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
										 NonAuthUserRepository repo, Queue<Packet> results,
										 Map<String, Object> settings) throws PacketErrorTypeException {
		if (session.isLocalDomain(packet.getStanzaTo().getDomain(), false)) {

			// This is a local user so we can quickly get his vCard from the database
			try {
				String strvCard = repo.getPublicData(packet.getStanzaTo().getBareJID(), ID, VCARD_KEY, null);
				Packet result = null;

				if (strvCard != null) {
					result = parseXMLData(strvCard, packet);
				} else {
					result = packet.okResult((String) null, 1);
				}    // end of if (vcard != null)
				result.setPacketTo(connectionId);
				results.offer(result);
			} catch (UserNotFoundException e) {
				results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "User not found", true));
			}    // end of try-catch
		} else {

			// Else forward the packet to a remote server
			results.offer(packet.copyElementOnly());
		}
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
											  NonAuthUserRepository repo, Queue<Packet> results,
											  Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getType() != null) {
			try {
				Packet result = null;

				switch (packet.getType()) {
					case get:
						String strvCard = session.getPublicData(ID, VCARD_KEY, null);

						if (strvCard != null) {
							result = parseXMLData(strvCard, packet);
						} else {
							result = packet.okResult((String) null, 1);
						}    // end of if (vcard != null) else

						break;

					case set:
						Element elvCard = packet.getElement().getChild(vCard);

						// This is added to support old vCard protocol where element
						// name was all upper cases. So here I am checking both
						// possibilities
						if (elvCard == null) {
							elvCard = packet.getElement().getChild(VCARD);
						}
						setVCard(session, elvCard);
						result = packet.okResult((String) null, 0);

						break;

					default:

						// Ignore all others...
				}
				if (result != null) {
					result.setPacketTo(session.getConnectionId());
					results.offer(result);
				}
			} catch (NoConnectionIdException ex) {

				// This should not happen unless somebody sends a result vcard packet
				// to the server itself
				log.warning("This should not happen, unless this is a vcard result packet " +
									"sent to the server, which should not happen: " + packet);
			} catch (NotAuthorizedException ex) {
				log.warning("Received vCard request but user session is not authorized yet: " + packet);
				results.offer(
						Authorization.NOT_AUTHORIZED.getResponseMessage(packet, "You must authorize session first.",
																		true));
			} catch (TigaseDBException ex) {
				log.warning("Database problem, please contact admin: " + ex);
				results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
																					 "Database access problem, please contact administrator.",
																					 true));
			}
		} else {

			// TODO: if this really happen that this is clearly protocol error, as
			// that would be
			// vCard packet with no type set, do we really need to handle such an
			// erro? Let's
			// ignore it for now.
		}
	}

	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo, Queue<Packet> results,
										 Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getType() == StanzaType.get) {
			try {
				String strvCard = repo.getPublicData(packet.getStanzaTo().getBareJID(), ID, VCARD_KEY, null);

				if (strvCard != null) {
					results.offer(parseXMLData(strvCard, packet));
				} else {
					results.offer(packet.okResult((String) null, 1));
				}    // end of if (vcard != null)
			} catch (UserNotFoundException e) {
				results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "User not found", true));
			}    // end of try-catch
		} else {

			// This is most likely a response to the user from the remote
			// entity with vCard request results.
			// Processed in processToUserPacket() method.
		}
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
										   Queue<Packet> results, Map<String, Object> settings) {

		// TODO: Hm, the server vCard should be sent here, not yet implemented....
	}

	@Override
	public void processToUserPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
									Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {
		processNullSessionPacket(packet, repo, results, settings);
		if ((session != null) && session.isAuthorized() && (packet.getType() != StanzaType.get)) {
			try {
				JID conId = session.getConnectionId(packet.getStanzaTo());

				if (conId == null) {

					// Drop it, user is no longer online.
					return;
				}

				Packet result = packet.copyElementOnly();

				result.setPacketTo(session.getConnectionId(packet.getStanzaTo()));
				results.offer(result);
			} catch (NoConnectionIdException ex) {

				// This should not happen unless somebody sends a result vcard packet
				// to the server itself
				log.warning("This should not happen, unless this is a vcard result packet " +
									"sent to the server, which should not happen: " + packet);
			}
		}
	}

	public void pepToVCardTemp_onPublication(BareJID userJid, XMPPResourceConnection session, String itemId, String mimeType, Supplier<JID> pubsubComponentJidSupplier, Consumer<Packet> writer) {
		Element iqEl = new Element("iq", new String[]{"type", "id"}, new String[]{"get", "sm-query-vcard-pep-" + mimeType});
		iqEl.withElement("pubsub", "http://jabber.org/protocol/pubsub", pubsubEl -> {
			pubsubEl.withElement("items", itemsEl -> {
				itemsEl.setAttribute("node", "urn:xmpp:avatar:data");
				itemsEl.withElement("item", itemEl -> {
					itemEl.setAttribute("id", itemId);
				});
			});
		});

		Iq iq = (Iq) Packet.packetInstance(iqEl, JID.jidInstanceNS(userJid), JID.jidInstanceNS(userJid));
		iq.setPacketTo(pubsubComponentJidSupplier.get());
		// lower priority to make sure that publication will take place before we would query for an item
		iq.setPriority(Priority.LOW);

		writer.accept(iq);
	}

	public void pepToVCardTemp_onDataRetrieved(Packet packet, XMPPResourceConnection session) {
		Element itemEl = packet.getElement().findChild(new String[] { "iq", "pubsub", "items", "item" });
		String mimeType = packet.getAttributeStaticStr("id").replace("sm-query-vcard-pep-", "");
		if (itemEl != null && mimeType != null) {
			String id = itemEl.getAttributeStaticStr("id");
			Element data = itemEl.getChild("data", "urn:xmpp:avatar:data");
			if (id != null && data != null) {
				try {
					Element vCard = this.parseXMLDataToElement(userRepository.getData(packet.getStanzaFrom().getBareJID(),
																			 PUBLIC_DATA_NODE + "/vcard-temp",
																			 VCardTemp.VCARD_KEY))
							.orElseGet(() -> new Element("vCard", new String[]{"xmlns"}, new String[]{"vcard-temp"}));

					Element photoEl = vCard.findChild(new String[]{"vCard", "PHOTO"});
					if (photoEl != null) {
						vCard.removeChild(photoEl);
					}

					if (data.getCData() != null) {
						photoEl = new Element("PHOTO");
						photoEl.withElement("TYPE", null, mimeType);
						photoEl.withElement("BINVAL", null, data.getCData());
						vCard.addChild(photoEl);
					}
					userRepository.setData(packet.getStanzaFrom().getBareJID(), ID, "pep-vcard-temp-conv-hash", id);
					if (session != null) {
						session.putCommonSessionData("pep-vcard-temp-conv-hash", id);
					}
					userRepository.setData(packet.getStanzaFrom().getBareJID(), PUBLIC_DATA_NODE + "/" + ID,
										   VCardTemp.VCARD_KEY, vCard.toString());
				} catch (RepositoryException ex) {
					log.log(Level.FINEST, "failed to update VCardTemp avatar on PEP User Avatar change!");
				}
			}
		}
	}

	@Override
	public Element extend(Element presence, XMPPResourceConnection session, Queue<Packet> results) {
		Element x = presence.getChild("x", "vcard-temp:x:update");
		Element photoEl = x == null ? null : x.getChild("photo");
		if (photoEl != null) {
			return null;
		}
		String hash = (String) session.computeCommonSessionDataIfAbsent("pep-vcard-temp-conv-hash", (key) -> {
			try {
				return Optional.ofNullable(userRepository.getData(session.getBareJID(), ID, "pep-vcard-temp-conv-hash"))
						.orElse("");
			} catch (NotAuthorizedException ex) {
				log.log(Level.FINEST, "failed to retrieve VCardTemp avatar hash - session not authorized yet!", ex);
			} catch (RepositoryException ex) {
				log.log(Level.FINEST, "failed to retrieve VCardTemp avatar hash!", ex);
			}
			return null;
		});
		if (hash.isEmpty()) {
			return null;
		}
		if (x == null) {
			x = new Element("x");
			x.setXMLNS("vcard-temp:x:update");
		} else if (photoEl != null) {
			presence.removeChild(x);
			x.removeChild(photoEl);
		}
		photoEl = new Element("photo", hash);
		x.addChild(photoEl);
		return x;
	}

	@Override
	public Element extend(XMPPResourceConnection session, Queue<Packet> results) {
		// this will never be called!
		throw new UnsupportedOperationException("It should never be called!");
	}

	@Override
	protected String getVCardXMLNS() {
		return XMLNS;
	}

	@Override
	protected void storeVCard(XMPPResourceConnection session, Element elvCard)
			throws TigaseDBException, NotAuthorizedException {
		if (elvCard != null) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Adding vCard: " + elvCard);
			}
			session.setPublicData(ID, VCARD_KEY, elvCard.toString());
		} else {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Removing vCard");
			}
			session.removePublicData(ID, VCARD_KEY);
		}    // end of else
	}

	private Packet parseXMLData(String data, Packet packet) {
		DomBuilderHandler domHandler = new DomBuilderHandler();

		parser.parse(domHandler, data.toCharArray(), 0, data.length());

		Queue<Element> elems = domHandler.getParsedElements();
		Packet result = packet.okResult((Element) null, 0);

		result.setPacketFrom(null);
		result.setPacketTo(null);
		for (Element el : elems) {
			result.getElement().addChild(el);
		}    // end of for (Element el: elems)

		return result;
	}

	private Optional<Element> parseXMLDataToElement(String data) {
		if (data == null) {
			return Optional.empty();
		}
		DomBuilderHandler domHandler = new DomBuilderHandler();

		parser.parse(domHandler, data.toCharArray(), 0, data.length());

		Queue<Element> elems = domHandler.getParsedElements();

		return Optional.ofNullable(elems.poll());
	}

}    // VCardTemp


