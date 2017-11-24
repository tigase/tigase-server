/*
 * Command.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

import java.util.List;
import java.util.logging.Logger;

//~--- JDK imports ------------------------------------------------------------

/**
 * Helper enum to make it easier to operate on packets with ad-hoc commands. It
 * allows to create a packet with command, add and retrieve command data field
 * values, set actions and so on.
 *
 * It contains predefined set of commands used internally by the Tigase server
 * and also 'OTHER' command which refers all other not predefined commands.
 *
 * Most of the implementation details, constants and parameters is based on the
 * <a href="http://xmpp.org/extensions/xep-0050.html">XEP-0050</a> for ad-hoc
 * commands protocol. Please refer to the XEP for more details.
 *
 * Created: Thu Feb 9 20:52:02 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public enum Command {

	/**
	 * Command sent from a connection manager to the session manager when a new
	 * stream from the client has been opened.
	 */
	STREAM_OPENED(Priority.SYSTEM),

	/**
	 * Command sent from connection manager to the session manager after TLS handshake if
	 * client sent certificate.
	 */
	TLS_HANDSHAKE_COMPLETE(Priority.SYSTEM),

	/**
	 * Command sent from session manager to the connection manager after successful
	 * user login.
	 */
	USER_LOGIN(Priority.SYSTEM),

	/**
	 * Command sent from a connection manager to the session manager when a
	 * connection or stream has been closed.
	 */
	STREAM_CLOSED(Priority.SYSTEM), STREAM_CLOSED_UPDATE(Priority.SYSTEM),
	
	/**
	 * Command sent from a connection manager to the session manager after
	 * last packet from closed connection stream has been sent.
	 */
	STREAM_FINISHED(Priority.NORMAL),

	/**
	 * Sends a command from SM to the connection holder to confirm whether the
	 * connection is still active. Expects result for ok, error or timeout if the
	 * connection is no longer active.
	 */
	CHECK_USER_CONNECTION(Priority.SYSTEM),

	/**
	 * Command sent from the session manager to a connection manager to start TLS
	 * handshaking over the client connection.
	 */                                                                                                                                                                  	
	STARTTLS(Priority.NORMAL),

	/**
	 * Command sent from the session manager to a connection manager to start zlib
	 * compression on the connection stream.
	 */
	STARTZLIB(Priority.NORMAL),

	/**
	 * Command sent between a connection manager and the session manager to
	 * retrieve stream features.
	 */
	GETFEATURES(Priority.HIGH),

	/**
	 * This is depreciated command sent between components in the Tigase server
	 * for service discovery handling.
	 */
	GETDISCO(Priority.HIGH),

	/**
	 * Command sent from the session manager to a client manager to close the
	 * client connection.
	 */
	CLOSE(Priority.SYSTEM),

	/**
	 * Command used by the StatisticsCollector to provide server statistics
	 * through ad-hoc command.
	 */
	GETSTATS(Priority.HIGH),

	/**
	 * Command sent to the session manager from an external entity to activate a
	 * user session with the connection end-point at the given address.
	 */
	USER_STATUS(Priority.NORMAL),

	/**
	 * Command used to set a broadcast message to all online users.
	 */
	BROADCAST_TO_ONLINE(Priority.NORMAL),

	/**
	 * Command used to set a broadcast message to all registered local users.
	 */
	BROADCAST_TO_ALL(Priority.NORMAL),

	/**
	 * Command used to redirect packets from a connection manager to other than
	 * default session manager. (Mostly used in the clustering.)
	 */
	REDIRECT(Priority.SYSTEM),

	/**
	 * Command sent to the VHostManager to reload virtual hosts from the database.
	 */
	VHOSTS_RELOAD(Priority.NORMAL),

	/**
	 * Command sent to the VHostManager to add or update existing virtual host.
	 */
	VHOSTS_UPDATE(Priority.NORMAL),

	/**
	 * Command sent to the VHostManager to remove existing virtual host.
	 */
	VHOSTS_REMOVE(Priority.NORMAL),

	/**
	 * Command sent to SessionManager to change connectionId of existing session.
	 */
	STREAM_MOVED(Priority.NORMAL),
	
	/**
	 * Identifies all other, not predefined commands.
	 */
	OTHER(Priority.NORMAL);

	/** Field description */
	public static final String COMMAND_EL = "command";

	/** Field description */
	@Deprecated
	public static final String FIELD_EL = DataForm.FIELD_EL;

	/** Field description */
	@Deprecated
	public static final String VALUE_EL = DataForm.VALUE_EL;

	/** Field description */
	public static final String XMLNS = "http://jabber.org/protocol/commands";

	/** Field description */
	@Deprecated
	protected static final String[] FIELD_VALUE_PATH = DataForm.FIELD_VALUE_PATH;

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger("tigase.server.Command");

	//~--- fields ---------------------------------------------------------------

	private Priority priority = Priority.NORMAL;

	//~--- constant enums -------------------------------------------------------

	/**
	 * Ad-hoc command actions ad defined in the XEP-0050.
	 */
	public enum Action {

		/**
		 * The command should be executed or continue to be executed. This is the
		 * default value.
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

	//~--- constructors ---------------------------------------------------------

	private Command(Priority priority) {
		this.priority = priority;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param action
	 */
	public static void addAction(final Packet packet, final Action action) {
		addActionEl(packet.getElement(), action);
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param f_name
	 * @param f_value
	 */
	public static void addCheckBoxField(Packet packet, String f_name, boolean f_value) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		
		DataForm.addCheckBoxField( command, f_name, f_value);
	}

	/**
	 * A simple method for adding a multi-line (text-multi) data field to the
	 * command data form. Only field name (variable name) and field default value
	 * can be set.
	 *
	 * @param packet
	 *          is a <code>Packet</code> instance of the ad-hoc command request to
	 *          be modified.
	 * @param f_name
	 *          is a <code>String</code> instance with the field name. In ad-hoc
	 *          command terms this is a variable name. This field name (variable
	 *          name) will be also displayed as the field label.
	 * @param f_value
	 *          is a list with lines of text to be displayed as a multi-line field
	 *          content.
	 */
	public static void addFieldMultiValue(final Packet packet, final String f_name,
					final List<String> f_value) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addFieldMultiValue( command, f_name, f_value);

	}

	public static void addFieldMultiValue(final Packet packet, final String f_name,
					final Throwable ex) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addFieldMultiValue( command, f_name, ex);

	}

	/**
	 * Simple method for adding a new field to the command data form. Only field
	 * name (variable name) and field default value can be set.
	 *
	 * @param packet
	 *          is a <code>Packet</code> instance of the ad-hoc command request to
	 *          be modified.
	 * @param f_name
	 *          is a <code>String</code> instance with the field name. In ad-hoc
	 *          command terms this is a variable name. This field name (variable
	 *          name) will be also displayed as the field label.
	 * @param f_value
	 *          is a <code>String</code> instance with the field default value.
	 */
	public static void addFieldValue(final Packet packet, final String f_name,
																	 final String f_value) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addFieldValue( command, f_name, f_value);
	}

	/**
	 * This method allows to add a new multi-option-select-one data field to the
	 * command data form. This is much more complex implementation allowing to set
	 * a field label and labels for all provided field options. It allows the
	 * end-user to select a single option from a given list.
	 *
	 * @param packet
	 *          is a <code>Packet</code> instance of the ad-hoc command request to
	 *          be modified.
	 * @param f_name
	 *          is a <code>String</code> instance with the field name. In ad-hoc
	 *          command terms this is a variable name.
	 * @param f_value
	 *          is a <code>String</code> instance with the field default value. It
	 *          must match one of the options vaulues provided as a list in
	 *          'options' parameter.
	 * @param label
	 *          is a <code>String</code> instance with the field label. This time
	 *          a label set here is displayed to the user instead of the field
	 *          name (variable name). This is useful if the variable name is not
	 *          suitable or clear enough to the end-user.
	 * @param labels
	 *          is an array with options labels which are displayed to the
	 *          end-user upon presenting the selection options.
	 * @param options
	 *          is an array with options values to be selected by the end-user.
	 *          Normally these values are not displayed to the end-user. Only
	 *          options labels are.
	 */
	public static void addFieldValue(final Packet packet, final String f_name,
																	 final String f_value, final String label,
																	 final String[] labels, final String[] options) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addFieldValue( command, f_name, f_value, label, labels, options);
	}

	/**
	 * This method allows to add a new multi-option-select-many data field to the
	 * command data form. This is much more complex implementation allowing to set
	 * a field label and labels for all provided field options. It allows the
	 * end-user to select many options from the given list.
	 *
	 * @param packet
	 *          is a <code>Packet</code> instance of the ad-hoc command request to
	 *          be modified.
	 * @param f_name
	 *          is a <code>String</code> instance with the field name. In ad-hoc
	 *          command terms this is a variable name.
	 * @param f_values
	 *          is an array of default values which are presented to the end user
	 *          as preselected options. They must match options vaulues provided
	 *          as a list in 'options' parameter.
	 * @param label
	 *          is a <code>String</code> instance with the field label. This time
	 *          a label set here is displayed to the user instead of the field
	 *          name (variable name). This is useful if the variable name is not
	 *          suitable or clear enough to the end-user.
	 * @param labels
	 *          is an array with options labels which are displayed to the
	 *          end-user upon presenting the selection options.
	 * @param options
	 *          is an array with options values to be selected by the end-user.
	 *          Normally these values are not displayed to the end-user. Only
	 *          options labels are.
	 */
	public static void addFieldValue(final Packet packet, final String f_name,
																	 final String[] f_values, final String label,
																	 final String[] labels, final String[] options) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addFieldValue( command, f_name, f_values, label, labels, options);
	}

	public static void addFieldValue(final Packet packet, final String f_name,
																	 final String f_value, final String label,
																	 final String[] labels, final String[] options,
																	 final String type) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addFieldValue( command, f_name, f_value, label, labels, options, type);
	}

	public static void addFieldValue(final Packet packet, final String f_name,
																	 final String f_value, final String type) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addFieldValue( command, f_name, f_value, type );
	}

	public static void addFieldValue(final Packet packet, final String f_name,
																	 final String f_value, final String type,
																	 final String label) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addFieldValue( command, f_name, f_value, type, label);
	}

	public static void addHiddenField(Packet packet, String f_name, String f_value) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addFieldValue(command, f_name, f_value, "hidden");
	}

	public static void addInstructions(final Packet packet, final String instructions) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addInstructions( command, instructions);
	}

	public static void addNote(final Packet packet, final String note) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		Element notes   = command.getChild("note");

		if (notes == null) {
			notes = new Element("note", new String[] { "type" }, new String[] { "info" });
			command.addChild(notes);
		}
		notes.setCData(XMLUtils.escape(note));
	}

	public static void addTextField(Packet packet, String f_name, String f_value) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addFieldValue(command, f_name, f_value, "fixed");
	}

	public static void addTitle(final Packet packet, final String title) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);
		DataForm.addTitle( command, title);
	}

	public static Element createIqCommand(JID from, JID to, final StanzaType type, final String id, final String node,
										  final DataType data_type) {
		Element iq = createIqCommand(from, to, type, id, node, data_type, Iq.CLIENT_XMLNS);

		return iq;
	}

	private static Element createIqCommand(JID from, JID to, final StanzaType type, final String id, final String node,
										   final DataType data_type, String xmlns) {
		Element iq = createIqCommandEl(from, to, type, id, node, data_type, xmlns);

		return iq;
	}

	public static Action getAction(final Packet packet) {
		String action = packet.getAttributeStaticStr(Iq.IQ_COMMAND_PATH, "action");

		try {
			return Action.valueOf(action);
		} catch (Exception e) {
			return Action.other;
		}
	}

	public static boolean getCheckBoxFieldValue(Packet packet, String f_name) {
		String result = getFieldValue(packet, f_name);

		if (result == null) {
			return false;
		}
		result = result.trim();

		return result.equalsIgnoreCase("true") || result.equals("1");
	}

	public static List<Element> getData(final Packet packet) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);

		return command.getChildren();
	}

	public static Element getData(final Packet packet, final String el_name,
																final String xmlns) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);

		return command.getChild(el_name, xmlns);
	}

	public static String getFieldValue(Packet packet, String f_name) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL, XMLNS);
		Element x       = command.getChild("x", "jabber:x:data");

		if (x != null) {
			List<Element> children = x.getChildren();

			if (children != null) {
				for (Element child : children) {
					if (child.getName().equals(FIELD_EL) &&
							child.getAttributeStaticStr("var").equals(f_name)) {
						String value = child.getChildCDataStaticStr(FIELD_VALUE_PATH);

						if (value != null) {
							return XMLUtils.unescape(value);
						}
					}
				}
			}
		}

		return null;
	}

	public static String getFieldValue(final Packet packet, final String f_name,
																		 boolean debug) {
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

		for (Element child : children) {
			log.info("Command form child: " + child.toString());
			if (child.getName().equals(FIELD_EL) &&
					child.getAttributeStaticStr("var").equals(f_name)) {
				String value = child.getChildCDataStaticStr(FIELD_VALUE_PATH);

				log.info("Command found: field=" + f_name + ", value=" + value);
				if (value != null) {
					return XMLUtils.unescape(value);
				}
			} else {
				log.info("Command not found: field=" + f_name + ", value=" +
								 child.getChildCDataStaticStr(FIELD_VALUE_PATH));
			}
		}

		return null;
	}

	public static String[] getFieldValues(final Packet packet, final String f_name) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL, XMLNS);

		return DataForm.getFieldValues( command, f_name);
	}

	public static boolean removeFieldValue(final Packet packet, final String f_name) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL, XMLNS);

		return DataForm.removeFieldValue( command, f_name);
	}

	public static String getFieldKeyStartingWith(Packet packet, String f_name) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL, XMLNS);

		return DataForm.getFieldKeyStartingWith( command, f_name);
	}

	public static void setData(final Packet packet, final Element data) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);

		command.addChild(data);
	}

	public static void setData(final Packet packet, final List<Element> data) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);

		command.addChildren(data);
	}

	public static void setStatus(final Packet packet, final Status status) {
		Element iq      = packet.getElement();
		Element command = iq.getChild(COMMAND_EL);

		command.setAttribute("status", status.toString());
	}

	public static Command valueof(String cmd) {
		try {
			return Command.valueOf(cmd);
		} catch (Exception e) {
			return OTHER;
		}    // end of try-catch
	}

	private static void addActionElToCommand(final Element command, Action action) {
		Element actions = command.getChild("actions");

		if (actions == null) {
			actions = new Element("actions", new String[] { Action.execute.toString() },
														new String[] { action.toString() });
			command.addChild(actions);
		}
		actions.addChild(new Element(action.toString()));
	}

	private static void addActionEl(Element iq, Action action) {
		Element command = iq.getChild(COMMAND_EL);
		addActionElToCommand(command, action);
	}

	private static Element addDataForm(Element command, DataType data_type) {
		Element x = new Element("x", new String[] { "xmlns", "type" },
														new String[] { "jabber:x:data",
						data_type.name() });

		command.addChild(x);

		return x;
	}

	protected static Element createCommandEl(String node, DataType data_type) {
		Element command = new Element(COMMAND_EL, new String[]{"xmlns", "node"}, new String[]{XMLNS, node});

		if (data_type != null) {
			addDataForm(command, data_type);
			if (data_type == DataType.result) {
				command.setAttribute("status", Status.completed.name());
			}
			if (data_type == DataType.form) {
				command.setAttribute("status", Status.executing.name());
				addActionElToCommand(command, Action.complete);
			}
		}
		return command;
	}

	private static Element createIqCommandEl(JID from, JID to, StanzaType type, String id, String node,
											 DataType data_type, String xmlns) {
		Element iq = new Element("iq", new String[]{"type"}, new String[]{type.toString()});

		if (xmlns != null) {
			iq.setAttribute("xmlns", xmlns);
		}

		if (from != null) {
			iq.setAttribute("from", from.toString());
		}

		// The IQ packet (and command for that matter) should have the id attribute
		// therefore if it is not set then NPE should be thrown.
		// if (id != null) {
		iq.setAttribute("id", id);

		// }
		if (to != null) {
			iq.setAttribute("to", to.toString());
		}

		iq.addChild(createCommandEl(node, data_type));

		return iq;
	}

	//~--- set methods ----------------------------------------------------------

	private static void setStatusEl(Element iq, Status status) {
		Element command = iq.getChild(COMMAND_EL);

		command.setAttribute("status", status.name());
	}

	/**
	 * Method returns instance of a Packet with command element added.
	 *
	 * WARNING: Returned packet will not have any XMLNS set!
	 *
	 * @param from
	 * @param to
	 * @param type
	 * @param id
	 * @return
	 */
	public Packet getPacket(JID from, JID to, final StanzaType type, final String id) {
		Element elem  = createIqCommand(from, to, type, id, this.toString(), null, null);
		Packet result = Packet.packetInstance(elem, from, to);

		// Added to ensure that command will be executed in proper thread
		// and to ensure that multiple threads will be used for processing
		// command packets
		result.setPacketFrom(from);
		result.setPacketTo(to);
		
		result.setPriority(priority);

		return result;
	}

	/**
	 * Method returns instance of a Packet with command element added.
	 *
	 * WARNING: Returned packet will not have any XMLNS set!
	 *
	 * @param from
	 * @param to
	 * @param type
	 * @param id
	 * @param data_type
	 * @return
	 */
	public Packet getPacket(JID from, JID to, StanzaType type, String id,
													DataType data_type) {
		Element elem  = createIqCommand(from, to, type, id, this.toString(), data_type, null);
		Packet result = Packet.packetInstance(elem, from, to);

		result.setPriority(priority);

		return result;
	}
}    // Command

