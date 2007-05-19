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

import tigase.server.Command;
import tigase.server.Packet;

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

	// Implementation of tigase.server.sreceiver.TaskCommandIfc

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
		Command.addFieldValue(result, TASK_NAME_FIELD,
			"", "text-single", "Enter task JID");
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
			Command.addFieldValue(result, TASK_NAME_FIELD, task_name, "hidden");
			String step = Command.getFieldValue(packet, STEP);
			if (step == null) {
				Command.addFieldValue(result, STEP, "main-screen", "hidden");
				taskMainScreen(result, task);
				return;
			} // end of if (step == null)
		} else {
			Command.setStatus(result, "executing");
			Command.addAction(result, "next");
			Command.addFieldValue(result, TASK_NAME_FIELD,
				"", "text-single", "Enter task JID");
		} // end of if (task != null) else
	}

} // TaskInstanceCommand
