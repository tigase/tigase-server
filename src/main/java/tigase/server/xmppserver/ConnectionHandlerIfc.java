/*
 *   Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.xmppserver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.xmpp.XMPPIOService;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;

//~--- interfaces -------------------------------------------------------------

/**
 * Describe interface ConnectionHandlerIfc here.
 *
 *
 * Created: Wed Jun 11 22:13:31 2008
 *
 * @param <IO>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ConnectionHandlerIfc<IO extends XMPPIOService> {
	boolean addOutPacket(Packet packet);

	void initNewConnection(Map<String, Object> port_props);

	boolean writePacketToSocket(IO serv, Packet packet);

	void writePacketsToSocket(IO serv, Queue<Packet> packets);
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
