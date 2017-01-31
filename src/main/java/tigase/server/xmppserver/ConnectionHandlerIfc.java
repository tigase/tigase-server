/*
 *   Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

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
 * @deprecated was replaced by {@link S2SConnectionManager}, will be removed in 7.2.0
 */
@Deprecated
public interface ConnectionHandlerIfc<IO extends XMPPIOService<?>> {
	boolean writePacketToSocket(IO serv, Packet packet);

	void writePacketsToSocket(IO serv, Queue<Packet> packets);
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
