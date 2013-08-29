/*
 * CommandListenerAbstract.java
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



/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.cluster.api;

//~--- non-JDK imports --------------------------------------------------------

import tigase.stats.StatisticsList;

/**
 *
 * @author kobit
 */
public abstract class CommandListenerAbstract
				implements CommandListener {
	private String commandName;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param name
	 */
	public CommandListenerAbstract(String name) {
		setName(name);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param cl
	 *
	 *
	 *
	 * @return a value of <code>int</code>
	 */
	@Override
	public int compareTo(CommandListener cl) {
		return commandName.compareTo(cl.getName());
	}

	/**
	 * Method description
	 *
	 *
	 * @param cl
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean equals(Object cl) {
		return ((cl != null) && (cl instanceof CommandListener) && commandName.equals(
				((CommandListener) cl).getName()));
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>int</code>
	 */
	@Override
	public int hashCode() {
		int hash = 265;

		hash = hash + ((this.commandName != null)
				? this.commandName.hashCode()
				: 0);

		return hash;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getName() {
		return commandName;
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	@Override
	public final void setName(String name) {
		commandName = name;
	}
}


//~ Formatted in Tigase Code Convention on 13/08/29
