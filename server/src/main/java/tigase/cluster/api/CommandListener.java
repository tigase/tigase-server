/*
 * CommandListener.java
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



package tigase.cluster.api;

//~--- non-JDK imports --------------------------------------------------------

import tigase.stats.StatisticsList;

import tigase.xml.Element;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import tigase.server.Priority;

/**
 * @author Artur Hefczyc Created Mar 16, 2011
 */
public interface CommandListener
				extends Comparable<CommandListener> {
	/**
	 * Method is responsible for executing commands from other nodes and
	 * appropriate processing
	 *
	 * @param fromNode     address of the node from which command was received
	 * @param visitedNodes collection of already visited nodes
	 * @param data         additional data associated with the command in addition
	 *                     to the main {@link Element}
	 * @param packets      collection of {@link Element} commands to be executed
	 *
	 * @throws ClusterCommandException execution exception
	 */
	void executeCommand(JID fromNode, Set<JID> visitedNodes, Map<String, String> data,
			Queue<Element> packets)
					throws ClusterCommandException;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method allows retrieval name of the particular command
	 *
	 * @return a value of <code>String</code> name of the command
	 */
	String getName();

	/**
	 * Method returns priority of particular command which should be used 
	 * to assign proper priority for processing of this command
	 * 
	 * @return 
	 */
	Priority getPriority();
	
	/**
	 * Method allows retrieval possible statistics for particular command
	 *
	 * @param list collection to which statistics should be appended
	 */
	void getStatistics(StatisticsList list);

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method allows setting name of the command
	 *
	 * @param name to be used
	 */
	void setName(String name);
}
