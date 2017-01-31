/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
 * or (at your option) any later version.
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

import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.db.xml.XMLRepository;

import tigase.server.Packet;
import tigase.server.xmppsession.SessionManagerHandler;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

import tigase.util.LogFormatter;
import tigase.util.TigaseStringprepException;
import tigase.vhosts.filter.DomainFilterPolicy;
import tigase.vhosts.VHostItem;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.*;

import static tigase.xmpp.impl.DomainFilter.ALLOWED_DOMAINS_KEY;
import static tigase.xmpp.impl.DomainFilter.ALLOWED_DOMAINS_LIST_KEY;

/**
 *
 * @author Wojciech Kapcia <wojciech.kapcia@tigase.org>
 */
public class DomainFilterTest extends ProcessorTestCase {

	private DomainFilter domainFilter;
	private static Logger log;
	String domain = "domain";
	JID recp1 = JID.jidInstanceNS( "user1", domain + 1, "resource1" );
	JID sameDomainUser = JID.jidInstanceNS( "user2", domain + 1, "resource2" );
	JID localDomainUser = JID.jidInstanceNS( "user2", domain + 2, "resource2" );
	JID externalDomainUser = JID.jidInstanceNS( "user2", domain + "-ext", "resource2" );
	JID connId1 = JID.jidInstanceNS( "c2s@localhost/recipient1-res1" );
	ArrayDeque<Packet> results;
	Packet p;
	XMPPResourceConnection session;

	@BeforeClass
	public static void setUpLogger() throws TigaseDBException, IOException {
		Level lvl;
//		lvl = Level.ALL;
		lvl = Level.CONFIG;
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel( lvl );
		consoleHandler.setFormatter( new LogFormatter() );
		log = Logger.getLogger( DomainFilter.class.getName() );
		log.setUseParentHandlers( false );
		log.setLevel( lvl );
		log.addHandler( consoleHandler );

	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		domainFilter = new DomainFilter();
		domainFilter.init( new HashMap<String, Object>() );
		results = new ArrayDeque<Packet>();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		domainFilter = null;
	}

	@Test
	public void testPolicyHierarchy() throws NotAuthorizedException, TigaseStringprepException, TigaseDBException {
		String[] domainsList;

		session = getSession( connId1, recp1, DomainFilterPolicy.LIST,
													new String[] { domain + 1, domain + 2, domain + 3 } );

		Assert.assertEquals( "Reading DomainFilterPolicy from VHost", DomainFilterPolicy.LIST,
												 domainFilter.getDomains( session ) );
		session.removeCommonSessionData( ALLOWED_DOMAINS_KEY );

		// configuration from UserRepository
		session.setData( null, ALLOWED_DOMAINS_KEY, DomainFilterPolicy.ALL.name() );
		Assert.assertEquals( "Reading DomainFilterPolicy from UserRepo, takes precendence over VHost",
												 DomainFilterPolicy.ALL, domainFilter.getDomains( session ) );
		session.removeCommonSessionData( ALLOWED_DOMAINS_KEY );

		// user session data takes precedence over repository and VHost configuration
		session.putCommonSessionData( ALLOWED_DOMAINS_KEY, DomainFilterPolicy.BLOCK );
		Assert.assertEquals( "Reading DomainFilterPolicy from user session, takes precendence over UserRepository",
												 DomainFilterPolicy.BLOCK, domainFilter.getDomains( session ) );
		session.removeCommonSessionData( ALLOWED_DOMAINS_KEY );

		// let's check hierarchy of domain list: VHost, UserRepo, session
		domainsList = domainFilter.getDomainsList( session );
		Assert.assertTrue( "Reading domain list from VHost", Arrays.asList( domainsList ).contains( domain + 1 ) );
		Assert.assertFalse( "Reading domain list from VHost", Arrays.asList( domainsList ).contains( domain + 5 ) );
		session.removeCommonSessionData( ALLOWED_DOMAINS_LIST_KEY );

		// configuration from UserRepository
		session.setData( null, ALLOWED_DOMAINS_LIST_KEY, ( domain + 11 + ";" + domain + 12 + ";" + domain + 13 ) );
		domainsList = domainFilter.getDomainsList( session );
		log.log( Level.FINE, Arrays.asList( domainsList ).toString() );
		Assert.assertTrue( "Reading domain list from repository", Arrays.asList( domainsList ).contains( domain + 11 ) );
		Assert.assertFalse( "Reading domain list from repository", Arrays.asList( domainsList ).contains( domain + 15 ) );
		session.removeCommonSessionData( ALLOWED_DOMAINS_LIST_KEY );

		// user session data takes precedence over repository and VHost configuration
		session.putCommonSessionData( ALLOWED_DOMAINS_LIST_KEY, new String[] { domain + 21, domain + 22, domain + 23 } );
		domainsList = domainFilter.getDomainsList( session );
		log.log( Level.FINE, Arrays.asList( domainsList ).toString() );
		Assert.assertTrue( "Reading domain list from session", Arrays.asList( domainsList ).contains( domain + 21 ) );
		Assert.assertFalse( "Reading domain list from session", Arrays.asList( domainsList ).contains( domain + 25 ) );
		session.removeCommonSessionData( ALLOWED_DOMAINS_LIST_KEY );

		session.logout();

	}

