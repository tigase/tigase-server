/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

import java.util.List;
import java.util.logging.Logger;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;

/**
 * Describe enum Command here.
 *
 *
 * Created: Thu Feb  9 20:52:02 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public enum Command {

	STREAM_OPENED,
		STREAM_CLOSED,
		STARTTLS,
		GETFEATURES,
		GETDISCO,
		CLOSE,
		GETSTATS,
		USER_STATUS,
		OTHER;

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log = Logger.getLogger("tigase.server.Command");

	public static final String XMLNS = "http://jabber.org/protocol/commands";

	public static Command valueof(String cmd) {
		try {
			return Command.valueOf(cmd);
		} catch (IllegalArgumentException e) {
			return OTHER;
		} // end of try-catch
	}

	public Packet getPacket(final String from, final String to,
		final StanzaType type, final String id) {
		Element elem = createIqCommand(from, to, type, id, this.toString());
		return new Packet(elem);
	}

	public static Element createIqCommand(final String from, final String to,
		final StanzaType type, final String id,	final String node) {
		Element iq = new Element("iq",
			new String[] {"from", "to", "type", "id"},
			new String[] {from, to, type.toString(), id});
		Element command = new Element("command",
			new String[] {"xmlns", "node"},
			new String[] {XMLNS, node});
		iq.addChild(command);
		return iq;
	}

	public static void addFieldValue(final Packet packet,
		final String f_name, final String f_value) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command");
		Element x = command.getChild("x", "jabber:x:data");
		if (x == null) {
			x = new Element("x",
				new String[] {"xmlns", "type"},
				new String[] {"jabber:x:data", "submit"});
			command.addChild(x);
		}
		Element field = new Element("field",
			new Element[] {new Element("value", f_value)},
			new String[] {"var"},
			new String[] {f_name});
		x.addChild(field);
	}

	public static void setData(final Packet packet, final Element data) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command");
		command.addChild(data);
	}

	public static void setData(final Packet packet,
		final List<Element> data) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command");
		command.addChildren(data);
	}

	public static String getFieldValue(final Packet packet,
		final String f_name) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command", XMLNS);
		Element x = command.getChild("x", "jabber:x:data");
		if (x == null) {
			return null;
		}
		List<Element> children = x.getChildren();
		for (Element child: children) {
			if (child.getName().equals("field")
				&& child.getAttribute("var").equals(f_name)) {
				return child.getChildCData("/field/value");
			}
		}
		return null;
	}

	public static String getFieldValue(final Packet packet,
		final String f_name, boolean debug) {
		Element iq = packet.getElement();
		log.info("Command iq: " + iq.toString());
		Element command = iq.getChild("command", XMLNS);
		log.info("Command command: " + command.toString());
		Element x = command.getChild("x", "jabber:x:data");
		if (x == null) {
			log.info("Command x: NULL");
			return null;
		}
		log.info("Command x: " + x.toString());
		List<Element> children = x.getChildren();
		for (Element child: children) {
			log.info("Command form child: " + child.toString());
			if (child.getName().equals("field")
				&& child.getAttribute("var").equals(f_name)) {
				log.info("Command found: field=" + f_name
					+ ", value=" + child.getChildCData("/field/value"));
				return child.getChildCData("value");
			} else {
				log.info("Command not found: field=" + f_name
					+ ", value=" + child.getChildCData("/field/value"));
			}
		}
		return null;
	}

	public static List<Element> getData(final Packet packet) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command");
		return command.getChildren();
	}

	public static Element getData(final Packet packet, final String el_name,
		final String xmlns) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command");
		return command.getChild(el_name, xmlns);
	}

} // Command
