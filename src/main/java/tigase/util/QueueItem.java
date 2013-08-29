/*
 * QueueItem.java
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



package tigase.util;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Created: Apr 21, 2009 9:05:23 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class QueueItem {
	private XMPPResourceConnection conn;
	private Packet                 packet;
	private XMPPProcessorIfc       processor;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param processor
	 * @param packet
	 * @param conn
	 */
	public QueueItem(XMPPProcessorIfc processor, Packet packet,
			XMPPResourceConnection conn) {
		if (processor == null) {
			throw new NullPointerException("Processor parameter cannot be null!");
		}
		if (packet == null) {
			throw new NullPointerException("Packet parameter cannot be null!");
		}
		this.processor = processor;
		this.packet    = packet;
		this.conn      = conn;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 *  the conn
	 *
	 * @return a value of XMPPResourceConnection
	 */
	public XMPPResourceConnection getConn() {
		return conn;
	}

	/**
	 *  the packet
	 *
	 * @return a value of Packet
	 */
	public Packet getPacket() {
		return packet;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of XMPPProcessorIfc
	 */
	public XMPPProcessorIfc getProcessor() {
		return processor;
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
