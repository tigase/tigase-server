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

/**
 * @author Artur Hefczyc Created Mar 16, 2011
 */
public interface CommandListener
				extends Comparable<CommandListener> {
	/**
	 * Method description
	 *
	 *
	 * @param fromNode
	 * @param visitedNodes
	 * @param data
	 * @param packets
	 *
	 * @throws ClusterCommandException
	 */
	void executeCommand(JID fromNode, Set<JID> visitedNodes, Map<String, String> data,
			Queue<Element> packets)
					throws ClusterCommandException;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	String getName();

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	void getStatistics(StatisticsList list);

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	void setName(String name);
}


//~ Formatted in Tigase Code Convention on 13/11/01
