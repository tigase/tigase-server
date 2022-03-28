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
import tigase.auth.BruteForceLockerBean;
import tigase.auth.TigaseSaslProvider;
import tigase.eventbus.EventBusFactory;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.JID;

import java.util.HashMap;
import java.util.LinkedList;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;

public class JabberIqAuthTest
		extends ProcessorTestCase {

	@Before
	public void prepare() {
		getKernel().registerBean("eventbus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		getKernel().registerBean("sess-man").asInstance(new SessionManager()).setActive(true).exportable().exec();
		getKernel().registerBean(BruteForceLockerBean.class).setActive(true).exportable().exec();
		getKernel().registerBean(TigaseSaslProvider.class).setActive(true).exportable().exec();
		getKernel().registerBean(JabberIqAuth.class).setActive(true).exec();

	}

	@Test
	public void testAuthMissingPassword() throws XMPPException, TigaseStringprepException {
		JID jid1 = JID.jidInstance("user1@example.com/res1");
		XMPPResourceConnection session = getSession(jid1, jid1, false);

		/*
		<iq id="mira622e13a35eac130_2" type="set" xmlns="jabber:client">
			<query xmlns="jabber:iq:auth">
				<username>abcde123</username>
				<password/>
				<resource>Miranda</resource>
			</query>
		</iq>
		 */

		Element iqElement = new Element("iq", new String[]{"xmlns", "type"},
								   new String[]{Iq.CLIENT_XMLNS, StanzaType.set.name()});
		final Element query = new Element("query", new String[]{"xmlns"}, new String[]{JabberIqAuth.ID});
		query.addChild(new Element("username", "abcde123"));
		query.addChild(new Element("password"));
		query.addChild(new Element("resource", "Miranda"));
		iqElement.addChild(query);
		Packet iq = Packet.packetInstance(iqElement);
		iq.setPacketFrom(jid1);

		JabberIqAuth processor = getKernel().getInstance(JabberIqAuth.class);
		assertEquals(Authorization.AUTHORIZED, processor.canHandle(iq, session));

		LinkedList<Packet> results = new LinkedList<>();
		
		processor.process(iq, session, null, results, new HashMap<>());

		assertEquals(2, results.size());
		assertNotNull(results.get(0).getElement().getChild("error").getChild("not-acceptable"));
		assertEquals("CLOSE", results.get(1).getElement().getChild("command").getAttributeStaticStr("node"));
	}
}
