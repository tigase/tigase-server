/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.cluster.api;

import tigase.server.Priority;
import tigase.stats.StatisticsList;

/**
 * @author kobit
 */
public abstract class CommandListenerAbstract
		implements CommandListener {

	private static long syncInTraffic = 0;
	private static long syncOutTraffic = 0;

	private String commandName;
	private Priority priority;

	public static long getSyncInTraffic() {
		return syncInTraffic;
	}

	public static long getSyncOutTraffic() {
		return syncOutTraffic;
	}

	public CommandListenerAbstract(String name, Priority priority) {
		setName(name);
		setPriority(priority);
	}

	@Override
	public int compareTo(CommandListener cl) {
		return commandName.compareTo(cl.getName());
	}

	@Override
	public boolean equals(Object cl) {
		return ((cl != null) && (cl instanceof CommandListener) &&
				commandName.equals(((CommandListener) cl).getName()));
	}

	@Override
	public int hashCode() {
		int hash = 265;

		hash = hash + ((this.commandName != null) ? this.commandName.hashCode() : 0);

		return hash;
	}

	public synchronized void incSyncInTraffic() {
		++syncInTraffic;
	}

	public synchronized void incSyncOutTraffic() {
		++syncOutTraffic;
	}

	@Override
	public String getName() {
		return commandName;
	}

	@Override
	public final void setName(String name) {
		commandName = name;
	}

	@Override
	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	@Override
	public void getStatistics(StatisticsList list) {
	}

}
