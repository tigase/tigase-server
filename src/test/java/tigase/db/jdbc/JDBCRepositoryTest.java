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
package tigase.db.jdbc;

import tigase.db.DBInitException;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;

import tigase.xmpp.BareJID;

import tigase.util.TigaseStringprepException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Wojtek
 */
public class JDBCRepositoryTest {

	UserRepository repo = null;

	public JDBCRepositoryTest() {
	}

	@Before
	public void setUp() throws DBInitException {

		HashMap map = new LinkedHashMap();
		map.put( RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY, "2");

		String repositoryURI = null;
//		repositoryURI = "jdbc:mysql://localhost:3306/tigasedb?user=tigase&password=tigase&useUnicode=true&characterEncoding=UTF-8&autoCreateUser=true";
//		repositoryURI = "jdbc:jtds:sqlserver://sqlserverhost:1433;databaseName=tigasedb;user=tigase;password=mypass;schema=dbo;lastUpdateCount=false;autoCreateUser=true";
//		repositoryURI = "jdbc:sqlserver://sqlserverhost:1433;databaseName=tigasedb;user=tigase;password=mypass;schema=dbo;lastUpdateCount=false;autoCreateUser=true";
//		repositoryURI = "jdbc:postgresql://localhost/tigasedb?user=tigase&password=tigase&useUnicode=true&characterEncoding=UTF-8&autoCreateUser=true";
//		repositoryURI = "jdbc:derby:/Users/wojtek/dev/tigase/tigase-server/derbyDb";
//		repositoryURI = "mongodb://localhost/tigase_test?autoCreateUser=true";
		Assume.assumeNotNull(repositoryURI);
		repo = new JDBCRepository();
		repo.initRepository( repositoryURI, map );

	}

	private void getData( BareJID user )  {
		if ( user == null ){
			user = BareJID.bareJIDInstanceNS( "user", "domain" );
		}
		System.out.println( "retrieve: " + user + " / thread: " + Thread.currentThread().getName() );
		try {
//			repo.getData( user, "privacy", "default-list", null );
			repo.addUser( user );
		} catch ( UserExistsException ex ) {
			System.out.println( "User exists, ignore: " + ex.getUserId() );
		} catch ( TigaseDBException ex ) {
			Logger.getLogger( JDBCRepositoryTest.class.getName() ).log( Level.SEVERE, null, ex );
		}

	}

	@Test
	public void testGetData() throws InterruptedException {

		System.out.println( "repo: " + repo );
		if ( repo != null ){
			LocalDateTime localNow = LocalDateTime.now();
//			getData( null );

			long initalDelay = 5;

			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 10 );
			final int iter = 50;
			final int threads = 10;

			for ( int i = 0 ; i < threads ; i++ ) {
				scheduler.scheduleAtFixedRate( new RunnableImpl( iter ), initalDelay, 100, TimeUnit.MILLISECONDS );
			}

			Thread.sleep( threads * 1000 );
		}


	}

	private class RunnableImpl implements Runnable {

		int count = 0;
		int max = 50;
		public RunnableImpl(int max) {
			this.max = max;
		}

		@Override
		public void run() {
			while ( count < max ) {
				count++;
				BareJID user;
				user = BareJID.bareJIDInstanceNS( String.valueOf( ( new Date() ).getTime() / 10 ), "domain" );
				getData( user );
			}
		}
	}

}
