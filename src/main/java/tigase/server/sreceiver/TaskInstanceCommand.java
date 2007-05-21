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

import java.util.List;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.stats.StatRecord;

/**
 * Describe class TaskInstanceCommand here.
 *
 *
 * Created: Fri May 18 22:45:40 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TaskInstanceCommand implements TaskCommandIfc {

	protected static final String ACTION_FIELD = "action-field";
	protected static final String CONFIRM = "confirm-field";

	public enum ACTION {
		TASK_CONFIGURATION, USER_MANAGEMENT, REMOVE_TASK;
		public static String[] strValues() {
			String[] possible_values = new String[values().length];
			int i = 0;
			for (Enum val: values()) {
				possible_values[i++] = val.toString();
			} // end of for (Enum en_v: en_val.values())
			return possible_values;
		}
	};

	/**
	 * Describe <code>getNodeName</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	public String getNodeName() {
		return "*";
	}

	/**
	 * Describe <code>getDescription</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	public String getDescription() {
		return "Manage task exiting instance";
	}

	private void taskMainScreen(Packet result, ReceiverTaskIfc task) {
		Command.setStatus(result, "executing");
		Command.addAction(result, "next");

		List<StatRecord> stats = task.getStats();
		if (stats != null) {
			for (StatRecord rec: stats) {
				Command.addFieldValue(result, "Info",
					rec.getDescription() + ": " + rec.getValue()
					, "fixed");
			} // end of for (StatRecord rec: stats)
		} // end of if (stats != null)

		String[] actions = ACTION.strValues();
		Command.addFieldValue(result, ACTION_FIELD, actions[0],
			"Select action", actions, actions);

	}

	public void removeTask(Packet packet, Packet result,
		StanzaReceiver receiv) {
		String confirm = Command.getFieldValue(packet, CONFIRM);
		String task_name = Command.getFieldValue(packet, TASK_NAME_FIELD);
		if (confirm == null) {
			Command.addFieldValue(result, CONFIRM, "true", "hidden");
			Command.setStatus(result, "executing");
			Command.addAction(result, "next");
			Command.addFieldValue(result, "Info",
				"Are you sure you want to remove task: "
				+ task_name
				+ " and all it's data?", "fixed");
			Command.addFieldValue(result, "Info",
				"Note! There is no undo for task deletion function", "fixed");
			return;
		} // end of if (confirm == null)
		receiv.removeTaskInstance(receiv.getTaskInstances().get(task_name));
		Command.addFieldValue(result, "Info",
			"Task " + task_name + " has been removed.", "fixed");
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
		String task_name = packet.getElemTo();
		ReceiverTaskIfc task = receiv.getTaskInstances().get(task_name);
		if (task == null) {
			task_name = Command.getFieldValue(packet, TASK_NAME_FIELD);
			if (task_name != null) {
				task = receiv.getTaskInstances().get(task_name);
			} // end of if (task_name != null)
		} // end of if (task == null)
		if (task != null) {
			if (!(receiv.isAdmin(packet.getElemFrom())
					|| task.isAdmin(packet.getElemFrom()))) {
				Command.addFieldValue(result, "Info",
					"You are not administrator of: " + task_name + " task.", "fixed");
				Command.addFieldValue(result, "Info",
					"You can not execute task commands.", "fixed");
				return;
			}
			Command.addFieldValue(result, TASK_NAME_FIELD, task_name, "hidden");
			String action_str = Command.getFieldValue(packet, ACTION_FIELD);
			if (action_str == null) {
				taskMainScreen(result, task);
				return;
			} // end of if (step == null)
			Command.addFieldValue(result, ACTION_FIELD, action_str, "hidden");
			ACTION action = ACTION.valueOf(action_str);
			switch (action) {
			case TASK_CONFIGURATION:

				break;
			case USER_MANAGEMENT:

				break;
			case REMOVE_TASK:
				removeTask(packet, result, receiv);
				break;
			default:
				break;
			} // end of switch (action)
		} else {
			Command.setStatus(result, "executing");
			Command.addAction(result, "next");
			Command.addFieldValue(result, TASK_NAME_FIELD,
				"", "text-single", "Enter task JID");
		} // end of if (task != null) else
	}

} // TaskInstanceCommand
