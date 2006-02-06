/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server;

import java.util.Queue;
import java.util.Set;

/**
 * Interface MessageReceiver
 *
 * Objects of this type can receive messages. They can be in fact routing
 * destination depending on target address. Message are routed to proper
 * destination in MessageRouter class.
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface MessageReceiver extends ServerComponent {

//   /**
//    * Returns array of Strings. Each String should be a regular expression
//    * defining destination addresses for which this receiver can process
//    * messages. There can be more than one message receiver for each messages.
//    *
//    * @return a <code>String[]</code> value
//    */
//   String[] getLocalAddresses();

	Set<String> getRoutings();

  /**
	 * Describe <code>addPacket</code> method here.
   *
	 * @param packet a <code>Packet</code> value
	 * @return a <code>boolean</code> value <code>true</code> if packet has been
	 * successfully added, <code>false</code> otherwise.
	 */
	boolean addPacket(Packet packet);

	/**
	 * Describe <code>addPackets</code> method here.
	 *
	 * @return a <code>boolean</code> value
	 */
	boolean addPackets(Queue<Packet> packets);

	/**
	 * Describe <code>setParent</code> method here.
	 *
	 * @param msg_rec a <code>MessageReceiver</code> value
	 */
	void setParent(MessageReceiver msg_rec);

	String getDefHostName();

	void start();

}
