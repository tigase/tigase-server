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
package tigase.net;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Describe interface ConnectionOpenListener here.
 *
 *
 * Created: Thu Jan 26 00:00:39 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ConnectionOpenListener {

	public static final int IPTOS_LOWCOST = 0x02;
	public static final int IPTOS_RELIABILITY = 0x04;
	public static final int IPTOS_THROUGHPUT = 0x08;
	public static final int IPTOS_LOWDELAY = 0x10;
	/**
	 * <code>RECEIVE_BUFFER_SIZE</code> defines a size for TCP/IP packets.
	 * XMPP data packets are quite small usually, below 1kB so we don't need
	 * big TCP/IP data buffers.
	 */
	public static final int DEF_RECEIVE_BUFFER_SIZE = 2*1024;
	public static final int DEF_TRAFFIC_CLASS = IPTOS_LOWCOST;


	void accept(SocketChannel sc);

	int getPort();

	String[] getIfcs();

        String getSRVType();
        
        String getRemoteHostname();

        InetSocketAddress getRemoteAddress();
        
	ConnectionType getConnectionType();
        
        SocketType getSocketType();

	int getReceiveBufferSize();

	int getTrafficClass();

} // ConnectionOpenListener
