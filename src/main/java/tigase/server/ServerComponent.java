/*
 * ServerComponent.java
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



package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Queue;

/**
 * Interface ServerComponent
 *
 * Object of this type can be managed by MessageRouter. All classes which are
 * loaded by MessageRouter must inherit this interface.
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ServerComponent {
	/**
	 * Method is called by <code>MessageRouter</code> when all the startup components of
	 * the server have been loaded and configured through setProperties(...) call.
	 * At this point the whole server should be loaded and functional, except
	 * initializations taking place in this routine.
	 *
	 */
	void initializationCompleted();

	/**
	 * <code>processPacket</code> is a blocking processing method implemented
	 * by all components. This method processes packet and returns results
	 * instantly without waiting for any resources.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param results
	 */
	void processPacket(Packet packet, Queue<Packet> results);

	/**
	 * Method description
	 *
	 */
	void release();

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	JID getComponentId();

	/**
	 * Allows to obtain various informations about components
	 *
	 * @return information about particular component
	 */
	ComponentInfo getComponentInfo();

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	String getName();

	/**
	 * Method returns information about whether the initialization process
	 * (initializationCompleted()) method has been called.
	 * @return <code>true</code> if initialization of the object has been completed
	 * <code>false</code> otherwise
	 */
	boolean isInitializationComplete();

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	void setName(String name);
}


//~ Formatted in Tigase Code Convention on 13/06/08
