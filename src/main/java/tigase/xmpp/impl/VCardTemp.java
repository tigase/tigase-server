/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;

import tigase.server.Packet;

import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class VCardTemp here.
 *
 *
 * Created: Thu Oct 19 23:37:23 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VCardTemp extends XMPPProcessorAbstract {

	/** Field description */
	public static final String VCARD_KEY = "vCard";

	/**
	 * Private logger for class instancess.
	 */
	private static Logger log = Logger.getLogger("tigase.xmpp.impl.VCardTemp");
	private static final String XMLNS = "vcard-temp";
	private static final String ID = XMLNS;

	// VCARD element is added to support old vCard protocol where element
	// name was all upper cases. Now the plugin should catch both cases.
	private static final String[] ELEMENTS = { "vCard", "VCARD" };
	private static final String[] XMLNSS = { XMLNS, XMLNS };
	private static final Element[] DISCO_FEATURES = {
		new Element("feature", new String[] { "var" }, new String[] { XMLNS }) };
	private static final SimpleParser parser = SingletonFactory.getParserInstance();

	//~--- methods --------------------------------------------------------------

	// Implementation of tigase.xmpp.XMPPImplIfc

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String id() {
		return ID;
	}

	/**
	 * Method description
	 *
	 *
	 * @param connectionId
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 *
	 * @throws PacketErrorTypeException
	 */
	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet,
			XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
				Map<String, Object> settings)
			throws PacketErrorTypeException {
		if (packet.getType() != null) {
			try {
				switch (packet.getType()) {
					case get :
						String strvCard = session.getPublicData(ID, VCARD_KEY, null);

						if (strvCard != null) {
							results.offer(parseXMLData(strvCard, packet));
						} else {
							results.offer(packet.okResult((String) null, 1));
						}    // end of if (vcard != null) else

						break;

					case set :
						Element elvCard = packet.getElement().getChild(ELEMENTS[0]);

						// This is added to support old vCard protocol where element
						// name was all upper cases. So here I am checking both
						// possibilities
						if (elvCard == null) {
							elvCard = packet.getElement().getChild(ELEMENTS[1]);
						}

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

						results.offer(packet.okResult((String) null, 0));

						break;

					default :

					// Ignore all others...
				}
			} catch (NotAuthorizedException ex) {
				log.warning("Received vCard request but user session is not authorized yet: "
						+ packet);
				results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
						"You must authorize session first.", true));
			} catch (TigaseDBException ex) {
				log.warning("Database problem, please contact admin: " + ex);
				results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
						"Database access problem, please contact administrator.", true));
			}
		} else {

			// TODO: if this really happen that this is clearly protocol error, as that would be
			// vCard packet with no type set, do we really need to handle such an erro? Let's
			// ignore it for now.
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param repo
	 * @param results
	 * @param settings
	 *
	 * @throws PacketErrorTypeException
	 */
	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {
		if (packet.getType() == StanzaType.get) {
			try {
				String strvCard = repo.getPublicData(packet.getStanzaTo().getBareJID().toString(), ID,
					VCARD_KEY, null);

				if (strvCard != null) {
					results.offer(parseXMLData(strvCard, packet));
				} else {
					results.offer(packet.okResult((String) null, 1));
				}    // end of if (vcard != null)
			} catch (UserNotFoundException e) {
				results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
						"User not found", true));
			}    // end of try-catch
		} else {

			// Skip the rest, we are not interested in any other packet, maybe we should
			// send an error instead?
			// TODO: investigate whether sending would be more appropriate here
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 */
	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) {

		// TODO: Hm, the server vCard should be sent here, not yet implemented....
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 *
	 * @throws PacketErrorTypeException
	 */
	@Override
	public void processToUserPacket(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {
		processNullSessionPacket(packet, repo, results, settings);
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supElements() {
		return ELEMENTS;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	private Packet parseXMLData(String data, Packet packet) {
		DomBuilderHandler domHandler = new DomBuilderHandler();

		parser.parse(domHandler, data.toCharArray(), 0, data.length());

		Queue<Element> elems = domHandler.getParsedElements();
		Packet result = packet.okResult((Element) null, 0);

		for (Element el : elems) {
			result.getElement().addChild(el);
		}    // end of for (Element el: elems)

		return result;
	}
}    // VCardTemp


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
