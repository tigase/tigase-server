/*
 * DialbackTest.java
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
package tigase.server.xmppserver.proc;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import junit.framework.TestCase;
import org.junit.Test;
import static tigase.net.IOService.PORT_TYPE_PROP_KEY;
import tigase.server.Packet;
import tigase.server.xmppserver.CID;
import tigase.server.xmppserver.CIDConnections;
import tigase.server.xmppserver.LocalhostException;
import tigase.server.xmppserver.NotLocalhostException;
import tigase.server.xmppserver.S2SConnectionHandlerIfc;
import tigase.server.xmppserver.S2SConnectionManager;
import tigase.server.xmppserver.S2SIOService;
import tigase.server.xmppserver.S2SRandomSelector;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;

/**
 *
 * @author andrzej
 */
public class DialbackTest extends TestCase {
	
	private Dialback dialback = new Dialback() {

		@Override
		public boolean skipTLSForHost(String hostname) {
			return true;
		}

		@Override
		protected boolean wasVerifyRequested(S2SIOService serv, String domain) {
			return true;
		}
		
	};
	private S2SConnectionHandlerIfc<S2SIOService> handler = null;
	
	private String remote1 = "remote1.com";
	private String remote2 = "remote2.com";
	private String local = "local.com";
	
	@Test
	public void testAuthorizationForSingleDomain() throws TigaseStringprepException {
		Queue<Packet> results = new ArrayDeque<>();
		handler = new S2SConnectionHandlerImpl(results);
		dialback.init(handler, new HashMap());
		
		String key = UUID.randomUUID().toString();
		
		S2SIOService serv = new S2SIOService();
		serv.setSessionId("sess-id-1");
		Map<String,Object> props = new HashMap<>();
		props.put(PORT_TYPE_PROP_KEY, "accept");
		serv.setSessionData(props);

		Packet p = null;
		
		Element resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote1);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);
		
		Packet r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "valid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);
		
		p = results.poll();
		assertTrue(p.getType() == StanzaType.valid && remote1.equals(p.getStanzaTo().getDomain()));
		
		serv.getCIDs().forEach((CID cid) -> assertEquals(remote1, cid.getRemoteHost()));
	}
	
	@Test
	public void testAuthorizationForSingleDomainFailure() throws TigaseStringprepException {
		Queue<Packet> results = new ArrayDeque<>();
		handler = new S2SConnectionHandlerImpl(results);
		dialback.init(handler, new HashMap());
		
		String key = UUID.randomUUID().toString();
		
		S2SIOService serv = new S2SIOService();
		serv.setSessionId("sess-id-1");
		Map<String,Object> props = new HashMap<>();
		props.put(PORT_TYPE_PROP_KEY, "accept");
		serv.setSessionData(props);

		Packet p = null;
		
		Element resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote1);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);
		
		Packet r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "invalid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);
		
		p = results.poll();
		assertTrue(p.getType() == StanzaType.invalid && remote1.equals(p.getStanzaTo().getDomain()));
		
		serv.getCIDs().forEach((CID cid) -> assertNotSame(remote1, cid.getRemoteHost()));
	}	
	
	@Test
	public void testAuthorizationWithMultiplexing() throws TigaseStringprepException {
		Queue<Packet> results = new ArrayDeque<>();
		handler = new S2SConnectionHandlerImpl(results);
		dialback.init(handler, new HashMap());
		
		String key = UUID.randomUUID().toString();
		
		S2SIOService serv = new S2SIOService();
		serv.setSessionId("sess-id-1");
		Map<String,Object> props = new HashMap<>();
		props.put(PORT_TYPE_PROP_KEY, "accept");
		serv.setSessionData(props);

		Packet p = null;
		
		Element resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote1);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);
		
		Packet r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "valid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);
		
		p = results.poll();
		assertTrue(p.getType() == StanzaType.valid && remote1.equals(p.getStanzaTo().getDomain()));
		serv.getCIDs().forEach((CID cid) -> assertEquals(remote1, cid.getRemoteHost()));
		
		key = UUID.randomUUID().toString();
		
		resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote2);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);
		
		r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "valid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);		
		
		p = results.poll();
		assertTrue(p.getType() == StanzaType.valid && remote2.equals(p.getStanzaTo().getDomain()));
		assertTrue(serv.getCIDs().stream().anyMatch((CID cid) -> remote1.equals(cid.getRemoteHost())));
		assertTrue(serv.getCIDs().stream().anyMatch((CID cid) -> remote2.equals(cid.getRemoteHost())));
	}

	@Test
	public void testAuthorizationWithMultiplexingWithFailure() throws TigaseStringprepException {
		Queue<Packet> results = new ArrayDeque<>();
		handler = new S2SConnectionHandlerImpl(results);
		dialback.init(handler, new HashMap());
		
		String key = UUID.randomUUID().toString();
		
		S2SIOService serv = new S2SIOService();
		serv.setSessionId("sess-id-1");
		Map<String,Object> props = new HashMap<>();
		props.put(PORT_TYPE_PROP_KEY, "accept");
		serv.setSessionData(props);

		Packet p = null;
		
		Element resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote1);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);
		
		Packet r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "valid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);
		
		p = results.poll();
		assertTrue(p.getType() == StanzaType.valid && remote1.equals(p.getStanzaTo().getDomain()));
		serv.getCIDs().forEach((CID cid) -> assertEquals(remote1, cid.getRemoteHost()));
		
		key = UUID.randomUUID().toString();
		
		resultEl = new Element("db:result");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("from", remote2);
		resultEl.setAttribute("to", local);
		resultEl.setCData(key);
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);
		
		r = results.poll();
		resultEl = new Element("db:verify");
		resultEl.setXMLNS(Dialback.XMLNS_DB_VAL);
		resultEl.setAttribute("id", r.getAttributeStaticStr("id"));
		resultEl.setAttribute("from", r.getAttributeStaticStr("to"));
		resultEl.setAttribute("to", r.getAttributeStaticStr("from"));
		resultEl.setAttribute("type", "invalid");
		p = Packet.packetInstance(resultEl);
		dialback.process(p, serv, results);		
		
		p = results.poll();
		assertTrue(p.getType() == StanzaType.invalid && remote2.equals(p.getStanzaTo().getDomain()));
		assertTrue(serv.getCIDs().stream().anyMatch((CID cid) -> remote1.equals(cid.getRemoteHost())));
		assertTrue(serv.getCIDs().stream().allMatch((CID cid) -> !remote2.equals(cid.getRemoteHost())));
	}

	
	private static class S2SConnectionHandlerImpl extends S2SConnectionManager {

		private Queue<Packet> results;
		
		public S2SConnectionHandlerImpl(Queue<Packet> results) {
			this.results = results;
		}

		@Override
		protected CIDConnections createNewCIDConnections(CID cid)
					throws NotLocalhostException, LocalhostException {

			CIDConnections conns =  new CIDConnections(cid, this, new S2SRandomSelector(),
						5, 5, 5,
						5000) {

					@Override
					public void sendHandshakingOnly(Packet verify_req) {
						results.offer(verify_req);
					}

					@Override
					public boolean sendControlPacket(String sessionId, Packet packet) {
						return results.offer(packet);
					}

					@Override
					public void sendPacket(Packet packet) {
						results.offer(packet);
					}
						
						};
			cidConnections.put(cid, conns);
			return conns;
		}

		@Override
		public boolean isTlsRequired(String domain) {
			return false;
		}
	}
	
	
}
