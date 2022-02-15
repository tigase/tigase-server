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

package tigase.server;

import org.junit.Assert;
import org.junit.Test;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xmpp.jid.JID;

import java.util.ArrayDeque;
import java.util.Queue;

public class MessageRouterTest {

	@Test
	public void processPacketMR() throws TigaseStringprepException {
		final ArrayDeque<Packet> results = new ArrayDeque<>();
		final MessageRouter messageRouter = new MessageRouter();
		String stanza = "<iq to=\"message-router@jabber.today\" type=\"error\" id=\"43C107AD-9B5C-499C-8A80-87C72ABF7C83\" xmlns=\"jabber:client\" from=\"a@jabber.today/iPhone\"><query xmlns=\"http://jabber.org/protocol/disco#info\"><identity name=\"Tigase ver. 8.2.0-SNAPSHOT-b11574/c2603d9c\" type=\"router\" category=\"component\"/><identity name=\"Tigase ver. 8.2.0-SNAPSHOT-b11574/c2603d9c\" type=\"im\" category=\"server\"/><feature var=\"http://jabber.org/protocol/commands\"/><x type=\"result\" xmlns=\"jabber:x:data\"><field type=\"hidden\" var=\"FORM_TYPE\"><value>http://jabber.org/network/serverinfo</value></field><field type=\"list-multi\" var=\"abuse-addresses\"><value>mailto:support@tigase.net</value><value>xmpp:tigase@muc.tigase.org</value><value>https://tigase.net/technical-support</value></field></x></query><error code=\"404\" type=\"wait\"><recipient-unavailable xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\"/><text xmlns=\"urn:ietf:params:xml:ns:xmpp-stanzas\" xml:lang=\"en\">The target is unavailable at this time.</text></error></iq>";

		SimpleParser parser = new SimpleParser();
		final DomBuilderHandler handler = new DomBuilderHandler();
		parser.parse(handler, stanza);
		final Queue<Element> parsedElements = handler.getParsedElements();
		final Element element = parsedElements.peek();
		final Packet packet = Packet.packetInstance(element);
		packet.setPacketFrom(JID.jidInstanceNS("sess-man@ip-172-31-38-91.us-west-2.compute.internal"));
		packet.setPacketTo(JID.jidInstanceNS("message-router@jabber.today"));
		packet.initVars();
		messageRouter.processPacketMR(packet, results);
		Assert.assertTrue(results.isEmpty());

	}
}