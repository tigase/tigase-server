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

import org.junit.Test;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;

import static org.junit.Assert.*;

public class ElementMatcherTest {

	@Test
	public void testXMLNSOnly() throws TigaseStringprepException {
		String matcherStr = "[urn:ietf:params:xml:ns:xmpp-sasl]";
		ElementMatcher matcher = ElementMatcher.create(matcherStr);
		assertNotNull(matcher);

		assertEquals(matcherStr, matcher.toString());

		Packet packet = Packet.packetInstance(new Element("message", new String[] { "xmlns" }, new String[] { "jabber:client" }));
		assertFalse(matcher.matches(packet));
		packet = Packet.packetInstance(new Element("message", new String[] { "xmlns" }, new String[] { "urn:ietf:params:xml:ns:xmpp-sasl" }));
		assertTrue(matcher.matches(packet));
	}

}