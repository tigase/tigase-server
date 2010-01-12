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

import java.util.Queue;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xml.DomBuilderHandler;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.StanzaType;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPException;
import tigase.db.NonAuthUserRepository;
import tigase.db.UserNotFoundException;
import tigase.db.TigaseDBException;
import tigase.xmpp.BareJID;

/**
 * Describe class VCardTemp here.
 *
 *
 * Created: Thu Oct 19 23:37:23 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VCardTemp extends XMPPProcessor implements XMPPProcessorIfc {

	public static final String VCARD_KEY = "vCard";

	/**
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.xmpp.impl.VCardTemp");

  private static final String XMLNS = "vcard-temp";
	private static final String ID = XMLNS;
	// VCARD element is added to support old vCard protocol where element
	// name was all upper cases. Now the plugin should catch both cases.
	private static final String[] ELEMENTS = {"vCard", "VCARD"};
  private static final String[] XMLNSS = {XMLNS, XMLNS};
  private static final Element[] DISCO_FEATURES =	{
		new Element("feature", new String[] {"var"}, new String[] {XMLNS})
	};

	@Override
  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }


	private static final SimpleParser parser =
		SingletonFactory.getParserInstance();

	// Implementation of tigase.xmpp.XMPPImplIfc

	@Override
	public String id() { return ID; }

	@Override
	public String[] supElements()	{ return ELEMENTS; }

	@Override
  public String[] supNamespaces()	{ return XMLNSS; }


	// Implementation of tigase.xmpp.XMPPProcessorIfc

	/**
	 * Describe <code>process</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param repo a <code>NonAuthUserRepository</code> value
	 * @param results a <code>Queue</code> value
	 * @param settings
	 * @throws XMPPException 
	 */
	@Override
	public void process(Packet packet, XMPPResourceConnection session,
		NonAuthUserRepository repo, Queue<Packet> results,
		final Map<String, Object> settings)
		throws XMPPException {

		if (session == null && packet.getType() != null
			&& packet.getType() == StanzaType.get) {
			try {
				String strvCard =
					repo.getPublicData(packet.getStanzaTo().getBareJID().toString(),
						ID, VCARD_KEY, null);
				if (strvCard != null) {
					results.offer(parseXMLData(strvCard, packet));
				} else {
					results.offer(packet.okResult((String)null, 1));
				} // end of if (vcard != null)
			} catch (UserNotFoundException e) {
				results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
						"User not found", true));
			} // end of try-catch
			return;
		} // end of if (session == null)

		if (session == null) {
			log.info("Session null, dropping packet: " + packet);
			return;
		} // end of if (session == null)

		try {
			// Not needed anymore. Packet filter does it for all stanzas.
// 			if (packet.getFrom().equals(session.getConnectionId())) {
// 				packet.getElement().setAttribute("from", session.getJID());
// 			} // end of if (packet.getFrom().equals(session.getConnectionId()))

			BareJID id = null;
			if (packet.getStanzaTo() != null) {
				id = packet.getStanzaTo().getBareJID();
			} // end of if (packet.getElemTo() != null)
			if (id == null || id.equals(session.getUserId())) {
				StanzaType type = packet.getType();
				switch (type) {
					case get:
						try {
							String strvCard =
									repo.getPublicData(session.getUserId().toString(), ID, VCARD_KEY, null);
							if (strvCard != null) {
								results.offer(parseXMLData(strvCard, packet));
							} else {
								results.offer(packet.okResult((String)null, 1));
							} // end of if (vcard != null) else
						} catch (UserNotFoundException e) {
							results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(
									packet, "User not found", true));
						} // end of try-catch
						break;
					case set:
						if (packet.getFrom().equals(session.getConnectionId())) {
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
							} // end of else
							results.offer(packet.okResult((String)null, 0));
						} else {
							results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(
									packet, "You are not authorized to set vcard data.", true));
						} // end of else
					break;
					case result:
					case error:
						Packet result = packet.copyElementOnly();
						result.setPacketTo(session.getConnectionId());
						result.setPacketFrom(packet.getTo());
						results.offer(result);
						break;
					default:
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
								"Request type is incorrect", false));
						break;
				} // end of switch (type)
			} else {
				// According to spec we must set proper FROM attribute
				//				result.setAttribute("from", session.getJID());
				results.offer(packet.copyElementOnly());
			} // end of else
		} catch (NotAuthorizedException e) {
			e.printStackTrace();
      log.warning(
					"Received vCard request but user session is not authorized yet: "
					+ packet);
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} catch (TigaseDBException e) {
			log.warning("Database problem, please contact admin: " +e);
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
					"Database access problem, please contact administrator.", true));
		} // end of try-catch

	}

	private Packet parseXMLData(String data, Packet packet) {
		DomBuilderHandler domHandler = new DomBuilderHandler();
		parser.parse(domHandler, data.toCharArray(), 0, data.length());
		Queue<Element> elems = domHandler.getParsedElements();
		Packet result = packet.okResult((Element)null, 0);
		for (Element el: elems) {
			result.getElement().addChild(el);
		} // end of for (Element el: elems)
		return result;
	}

} // VCardTemp
