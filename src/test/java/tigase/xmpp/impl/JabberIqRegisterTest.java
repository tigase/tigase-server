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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tigase.form.Field;
import tigase.form.Form;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.UUID;

public class JabberIqRegisterTest
		extends ProcessorTestCase {

	private JabberIqRegister jabberIqRegister = new JabberIqRegister();

	@Before
	@Override
	public void setUp() throws Exception {
		jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(new HashMap<String, Object>());
		super.setUp();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		jabberIqRegister = null;
		super.tearDown();
	}

	@Test
	public void testRegistrationForm() throws TigaseStringprepException, XMPPException {
		final Element iq = new Element("iq", new String[]{"type", "to", "id"},
									   new String[]{"set", "sure.im", "some-id"});
		final Element query = new Element("query", new String[]{"xmlns"}, new String[]{"jabber:iq:register"});
		final Form form = new Form("submit", "Registration form", "Fill out the form");
		form.addField(Field.fieldTextSingle("username", "wojtektest", "Username"));
		form.addField(Field.fieldTextPrivate("password", "wojtektestpass", "Password"));
		form.addField(Field.fieldTextSingle("email", "wojtek@example.com ", "Email (MUST BE VALID!)"));

		query.addChild(form.getElement());
		iq.addChild(query);

		final Packet packet = Packet.packetInstance(iq);
		final JID connId = JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString());
		packet.setPacketFrom(connId);

		BareJID userJid = BareJID.bareJIDInstance("wojtektest@example.com");
		JID userResource = JID.jidInstance(userJid, "resource");
		XMPPResourceConnection session2 = getSession(connId, userResource);
		Queue<Packet> results = new ArrayDeque<>();
		jabberIqRegister.process(packet, session2, null, results, null);

		Assert.assertTrue(!results.isEmpty());
		final Packet response = results.poll();
		Assert.assertTrue(StanzaType.result.equals(response.getType()));
	}
}