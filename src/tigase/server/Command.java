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
package tigase.server;

import tigase.xml.Element;
import tigase.xmpp.StanzaType;

/**
 * Describe enum Command here.
 *
 *
 * Created: Thu Feb  9 20:52:02 2006
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public enum Command {

	STREAM_OPENED,
		STREAM_CLOSED,
		STARTTLS,
		GETFEATURES,
		GETDISCO,
		CLOSE;

	public Packet getPacket(final String from, final String to,
		final StanzaType type, final String id) {
		Element elem = new Element(this.toString());
		elem.setAttribute("type", type.toString());
		elem.setAttribute("from", from);
		elem.setAttribute("to", to);
		elem.setAttribute("id", id);
		elem.setXMLNS("tigase:command");
		return new Packet(elem);
	}

	public Packet getPacket(final String from, final String to,
		final StanzaType type, final String id, final String cdata) {
		Packet packet = getPacket(from, to, type, id);
		if (cdata != null) {
			packet.getElement().setCData(cdata);
		} // end of if (cdata != null)
		return packet;
	}

} // Command
