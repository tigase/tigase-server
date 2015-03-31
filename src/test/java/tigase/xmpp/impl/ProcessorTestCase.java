/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.db.xml.XMLRepository;
import tigase.server.xmppsession.SessionManagerHandler;
import tigase.util.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

/**
 *
 * @author andrzej
 */
public abstract class ProcessorTestCase  {
	
	private static Logger log;
	
	private SessionManagerHandler loginHandler;
	
	//@Override
	public void setUp() throws Exception {
		loginHandler = new SessionManagerHandlerImpl();
	}
	
	//@Override
	public void tearDown() throws Exception {
		loginHandler = null;
	}	
	
	protected XMPPResourceConnection getSession( JID connId, JID userJid) throws NotAuthorizedException, TigaseStringprepException {

		String xmlRepositoryURI = "memory://xmlRepo?autoCreateUser=true";
		XMLRepository xmlRepository = new XMLRepository();
		xmlRepository.initRepository( xmlRepositoryURI, null );

		XMPPResourceConnection conn = new XMPPResourceConnection( connId, (UserRepository) xmlRepository, (AuthRepository) xmlRepository, loginHandler );
		VHostItem vhost = new VHostItem();
		vhost.setVHost( userJid.getDomain() );
		conn.setDomain( vhost );
		conn.authorizeJID( userJid.getBareJID(), false );
		conn.setResource( userJid.getResource() );

		return conn;
	}	

	private class SessionManagerHandlerImpl implements SessionManagerHandler {

		public SessionManagerHandlerImpl() {
		}
		Map<BareJID, XMPPSession> sessions = new HashMap<BareJID, XMPPSession>();

		@Override
		public JID getComponentId() {
			return JID.jidInstanceNS( "sess-man@localhost" );
		}

		@Override
		public void handleLogin( BareJID userId, XMPPResourceConnection conn ) {
			XMPPSession session = sessions.get( userId );
			if ( session == null ){
				session = new XMPPSession( userId.getLocalpart() );
				sessions.put( userId, session );
			}
			try {
				session.addResourceConnection( conn );
			} catch ( TigaseStringprepException ex ) {
				log.log( Level.SEVERE, null, ex );
			}
		}

		@Override
		public void handleLogout( BareJID userId, XMPPResourceConnection conn ) {
			XMPPSession session = sessions.get( conn );
			if ( session != null ){
				session.removeResourceConnection( conn );
				if ( session.getActiveResourcesSize() == 0 ){
					sessions.remove( userId );
				}
			}
		}

		@Override
		public void handlePresenceSet( XMPPResourceConnection conn ) {
		}

		@Override
		public void handleResourceBind( XMPPResourceConnection conn ) {
		}

		@Override
		public boolean isLocalDomain( String domain, boolean includeComponents ) {
			return !domain.contains( "-ext" );
		}

		@Override
		public void handleDomainChange(String domain, XMPPResourceConnection conn) {
		}
	}
	
}
