/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
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

/**
 * Class Packet
 *
 * Represent one XMPP packet.
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Packet {

	private Element elem = null;
	private String to = null;
	private String from = null;

  public Packet(Element elem) {
		if (elem == null) {
			throw new NullPointerException();
		} // end of if (elem == null)
		this.elem = elem;
	}

	public String getTo() {
		return to != null ? to : getElemTo();
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getFrom() {
		return from != null ? from : getElemFrom();
	}

	public void setFrom(String from) {
		this.from = from;
	}

	/**
   * Returns packet destination address.
   */
  public String getElemTo() {
    return elem.getAttribute("to");
  }

  /**
   * Returns packet source address.
   */
  public String getElemFrom() {
    return elem.getAttribute("from");
  }

  public byte[] getByteData() {
    return elem.toString().getBytes();
  }

  public String getStringData() {
    return elem.toString();
  }

  public char[] getCharData() {
    return elem.toString().toCharArray();
  }

	public boolean isRouted() {
		return elem.getName().equals("route");
	}

	public Packet unpackRouted() {
		return new Packet(elem.getChildren().get(0));
	}

	public Packet packRouted(String to, String from) {
		Element routed = new Element("route", null, new String[] {"to", "from"},
			new String[] {to, from});
		routed.addChild(elem);
		return new Packet(routed);
	}

	public Packet packRouted() {
		Element routed = new Element("route", null, new String[] {"to", "from"},
			new String[] {getTo(), getFrom()});
		routed.addChild(elem);
		return new Packet(routed);
	}

	public Packet swapFromTo(Element el) {
		Packet packet = new Packet(el);
		packet.setTo(getFrom());
		packet.setFrom(getTo());
		return packet;
	}

	public Packet swapElemFromTo() {
		Element copy = (Element)elem.clone();
		copy.setAttribute("to", getElemFrom());
		copy.setAttribute("from", getElemTo());
		return new Packet(copy);
	}

//   public PacketType getType() {
//     return null;
//   }

}
