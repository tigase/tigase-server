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

import tigase.server.Packet;

/**
 * Describe class TaskCommandIfc here.
 *
 *
 * Created: Wed May 16 18:29:18 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface TaskCommandIfc {

	public static final String TASK_NAME_FIELD = "Task name";
	public static final String TASK_TYPE_FIELD = "Task type";
	public static final String STEP = "step";

	/**
	 * Describe <code>getNodeName</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	String getNodeName();

	/**
	 * Describe <code>getDescription</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	String getDescription();

	/**
	 * Describe <code>processCommand</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param result a <code>Packet</code> value
	 * @param reciv a <code>StanzaReceiver</code> value
	 */
	void processCommand(Packet packet, Packet result, StanzaReceiver reciv);

} // TaskCommandIfc
