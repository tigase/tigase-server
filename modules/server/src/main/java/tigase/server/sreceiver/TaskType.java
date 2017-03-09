/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.sreceiver;

/**
 * Describe class TaskType here.
 *
 *
 * Created: Mon May 28 08:52:07 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TaskType {

	private ReceiverTaskIfc task = null;
	private int max_instances = 1;
	private TaskCreationPolicy creation_policy = TaskCreationPolicy.ADMIN;
	private int curr_instances = 0;

	/**
	 * Creates a new <code>TaskType</code> instance.
	 *
	 *
	 * @param task
	 */
	public TaskType(ReceiverTaskIfc task) {
		this.task = task;
	}

	public ReceiverTaskIfc getTaskType() {
		return task;
	}

	public ReceiverTaskIfc getTaskInstance() {
		return task.getInstance();
	}

	public void instanceAdded() {
		++curr_instances;
	}

	public void instanceRemoved() {
		--curr_instances;
	}

	public int getInstancesNo() {
		return curr_instances;
	}

	public int getMaxInstancesNo() {
		return max_instances;
	}

	public void setMaxInstancesNo(int max_instances) {
		this.max_instances = max_instances;
	}

	public TaskCreationPolicy getCreationPolicy() {
		return creation_policy;
	}

	public void setCreationPolicy(TaskCreationPolicy policy) {
		this.creation_policy = policy;
	}

}
