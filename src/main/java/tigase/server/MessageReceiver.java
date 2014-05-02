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

package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Queue;

//~--- interfaces -------------------------------------------------------------

/**
 * Interface MessageReceiver
 *
 * Objects of this type can receive messages. They can be in fact routing
 * destination depending on target address. Message are routed to proper
 * destination in MessageRouter class.
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface MessageReceiver extends ServerComponent {

	/**
	 * Describe <code>addPacket</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @return a <code>boolean</code> value <code>true</code> if packet has been
	 * successfully added, <code>false</code> otherwise.
	 */
	boolean addPacket(Packet packet);

	boolean addPacketNB(Packet packet);

	/**
	 * Describe <code>addPackets</code> method here.
	 *
	 * @param packets
	 * @return a <code>boolean</code> value
	 */
	boolean addPackets(Queue<Packet> packets);

	//~--- get methods ----------------------------------------------------------

	BareJID getDefHostName();

///**
// * Returns array of Strings. Each String should be a regular expression
// * defining destination addresses for which this receiver can process
// * messages. There can be more than one message receiver for each messages.
// *
// * @return a <code>String[]</code> value
// */
//String[] getLocalAddresses();
	// Set<String> getRoutings();
	boolean isInRegexRoutings(String address);

	//~--- set methods ----------------------------------------------------------

	/**
	 * Describe <code>setParent</code> method here.
	 *
	 * @param msg_rec a <code>MessageReceiver</code> value
	 */
	void setParent(MessageReceiver msg_rec);

	//~--- methods --------------------------------------------------------------

	void start();

}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
