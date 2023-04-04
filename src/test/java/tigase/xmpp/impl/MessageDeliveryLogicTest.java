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

import org.junit.Before;
import org.junit.Test;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.vhosts.DummyVHostManager;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author andrzej
 */
public class MessageDeliveryLogicTest
		extends ProcessorTestCase {

	final static String domain = "example.com";
	private MessageDeliveryLogic messageDeliveryLogic;

	@Override
	protected void registerBeans(Kernel kernel) {
		super.registerBeans(kernel);
		kernel.registerBean(MessageDeliveryLogic.class).exec();
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		messageDeliveryLogic = getInstance(MessageDeliveryLogic.class);
		var vHostManager = getInstance(DummyVHostManager.class);
		vHostManager.addVhost(domain);
	}

	@Test
	public void testProcessingErrorMessageBareJidOffline() throws Exception {
		var destinationUserJid = BareJID.bareJIDInstance("user1", domain);
		var packetElelement = new Element("message", new String[]{"type", "from", "to"},
		                                  new String[]{"error", "remote-user@test.com/res1",
		                                               destinationUserJid.toString()});
		var packet = Packet.packetInstance(packetElelement);
		final boolean block = messageDeliveryLogic.preProcessFilter(packet, null);
		assertTrue("BareJID: No Available or Connected Resources > 8.5.2.2.1. Message: drop message", block);
	}

	@Test
	public void testProcessingErrorMessageBareJidOnline() throws Exception {
		var destinationUserJid = BareJID.bareJIDInstance("user1", domain);
		var fullDestinationJid = JID.jidInstance(destinationUserJid, "res1");
		var session = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID()), fullDestinationJid);

		var packetElelement = new Element("message", new String[]{"type", "from", "to"},
		                                  new String[]{"error", "remote-user@test.com/res1",
		                                               destinationUserJid.toString()});
		var packet = Packet.packetInstance(packetElelement);
		final boolean block = messageDeliveryLogic.preProcessFilter(packet, session);
		assertTrue("BareJID: Available or Connected Resources > 8.5.2.1.1. Message: drop message", block);
	}

	@Test
	public void testProcessingErrorMessageFullJidOnlineWithMatchingResource() throws Exception {
		var destinationUserJid = BareJID.bareJIDInstance("user1", domain);
		var fullDestinationJid = JID.jidInstance(destinationUserJid, "res1");
		var session = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID()), fullDestinationJid);

		var packetElelement = new Element("message", new String[]{"type", "from", "to"},
		                                  new String[]{"error", "remote-user@test.com/res1",
		                                               fullDestinationJid.toString()});
		var packet = Packet.packetInstance(packetElelement);
		final boolean block = messageDeliveryLogic.preProcessFilter(packet, session);
		assertFalse("FullJID: 8.5.3.1.  Resource Matches. Message: deliver message", block);
	}

	@Test
	public void testProcessingErrorMessageFullJidOnlineWithouthMatchingResource() throws Exception {
		var destinationUserJid = BareJID.bareJIDInstance("user1", domain);
		var fullDestinationJidPacketTo = JID.jidInstance(destinationUserJid, "res1");
		var fullDestinationJidSession = JID.jidInstance(destinationUserJid, "res2");
		var session = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID()), fullDestinationJidSession);

		var packetElelement = new Element("message", new String[]{"type", "from", "to"},
		                                  new String[]{"error", "remote-user@test.com/res1",
		                                               fullDestinationJidPacketTo.toString()});
		var packet = Packet.packetInstance(packetElelement);
		final boolean block = messageDeliveryLogic.preProcessFilter(packet, session);
		assertTrue("FullJID: No Resource Matches > 8.5.3.2.1. Message: drop message", block);
	}

	@Test
	public void testProcessingErrorMessagePubSubPayload() throws Exception {

		var destinationChannelJid = JID.jidInstance("channel", "mix." + domain, UUID.randomUUID().toString());
		var session = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID()),
		                         JID.jidInstance("user1@example.com/res1"));

		var packetElelement = new Element("message", new String[]{"type", "from", "to"},
		                                  new String[]{"error", "remote-user@test.com/res1",
		                                               destinationChannelJid.toString()});
		var packet = Packet.packetInstance(packetElelement);
		boolean block = messageDeliveryLogic.preProcessFilter(packet, session);
		assertFalse("Message error addressed to PubSub/MIX should be correctly forwarded to the component", block);

		var destinationChannelFullJid = BareJID.bareJIDInstance("channel", "mix." + domain);
		packetElelement = new Element("message", new String[]{"type", "from", "to"},
		                              new String[]{"error", "remote-user@test.com/res1",
		                                           destinationChannelFullJid.toString()});
		packet = Packet.packetInstance(packetElelement);
		block = messageDeliveryLogic.preProcessFilter(packet, session);
		assertFalse("Message error addressed to PubSub/MIX should be correctly forwarded to the component", block);
	}

	@Test
	public void testProcessingFailureErrorMissingToMissingFrom() throws Exception {

		//<failure xmlns="urn:ietf:params:xml:ns:xmpp-sasl" type="error">
		//    <not-authorized xmlns="urn:ietf:params:xml:ns:xmpp-sasl"/>
		//    <text xml:lang="en">Password not verified</text>
		//    <error code="404" type="wait">
		//        <recipient-unavailable xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>
		//    </error>
		//</failure>

		var destinationUserJid = BareJID.bareJIDInstance("user1", domain);
		var fullDestinationJid = JID.jidInstance(destinationUserJid, "res1");
		var session = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID()), fullDestinationJid);

		var packetElelement = new Element("failure", new String[]{"type", "xmlns"},
		                                  new String[]{"error", "urn:ietf:params:xml:ns:xmpp-sasl"});
		packetElelement.addChild(new Element("not-authorized", new String[]{" xmlns"},
		                                     new String[]{"urn:ietf:params:xml:ns:xmpp-sasl"}));
		packetElelement.addChild(new Element("text", "Password not verified"));
		var packet = Packet.packetInstance(packetElelement);
		final boolean block = messageDeliveryLogic.preProcessFilter(packet, session);
		assertFalse("Only Message packets should be processed", block);
	}

	@Test
	public void testProcessingIqErrorMissingTo() throws Exception {

		//<iq xmlns="jabber:client" from="user@sure.im/movimklqp20" type="error" id="id-1"/>

		var destinationUserJid = BareJID.bareJIDInstance("user1", domain);
		var fullDestinationJid = JID.jidInstance(destinationUserJid, "res1");
		var session = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID()), fullDestinationJid);

		var packetElelement = new Element("iq", new String[]{"type", "from"},
		                                  new String[]{"error", "remote-user@test.com/res1"});
		var packet = Packet.packetInstance(packetElelement);
		final boolean block = messageDeliveryLogic.preProcessFilter(packet, session);
		assertFalse("Only Message packets should be processed", block);
	}
}