	@Test
	public void testFilterAllPolicy() throws NotAuthorizedException, TigaseStringprepException {
		session = getSession( connId1, recp1, DomainFilterPolicy.ALL, null );

		filterPacket( session, sameDomainUser );
		Assert.assertFalse( "ALL policy, message between same domains",
												results.pop().getType().equals( StanzaType.error ) );

		// two local domains
		filterPacket( session, localDomainUser );
		Assert.assertFalse( "ALL policy, message between different local domains",
												results.pop().getType().equals( StanzaType.error ) );

		filterPacket( session, externalDomainUser );
		Assert.assertFalse( "ALL policy, message to external domain",
												results.pop().getType().equals( StanzaType.error ) );

		session.logout();

	}



	@Test
	public void testFilterLocalPolicy() throws NotAuthorizedException, TigaseStringprepException {
		session = getSession( connId1, recp1, DomainFilterPolicy.LOCAL, null );

		filterPacket( session, sameDomainUser );
		Assert.assertFalse( "LOCAL policy, message between same domains",
												results.pop().getType().equals( StanzaType.error ) );

		filterPacket( session, localDomainUser );
		Assert.assertFalse( "LOCAL policy, message between different local domains",
												results.pop().getType().equals( StanzaType.error ) );

		filterPacket( session, externalDomainUser );
		Assert.assertTrue( "LOCAL policy, message to external domain",
											 results.pop().getType().equals( StanzaType.error ) );

		session.logout();

	}

	@Test
	public void testFilterOwnPolicy() throws NotAuthorizedException, TigaseStringprepException {
		session = getSession( connId1, recp1, DomainFilterPolicy.OWN, null );

		filterPacket( session, sameDomainUser );
		Assert.assertFalse( "OWN policy, message between same domains",
												results.pop().getType().equals( StanzaType.error ) );

		filterPacket( session, localDomainUser );
		Assert.assertTrue( "OWN policy, message between different local domains",
											 results.pop().getType().equals( StanzaType.error ) );

		filterPacket( session, externalDomainUser );
		Assert.assertTrue( "OWN policy, message to external domain",
											 results.pop().getType().equals( StanzaType.error ) );

		session.logout();

	}

	@Test
	public void testFilterWhielistPolicy() throws NotAuthorizedException, TigaseStringprepException {
		String[] whitelistDomains = new String[] { "domain1", externalDomainUser.getDomain() };

		session = getSession( connId1, recp1, DomainFilterPolicy.LIST, whitelistDomains );

		filterPacket( session, sameDomainUser );
		Assert.assertFalse( "WHITELIST policy, message between same domains (both whitelist)",
												results.pop().getType().equals( StanzaType.error ) );

		filterPacket( session, localDomainUser );
		Assert.assertTrue( "WHITELIST policy, message between different local domains (only sender whitelist)",
											 results.pop().getType().equals( StanzaType.error ) );

		filterPacket( session, externalDomainUser );
		Assert.assertFalse( "WHITELIST policy, message to external domain (whitelisted)",
												results.pop().getType().equals( StanzaType.error ) );

		session.logout();

	}

