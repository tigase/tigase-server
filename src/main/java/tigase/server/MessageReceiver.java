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
package tigase.server;

import tigase.xmpp.jid.BareJID;

import java.util.Queue;

/**
 * Interface MessageReceiver
 * <br>
 * Objects of this type can receive messages. They can be in fact routing destination depending on target address.
 * Message are routed to proper destination in MessageRouter class.
 * <br>
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface MessageReceiver
		extends ServerComponent {

	boolean addPacket(Packet packet);

	boolean addPacketNB(Packet packet);

	boolean addPackets(Queue<Packet> packets);

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

	void setParent(MessageReceiver msg_rec);

	void start();

}
