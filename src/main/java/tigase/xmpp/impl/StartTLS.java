/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
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

import tigase.server.Command;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class StartTLS here.
 *
 *
 * Created: Fri Mar 24 07:22:57 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StartTLS extends XMPPProcessor implements XMPPProcessorIfc {
	private static final Logger log = Logger.getLogger(StartTLS.class.getName());
	private static final String XMLNS = "urn:ietf:params:xml:ns:xmpp-tls";

	// private static final String TLS_STARTED_KEY = "TLS-Started";

	/** Field description */
	public static final String TLS_REQUIRED_KEY = "tls-required";
	protected static final String ID = "starttls";
	private static final String[] ELEMENTS = { "starttls", "proceed", "failure" };
	private static final String[] XMLNSS = { XMLNS, XMLNS, XMLNS };
	private static final Element[] F_REQUIRED = {
		new Element("starttls", new Element[] { new Element("required") }, new String[] { "xmlns" },
			new String[] { XMLNS }) };
	private static final Element[] F_NOT_REQUIRED = {
		new Element("starttls", new String[] { "xmlns" }, new String[] { XMLNS }) };

	//~--- fields ---------------------------------------------------------------

	private Element proceed = new Element("proceed", new String[] { "xmlns" },
		new String[] { XMLNS });
	private Element failure = new Element("failure", new String[] { "xmlns" },
		new String[] { XMLNS });

	//~--- methods --------------------------------------------------------------

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
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 */
	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results,
				final Map<String, Object> settings) {
		if (session == null) {
			return;
		}    // end of if (session == null)

		if (packet.isElement("starttls", XMLNS)) {
			if (session.getSessionData(ID) != null) {

				// Somebody tries to activate multiple TLS layers.
				// This is possible and can even work but this can also be
				// a DOS attack. Blocking it now, unless someone requests he wants
				// to have multiple layers of TLS for his connection
				log.log(Level.WARNING,
						"Multiple TLS requests, possible DOS attack, closing connection: {0}", packet);
				results.offer(packet.swapFromTo(failure, null, null));
				results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set,
						session.nextStanzaId()));

				return;
			}

			session.putSessionData(ID, "true");

			Packet result = Command.STARTTLS.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set,
				session.nextStanzaId(), Command.DataType.submit);

			Command.setData(result, proceed);
			results.offer(result);
		} else {
			log.log(Level.WARNING, "Unknown TLS element: {0}", packet);
			results.offer(packet.swapFromTo(failure, null, null));
			results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set,
					session.nextStanzaId()));
		}    // end of if (packet.getElement().getName().equals("starttls")) else
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

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {

		// If session does not exist, just return null, we don't provide features
		// for non-existen stream
		if ((session != null) && (session.getSessionData(ID) == null)) {
			if ((session.getSessionData(TLS_REQUIRED_KEY) != null)
					&& session.getSessionData(TLS_REQUIRED_KEY).equals("true")) {
				return F_REQUIRED;
			} else {
				return F_NOT_REQUIRED;
			}
		}    // end of if (session.isAuthorized())
				else {
			return null;
		}    // end of if (session.isAuthorized()) else
	}
}    // StartTLS


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
