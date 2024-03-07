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
import tigase.eventbus.EventBusFactory;
import tigase.kernel.core.Kernel;
import tigase.server.Iq;
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

import static org.junit.Assert.assertEquals;

public class BindResourceTest extends ProcessorTestCase {

	private BindResource bindResource;

	@Before
	@Override
	public void setUp() throws Exception {
		bindResource = getInstance(BindResource.class);
		bindResource.init(new HashMap<>());
		super.setUp();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		bindResource = null;
		super.tearDown();
	}

	@Test
	public void testNoEscaping() throws TigaseStringprepException, XMPPException {
		BareJID user = BareJID.bareJIDInstance("test@example.com");
		testAuthentication(user, "test-1", "test-1");
	}

	@Test
	public void testEscapingApos() throws TigaseStringprepException, XMPPException {
		BareJID user = BareJID.bareJIDInstance("test@example.com");
		testAuthentication(user, "test's", "test&apos;s");
	}

	@Test
	public void testEscapingQuote() throws TigaseStringprepException, XMPPException {
		BareJID user = BareJID.bareJIDInstance("test@example.com");
		testAuthentication(user, "test \"cat\"", "test &quot;cat&quot;");
	}
	
	private void testAuthentication(BareJID user, String resource, String expectedResource) throws TigaseStringprepException, XMPPException {
		XMPPResourceConnection conn = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID().toString()),
												 JID.jidInstance(user, "res"), false);
		conn.authorizeJID(user, false);
		Queue<Packet> results = new ArrayDeque<>();
		Element iq = new Element("iq").withAttribute("type", "set")
				.withAttribute("id", "test-1")
				.withElement("bind", BindResource.ID, bindEl -> {
					bindEl.withElement("resource", null, resource);
				});
		bindResource.process(new Iq(iq), conn, null, results, null);
		assertEquals(1, results.size());
		assertEquals(expectedResource, conn.getResource());
		Packet result = results.poll();
		assertEquals(StanzaType.result, result.getType());
		assertEquals(JID.jidInstance(user, expectedResource).toString(), result.getElemChild("bind", BindResource.ID).getChild("jid").getCData());
	}

	@Override
	protected void registerBeans(Kernel kernel) {
		super.registerBeans(kernel);
		kernel.registerBean("eventbus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		kernel.registerBean(BindResource.class).setActive(true).exportable().exec();
	}
}
