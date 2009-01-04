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
 * Helper enum to make it easier to operate on packets with ad-hoc commands.
 * It allows to create a packet with command, add and retrieve command data
 * field values, set actions and so on.
 *
 * It contains predefined set of commands used internally by the Tigase server
 * and also 'OTHER' command which refers all other not predefined commands.
 *
 * Most of the implementation details, constants and parameters is based
 * on the <a href="http://xmpp.org/extensions/xep-0050.html">XEP-0050</a>
 * for ad-hoc commands protocol. Please refer to the XEP for more details.
 *
 * Created: Thu Feb  9 20:52:02 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public enum Command {

	/**
	 * Command sent from a connection manager to the session manager when a
	 * new stream from the client has been opened.
	 */
	STREAM_OPENED,
	/**
	 * Command sent from a connection manager to the session manager when a
	 * connection or stream has been closed.
	 */
	STREAM_CLOSED,
	/**
	 * Command sent from the session manager to a connection manager to start
	 * TLS handshaking over the client connection.
	 */
	STARTTLS,
	/**
	 * Command sent between a connection manager and the session manager to
	 * retrieve stream features.
	 */
	GETFEATURES,
	/**
	 * This is deprecated command sent between components in the Tigase server
	 * for service discovery handling.
	 */
	GETDISCO,
	/**
	 * Command sent from the session manager to a client manager to close the
	 * client connection.
	 */
	CLOSE,
	/**
	 * Command used by the StatisticsCollector to provide server statistics
	 * through ad-hoc command.
	 */
	GETSTATS,
	/**
	 * Command sent to the session manager from an external entity to activate
	 * a user session with the connection end-point at the given address.
	 */
	USER_STATUS,
	/**
	 * Command used to set a broadcast message to all online users.
	 */
	BROADCAST_TO_ONLINE,
	/**
	 * Command used to set a broadcast message to all registered local users.
	 */
	BROADCAST_TO_ALL,
	/**
	 * Command used to redirect packets from a connection manager to other
	 * than default session manager. (Mostly used in the clustering.)
	 */
	REDIRECT,
	/**
	 * Command sent to the VHostManager to reload virtual hosts from the database.
	 */
	VHOSTS_RELOAD,
	/**
	 * Command sent to the VHostManager to add or update existing virtual host.
	 */
	VHOSTS_UPDATE,
	/**
	 * Command sent to the VHostManager to remove existing virtual host.
	 */
	VHOSTS_REMOVE,
	/**
	 * Identifies all other, not predefined commands.
	 */
	OTHER;

	/**
	 * Ad-hoc command statuses as defined in the XEP-0050.
	 */
	public enum Status {
		/**
		 * The command is being executed.
		 */
		executing,
		/**
		 * The command has completed. The command session has ended.
		 */
		completed,
		/**
		 * The command has been canceled. The command session has ended.
		 */
		canceled,
		/**
		 * Other, not recognized command status.
		 */
		other;
	}

	/**
	 * Ad-hoc command actions ad defined in the XEP-0050.
	 */
	public enum Action {
		/**
		 * The command should be executed or continue to be executed.
		 * This is the default value.
		 */
		execute,
		/**
		 * The command should be canceled.
		 */
		cancel,
		/**
		 * The command should be digress to the previous stage of execution.
		 */
		prev,
		/**
		 * The command should progress to the next stage of execution.
		 */
		next,
		/**
		 * The command should be completed (if possible).
		 */
		complete,
		/**
		 * Other, not recognized command action.
		 */
		other;
	}

	/**
	 * Data form-types as defined in the XEP-0050.
	 */
	public enum DataType {
		/**
		 * This is a form with ad-hoc command result data.
		 */
		result,
		/**
		 * This is a form querying for more data from the user.
		 */
		form,
		/**
		 * Form filled with data sent as a response to 'form' request.
		 */
		submit;
	}

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log = Logger.getLogger("tigase.server.Command");

	public static final String XMLNS = "http://jabber.org/protocol/commands";
	public static final String COMMAND_EL = "command";

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
		final StanzaType type, final String id, final DataType data_type) {
		Element elem =
			createIqCommand(from, to, type, id, this.toString(), data_type);
		return new Packet(elem);
	}

	private static Element addDataForm(Element command, DataType data_type) {
		Element x = new Element("x",
						new String[]{"xmlns", "type"},
						new String[]{"jabber:x:data", data_type.name()});
		command.addChild(x);
		return x;
	}

	private static void setStatusEl(Element iq, Status status) {
		Element command = iq.getChild(COMMAND_EL);
		command.setAttribute("status", status.name());
	}

	private static void addActionEl(Element iq, Action action) {
		Element command = iq.getChild(COMMAND_EL);
		Element actions = command.getChild("actions");
		if (actions == null) {
			actions = new Element("actions",
				new String[] {Action.execute.toString()},
				new String[] {action.toString()});
			command.addChild(actions);
		}
		actions.addChild(new Element(action.toString()));

	}

	private static Element createCommandEl(String from, String to,
					StanzaType type, String id, String node, DataType data_type) {
		Element iq = new Element("iq",
			new String[] {"type", "id"},
			new String[] {type.toString(), id});
		if (from != null) {
			iq.setAttribute("from", from);
		}
		if (to != null) {
			iq.setAttribute("to", to);
		}
		Element command = new Element(COMMAND_EL,
			new String[] {"xmlns", "node"},
			new String[] {XMLNS, node});
		iq.addChild(command);
		if (data_type != null) {
			addDataForm(command, data_type);
			if (data_type == DataType.result) {
				setStatusEl(iq, Status.completed);
			}
			if (data_type == DataType.form) {
				setStatusEl(iq, Status.executing);
				addActionEl(iq, Action.complete);
			}
		}
		return iq;
	}

	public static Element createIqCommand(final String from, final String to,
		final StanzaType type, final String id,	final String node,
		final DataType data_type) {
		Element iq = createCommandEl(from, to, type, id, node, data_type);
		return iq;
	}

	public static void setStatus(final Packet packet, final Status status) {
		Element iq = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		command.setAttribute("status", status.toString());
	}

	public static void addAction(final Packet packet, final Action action) {
		addActionEl(packet.getElement(), action);
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
		Element command = iq.getChild(COMMAND_EL);
		Element notes = command.getChild("note");
		if (notes == null) {
			notes = new Element("note",
				new String[] {"type"},
				new String[] {"info"});
			command.addChild(notes);
		}
		notes.setCData(note);
	}

	/**
	 * Simple method for adding a new field to the command data form. Only field
	 * name (variable name) and field default value can be set.
	 *
	 * @param packet is a <code>Packet</code> instance of the ad-hoc command
	 * request to be modified.
	 * @param f_name is a <code>String</code> instance with the field name.
	 * In ad-hoc command terms this is a variable name. This field name (variable
	 * name) will be also displayed as the field label.
	 * @param f_value is a <code>String</code> instance with the field default
	 * value.
	 */
	public static void addFieldValue(final Packet packet,
		final String f_name, final String f_value) {
		Element iq = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		Element x = command.getChild("x", "jabber:x:data");
		if (x == null) {
			x = addDataForm(command, DataType.submit);
		}
		Element field = new Element("field",
			new Element[] {new Element("value", f_value)},
			new String[] {"var"},
			new String[] {f_name});
		x.addChild(field);
	}

	/**
	 * A simple method for adding a multi-line (text-multi) data field to the
	 * command data form. Only field name (variable name) and field default
	 * value can be set.
	 *
	 * @param packet is a <code>Packet</code> instance of the ad-hoc command
	 * request to be modified.
	 * @param f_name is a <code>String</code> instance with the field name.
	 * In ad-hoc command terms this is a variable name. This field name (variable
	 * name) will be also displayed as the field label.
	 * @param f_value is a list with lines of text to be displayed as a
	 * multi-line field content.
	 */
	public static void addFieldMultiValue(final Packet packet,
		final String f_name, final List<String> f_value) {
		Element iq = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		Element x = command.getChild("x", "jabber:x:data");
		if (x == null) {
			x = addDataForm(command, DataType.submit);
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

	/**
	 * This method allows to add a new multi-option-select-one data field to
	 * the command data form. This is much more complex implementation allowing
	 * to set a field label and labels for all provided field options.
	 * It allows the end-user to select a single option from a given list.
	 *
	 * @param packet is a <code>Packet</code> instance of the ad-hoc command
	 * request to be modified.
	 * @param f_name is a <code>String</code> instance with the field name.
	 * In ad-hoc command terms this is a variable name.
	 * @param f_value is a <code>String</code> instance with the field default
	 * value. It must match one of the options vaulues provided as a list in
	 * 'options' parameter.
	 * @param label is a <code>String</code> instance with the field label.
	 * This time a label set here is displayed to the user instead of the field
	 * name (variable name). This is useful if the variable name is not suitable
	 * or clear enough to the end-user.
	 * @param labels is an array with options labels which are displayed to the
	 * end-user upon presenting the selection options.
	 * @param options is an array with options values to be selected by the
	 * end-user. Normally these values are not displayed to the end-user. Only
	 * options labels are.
	 */
	public static void addFieldValue(final Packet packet,
		final String f_name, final String f_value, final String label,
		final String[] labels, final String[] options) {
		Element iq = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		Element x = command.getChild("x", "jabber:x:data");
		if (x == null) {
			x = addDataForm(command, DataType.submit);
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

	/**
	 * This method allows to add a new multi-option-select-many data field to
	 * the command data form. This is much more complex implementation allowing
	 * to set a field label and labels for all provided field options.
	 * It allows the end-user to select many options from the given list.
	 *
	 * @param packet is a <code>Packet</code> instance of the ad-hoc command
	 * request to be modified.
	 * @param f_name is a <code>String</code> instance with the field name.
	 * In ad-hoc command terms this is a variable name.
	 * @param f_values is an array of default values which are presented to the
	 * end user as preselected options. They must match options vaulues provided
	 * as a list in 'options' parameter.
	 * @param label is a <code>String</code> instance with the field label.
	 * This time a label set here is displayed to the user instead of the field
	 * name (variable name). This is useful if the variable name is not suitable
	 * or clear enough to the end-user.
	 * @param labels is an array with options labels which are displayed to the
	 * end-user upon presenting the selection options.
	 * @param options is an array with options values to be selected by the
	 * end-user. Normally these values are not displayed to the end-user. Only
	 * options labels are.
	 */
	public static void addFieldValue(final Packet packet,
		final String f_name, final String[] f_values, final String label,
		final String[] labels, final String[] options) {
		Element iq = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		Element x = command.getChild("x", "jabber:x:data");
		if (x == null) {
			x = addDataForm(command, DataType.submit);
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
		Element command = iq.getChild(COMMAND_EL);
		Element x = command.getChild("x", "jabber:x:data");
		if (x == null) {
			x = addDataForm(command, DataType.submit);
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

	public static void addTextField(final Packet packet, final String t_name,
					final String t_value) {
		addFieldValue(packet, t_name, t_value, "fixed");
	}

	public static void addFieldValue(final Packet packet,
		final String f_name, final String f_value, final String type) {
		Element iq = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		Element x = command.getChild("x", "jabber:x:data");
		if (x == null) {
			x = addDataForm(command, DataType.submit);
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
		Element command = iq.getChild(COMMAND_EL);
		Element x = command.getChild("x", "jabber:x:data");
		if (x == null) {
			x = addDataForm(command, DataType.submit);
		}
		Element field = new Element("field",
			new Element[] {new Element("value", f_value)},
			new String[] {"var", "type", "label"},
			new String[] {f_name, type, label});
		x.addChild(field);
	}

	public static void setData(final Packet packet, final Element data) {
		Element iq = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		command.addChild(data);
	}

	public static void setData(final Packet packet,
		final List<Element> data) {
		Element iq = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		command.addChildren(data);
	}

	public static String getFieldValue(final Packet packet,
		final String f_name) {
		Element iq = packet.getElement();
		Element command = iq.getChild(COMMAND_EL, XMLNS);
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
		Element command = iq.getChild(COMMAND_EL, XMLNS);
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
		Element command = iq.getChild(COMMAND_EL, XMLNS);
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
		Element command = iq.getChild(COMMAND_EL, XMLNS);
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
		Element command = iq.getChild(COMMAND_EL);
		return command.getChildren();
	}

	public static Element getData(final Packet packet, final String el_name,
		final String xmlns) {
		Element iq = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		return command.getChild(el_name, xmlns);
	}

} // Command
