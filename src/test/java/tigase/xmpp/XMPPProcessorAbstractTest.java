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
import tigase.xmpp.jid.JID;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

import static org.junit.Assert.assertTrue;

public class XMPPProcessorAbstractTest {

	private XMPPProcessorAbstract processor;

	@Before
	public void setUp() {
		processor = new XMPPProcessorAbstract() {
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
		};
	}

	@Test
	public void testProcessToUserPacket() throws TigaseStringprepException, PacketErrorTypeException {
		final Element iqElement = new Element("iq", new String[]{"type", "to", "from"},
											  new String[]{"result", "toUser@domain.com", "fromUser@domain.com"});
		iqElement.addChild(new Element("ping", new String[]{"xmlns"}, new String[]{"urn:xmpp:ping"}));
		final Iq iq = new Iq(iqElement);

		final ArrayDeque<Packet> results = new ArrayDeque<>();
		final XMPPResourceConnection emptySession = new XMPPResourceConnection(null, null, null, null);
		processor.processToUserPacket(iq, emptySession, null, results, null);
		assertTrue(results.isEmpty());
	}
}