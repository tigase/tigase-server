/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

package tigase.server;

import tigase.stats.StatisticsList;

/**
 * An interface for loadable packet filters to the Tigase server. Every Tigase component
 * can have an independent list of packet filters for outgoing and incoming traffic.
 * A filter can make any change to the processed packet or can block the packet from
 * further processing. Please refer to the <code>filter()</code> method for more details.
 *
 * Created: Jun 8, 2009 1:29:49 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface PacketFilterIfc {

	/**
	 * The method initializes the filter. It is always called only once after an instance
	 * of the filter has been created.
	 * @param name is a component name which loaded and initialized the filter. This is
	 * the name of the component which uses the filter.
	 * @param qType is a packet queue type, differnt one for outgoing traffic and different
	 * for incoming. A filter may want to treat the traffic differently depending on the
	 * direction it flows.
	 */
	void init(String name, QueueType qType);

	/**
	 * This is the actual packet filtering method. It receives a packet as a parameter
	 * and may make any change to the packet it wishes, remove or add specific payloads
	 * or redirect the packet to specific destination. <strong>Please note!</strong> it is
	 * recommended not to modify the actual packet itself. If the filter needs to make
	 * any changes to the packet it should create a copy of the object, then make
	 * any changes on the copy and return the copy as the result.
	 * It may also optionally block the packet from further processing. This means
	 * that the packet is effectivelly dropped and forgotten.
	 *
	 * If the method returns a <code>Packet</code> as a result. It is normally recommended
	 * not to modify the existing packet as it maybe processed simultanuously by other
	 * components/threads at the same time. Modifying packet while it is being processed
	 * may lead to unpredictable results. Therefore, if the filter wants to modify the
	 * packet it should create a copy of the packet and return modified copy from
	 * the method.
	 * If the filter decided to block the packet it just has to return null. In most cases,
	 * however the method returns the packet it received as a parameter to method call.
	 * @param packet for the filter processing.
	 *
	 * Please note, the packet filtering may affect performance significantly therefore
	 * this method should be carefully tested and optimized under a high load.
	 *
	 * @return a Packet object which is further processed by the system. If the
	 * method decided to block the packet it returns null. If the method want the
	 * packet to be processed without any modifications it returns the same object
	 * it received as a parameter. It may also return a modified copy of the Packet.
	 */
	Packet filter(Packet packet);

	/**
	 * A filter may optionally return some processing statistics. Please note the method
	 * may be called quite frequently (once a second) therefore no expensive calculation
	 * should be performed inside the method.
	 * @param list of statistics created by the master object. The packet instance should
	 * add its statistics to the list.
	 */
	void getStatistics(StatisticsList list);

}
