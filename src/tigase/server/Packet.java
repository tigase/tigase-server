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
import tigase.xmpp.IqType;

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

  private static final String ERROR_NS = "urn:ietf:params:xml:ns:xmpp-stanzas";

	private final Element elem;
	private final Command command;
	private final IqType type;
	private final boolean routed;
	private String to = null;
	private String from = null;

  public Packet(final Element elem) {
		if (elem == null) {
			throw new NullPointerException();
		} // end of if (elem == null)
		this.elem = elem;
		if (elem.getXMLNS() != null && elem.getXMLNS().equals("tigase:command")) {
			command = Command.valueOf(elem.getName());
		} else {
			command = null;
		} // end of else
		if (elem.getAttribute("type") != null) {
			type = IqType.valueOf(elem.getAttribute("type"));
		} // end of if (elem.getAttribute("type") != null)
		else {
			type = null;
		} // end of if (elem.getAttribute("type") != null) else
		if (elem.getName().equals("route")) {
			routed = true;
		} // end of if (elem.getName().equals("route"))
		else {
			routed = false;
		} // end of if (elem.getName().equals("route")) else
	}

	public Command getCommand() {
		return command;
	}

	public IqType getType() {
		return type;
	}

	public Element getElement() {
		return elem;
	}

	public boolean isCommand() {
		return command != null;
	}

	public String getTo() {
		return to != null ? to : getElemTo();
	}

	public void setTo(final String to) {
		this.to = to;
	}

	public String getFrom() {
		return from != null ? from : getElemFrom();
	}

	public void setFrom(final String from) {
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

	public String getElemId() {
    return elem.getAttribute("id");
	}

	public String getElemCData(final String path) {
		return elem.getCData(path);
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
		return routed;
	}

	public Packet unpackRouted() {
		Packet result = new Packet(elem.getChildren().get(0));
		if (result.getTo() == null) {
			result.setTo(getTo());
		} // end of if (result.getTo() == null)
		return result;
	}

	public Packet packRouted(final String to, final String from) {
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

	public Packet swapFromTo(final Element el) {
		Packet packet = new Packet(el);
		packet.setTo(getFrom());
		packet.setFrom(getTo());
		return packet;
	}

	public Packet commandResult(final String data) {
		Packet result = command.getPacket(getTo(), getFrom(),
			IqType.result, elem.getAttribute("id"));
		result.getElement().setCData(data);
		return result;
	}

	public Packet errorResult(final String errorType, final String errorCondition,
		final String errorText, final boolean includeOriginalXML) {
		Element reply = new Element(elem.getName());
		reply.setAttribute("type", IqType.error.toString());
		if (getElemFrom() != null) {
			reply.setAttribute("to", getElemFrom());
		} // end of if (getElemFrom() != null)
		if (getElemId() != null) {
			reply.setAttribute("id", getElemId());
		} // end of if (getElemId() != null)
		if (includeOriginalXML) {
			reply.addChildren(elem.getChildren());
		} // end of if (includeOriginalXML)
		Element error = new Element("error");
		error.setAttribute("type", errorType);
		Element cond = new Element(errorCondition);
		cond.setXMLNS(ERROR_NS);
		error.addChild(cond);
		if (errorText != null && errorText.length() > 0) {
			Element t = new Element("text");
			t.setAttribute("xml:lang", "en");
			t.setXMLNS(ERROR_NS);
			t.setCData(errorText);
			error.addChild(t);
		} // end of if (text != null && text.length() > 0)
		reply.addChild(error);
		return swapFromTo(reply);
	}

	public Packet okResult(final String includeXML) {
		Element reply = new Element(elem.getName());
		reply.setAttribute("type", IqType.result.toString());
		if (getElemFrom() != null) {
			reply.setAttribute("to", getElemFrom());
		} // end of if (getElemFrom() != null)
		if (getElemId() != null) {
			reply.setAttribute("id", getElemId());
		} // end of if (getElemId() != null)
		if (includeXML != null) {
			reply.setCData(includeXML);
		} // end of if (includeOriginalXML)
		return swapFromTo(reply);
	}

	public Packet swapElemFromTo() {
		Element copy = (Element)elem.clone();
		copy.setAttribute("to", getElemFrom());
		copy.setAttribute("from", getElemTo());
		return new Packet(copy);
	}

}
