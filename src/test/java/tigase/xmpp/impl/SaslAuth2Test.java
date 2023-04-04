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
import tigase.auth.BruteForceLockerBean;
import tigase.auth.TigaseSaslProvider;
import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.util.Base64;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;

import static tigase.xmpp.impl.SaslAuth.ALLOWED_SASL_MECHANISMS_KEY;

public class SaslAuth2Test
		extends ProcessorTestCase {

	private MockSessionmanager mockSessionmanager;
	private SaslAuth2 saslAuth2;

	private XMPPResourceConnection authenticateSession(AuthRepository.AccountStatus accountStatus,
	                                                   Queue<Packet> results)
			throws TigaseDBException, TigaseStringprepException, NotAuthorizedException {
		final BareJID user = BareJID.bareJIDInstanceNS("user@example.com");
		getUserRepository().addUser(user);
		getAuthRepository().updateCredential(user, null, "password");
		getAuthRepository().setAccountStatus(user, accountStatus);
		JID res = JID.jidInstance(user, "res");
		XMPPResourceConnection session = getSession(JID.jidInstance("c2s@example.com/" + UUID.randomUUID()),
		                                            res, false);
		session.putSessionData(ALLOWED_SASL_MECHANISMS_KEY, Collections.singletonList("PLAIN"));

		Packet packet = getPlainSaslPacket();
		try {
			saslAuth2.process(packet, session, null, results, null);
		} catch (XMPPException e) {
			throw new RuntimeException(e);
		}
		return session;
	}

	private Packet getPlainSaslPacket() throws TigaseStringprepException {
		Packet packet;
		Element packetEl = new Element("authenticate", new String[]{"xmlns", "mechanism"},
		                               new String[]{"urn:xmpp:sasl:2", "PLAIN"});
		final byte[] bytes = "\0user\0password".getBytes();
		packetEl.addChild(new Element("initial-response", Base64.encode(bytes)));
		final Element userAgent = new Element("user-agent");
		userAgent.setAttribute("id", "d4565fa7-4d72-4749-b3d3-740edbf87770");
		userAgent.addChild(new Element("software", "AwesomeXMPP"));
		userAgent.addChild(new Element("device", "Kiva's Phone"));
		packetEl.addChild(userAgent);
		packet = Packet.packetInstance(packetEl);
		return packet;
	}

	@Override
	protected void registerBeans(Kernel kernel) {
		super.registerBeans(kernel);
		kernel.registerBean("eventbus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		kernel.registerBean("sess-man").asInstance(new MockSessionmanager()).setActive(true).exportable().exec();
		kernel.registerBean(Bind2.class).setActive(true).exportable().exec();
		kernel.registerBean(StreamManagementInline.class).setActive(true).exportable().exec();
		kernel.registerBean(BruteForceLockerBean.class).setActive(true).exportable().exec();
		kernel.registerBean(TigaseSaslProvider.class).setActive(true).exportable().exec();
		kernel.registerBean(SaslAuth2.class).setActive(true).exportable().exec();
	}

	@Before
	@Override
	public void setUp() throws Exception {
		saslAuth2 = getInstance(SaslAuth2.class);
		saslAuth2.init(new HashMap<>());
		mockSessionmanager = getInstance(MockSessionmanager.class);
		mockSessionmanager.clearQueue();
		super.setUp();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		saslAuth2 = null;
		super.tearDown();
	}

	@Test
	public void testAuthenticationAccountStatusActive() throws Exception {
		final AuthRepository.AccountStatus accountStatus = AuthRepository.AccountStatus.active;
		Queue<Packet> results = new ArrayDeque<>();
		final XMPPResourceConnection session = authenticateSession(accountStatus, results);
		Assert.assertTrue(session.isAuthorized());
		Packet result = mockSessionmanager.outQueue.poll();
		Assert.assertNotNull(result);
		Assert.assertEquals("success", result.getElemName());
	}

	@Test
	public void testAuthenticationAccountStatusDisabled() throws Exception {
		final AuthRepository.AccountStatus accountStatus = AuthRepository.AccountStatus.disabled;
		Queue<Packet> results = new ArrayDeque<>();
		final XMPPResourceConnection session = authenticateSession(accountStatus, results);
		Assert.assertFalse(session.isAuthorized());
		Packet result = results.poll();
		Assert.assertNotNull(result);
		Assert.assertEquals("failure", result.getElemName());
	}

	@Test
	public void testAuthenticationAccountStatusPaid() throws Exception {
		final AuthRepository.AccountStatus accountStatus = AuthRepository.AccountStatus.paid;
		Queue<Packet> results = new ArrayDeque<>();
		final XMPPResourceConnection session = authenticateSession(accountStatus, results);
		Assert.assertTrue(session.isAuthorized());
		Packet result = mockSessionmanager.outQueue.poll();
		Assert.assertNotNull(result);
		Assert.assertEquals("success", result.getElemName());
	}

	@Test
	public void testAuthenticationAccountStatusPending() throws Exception {
		final AuthRepository.AccountStatus accountStatus = AuthRepository.AccountStatus.pending;
		Queue<Packet> results = new ArrayDeque<>();
		final XMPPResourceConnection session = authenticateSession(accountStatus, results);
		Assert.assertFalse(session.isAuthorized());
		Packet result = results.poll();
		Assert.assertNotNull(result);
		Assert.assertEquals("failure", result.getElemName());
	}

	@Test
	public void testAuthenticationAccountStatusSystem() throws Exception {
		final AuthRepository.AccountStatus accountStatus = AuthRepository.AccountStatus.system;
		Queue<Packet> results = new ArrayDeque<>();
		final XMPPResourceConnection session = authenticateSession(accountStatus, results);
		Assert.assertFalse(session.isAuthorized());
		Packet result = results.poll();
		Assert.assertNotNull(result);
		Assert.assertEquals("failure", result.getElemName());
	}

	@Test
	public void testAuthenticationAccountStatusVip() throws Exception {
		final AuthRepository.AccountStatus accountStatus = AuthRepository.AccountStatus.vip;
		Queue<Packet> results = new ArrayDeque<>();
		final XMPPResourceConnection session = authenticateSession(accountStatus, results);
		Assert.assertTrue(session.isAuthorized());
		Packet result = mockSessionmanager.outQueue.poll();
		Assert.assertNotNull(result);
		Assert.assertEquals("success", result.getElemName());
	}

	private static class MockSessionmanager
			extends SessionManager {

		private final Queue<Packet> outQueue = new ArrayDeque<>();

		@Override
		public boolean addOutPacket(Packet packet) {
			return outQueue.add(packet);
		}

		private void clearQueue() {
			outQueue.clear();
		}
	}
}