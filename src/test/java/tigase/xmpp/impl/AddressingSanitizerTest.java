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
package tigase.xmpp.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tigase.TestLogger;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * @author Wojtek
 */
public class AddressingSanitizerTest
		extends ProcessorTestCase {

	private static final Logger log = TestLogger.getLogger(AddressingSanitizerTest.class);

	private AddressingSanitizer addressingSanitizer;
	private JID recipientJid = JID.jidInstanceNS("recipient@example.com/res-2");
	private Queue<Packet> results;
	private JID senderJid = JID.jidInstanceNS("sender@example.com/res-1");
	private XMPPResourceConnection senderSession;
	private Map<String, Object> settings = new HashMap<>();

	public AddressingSanitizerTest() {
	}

	@Before
	public void setUp() throws Exception {
		addressingSanitizer = new AddressingSanitizer();
		senderSession = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()), senderJid);
		results = new ArrayDeque<>();
	}

	@After
	public void tearDown() throws Exception {
		addressingSanitizer = null;
		senderSession = null;
		results = null;
	}

	@Test
	public void testMessageWithFromAndTo()
			throws TigaseStringprepException, NotAuthorizedException, NoConnectionIdException {
		Packet p = getPacket("message", null, null);
		p.setPacketFrom(senderSession.getConnectionId());

		p.getElement().setAttribute("from", senderJid.getBareJID().toString());
		p.getElement().setAttribute("to", recipientJid.toString());
		p.initVars();

		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(senderJid, p.getStanzaFrom());
		assertEquals(recipientJid, p.getStanzaTo());
	}

	@Test
	public void testMessageWithIncorrectFromInStanza()
			throws TigaseStringprepException, NotAuthorizedException, NoConnectionIdException {
		Packet p = getPacket("message", null, "my_user@domain.com");
		p.setPacketFrom(senderSession.getConnectionId());

		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(senderJid, p.getStanzaFrom());
		assertEquals(senderJid.copyWithoutResource(), p.getStanzaTo());
	}

	@Test
	public void testMessageWithSameFromInStanza()
			throws TigaseStringprepException, NotAuthorizedException, NoConnectionIdException {
		Packet p = getPacket("message", null, senderJid.toString());
		p.setPacketFrom(senderSession.getConnectionId());

		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(senderJid, p.getStanzaFrom());
		assertEquals(senderJid.copyWithoutResource(), p.getStanzaTo());
	}

	@Test
	public void testMessageWithoutFrom()
			throws TigaseStringprepException, NotAuthorizedException, NoConnectionIdException {
		Packet p = getPacket("message", null, null);
		p.setPacketFrom(senderSession.getConnectionId());

		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(senderJid, p.getStanzaFrom());
		assertEquals(senderJid.copyWithoutResource(), p.getStanzaTo());
	}

	@Test
	public void testPresenceSubscriptionWithFromBareJid()
			throws TigaseStringprepException, NotAuthorizedException, NoConnectionIdException {
		Packet p = getPacket("presence", StanzaType.subscribe, senderJid.getBareJID().toString());
		p.getElement().setAttribute("type", "subscribe");
		p.setPacketFrom(senderSession.getConnectionId());

		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(JID.jidInstance(senderJid.getBareJID()), p.getStanzaFrom());
		assertNull(p.getStanzaTo());

		// temporarily we set authorised stanza in connection manger and we don't apply full logic here, let's make sure
		// the result is correct as well (authorised from is _always_ correct).
		p.setServerAuthorisedStanzaFrom(JID.jidInstanceNS(senderJid.getBareJID()));
		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(JID.jidInstance(senderJid.getBareJID()), p.getStanzaFrom());
		assertNull(p.getStanzaTo());
	}

	@Test
	public void testPresenceSubscriptionWithFromFullJid()
			throws TigaseStringprepException, NotAuthorizedException, NoConnectionIdException {
		Packet p = getPacket("presence", StanzaType.subscribe, senderJid.toString());
		p.getElement().setAttribute("type", "subscribe");
		p.setPacketFrom(senderSession.getConnectionId());

		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(JID.jidInstance(senderJid.getBareJID()), p.getStanzaFrom());

		// temporarily we set authorised stanza in connection manger and we don't apply full logic here, let's make sure
		// the result is correct as well (authorised from is _always_ correct).
		p.setServerAuthorisedStanzaFrom(senderJid);
		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(JID.jidInstance(senderJid.getBareJID()), p.getStanzaFrom());
		assertNull(p.getStanzaTo());
	}

	@Test
	public void testPresenceSubscriptionWithIncorrectFrom()
			throws TigaseStringprepException, NotAuthorizedException, NoConnectionIdException {
		final JID from = JID.jidInstanceNS("my_user@domain.com");
		Packet p = getPacket("presence", StanzaType.subscribe, from.toString());
		p.getElement().setAttribute("type", "subscribe");
		p.setPacketFrom(senderSession.getConnectionId());

		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(JID.jidInstance(senderJid.getBareJID()), p.getStanzaFrom());
		assertNull(p.getStanzaTo());

		// temporarily we set authorised stanza in connection manger and we don't apply full logic here, let's make sure
		// the result is correct as well (authorised from is _always_ correct).
		p.setServerAuthorisedStanzaFrom(senderJid);
		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(JID.jidInstance(senderJid.getBareJID()), p.getStanzaFrom());
		assertNull(p.getStanzaTo());
	}

	@Test
	public void testPresenceSubscriptionWithoutFrom()
			throws TigaseStringprepException, NotAuthorizedException, NoConnectionIdException {
		Packet p = getPacket("presence", StanzaType.subscribe, null);
		p.setPacketFrom(senderSession.getConnectionId());
		p.initVars();

		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(JID.jidInstance(senderJid.getBareJID()), p.getStanzaFrom());
		assertNull(p.getStanzaTo());

		// temporarily we set authorised stanza in connection manger and we don't apply full logic here.
		p.setServerAuthorisedStanzaFrom(null);
		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(JID.jidInstance(senderJid.getBareJID()), p.getStanzaFrom());
		assertNull(p.getStanzaTo());
	}

	@Test
	public void testPresenceWithFrom()
			throws TigaseStringprepException, NotAuthorizedException, NoConnectionIdException {
		Packet p = getPacket("presence", null, senderJid.getBareJID().toString());
		p.setPacketFrom(senderSession.getConnectionId());

		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(senderJid, p.getStanzaFrom());
		assertNull(p.getStanzaTo());
	}

	@Test
	public void testPresenceWithWrongFrom()
			throws TigaseStringprepException, NotAuthorizedException, NoConnectionIdException {
		Packet p = getPacket("presence", null, "my_user@domain.com");
		p.setPacketFrom(senderSession.getConnectionId());

		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(senderJid, p.getStanzaFrom());
		assertNull(p.getStanzaTo());
	}

	@Test
	public void testPresenceWithoutFrom()
			throws TigaseStringprepException, NotAuthorizedException, NoConnectionIdException {
		Packet p = getPacket("presence", null, null);
		p.setPacketFrom(senderSession.getConnectionId());

		assertFalse(addressingSanitizer.preProcess(p, senderSession, null, results, settings));
		assertEquals(0, results.size());
		assertEquals(senderJid, p.getStanzaFrom());
		assertNull(p.getStanzaTo());
	}

	private Packet getPacket(String elementName, StanzaType type, String from) throws TigaseStringprepException {
		Element element = new Element(elementName);
		if (from != null && !from.isEmpty()) {
			element.setAttribute("from", from);
		}
		if (type != null) {
			element.setAttribute("type", type.toString());
		}
		return Packet.packetInstance(element);
	}

}
