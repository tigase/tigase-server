/**
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
import tigase.db.TigaseDBException;
import tigase.eventbus.EventBusFactory;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Test class for JabberIqPrivacyTest
 * <br>
 * Currently class tests validateList method checking validation of type, subscription and action. Other cases are not
 * tested due to missing instance of XMPPResourceConnection
 */
public class JabberIqPrivacyTest
		extends ProcessorTestCase {

	private static final Logger log = TestLogger.getLogger(JabberIqPrivacyTest.class);

	private JabberIqPrivacy privacyFilter;
	private ArrayDeque<Packet> results;

	@Before
	public void setUp() throws Exception {
		try {
			getKernel().registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();
			getKernel().registerBean(RosterFactory.Bean.class).setActive(true).exec();
			getKernel().getInstance(RosterFactory.Bean.class);
			getKernel().registerBean(JabberIqPrivacy.class).exec();

			privacyFilter = getInstance(JabberIqPrivacy.class);
			getKernel().getDependencyManager()
					.getBeanConfig("jabber:iq:privacy")
					.getKernel()
					.setBeanActive("privacyListOfflineCache", true);
			results = new ArrayDeque<Packet>();
		} catch (Exception ex) {
			ex.printStackTrace();
			assert (false);
		}
	}

	@After
	public void tearDown() throws Exception {
		privacyFilter = null;
	}

	@Test
	public void testValidateListGood() {
		List<Element> items = new ArrayList<Element>();

		Authorization result = null;

		items.add(new Element("item", new String[]{"type", "value", "action", "order"},
							  new String[]{"subscription", "both", "allow", "10"}));
		items.add(new Element("item", new String[]{"action", "order"}, new String[]{"deny", "15"}));

		// session is allowed to be null here
		result = JabberIqPrivacy.validateList(null, items);
		assertEquals(null, result);
	}

	@Test
	public void testValidateListBadAction() {
		List<Element> items = new ArrayList<Element>();

		Authorization result = null;

		items.add(new Element("item", new String[]{"type", "value", "action", "order"},
							  new String[]{"subscription", "both", "ignore", "10"}));
		items.add(new Element("item", new String[]{"action", "order"}, new String[]{"deny", "15"}));

		// session is allowed to be null here
		result = JabberIqPrivacy.validateList(null, items);
		assertEquals(Authorization.BAD_REQUEST, result);
	}

	@Test
	public void testValidateListBadSubscription() {
		List<Element> items = new ArrayList<Element>();

		Authorization result = null;

		items.add(new Element("item", new String[]{"type", "value", "action", "order"},
							  new String[]{"subscription", "or", "allow", "10"}));
		items.add(new Element("item", new String[]{"action", "order"}, new String[]{"deny", "15"}));

		// session is allowed to be null here
		result = JabberIqPrivacy.validateList(null, items);
		assertEquals(Authorization.BAD_REQUEST, result);
	}

	@Test
	public void testValidateListBadType() {
		List<Element> items = new ArrayList<Element>();

		Authorization result = null;

		items.add(new Element("item", new String[]{"type", "value", "action", "order"},
							  new String[]{"other", "both", "allow", "10"}));
		items.add(new Element("item", new String[]{"action", "order"}, new String[]{"deny", "15"}));

		// session is allowed to be null here
		result = JabberIqPrivacy.validateList(null, items);
		assertEquals(Authorization.BAD_REQUEST, result);
	}

	@Test
	public void testValidateListOrderUnsignedInt() {
		List<Element> items = new ArrayList<Element>();

		Authorization result = null;

		items.add(new Element("item", new String[]{"type", "value", "action", "order"},
							  new String[]{"subscription", "both", "allow", "-10"}));
		items.add(new Element("item", new String[]{"action", "order"}, new String[]{"deny", "15"}));

		// session is allowed to be null here
		result = JabberIqPrivacy.validateList(null, items);
		assertEquals(Authorization.BAD_REQUEST, result);

	}

	@Test
	public void testValidateListOrderAttributeDuplicate() {
		List<Element> items = new ArrayList<Element>();

		Authorization result = null;

		items.add(new Element("item", new String[]{"type", "value", "action", "order"},
							  new String[]{"subscription", "both", "allow", "10"}));
		items.add(new Element("item", new String[]{"action", "order"}, new String[]{"deny", "10"}));

		// session is allowed to be null here
		result = JabberIqPrivacy.validateList(null, items);
		assertEquals(Authorization.BAD_REQUEST, result);

	}

	@Test
	public void testFilterPresenceOut() throws Exception {
		JID jid = JID.jidInstance("test@example/res-1");
		JID connId = JID.jidInstance("c2s@example.com/asdasd");
		XMPPResourceConnection session = getSession(connId, jid);

		//List<Element> items = new ArrayList<Element>();
		Element list = new Element("list", new String[]{"name"}, new String[]{"default"});
		Element item = new Element("item", new String[]{"type", "value", "action", "order"},
								   new String[]{"jid", "test1.example.com", "deny", "100"});
		item.addChild(new Element("presence-out"));
		list.addChild(item);
		list.addChild(new Element("item", new String[]{"action", "order"}, new String[]{"allow", "110"}));

		session.putSessionData("active-list", PrivacyList.create(null, list));

		Packet presence = Packet.packetInstance(new Element("presence", new String[]{"from", "to"},
															new String[]{"test@example/res-1", "test1.example.com"}));

		assertFalse(privacyFilter.allowed(presence, session));

		presence = Packet.packetInstance(new Element("presence", new String[]{"to", "from"},
													 new String[]{"test@example/res-1", "test1.example.com"}));

		assertTrue(privacyFilter.allowed(presence, session));
	}

	@Test
	public void testGroupSubscriptionTypeFiltering() throws Exception {
		JID jid = JID.jidInstance("test@example/res-1");
		JID connId = JID.jidInstance("c2s@example.com/asdasd");
		XMPPResourceConnection session = getSession(connId, jid);
		session.putCommonSessionData("roster", new ConcurrentHashMap<BareJID, RosterElement>());

		//List<Element> items = new ArrayList<Element>();
		Element list = new Element("list", new String[]{"name"}, new String[]{"default"});
		Element item = new Element("item", new String[]{"type", "value", "action", "order"},
								   new String[]{"subscription", "none", "deny", "100"});
		item.addChild(new Element("presence-out"));
		item.addChild(new Element("presence-in"));
		item.addChild(new Element("message"));
		list.addChild(item);
		list.addChild(new Element("item", new String[]{"action", "order"}, new String[]{"allow", "110"}));

		session.putSessionData("active-list",
							   PrivacyList.create((Map<BareJID, RosterElement>) session.getCommonSessionData("roster"),
												  list));

		Packet presence = Packet.packetInstance(new Element("presence", new String[]{"from", "to"},
															new String[]{"test@example/res-1", "test1.example.com"}));

		assertFalse(privacyFilter.allowed(presence, session));

		Packet message = Packet.packetInstance(new Element("message", new String[]{"from", "to"},
														   new String[]{"test1.example.com",
																		"test@example.com/res-1"}));
		assertFalse(privacyFilter.allowed(message, session));

		Packet iq = Packet.packetInstance(new Element("iq", new String[]{"from", "to"},
													  new String[]{"test1.example.com", "test@example.com/res-1"}));
		assertTrue(privacyFilter.allowed(iq, session));
	}

	@Test
	public void testStanzaType() throws Exception {
		JID jid = JID.jidInstance("test@example/res-1");
		JID connId = JID.jidInstance("c2s@example.com/asdasd");
		XMPPResourceConnection session = getSession(connId, jid);

		checkStanzaType(session, "get", null, 1, StanzaType.result);
		checkStanzaType(session, "set", new Element("active"), 1, StanzaType.result);
		checkStanzaType(session, "error", null, 0, null);
		checkStanzaType(session, "result", null, 0, null);
		checkStanzaType(session, "probe", null, 1, StanzaType.error);
		checkStanzaType(session, null, null, 1, StanzaType.error);
	}

	@Test
	public void testFilterJidCase() throws Exception {
		JID jid = JID.jidInstance("test@example/res-1");
		JID connId = JID.jidInstance("c2s@example.com/asdasd");
		XMPPResourceConnection session = getSession(connId, jid);

		String blockedJID = "CapitalisedJID@test.domain.com";

		Element list = new Element("list", new String[]{"name"}, new String[]{"default"});
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"jid", blockedJID.toLowerCase(), "deny", "100"}));
		list.addChild(new Element("item", new String[]{"action", "order"}, new String[]{"allow", "110"}));

		session.putSessionData("active-list", PrivacyList.create(null, list));

		Packet presence = Packet.packetInstance(
				new Element("presence", new String[]{"from", "to"}, new String[]{blockedJID, jid.toString()}));

		assertFalse(privacyFilter.allowed(presence, session));

		presence = Packet.packetInstance(new Element("presence", new String[]{"from", "to"},
													 new String[]{blockedJID.toLowerCase(), jid.toString()}));

		assertFalse(privacyFilter.allowed(presence, session));

		presence = Packet.packetInstance(
				new Element("presence", new String[]{"from", "to"}, new String[]{jid.toString(), blockedJID}));

		assertFalse(privacyFilter.allowed(presence, session));

		presence = Packet.packetInstance(new Element("presence", new String[]{"from", "to"},
													 new String[]{jid.toString(), blockedJID.toLowerCase()}));

		assertFalse(privacyFilter.allowed(presence, session));

	}

	@Test
	public void testPartialJidMatching() throws Exception {

		JID jid = JID.jidInstance("test@example/res-1");
		JID connId = JID.jidInstance("c2s@example.com/resource");
		XMPPResourceConnection session = getSession(connId, jid);

		testList(session, "partial_blocked@test.domain.com/resource", "partial_blocked@test.domain.com/resource",
				 false);
		testList(session, "partial_blocked@test.domain.com/resource", "partial_blocked@test2.domain.com/resource",
				 true);
		testList(session, "partial_blocked@test.domain.com/resource", "partial_blocked@test.domain.com/resource2",
				 true);
		testList(session, "partial_blocked@test.domain.com/resource", "partial_blocked2@test.domain.com/resource",
				 true);
		testList(session, "partial_blocked@test.domain.com/resource", "partial_blocked@test.domain.com", true);
		testList(session, "partial_blocked@test.domain.com/resource", "test.domain.com/true", true);
		testList(session, "partial_blocked@test.domain.com/resource", "test.domain.com", true);

		testList(session, "partial_blocked@test.domain.com", "partial_blocked@test.domain.com/resource", false);
		testList(session, "partial_blocked@test.domain.com", "partial_blocked@test.domain.com", false);
		testList(session, "partial_blocked@test.domain.com", "partial_blocked2@test.domain.com", true);
		testList(session, "partial_blocked@test.domain.com", "partial_blocked@test2.domain.com", true);
		testList(session, "partial_blocked@test.domain.com", "test.domain.com/true", true);
		testList(session, "partial_blocked@test.domain.com", "test.domain.com", true);

		testList(session, "test.domain.com/true", "partial_blocked@test.domain.com/resource", true);
		testList(session, "test.domain.com/true", "partial_blocked@test.domain.com", true);
		testList(session, "test.domain.com/true", "test.domain.com/true", false);
		testList(session, "test.domain.com/true", "test.domain.com", true);
		testList(session, "test.domain.com/true", "test.domain.com/true2", true);
		testList(session, "test.domain.com/true", "test.2domain.com", true);

		testList(session, "test.domain.com/true", "partial_blocked@test2.domain.com/resource", true);
		testList(session, "test.domain.com/true", "partial_blocked@test2.domain.com", true);
		testList(session, "test.domain.com/true", "test2.domain.com/true", true);
		testList(session, "test.domain.com/true", "test.domain.com/true2", true);
		testList(session, "test.domain.com/true", "test2.domain.com", true);

		testList(session, "test.domain.com", "partial_blocked@test.domain.com/resource", false);
		testList(session, "test.domain.com", "partial_blocked@test.domain.com", false);
		testList(session, "test.domain.com", "test.domain.com/true", false);
		testList(session, "test.domain.com", "test.domain.com", false);

		testList(session, "test.domain.com", "partial_blocked@test2.domain.com/resource", true);
		testList(session, "test.domain.com", "partial_blocked@test2.domain.com", true);
		testList(session, "test.domain.com", "test2.domain.com/true", true);
		testList(session, "test.domain.com", "test2.domain.com", true);

	}

	@Test
	public void testPartialJidMatchingOffline() throws Exception {

		XMPPResourceConnection session = null;

		testList(session, "partial_blocked@test.domain.com/resource", "partial_blocked@test.domain.com/resource",
				 false);
		testList(session, "partial_blocked@test.domain.com/resource", "partial_blocked@test2.domain.com/resource",
				 true);
		testList(session, "partial_blocked@test.domain.com/resource", "partial_blocked@test.domain.com/resource2",
				 true);
		testList(session, "partial_blocked@test.domain.com/resource", "partial_blocked2@test.domain.com/resource",
				 true);
		testList(session, "partial_blocked@test.domain.com/resource", "partial_blocked@test.domain.com", true);
		testList(session, "partial_blocked@test.domain.com/resource", "test.domain.com/true", true);
		testList(session, "partial_blocked@test.domain.com/resource", "test.domain.com", true);

		testList(session, "partial_blocked@test.domain.com", "partial_blocked@test.domain.com/resource", false);
		testList(session, "partial_blocked@test.domain.com", "partial_blocked@test.domain.com", false);
		testList(session, "partial_blocked@test.domain.com", "partial_blocked2@test.domain.com", true);
		testList(session, "partial_blocked@test.domain.com", "partial_blocked@test2.domain.com", true);
		testList(session, "partial_blocked@test.domain.com", "test.domain.com/true", true);
		testList(session, "partial_blocked@test.domain.com", "test.domain.com", true);

		testList(session, "test.domain.com/true", "partial_blocked@test.domain.com/resource", true);
		testList(session, "test.domain.com/true", "partial_blocked@test.domain.com", true);
		testList(session, "test.domain.com/true", "test.domain.com/true", false);
		testList(session, "test.domain.com/true", "test.domain.com", true);
		testList(session, "test.domain.com/true", "test.domain.com/true2", true);
		testList(session, "test.domain.com/true", "test.2domain.com", true);

		testList(session, "test.domain.com/true", "partial_blocked@test2.domain.com/resource", true);
		testList(session, "test.domain.com/true", "partial_blocked@test2.domain.com", true);
		testList(session, "test.domain.com/true", "test2.domain.com/true", true);
		testList(session, "test.domain.com/true", "test.domain.com/true2", true);
		testList(session, "test.domain.com/true", "test2.domain.com", true);

		testList(session, "test.domain.com", "partial_blocked@test.domain.com/resource", false);
		testList(session, "test.domain.com", "partial_blocked@test.domain.com", false);
		testList(session, "test.domain.com", "test.domain.com/true", false);
		testList(session, "test.domain.com", "test.domain.com", false);

		testList(session, "test.domain.com", "partial_blocked@test2.domain.com/resource", true);
		testList(session, "test.domain.com", "partial_blocked@test2.domain.com", true);
		testList(session, "test.domain.com", "test2.domain.com/true", true);
		testList(session, "test.domain.com", "test2.domain.com", true);

	}
	
	private void checkStanzaType(XMPPResourceConnection session, String type, Element additionalChild,
								 int expectedResultSize, StanzaType expectedStanzaType)
			throws TigaseStringprepException, XMPPException {
		Element iq = new Element("iq");
		if (type != null) {
			iq.setAttribute("type", type);
		}
		Element query = new Element("query", new String[]{"xmlns"}, new String[]{JabberIqPrivacy.XMLNS});
		if (null != additionalChild) {
			query.addChild(additionalChild);
		}
		iq.addChild(query);
		Packet p = Packet.packetInstance(iq);
		privacyFilter.process(p, session, null, results, null);
		assertEquals(expectedResultSize, results.size());
		if (expectedResultSize > 0) {
			Packet result = results.poll();
			assertNotNull(result);
			assertEquals(Iq.ELEM_NAME, result.getElemName());
			assertEquals(expectedStanzaType, result.getType());
		}
	}

	private void testList(XMPPResourceConnection session, String listJID, String testJID, boolean shouldBeAllowed)
			throws TigaseStringprepException, NotAuthorizedException, TigaseDBException {

		JID jid = JID.jidInstance("test@example/res-1");
		JID blockedJID = JID.jidInstance(listJID);

		Element list = new Element("list", new String[]{"name"}, new String[]{"default"});
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"jid", blockedJID.toString(), "deny", "100"}));
		list.addChild(new Element("item", new String[]{"action", "order"}, new String[]{"allow", "110"}));

		if (session != null) {
			session.putSessionData("active-list", PrivacyList.create(null, list));
		} else {
			this.getUserRepository().setData(jid.getBareJID(), "privacy", "default-list", "default");
			this.getUserRepository().setData(jid.getBareJID(), "privacy/default", "privacy-list", list.toString());
			this.privacyFilter.cache.clear();
		}

		Packet presence = Packet.packetInstance(
				new Element("presence", new String[]{"from", "to"}, new String[]{testJID, jid.toString()}));
		boolean isAllowed = privacyFilter.allowed(presence, session);
		log.log(Level.FINE, "Privacy item: " + listJID + ", tested item: " + testJID + ", result: " + isAllowed);
		if (shouldBeAllowed) {
			assertTrue(isAllowed);
		} else {
			assertFalse(isAllowed);
		}

	}

}