	@Test
	public void testFilterBlacklistPolicy() throws NotAuthorizedException, TigaseStringprepException {
		String[] blacklistedDomains = new String[] { localDomainUser.getDomain() };

		session = getSession( connId1, recp1, DomainFilterPolicy.BLACKLIST, blacklistedDomains );

		filterPacket( session, sameDomainUser );
		Assert.assertFalse( "BLACKLIST policy, message between same domains (not on blacklist)",
												results.pop().getType().equals( StanzaType.error ) );

		filterPacket( session, localDomainUser );
		Assert.assertTrue( "BLACKLIST policy, message between different local domains (receiver domain blacklisted)",
											 results.pop().getType().equals( StanzaType.error ) );

		filterPacket( session, externalDomainUser );
		Assert.assertFalse( "BLACKLIST policy, message to external domain (not blacklisted)",
												results.pop().getType().equals( StanzaType.error ) );

		session.logout();

	}

	@Test
	public void testFilterBlockPolicy() throws NotAuthorizedException, TigaseStringprepException {
		String[] blacklistedDomains = new String[] { localDomainUser.getDomain() };

		session = getSession( connId1, recp1, DomainFilterPolicy.BLOCK, blacklistedDomains );

		filterPacket( session, sameDomainUser );
		Assert.assertTrue( "BLOCK policy, message between same domains",
											 results.pop().getType().equals( StanzaType.error ) );

		filterPacket( session, localDomainUser );
		Assert.assertTrue( "BLOCK policy, message between different local domains",
											 results.pop().getType().equals( StanzaType.error ) );

		filterPacket( session, externalDomainUser );
		Assert.assertTrue( "BLOCK policy, message to external domain",
											 results.pop().getType().equals( StanzaType.error ) );

		session.logout();

	}

	@Test
	public void testPreprocessorAllPolicy() throws NotAuthorizedException, TigaseStringprepException {
		session = getSession( connId1, recp1, DomainFilterPolicy.ALL, null );

		processPacket( session, sameDomainUser );
		Assert.assertTrue( "ALL policy, message between same domains",
											 results.isEmpty() );

		processPacket( session, localDomainUser );
		Assert.assertTrue( "ALL policy, message between different local domains",
											 results.isEmpty() );

		processPacket( session, externalDomainUser );
		Assert.assertTrue( "ALL policy, message to external domain",
											 results.isEmpty() );

		session.logout();

	}

	@Test
	public void testPreprocessorLocalPolicy() throws NotAuthorizedException, TigaseStringprepException {
		session = getSession( connId1, recp1, DomainFilterPolicy.LOCAL, null );

		processPacket( session, sameDomainUser );
		Assert.assertTrue( "LOCAL policy, message between same domains",
											 results.isEmpty() );

		processPacket( session, localDomainUser );
		Assert.assertTrue( "LOCAL policy, message between different local domains",
											 results.isEmpty() );

		processPacket( session, externalDomainUser );
		Assert.assertTrue( "LOCAL policy, message to external domain",
											 results.pop().getType().equals( StanzaType.error ) );

		session.logout();

	}

	@Test
	public void testPreprocessorOwnPolicy() throws NotAuthorizedException, TigaseStringprepException {
		session = getSession( connId1, recp1, DomainFilterPolicy.OWN, null );

		processPacket( session, sameDomainUser );
		Assert.assertTrue( "OWN policy, message between same domains",
											 results.isEmpty() );

		processPacket( session, localDomainUser );
		Assert.assertTrue( "OWN policy, message between different local domains",
											 results.pop().getType().equals( StanzaType.error ) );

		processPacket( session, externalDomainUser );
		Assert.assertTrue( "OWN policy, message to external domain",
											 results.pop().getType().equals( StanzaType.error ) );

		session.logout();

	}

