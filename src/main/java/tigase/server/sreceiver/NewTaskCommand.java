/*  Tigase Project
 *  Copyright (C) 2001-2007
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
package tigase.server.sreceiver;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xml.XMLUtils;

import static tigase.server.sreceiver.PropertyConstants.*;
import static tigase.server.sreceiver.TaskCommons.*;

/**
 * Describe class NewTaskCommand here.
 *
 *
 * Created: Thu May 17 22:19:28 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class NewTaskCommand implements TaskCommandIfc {

  private static Logger log =
		Logger.getLogger("tigase.server.sreceiver.NewTaskCommand");

	/**
	 * Describe <code>getNodeName</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	public String getNodeName() {
		return "new-task";
	}

	/**
	 * Describe <code>getDescription</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	public String getDescription() {
		return "Add new task...";
	}

	private boolean checkTaskName(String task_name, Packet result,
		StanzaReceiver receiv) {
		String msg = JIDUtils.checkNickName(task_name);
		if (msg != null) {
			Command.addFieldValue(result, "Info",
				"Note!! " + msg + ", please provide valid task name.", "fixed");
			return false;
		} // end of if (msg != null)
		ReceiverTaskIfc task = receiv.getTaskInstances().get(task_name);
		if (task != null) {
				Command.addFieldValue(result, "Info",
					"Note!! Task with provided name already exists.", "fixed");
				Command.addFieldValue(result, "Info",
					"Please provide different task name.",	"fixed");
			return false;
		} // end of if (task != null)
		return true;
	}

	private void newTask_Step1(Packet result, StanzaReceiver receiv) {
		Command.addFieldValue(result, "Info",	"Press:", "fixed");
		Command.addFieldValue(result, "Info",
			"'Next' to set all parameters for the new task.", "fixed");
		Command.setStatus(result, "executing");
		Command.addAction(result, "next");
		Command.addFieldValue(result, TASK_NAME_FIELD,
			"", "text-single", TASK_NAME_FIELD);
		String[] task_types =
			receiv.getTaskTypes().keySet().toArray(new String[0]);
		Command.addFieldValue(result, TASK_TYPE_FIELD, task_types[0],
			TASK_TYPE_FIELD, task_types, task_types);
	}

	private void newTask_Step2(Packet packet, Packet result,
		StanzaReceiver receiv) {
		String task_type = Command.getFieldValue(packet, TASK_TYPE_FIELD);
		ReceiverTaskIfc task_t =
			receiv.getTaskTypes().get(task_type).getTaskType();
		int start = 0;
		int line_len = 60;
		do {
			int end = ((start + line_len < task_t.getHelp().length()) ?
				start + line_len :
				task_t.getHelp().length());
			while (end < task_t.getHelp().length()
				&& task_t.getHelp().charAt(end) != ' ') {
				++end;
			}
			Command.addFieldValue(result, "task" + start,
				task_t.getHelp().substring(start, end), "fixed");
			start = end;
		} while (start < task_t.getHelp().length());
		Command.addFieldValue(result, "Info2",
			"1. 'Finish' to create component with this parameters.", "fixed");
		Command.addFieldValue(result, "Info3",
			"2. 'Previous' to go back and select different component.", "fixed");
		Command.setStatus(result, "executing");
		Command.addAction(result, "complete");
		Command.addAction(result, "prev");
		Map<String, PropertyItem> default_props = task_t.getDefaultParams();
		PropertyItem pi = default_props.get(TASK_OWNER_PROP_KEY);
		if (pi != null) {
			pi.setValue(JIDUtils.getNodeID(packet.getElemFrom()));
		}
		propertyItems2Command(default_props, result);
	}

	private void newTask_Step3(Packet packet, Packet result,
		StanzaReceiver receiv) {
		String task_name = Command.getFieldValue(packet, TASK_NAME_FIELD);
		String task_type = Command.getFieldValue(packet, TASK_TYPE_FIELD);
		Map<String, PropertyItem> default_props =
			receiv.getTaskTypes().get(task_type).getTaskType().getDefaultParams();
		Map<String, Object> new_params = new HashMap<String, Object>();
		for (String key: default_props.keySet()) {
			String value = Command.getFieldValue(packet, XMLUtils.escape(key));
			if (value == null) {
				value = "";
			} // end of if (value == null)
			value = XMLUtils.unescape(value);
			new_params.put(key, value);
		} // end of for (String key: default_props.keySet())
		receiv.addTaskInstance(task_type, task_name, new_params);
		Command.addFieldValue(result, "Info",
			"Created task: " + task_name, "fixed");
	}

	/**
	 * Describe <code>processCommand</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param result a <code>Packet</code> value
	 * @param reciv a <code>StanzaReceiver</code> value
	 */
	public void processCommand(Packet packet, Packet result,
		StanzaReceiver receiv) {
		String task_name = Command.getFieldValue(packet, TASK_NAME_FIELD);
		if (task_name == null || !checkTaskName(task_name, result, receiv)
			|| (Command.getAction(packet) != null &&
				Command.getAction(packet).equals("prev"))) {
			Command.addFieldValue(result, STEP, "step1", "hidden");
			newTask_Step1(result, receiv);
			return;
		} // end of if (!checkTaskName(receiv, task_name))
		String task_type = Command.getFieldValue(packet, TASK_TYPE_FIELD);
		TaskType tt = receiv.getTaskTypes().get(task_type);
		if (tt == null) {
			Command.addFieldValue(result, "Info",
				"I am sorry there is a problem with instantiating task of this type: "
				+ task_type, "fixed");
			return;
		}
		if (tt.getMaxInstancesNo() <= tt.getInstancesNo()) {
			Command.addFieldValue(result, "Info",
				"I am sorry, maximum number of tasks instances for this type has been"
				+ " exceeded: "
				+ task_type, "fixed");
			return;
		}
		if (!receiv.isAllowedCreate(packet.getFrom(), task_type)) {
			Command.addFieldValue(result, "Info",
				"I am sorry, you are not allowed to create task of this type: "
				+ task_type, "fixed");
			return;
		}
		Command.addFieldValue(result, TASK_NAME_FIELD, task_name, "hidden");
		Command.addFieldValue(result, TASK_TYPE_FIELD, task_type, "hidden");
		String step = Command.getFieldValue(packet, STEP);
		if (step == null || step.equals("step1")) {
			Command.addFieldValue(result, STEP, "step2", "hidden");
			newTask_Step2(packet, result, receiv);
			return;
		} // end of if (step == null || step.equals("step1"))
		newTask_Step3(packet, result, receiv);
	}

} // NewTaskCommand
