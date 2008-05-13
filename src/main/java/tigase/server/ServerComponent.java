/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.server;

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
  // Methods

  void setName(String name);

  String getName();

  void release();

  /**
	 * <code>processPacket</code> is a blocking processing method implemented
	 * by all components. This method processes packet and returns results
	 * instantly without waiting for any resources.
	 *
	 * @param packet a <code>Packet</code> value
	 */
	void processPacket(Packet packet, Queue<Packet> results);

}
