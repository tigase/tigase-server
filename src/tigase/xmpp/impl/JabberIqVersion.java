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
import tigase.server.XMPPServer;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPResourceConnection;
import tigase.util.JID;

/**
 * JEP-0092: Software Version
 *
 *
 * Created: Tue Mar 21 06:45:51 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqVersion extends XMPPProcessor {

  private static final Logger log =
    Logger.getLogger("tigase.xmpp.impl.JabberIQVersion");

	protected static final String ID = "jabber:iq:version";
  protected static final String[] ELEMENTS = {"query"};
  protected static final String[] XMLNSS = {"jabber:iq:version"};
  protected static final String[] DISCO_FEATURES = {"jabber:iq:version"};

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

  public String[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

	public void process(final Packet packet, final XMPPResourceConnection session,
		final Queue<Packet> results) {
		// Maybe it is message to admininstrator:
		String id = session.getDomain();
		if (packet.getElemTo() != null) {
			id = JID.getNodeID(packet.getElemTo());
		} // end of if (packet.getElemTo() != null)
		// If ID part of user account contains only host name
		// and this is local domain it is message to admin
		if (id == null || id.equals("")
			|| id.equalsIgnoreCase(session.getDomain())) {

			StringBuilder reply = new StringBuilder();
			reply.append("<name>" + XMPPServer.NAME + "</name>");
			reply.append(
				"<version>"
				+ XMPPServer.getImplementationVersion()
				+ "</version>");
			reply.append(
				"<os>"
				+ System.getProperty("os.name")
				+ "-" + System.getProperty("os.arch")
				+ "-" + System.getProperty("os.version")
				+ ", " + System.getProperty("java.vm.name")
				+ "-" + System.getProperty("java.vm.version")
				+ "-" + System.getProperty("java.vm.vendor")
				+ "</os>"
									 );
			results.offer(packet.okResult(reply.toString(), 1));
			return;
		}

		try {
			// According to JEP-0092 user doesn't need to be logged in to
			// retrieve server version information, so this part is executed
			// after checking if this message is just to local server
			if (id.equals(session.getUserId())) {
				// Yes this is message to 'this' client
				Element elem = (Element)packet.getElement().clone();
				Packet result = new Packet(elem);
				result.setTo(session.getConnectionId());
				result.setFrom(packet.getTo());
				results.offer(result);
			} else {
				// This is message to some other client so I need to
				// set proper 'from' attribute whatever it is set to now.
				// Actually processor should not modify request but in this
				// case it is absolutely safe and recommended to set 'from'
				// attribute
				Element result = (Element)packet.getElement().clone();
				// According to spec we must set proper FROM attribute
				result.setAttribute("from", session.getJID());
				results.offer(new Packet(result));
			} // end of else
		} catch (NotAuthorizedException e) {
			// Do nothing, shouldn't happen....
			log.warning("NotAuthorizedException for packet: "
				+ packet.getStringData());
		} // end of try-catch
	}

} // JabberIqVersion
