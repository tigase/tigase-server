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
import tigase.server.Packet;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xml.Element;

/**
 * Describe class Dialback here.
 *
 *
 * Created: Wed Aug 16 18:27:31 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Dialback extends XMPPProcessor {

	public static final String MY_HOSTNAME_KEY = "my-hostname";
	public static final String REMOTE_HOSTNAME_KEY = "remote-hostname";

	private static final String XMLNS = "jabber:server";
	protected static final String ID = XMLNS + ":dialback";
  protected static final String[] ELEMENTS =
	{"db:result", "db:verify"};
  protected static final String[] XMLNSS = {XMLNS};

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

  public void process(final Packet packet, final XMPPResourceConnection session,
		final Queue<Packet> results) {
		if (packet == null) {
			// Well, I have to send db:result with unique session ID
			String my_hostname = (String)session.getSessionData(MY_HOSTNAME_KEY);
			String remote_hostname =
				(String)session.getSessionData(REMOTE_HOSTNAME_KEY);
			Element elem = new Element("db:result", "SECRET-ID-HERE");
			elem.addAttribute("to", remote_hostname);
			elem.addAttribute("from", my_hostname);
			results.offer(new Packet(elem));
		} // end of if (packet == null)

		

		// Process verification request
		if (packet != null && packet.getElemName().equals("db:verify")
			&& packet.getType() == null) {
			results.offer(packet.swapElemFromTo(StanzaType.valid));
		} // end of if (packet != null && packet.getElemName().equals("db:verify")
			// && packet.getType() == null)
	}

} // Dialback
