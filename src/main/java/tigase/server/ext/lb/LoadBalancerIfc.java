/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2011 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
* $Rev: 2411 $
* Last modified by $Author: kobit $
* $Date: 2010-10-27 20:27:58 -0600 (Wed, 27 Oct 2010) $
 * 
 */
package tigase.server.ext.lb;

import java.util.ArrayList;

import tigase.server.Packet;
import tigase.server.ext.ComponentConnection;
import tigase.server.ext.ComponentIOService;

/**
 * @author Artur Hefczyc
 * Created Jul 9, 2011
 */
public interface LoadBalancerIfc {

	/**
	 * @param p
	 * @param conns
	 * @return
	 */
	ComponentIOService selectConnection(Packet p, ArrayList<ComponentConnection> conns);

}
