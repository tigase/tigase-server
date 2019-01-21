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
package tigase.util.processing;

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
	private Packet packet;
	private XMPPProcessorIfc processor;

	public QueueItem(XMPPProcessorIfc processor, Packet packet, XMPPResourceConnection conn) {
		if (processor == null) {
			throw new NullPointerException("Processor parameter cannot be null!");
		}

		if (packet == null) {
			throw new NullPointerException("Packet parameter cannot be null!");
		}

		this.processor = processor;
		this.packet = packet;
		this.conn = conn;
	}

	public XMPPResourceConnection getConn() {
		return conn;
	}

	public Packet getPacket() {
		return packet;
	}

	public XMPPProcessorIfc getProcessor() {
		return processor;
	}
}