	@Test
	public void testPreprocessorWhielistPolicy() throws NotAuthorizedException, TigaseStringprepException {
		String[] whitelistDomains = new String[] { "domain1", externalDomainUser.getDomain() };

		session = getSession( connId1, recp1, DomainFilterPolicy.LIST, whitelistDomains );

		processPacket( session, sameDomainUser );
		Assert.assertTrue( "WHITELIST policy, message between same domains (both whitelist)",
											 results.isEmpty() );

		processPacket( session, localDomainUser );
		Assert.assertTrue( "WHITELIST policy, message between different local domains (only sender whitelist)",
											 results.pop().getType().equals( StanzaType.error ) );

		processPacket( session, externalDomainUser );
		Assert.assertTrue( "WHITELIST policy, message to external domain (whitelisted)",
											 results.isEmpty() );

		session.logout();

	}

	@Test
	public void testPreprocessorBlacklistPolicy() throws NotAuthorizedException, TigaseStringprepException {

		// blacklisting other local user
		String[] blacklistedDomains = new String[] { localDomainUser.getDomain() };

		session = getSession( connId1, recp1, DomainFilterPolicy.BLACKLIST, blacklistedDomains );

		processPacket( session, sameDomainUser );
		Assert.assertTrue( "BLACKLIST policy, message between same domains (not on blacklist)",
											 results.isEmpty() );

		processPacket( session, localDomainUser );
		Assert.assertTrue( "BLACKLIST policy, message between different local domains (receiver domain blacklisted)",
											 results.pop().getType().equals( StanzaType.error ) );

		processPacket( session, externalDomainUser );
		Assert.assertTrue( "BLACKLIST policy, message to external domain (not blacklisted)",
											 results.isEmpty() );

		session.logout();

	}

	@Test
	public void testPreprocessorBlockPolicy() throws NotAuthorizedException, TigaseStringprepException {

		String[] blacklistedDomains = new String[] { localDomainUser.getDomain() };

		session = getSession( connId1, recp1, DomainFilterPolicy.BLOCK, blacklistedDomains );

		processPacket( session, sameDomainUser );
		Assert.assertTrue( "BLOCK policy, message between same domains",
											 results.pop().getType().equals( StanzaType.error ) );

		processPacket( session, localDomainUser );
		Assert.assertTrue( "BLOCK policy, message between different local domains",
											 results.pop().getType().equals( StanzaType.error ) );

		processPacket( session, externalDomainUser );
		Assert.assertTrue( "BLOCK policy, message to external domain",
											 results.pop().getType().equals( StanzaType.error ) );

		session.logout();

	}

	private void filterPacket( XMPPResourceConnection session, JID reciever ) throws TigaseStringprepException {
		p = Packet.packetInstance( "message", recp1.toString(), reciever.toString(), StanzaType.chat );
		p.setPacketFrom( connId1 );
		results.offer( p );
		domainFilter.filter( p, session, null, results );
		log.log( Level.FINEST, "results: " + results );
	}

	private void processPacket( XMPPResourceConnection session, JID reciever ) throws TigaseStringprepException {
		p = Packet.packetInstance( "message", recp1.toString(), reciever.toString(), StanzaType.chat );
		p.setPacketFrom( connId1 );
		domainFilter.preProcess( p, session, null, results, null );
		log.log( Level.FINEST, "results: " + results );
	}

	private XMPPResourceConnection getSession( JID connId, JID userJid, DomainFilterPolicy dfp, String[] domains ) throws NotAuthorizedException, TigaseStringprepException {
		XMPPResourceConnection conn = super.getSession(connId, userJid);
		VHostItem vhost = conn.getDomain();
		vhost.setDomainFilter( dfp );
		vhost.setDomainFilterDomains( domains );

		return conn;
	}
}
