/*
 * ClusterConnectionSelectorTest.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */
package tigase.cluster;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.junit.Test;
import static tigase.cluster.ClusterConnectionSelector.CLUSTER_SYS_CONNECTIONS_PER_NODE_PROP_KEY;
import tigase.cluster.api.ClusterConnectionHandler;
import tigase.server.Packet;
import tigase.server.Priority;
import tigase.xml.Element;
import tigase.xmpp.XMPPIOService;

/**
 *
 * @author andrzej
 */
public class ClusterConnectionSelectorTest extends TestCase {

	@Test
	public void testSelectConnection() throws Exception {
		ClusterConnection conn = new ClusterConnection("test");
		ClusterConnectionSelector selector = new ClusterConnectionSelector();
		selector.setClusterConnectionHandler(new ClusterConnectionHandler() {

			@Override
			public int hashCodeForPacket(Packet packet) {
				return packet.getStanzaFrom().hashCode();
			}
		});

		Map<String,Object> props = new HashMap<>();
		props.put(CLUSTER_SYS_CONNECTIONS_PER_NODE_PROP_KEY, 1);
		selector.setProperties(props);
		
		Element el = new Element("iq", new String[] { "from" }, new String[] { "test1" });
		Packet p = Packet.packetInstance(el);
		assertNull(selector.selectConnection(p, conn));
		
		XMPPIOService<Object> serv1 = new XMPPIOService<Object>();
		conn.addConn(serv1);
		assertEquals(serv1, selector.selectConnection(p, conn));
		
		p.setPriority(Priority.SYSTEM);
		assertEquals(serv1, selector.selectConnection(p, conn));

		p.setPriority(null);
		XMPPIOService<Object> serv2 = new XMPPIOService<Object>();
		conn.addConn(serv2);
		assertEquals(2, conn.size());
		assertEquals(serv2, selector.selectConnection(p, conn));
		
		p.setPriority(Priority.SYSTEM);
		assertEquals(serv1, selector.selectConnection(p, conn));

		p.setPriority(null);
		XMPPIOService<Object> serv3 = new XMPPIOService<Object>();
		conn.addConn(serv3);
		assertEquals(3, conn.size());
		assertNotSame(serv1, selector.selectConnection(p, conn));
		
		p.setPriority(Priority.SYSTEM);
		assertEquals(serv1, selector.selectConnection(p, conn));
		
		el = new Element("iq", new String[] { "from" }, new String[] { "test2" });
		p = Packet.packetInstance(el);
		assertEquals(3, conn.size());
		assertNotSame(serv1, selector.selectConnection(p, conn));

		el = new Element("iq", new String[] { "from" }, new String[] { "test3" });
		p = Packet.packetInstance(el);
		assertEquals(3, conn.size());
		assertNotSame(serv1, selector.selectConnection(p, conn));

		el = new Element("iq", new String[] { "from" }, new String[] { "test4" });
		p = Packet.packetInstance(el);
		assertEquals(3, conn.size());
		assertNotSame(serv1, selector.selectConnection(p, conn));		
	}
	
	@Test
	public void testSelectConnectionFor2() throws Exception {
		ClusterConnection conn = new ClusterConnection("test");
		ClusterConnectionSelector selector = new ClusterConnectionSelector();
		selector.setClusterConnectionHandler(new ClusterConnectionHandler() {

			@Override
			public int hashCodeForPacket(Packet packet) {
				return packet.getStanzaFrom().hashCode();
			}
		});
		Map<String,Object> props = new HashMap<>();
		props.put(CLUSTER_SYS_CONNECTIONS_PER_NODE_PROP_KEY, 2);
		selector.setProperties(props);
		
		Element el = new Element("iq", new String[] { "from" }, new String[] { "test1" });
		Packet p = Packet.packetInstance(el);
		assertNull(selector.selectConnection(p, conn));
		
		XMPPIOService<Object> serv1 = new XMPPIOService<Object>();
		conn.addConn(serv1);
		assertEquals(serv1, selector.selectConnection(p, conn));
		
		p.setPriority(Priority.SYSTEM);
		assertEquals(serv1, selector.selectConnection(p, conn));

		p.setPriority(null);
		XMPPIOService<Object> serv2 = new XMPPIOService<Object>();
		conn.addConn(serv2);
		Set<XMPPIOService<Object>> sysServs = new HashSet<>(Arrays.asList(serv1, serv2));
		assertEquals(2, conn.size());
		assertTrue(sysServs.contains(selector.selectConnection(p, conn)));
		
		p.setPriority(Priority.SYSTEM);
		assertTrue(sysServs.contains(selector.selectConnection(p, conn)));

		p.setPriority(null);
		XMPPIOService<Object> serv3 = new XMPPIOService<Object>();
		conn.addConn(serv3);
		assertEquals(3, conn.size());
		assertSame(serv3, selector.selectConnection(p, conn));
		
		p.setPriority(Priority.SYSTEM);
		assertTrue(sysServs.contains(selector.selectConnection(p, conn)));
		
		el = new Element("iq", new String[] { "from" }, new String[] { "test2" });
		p = Packet.packetInstance(el);
		assertEquals(3, conn.size());
		assertSame(serv3, selector.selectConnection(p, conn));

		el = new Element("iq", new String[] { "from" }, new String[] { "test3" });
		p = Packet.packetInstance(el);
		assertEquals(3, conn.size());
		assertSame(serv3, selector.selectConnection(p, conn));

		el = new Element("iq", new String[] { "from" }, new String[] { "test4" });
		p = Packet.packetInstance(el);
		assertEquals(3, conn.size());
		assertSame(serv3, selector.selectConnection(p, conn));
	}

}
