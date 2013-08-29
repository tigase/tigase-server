/*
 * XMPPIOProcessor.java
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



package tigase.server.xmppclient;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.ConnectionManager;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.XMPPIOService;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.Map;

/**
 *
 * @author andrzej
 */
public interface XMPPIOProcessor {
	/**
	 * Method is called when all waiting data was written to socket.
	 *
	 * @param service
	 * @throws IOException
	 */
	void packetsSent(XMPPIOService service) throws IOException;

	/**
	 * Process command execution which may be sent from other component and
	 * should be processed by processor
	 *
	 *
	 * @param service
	 * @param packet
	 */
	void processCommand(XMPPIOService service, Packet packet);

	/**
	 * Process packets read from socket as they are sent to SessionManager.
	 *
	 * @param service
	 * @param packet
	 *  true if packet should not be forwarded
	 *
	 * @return a value of boolean
	 */
	boolean processIncoming(XMPPIOService service, Packet packet);

	/**
	 * Process outgoing packets as they are added to XMPPIOService outgoing
	 * packets queue.
	 *
	 * @param service
	 * @param packet
	 *  true if packet should be removed
	 *
	 * @return a value of boolean
	 */
	boolean processOutgoing(XMPPIOService service, Packet packet);

	/**
	 * Method called when XMPPIOService is closed.
	 *
	 * @param service
	 * @param streamClosed
	 *  true if connecton manager should not be notified about stopping
	 *        of this service
	 *
	 * @return a value of boolean
	 */
	boolean serviceStopped(XMPPIOService service, boolean streamClosed);

	/**
	 * Returns array of features added by this processor
	 *
	 * @param service
	 *
	 *
	 * @return a value of Element[]
	 */
	Element[] supStreamFeatures(XMPPIOService service);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Returns identifier of processor
	 *
	 *
	 *
	 * @return a value of String
	 */
	String getId();

	//~--- set methods ----------------------------------------------------------

	/**
	 * Sets connection manager instance for which this XMPPIOProcessor is used
	 *
	 * @param connectionManager
	 */
	void setConnectionManager(ConnectionManager connectionManager);

	/**
	 * Method used for setting properties
	 *
	 * @param props
	 */
	void setProperties(Map<String, Object> props);
}


//~ Formatted in Tigase Code Convention on 13/08/28
