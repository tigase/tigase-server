/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.xmpp.impl;

import java.util.Queue;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xml.DomBuilderHandler;
import tigase.xmpp.XMPPImplIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.StanzaType;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.util.JID;
import tigase.db.NonAuthUserRepository;
import tigase.db.UserNotFoundException;

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

  protected static final String XMLNS = "vcard-temp";
	protected static final String ID = XMLNS;
	protected static final String[] ELEMENTS = {"vCard"};
  protected static final String[] XMLNSS = {XMLNS};

	private static final SimpleParser parser =
		SingletonFactory.getParserInstance();

	// Implementation of tigase.xmpp.XMPPImplIfc

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }


	// Implementation of tigase.xmpp.XMPPProcessorIfc

	/**
	 * Describe <code>process</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param repo a <code>NonAuthUserRepository</code> value
	 * @param results a <code>Queue</code> value
	 */
	public void process(Packet packet, XMPPResourceConnection session,
		NonAuthUserRepository repo, Queue<Packet> results) {

		if (session == null && packet.getType() != null
			&& packet.getType() == StanzaType.get) {
			try {
				String strvCard =
					repo.getPublicData(JID.getNodeID(packet.getElemTo()), ID, VCARD_KEY, null);
				if (strvCard != null) {
					results.offer(parseVCard(strvCard, packet));
				} // end of if (vcard != null)
			} catch (UserNotFoundException e) {
				// Just ignore....
			} // end of try-catch
			return;
		} // end of if (session == null)

		try {
			if (packet.getFrom().equals(session.getConnectionId())) {
				packet.getElement().setAttribute("from", session.getJID());
			} // end of if (packet.getFrom().equals(session.getConnectionId()))

			String id = null;
			if (packet.getElemTo() != null) {
				id = JID.getNodeID(packet.getElemTo());
			} // end of if (packet.getElemTo() != null)
			if (id == null || id.equals(session.getUserId())) {
				StanzaType type = packet.getType();
				switch (type) {
				case get:
					String strvCard = session.getPublicData(ID, VCARD_KEY, null);
					if (strvCard != null) {
						results.offer(parseVCard(strvCard, packet));
					} else {
						results.offer(packet.okResult((String)null, 1));
					} // end of if (vcard != null) else
					break;
				case set:
					if (packet.getFrom().equals(session.getConnectionId())) {
						Element elvCard = packet.getElement().getChild("vCard");
						if (elvCard != null) {
							log.finer("Adding vCard: " + elvCard.toString());
							session.setPublicData(ID, VCARD_KEY, elvCard.toString());
						} else {
							log.finer("Removing vCard");
							session.removePublicData(ID, VCARD_KEY);
						} // end of else
						results.offer(packet.okResult((String)null, 0));
					} else {
						results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
								"You are not authorized to set vcard data.", true));
					} // end of else
					break;
				case result:
					Element elem = (Element)packet.getElement().clone();
					Packet result = new Packet(elem);
					result.setTo(session.getConnectionId());
					result.setFrom(packet.getTo());
					results.offer(result);
					break;
				default:
					results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
							"Request type is incorrect", false));
					break;
				} // end of switch (type)
			} else {
				Element result = (Element)packet.getElement().clone();
				// According to spec we must set proper FROM attribute
				//				result.setAttribute("from", session.getJID());
				results.offer(new Packet(result));
			} // end of else
		} catch (NotAuthorizedException e) {
      log.warning(
				"Received privacy request but user session is not authorized yet: " +
        packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch

	}

	private Packet parseVCard(String vcard, Packet packet) {
		DomBuilderHandler domHandler = new DomBuilderHandler();
		parser.parse(domHandler, vcard.toCharArray(), 0, vcard.length());
		Queue<Element> elems = domHandler.getParsedElements();
		Packet result = packet.okResult((Element)null, 0);
		for (Element el: elems) {
			result.getElement().addChild(el);
		} // end of for (Element el: elems)
		return result;
	}

} // VCardTemp
