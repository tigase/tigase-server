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
package tigase.server;

import java.util.List;
import java.util.LinkedList;
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
	BROADCAST_TO_ONLINE,
	BROADCAST_TO_ALL,
	REDIRECT,
	VHOSTS_RELOAD,
	VHOSTS_UPDATE,
	VHOSTS_REMOVE,
	OTHER;

	public enum Status {
		executing,
		completed,
		canceled,
		other;
	}

	public enum Action {
		execute,
		cancel,
		prev,
		next,
		complete,
		other;
	}

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log = Logger.getLogger("tigase.server.Command");

	public static final String XMLNS = "http://jabber.org/protocol/commands";

	public static Command valueof(String cmd) {
		try {
			return Command.valueOf(cmd);
		} catch (Exception e) {
			return OTHER;
		} // end of try-catch
	}

	public Packet getPacket(final String from, final String to,
		final StanzaType type, final String id) {
		Element elem =
			createIqCommand(from, to, type, id, this.toString(), null);
		return new Packet(elem);
	}

	public Packet getPacket(final String from, final String to,
		final StanzaType type, final String id, final String data_type) {
		Element elem =
			createIqCommand(from, to, type, id, this.toString(), data_type);
		return new Packet(elem);
	}

	public static Element createIqCommand(final String from, final String to,
		final StanzaType type, final String id,	final String node,
		final String data_type) {
		Element iq = new Element("iq",
			new String[] {"from", "to", "type", "id"},
			new String[] {from, to, type.toString(), id});
		Element command = new Element("command",
			new String[] {"xmlns", "node"},
			new String[] {XMLNS, node});
		if (data_type != null) {
			Element x = new Element("x",
				new String[] {"xmlns", "type"},
				new String[] {"jabber:x:data", data_type});
			command.addChild(x);
			if (data_type.equals("result")) {
				command.setAttribute("status", Status.completed.toString());
			}
		}
		iq.addChild(command);
		return iq;
	}

	public static void setStatus(final Packet packet, final Status status) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command");
		command.setAttribute("status", status.toString());
	}

	public static void addAction(final Packet packet, final Action action) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command");
		Element actions = command.getChild("actions");
		if (actions == null) {
			actions = new Element("actions",
				new String[] {Action.execute.toString()},
				new String[] {action.toString()});
			command.addChild(actions);
		}
		actions.addChild(new Element(action.toString()));
	}

	public static Action getAction(final Packet packet) {
		String action = packet.getElement().getAttribute("/iq/command", "action");
		try {
			return Action.valueOf(action);
		} catch (Exception e) {
			return Action.other;
		}
	}

	public static void addNote(final Packet packet, final String note) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command");
		Element notes = command.getChild("note");
		if (notes == null) {
			notes = new Element("note",
				new String[] {"type"},
				new String[] {"info"});
			command.addChild(notes);
		}
		notes.setCData(note);
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

	public static void addFieldMultiValue(final Packet packet,
		final String f_name, final List<String> f_value) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command");
		Element x = command.getChild("x", "jabber:x:data");
		if (x == null) {
			x = new Element("x",
				new String[] {"xmlns", "type"},
				new String[] {"jabber:x:data", "submit"});
			command.addChild(x);
		}
		if (f_value != null && f_value.size() > 0) {
			Element field = new Element("field",
				new String[] {"var", "type"},
				new String[] {f_name, "text-multi"});
			for (String val: f_value) {
				Element value = new Element("value", val);
				field.addChild(value);
			}
			x.addChild(field);
		}
	}

	public static void addFieldValue(final Packet packet,
		final String f_name, final String f_value, final String label,
		final String[] labels, final String[] options) {
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
			new String[] {"var", "type", "label"},
			new String[] {f_name, "list-single", label});
		for (int i = 0; i < labels.length; i++) {
			field.addChild(new Element("option",
					new Element[] {new Element("value", options[i])},
					new String[] {"label"},
					new String[] {labels[i]}));
		}
		x.addChild(field);
	}

	public static void addFieldValue(final Packet packet,
		final String f_name, final String[] f_values, final String label,
		final String[] labels, final String[] options) {
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
			new String[] {"var", "type", "label"},
			new String[] {f_name, "list-multi", label});
		for (int i = 0; i < labels.length; i++) {
			field.addChild(new Element("option",
					new Element[] {new Element("value", options[i])},
					new String[] {"label"},
					new String[] {labels[i]}));
		}
		for (int i = 0; i < f_values.length; i++) {
			field.addChild(new Element("value", f_values[i]));
		}

		x.addChild(field);
	}

	public static void addFieldValue(final Packet packet,
		final String f_name, final String f_value, final String label,
		final String[] labels, final String[] options, final String type) {
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
			new String[] {"var", "type", "label"},
			new String[] {f_name, type, label});
		for (int i = 0; i < labels.length; i++) {
			field.addChild(new Element("option",
					new Element[] {new Element("value", options[i])},
					new String[] {"label"},
					new String[] {labels[i]}));
		}
		x.addChild(field);
	}

	public static void addFieldValue(final Packet packet,
		final String f_name, final String f_value, final String type) {
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
			new String[] {"var", "type"},
			new String[] {f_name, type});
		x.addChild(field);
	}

	public static void addFieldValue(final Packet packet,
		final String f_name, final String f_value,
		final String type, final String label) {
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
			new String[] {"var", "type", "label"},
			new String[] {f_name, type, label});
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
		if (x != null) {
			List<Element> children = x.getChildren();
			if (children != null) {
				for (Element child: children) {
					if (child.getName().equals("field")
						&& child.getAttribute("var").equals(f_name)) {
						return child.getChildCData("/field/value");
					}
				}
			}
		}
		return null;
	}

	public static String[] getFieldValues(final Packet packet,
		final String f_name) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command", XMLNS);
		Element x = command.getChild("x", "jabber:x:data");
		if (x != null) {
			List<Element> children = x.getChildren();
			if (children != null) {
				for (Element child: children) {
					if (child.getName().equals("field")
						&& child.getAttribute("var").equals(f_name)) {
						List<String> values = new LinkedList<String>();
						List<Element> val_children = child.getChildren();
						for (Element val_child: val_children) {
							if (val_child.getName().equals("value")) {
								values.add(val_child.getCData());
							}
						}
						return values.toArray(new String[0]);
					}
				}
			}
		}
		return null;
	}

	public static boolean removeFieldValue(final Packet packet,
		final String f_name) {
		Element iq = packet.getElement();
		Element command = iq.getChild("command", XMLNS);
		Element x = command.getChild("x", "jabber:x:data");
		if (x != null) {
			List<Element> children = x.getChildren();
			if (children != null) {
				for (Element child: children) {
					if (child.getName().equals("field")
						&& child.getAttribute("var").equals(f_name)) {
						return x.removeChild(child);
					}
				}
			}
		}
		return false;
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
