/*
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
package tigase.cluster.strategy;

import org.junit.Test;
import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.CommandListener;
import tigase.cluster.api.SessionManagerClusteredIfc;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.AbstractKernelTestCase;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.util.Algorithms;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class DefaultClusterStrategyTest extends AbstractKernelTestCase {
	
	@Override
	protected void registerBeans(Kernel kernel) {
		super.registerBeans(kernel);
		kernel.registerBean("eventbus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		kernel.registerBean("sess-man").asInstance(new DummySessionManagerClustered()).exportable().exec();
		kernel.registerBean("cluster-contr").asInstance(new DummyClusterController()).exportable().exec();
		kernel.registerBean("strategy").asClass(getStrategyClass()).setActive(true).exportable().exec();
	}

	protected Class<? extends DefaultClusteringStrategyAbstract> getStrategyClass() {
		return DefaultClusteringStrategy.class;
	}
	
	@Test
	public void testIqResponseRedirection() throws TigaseStringprepException {
		DefaultClusteringStrategyAbstract strategy = getInstance(DefaultClusteringStrategyAbstract.class);
		assertEquals(Collections.emptyList(), strategy.getNodesConnected());

		String node1 = UUID.randomUUID().toString();
		String node2 = UUID.randomUUID().toString();
		String node3 = UUID.randomUUID().toString(); // local node

		JID senderJid = JID.jidInstance(UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);

		DummySessionManagerClustered sm = getInstance(DummySessionManagerClustered.class);
		sm.nodeConnected(node1);
		strategy.nodeConnected(JID.jidInstance("sess-man", node1));
		sm.nodeConnected(node2);
		strategy.nodeConnected(JID.jidInstance("sess-man", node2));

		Packet packet = Packet.packetInstance(new Element("iq").withAttribute("type", "result"), senderJid, JID.jidInstance(null, "localhost",
																															Algorithms.sha256(node2)));
		assertTrue(strategy.isSuitableForForward(packet));
		assertTrue(strategy.isIqResponseToNode(packet));
		assertEquals(Collections.singletonList(JID.jidInstance("sess-man", node2)), strategy.getNodesForIqResponse(packet));
		assertEquals(Collections.singletonList(JID.jidInstance("sess-man", node2)), strategy.getNodesForPacketForward(sm.getComponentId(), null, packet));

		packet = Packet.packetInstance(new Element("iq").withAttribute("type", "result"), senderJid, JID.jidInstance(null, "localhost",
																															Algorithms.sha256(node3)));
		assertTrue(strategy.isSuitableForForward(packet));
		assertTrue(strategy.isIqResponseToNode(packet));
		assertEquals(null, strategy.getNodesForIqResponse(packet));
		assertEquals(null, strategy.getNodesForPacketForward(sm.getComponentId(), null, packet));
	}

	private class DummyClusterController implements ClusterControllerIfc {

		private List<String> nodes = new ArrayList<>();

		@Override
		public void handleClusterPacket(Element packet) {

		}

		@Override
		public void nodeConnected(String addr) {
			if (!nodes.contains(addr)) {
				nodes.add(addr);
			}
		}

		@Override
		public void nodeDisconnected(String addr) {
			nodes.remove(addr);
		}

		@Override
		public void removeCommandListener(CommandListener listener) {

		}

		@Override
		public void sendToNodes(String command, Map<String, String> data, Queue<Element> packets, JID fromNode,
								Set<JID> visitedNodes, JID... toNodes) {

		}

		@Override
		public void sendToNodes(String command, Queue<Element> packets, JID fromNode, Set<JID> visitedNodes,
								JID... toNodes) {

		}

		@Override
		public void sendToNodes(String command, Map<String, String> data, JID fromNode, Set<JID> visitedNodes,
								JID... toNodes) {

		}

		@Override
		public void sendToNodes(String command, Map<String, String> data, JID fromNode, JID... toNodes) {

		}

		@Override
		public void sendToNodes(String command, JID fromNode, JID... toNodes) {

		}

		@Override
		public void sendToNodes(String command, Element packet, JID fromNode, Set<JID> visitedNodes, JID... toNodes) {

		}

		@Override
		public void sendToNodes(String command, Map<String, String> data, Element packet, JID fromNode,
								Set<JID> visitedNodes, JID... toNodes) {

		}

		@Override
		public void setCommandListener(CommandListener listener) {

		}
	}

	private class DummySessionManagerClustered
			implements SessionManagerClusteredIfc {

		@Override
		public boolean fastAddOutPacket(Packet packet) {
			return false;
		}

		@Override
		public void processPacket(Packet el_packet, XMPPResourceConnection conn) {

		}

		@Override
		public void processPresenceUpdate(XMPPSession session, Element element) {

		}

		@Override
		public XMPPResourceConnection getXMPPResourceConnection(Packet el_packet) {
			return null;
		}

		@Override
		public ConcurrentHashMap<JID, XMPPResourceConnection> getXMPPResourceConnections() {
			return null;
		}

		@Override
		public ConcurrentHashMap<BareJID, XMPPSession> getXMPPSessions() {
			return null;
		}

		@Override
		public boolean hasXMPPResourceConnectionForConnectionJid(JID connJid) {
			return false;
		}

		@Override
		public JID getComponentId() {
			return JID.jidInstanceNS("sess-man@" + UUID.randomUUID().toString());
		}

		@Override
		public void handleLogin(BareJID userId, XMPPResourceConnection conn) {

		}

		@Override
		public void handleDomainChange(String domain, XMPPResourceConnection conn) {

		}

		@Override
		public void handleLogout(BareJID userId, XMPPResourceConnection conn) {

		}

		@Override
		public void handlePresenceSet(XMPPResourceConnection conn) {

		}

		@Override
		public void handleResourceBind(XMPPResourceConnection conn) {

		}

		@Override
		public boolean isLocalDomain(String domain, boolean includeComponents) {
			return "localhost".equals(domain);
		}

		private List<JID> connectedNodes = new ArrayList<>();

		@Override
		public List<JID> getNodesConnected() {
			return connectedNodes;
		}

		public String getName() {
			return "sess-man";
		}

		public void nodeConnected(String node) {
			JID jid = JID.jidInstanceNS(getName(), node, null);
			if (!connectedNodes.contains(jid) && !getComponentId().equals(jid)) {
				JID[] tmp = connectedNodes.toArray(new JID[connectedNodes.size() + 1]);
				tmp[tmp.length - 1] = jid;
				Arrays.sort(tmp);
				int pos = Arrays.binarySearch(tmp, jid);
				connectedNodes.add(pos, jid);
			}
		}

		public void nodeDisconnected(String node) {
			JID jid = JID.jidInstanceNS(getName(), node, null);
			connectedNodes.remove(jid);
		}
	}

}
