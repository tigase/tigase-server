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

import java.util.logging.Logger;
import java.util.Arrays;
import java.util.Queue;
import java.util.List;
import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xml.DomBuilderHandler;
import tigase.xmpp.XMPPImplIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.util.JIDUtils;
import tigase.xmpp.NotAuthorizedException;
import tigase.db.NonAuthUserRepository;
import tigase.db.UserNotFoundException;

/**
 * Describe class JabberIqPrivate here.
 *
 *
 * Created: Mon Apr 16 08:28:18 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqPrivate extends XMPPProcessor implements XMPPProcessorIfc {

	/**
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.xmpp.impl.JabberIqPrivate");

  private static final String XMLNS = "jabber:iq:private";

	private static final String PRIVATE_KEY = XMLNS;

	private static final String ID = XMLNS;
	private static final String[] ELEMENTS = {"query"};
  private static final String[] XMLNSS = {XMLNS};
  private static final Element[] DISCO_FEATURES =	{
		new Element("feature", new String[] {"var"}, new String[] {XMLNS})
	};

  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return Arrays.copyOf(DISCO_FEATURES, DISCO_FEATURES.length); }


	private static final SimpleParser parser =
		SingletonFactory.getParserInstance();

	// Implementation of tigase.xmpp.XMPPImplIfc

	public String id() { return ID; }

	public String[] supElements()
	{ return Arrays.copyOf(ELEMENTS, ELEMENTS.length); }

  public String[] supNamespaces()
	{ return Arrays.copyOf(XMLNSS, XMLNSS.length); }

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

		// Don't do anything if session is null
		if (session == null) {
			log.info("Session null, dropping packet: " + packet.getStringData());
			return;
		} // end of if (session == null)

		try {
			if (packet.getElemTo() != null &&
				!JIDUtils.getNodeID(packet.getElemTo()).equals(session.getUserId())) {
				results.offer(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
						"You are not authorized to access this private storage.", true));
				return;
			}
			if (packet.getFrom().equals(session.getConnectionId())) {
				List<Element> elems = packet.getElemChildren("/iq/query");
				if (elems != null && elems.size() > 0) {
					Element elem = elems.get(0);
					StanzaType type = packet.getType();
					switch (type) {
					case get:
						String priv = session.getData(PRIVATE_KEY,
							elem.getName()+elem.getXMLNS(), null);
						log.finest("Loaded private data for key: "
							+ elem.getName() + ": " + priv);
						if (priv != null) {
							results.offer(parseXMLData(priv, packet));
							break;
						}
						results.offer(packet.okResult((String)null, 2));
						break;
					case set:
						log.finest("Saving private data: " + elem.toString());
						session.setData(PRIVATE_KEY,
							elem.getName()+elem.getXMLNS(), elem.toString());
						results.offer(packet.okResult((String)null, 0));
						break;
					case result:
						// Should never happen, it is an error and should be ignored
						break;
					default:
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
								"Request type is incorrect", false));
						break;
					} // end of switch (type)
				} else {
					results.offer(Authorization.NOT_ACCEPTABLE.getResponseMessage(packet,
							"Missing query child element", true));
				}
			} else {
				results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
						"You are not authorized to access this private storage.", true));
			} // end of else
		} catch (NotAuthorizedException e) {
      log.warning(
				"Received privacy request but user session is not authorized yet: " +
        packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch

	}

	private Packet parseXMLData(String data, Packet packet) {
		DomBuilderHandler domHandler = new DomBuilderHandler();
		parser.parse(domHandler, data.toCharArray(), 0, data.length());
		Queue<Element> elems = domHandler.getParsedElements();
		Packet result = packet.okResult((Element)null, 1);
		Element query = result.getElement().findChild("/iq/query");
		for (Element el: elems) {
			query.addChild(el);
		} // end of for (Element el: elems)
		return result;
	}

}