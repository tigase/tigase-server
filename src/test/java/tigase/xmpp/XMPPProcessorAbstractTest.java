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
package tigase.xmpp;

import org.junit.Before;
import org.junit.Test;
import tigase.db.NonAuthUserRepository;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.impl.ProcessorTestCase;
import tigase.xmpp.jid.JID;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

import static org.junit.Assert.*;

public class XMPPProcessorAbstractTest
		extends ProcessorTestCase {

	JID connId = JID.jidInstanceNS("c2s@localhost/recipient1-res1");
	String domain = "domain";
	JID recipient = JID.jidInstanceNS("recipient", domain, "resource");
	JID sender = JID.jidInstanceNS("sender", domain, "resource");
	private XMPPProcessorAbstract processor;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		processor = new SimpleXMPPProcessor();
	}

	@Test
	public void testProcessToUserPacketWithSession()
			throws TigaseStringprepException, PacketErrorTypeException, NotAuthorizedException {
		final Iq iq = getIqPacket();

		final ArrayDeque<Packet> results = new ArrayDeque<>();
		final XMPPResourceConnection session = getSession(connId, recipient, true);
		processor.processToUserPacket(iq, session, null, results, null);
		assertFalse(results.isEmpty());
		assertEquals(results.poll().getType(), StanzaType.result);
	}

	@Test
	public void testProcessToUserPacketWithoutSession()
			throws TigaseStringprepException, PacketErrorTypeException, NotAuthorizedException {
		final Iq iq = getIqPacket();

		final ArrayDeque<Packet> results = new ArrayDeque<>();
		final XMPPResourceConnection emptySession = getSession(connId, recipient, false);

		processor.processToUserPacket(iq, emptySession, null, results, null);
		assertTrue(results.isEmpty());
	}

	private Iq getIqPacket() throws TigaseStringprepException {
		final Element iqElement = new Element("iq").withAttribute("type", StanzaType.result.toString())
				.withAttribute("from", sender.toString())
				.withAttribute("to", recipient.toString());
		iqElement.addChild(new Element("ping").withAttribute("xmlns", "urn:xmpp:ping"));
		return new Iq(iqElement);
	}

	private static class SimpleXMPPProcessor
			extends XMPPProcessorAbstract {

		@Override
		public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
												  NonAuthUserRepository repo, Queue<Packet> results,
												  Map<String, Object> settings) throws PacketErrorTypeException {

		}

		@Override
		public void processServerSessionPacket(Packet packet, XMPPResourceConnection session,
											   NonAuthUserRepository repo, Queue<Packet> results,
											   Map<String, Object> settings) throws PacketErrorTypeException {

		}
	}
}