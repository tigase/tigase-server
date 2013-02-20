/*
 * WebSocketClientConnectionManager.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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



package tigase.server.websocket;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.XMPPIOService;

/**
 * Class implements basic support allowing clients to connect using WebSocket
 * protocol
 *
 * @author andrzej
 */
public class WebSocketClientConnectionManager
				extends tigase.server.xmppclient.ClientConnectionManager {
	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	protected int[] getDefPlainPorts() {
		return new int[] { 5290 };
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	protected int[] getDefSSLPorts() {
		return null;
	}

	/**
	 * Method returns XMPPIOService instance implementing WebSocketXMPPIOService
	 *
	 * @return
	 */
	@Override
	protected XMPPIOService<Object> getXMPPIOServiceInstance() {
		return new WebSocketXMPPIOService<Object>();
	}
}


//~ Formatted in Tigase Code Convention on 13/02/20
