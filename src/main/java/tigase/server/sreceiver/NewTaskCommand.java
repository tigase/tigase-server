/*
 * NewTaskCommand.java
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



package tigase.server.sreceiver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Command;
import tigase.server.Packet;

import tigase.util.TigaseStringprepException;

import tigase.xml.XMLUtils;

import tigase.xmpp.BareJID;

import static tigase.server.sreceiver.PropertyConstants.*;
import static tigase.server.sreceiver.TaskCommons.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

/**
 * Describe class NewTaskCommand here.
 *
 *
 * Created: Thu May 17 22:19:28 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class NewTaskCommand
				implements TaskCommandIfc {
	private static Logger log = Logger.getLogger("tigase.server.sreceiver.NewTaskCommand");

	//~--- methods --------------------------------------------------------------

	@Override
	public void processCommand(Packet packet, Packet result, StanzaReceiver receiv) {
		String task_name = Command.getFieldValue(packet, TASK_NAME_FIELD);

		if ((task_name == null) ||!checkTaskName(task_name, result, receiv) || (Command
				.getAction(packet) == Command.Action.prev)) {
			Command.addFieldValue(result, STEP, "step1", "hidden");
			newTask_Step1(result, receiv);

			return;
		}    // end of if (!checkTaskName(receiv, task_name))

		String   task_type = Command.getFieldValue(packet, TASK_TYPE_FIELD);
		TaskType tt        = receiv.getTaskTypes().get(task_type);

		if (tt == null) {
			Command.addFieldValue(result, "Info",
					"I am sorry there is a problem with instantiating task of this type: " +
					task_type, "fixed");

			return;
		}
		if (tt.getMaxInstancesNo() <= tt.getInstancesNo()) {
			Command.addFieldValue(result, "Info",
					"I am sorry, maximum number of tasks instances for this type has been" +
					" exceeded: " + task_type, "fixed");

			return;
		}
		if (!receiv.isAllowedCreate(packet.getStanzaFrom(), task_type)) {
			Command.addFieldValue(result, "Info",
					"I am sorry, you are not allowed to create task of this type: " + task_type,
					"fixed");

			return;
		}
		Command.addFieldValue(result, TASK_NAME_FIELD, task_name, "hidden");
		Command.addFieldValue(result, TASK_TYPE_FIELD, task_type, "hidden");

		String step = Command.getFieldValue(packet, STEP);

		if ((step == null) || step.equals("step1")) {
			Command.addFieldValue(result, STEP, "step2", "hidden");
			newTask_Step2(packet, result, receiv);

			return;
		}    // end of if (step == null || step.equals("step1"))
		try {
			newTask_Step3(packet, result, receiv);
		} catch (TigaseStringprepException ex) {
			Logger.getLogger(NewTaskCommand.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getDescription() {
		return "Add new task...";
	}

	@Override
	public String getNodeName() {
		return "new-task";
	}

	//~--- methods --------------------------------------------------------------

	private boolean checkTaskName(String task_name, Packet result, StanzaReceiver receiv) {
		String msg = BareJID.parseJID(task_name)[0];

		if (msg != null) {
			Command.addFieldValue(result, "Info", "Note!! " + msg +
					", please provide valid task name.", "fixed");

			return false;
		}    // end of if (msg != null)

		ReceiverTaskIfc task = receiv.getTaskInstances().get(task_name);

		if (task != null) {
			Command.addFieldValue(result, "Info",
					"Note!! Task with provided name already exists.", "fixed");
			Command.addFieldValue(result, "Info", "Please provide different task name.",
					"fixed");

			return false;
		}    // end of if (task != null)

		return true;
	}

	private void newTask_Step1(Packet result, StanzaReceiver receiv) {
		Command.addFieldValue(result, "Info", "Press:", "fixed");
		Command.addFieldValue(result, "Info",
				"'Next' to set all parameters for the new task.", "fixed");
		Command.setStatus(result, Command.Status.executing);
		Command.addAction(result, Command.Action.next);
		Command.addFieldValue(result, TASK_NAME_FIELD, "", "text-single", TASK_NAME_FIELD);

		String[] task_types = receiv.getTaskTypes().keySet().toArray(new String[0]);

		Command.addFieldValue(result, TASK_TYPE_FIELD, task_types[0], TASK_TYPE_FIELD,
				task_types, task_types);
	}

	private void newTask_Step2(Packet packet, Packet result, StanzaReceiver receiv) {
		String          task_type = Command.getFieldValue(packet, TASK_TYPE_FIELD);
		ReceiverTaskIfc task_t    = receiv.getTaskTypes().get(task_type).getTaskType();
		int             start     = 0;
		int             line_len  = 60;

		do {
			int end = ((start + line_len < task_t.getHelp().length())
					? start + line_len
					: task_t.getHelp().length());

			while ((end < task_t.getHelp().length()) && (task_t.getHelp().charAt(end) != ' ')) {
				++end;
			}
			Command.addFieldValue(result, "task" + start, task_t.getHelp().substring(start,
					end), "fixed");
			start = end;
		} while (start < task_t.getHelp().length());
		Command.addFieldValue(result, "Info2",
				"1. 'Finish' to create component with this parameters.", "fixed");
		Command.addFieldValue(result, "Info3",
				"2. 'Previous' to go back and select different component.", "fixed");
		Command.setStatus(result, Command.Status.executing);
		Command.addAction(result, Command.Action.complete);
		Command.addAction(result, Command.Action.prev);

		Map<String, PropertyItem> default_props = task_t.getDefaultParams();
		PropertyItem              pi            = default_props.get(TASK_OWNER_PROP_KEY);

		if (pi != null) {
			pi.setValue(packet.getStanzaFrom().getBareJID().toString());
		}
		propertyItems2Command(default_props, result);
	}

	private void newTask_Step3(Packet packet, Packet result, StanzaReceiver receiv)
					throws TigaseStringprepException {
		String                    task_name = Command.getFieldValue(packet, TASK_NAME_FIELD);
		String                    task_type = Command.getFieldValue(packet, TASK_TYPE_FIELD);
		Map<String, PropertyItem> default_props = receiv.getTaskTypes().get(task_type)
				.getTaskType().getDefaultParams();
		Map<String, Object> new_params = new LinkedHashMap<String, Object>();

		for (String key : default_props.keySet()) {
			if (default_props.get(key).isMultiValue()) {
				String[] values = Command.getFieldValues(packet, XMLUtils.escape(key));

				new_params.put(key, values);
			} else {
				String value = Command.getFieldValue(packet, XMLUtils.escape(key));

				if (value == null) {
					value = "";
				}    // end of if (value == null)
				value = XMLUtils.unescape(value);
				new_params.put(key, value);
			}
		}        // end of for (String key: default_props.keySet())
		receiv.addTaskInstance(task_type, task_name, new_params);
		Command.addFieldValue(result, "Info", "Created task: " + task_name, "fixed");
	}
}


//~ Formatted in Tigase Code Convention on 13/09/21
