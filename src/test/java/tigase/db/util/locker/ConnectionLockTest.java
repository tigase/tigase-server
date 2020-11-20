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

package tigase.db.util.locker;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import tigase.TestLogger;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionLockTest {

	protected static String uri = System.getProperty("testDbUri");
//	protected static String uri = "jdbc:derby://derbydb;create=true";
//	protected static String uri = "jdbc:postgresql://localhost:5432/postgres?user=root&password=root";
//	protected static String uri = "jdbc:mysql://localhost:3306/?user=root&password=root&useSSL=false";
//	protected static String uri = "jdbc:jtds:sqlserver://localhost:1433;user=sa;password=1Secure*Password1;schema=dbo;lastUpdateCount=false";


	@ClassRule
	public static TestRule rule = (statement, description) -> {
		if (uri == null || !uri.startsWith("jdbc:")) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					Assume.assumeTrue("Ignored due to not passed DB URI!", false);
				}
			};
		}
		return statement;
	};

	@BeforeClass
	public static void setUp() throws Exception {
		TestLogger.configureLogger(Logger.getLogger("tigase.db.util.locker"), Level.FINEST);
	}

	@Test
	public void permitSkipIfAbsent() {
		final Optional<ConnectionLock> connectionLock = ConnectionLock.getConnectionLocker(uri);
		assertTrue(connectionLock.isEmpty() || connectionLock.get().lock());
		if (connectionLock.isPresent()) {
			final ConnectionLock lock = connectionLock.get();
			assertTrue(lock.isLocked());
			assertTrue(lock.unlock());
		}
	}
	@Test
	public void singleLockAndUnlock() {
		final Optional<ConnectionLock> connectionLocker = ConnectionLock.getConnectionLocker(uri);
		if (connectionLocker.isPresent()) {
			final ConnectionLock lock = connectionLocker.get();
			assertFalse(lock.isLocked());
			assertTrue(lock.lock());
			assertTrue(lock.isLocked());
			assertTrue(lock.unlock());
			assertFalse(lock.isLocked());
		}
	}

	@Test
	public void multipleLockAndUnlock() {
		final Optional<ConnectionLock> connectionLocker = ConnectionLock.getConnectionLocker(uri);
		final Optional<ConnectionLock> connectionLocker2 = ConnectionLock.getConnectionLocker(uri);
		if (connectionLocker.isPresent() && connectionLocker2.isPresent()) {
			final ConnectionLock lock = connectionLocker.get();
			final ConnectionLock lock2 = connectionLocker2.get();
			connectionLocker.get().lockAttemptsLimit = 3;
			connectionLocker2.get().lockAttemptsLimit = 3;
			assertFalse(lock.isLocked());
			assertFalse(lock2.isLocked());
			assertTrue(lock.lock());
			assertTrue(lock.isLocked());
			assertFalse(lock2.lock());
			assertFalse(lock2.isLocked());
			assertTrue(lock.unlock());
			assertFalse(lock.isLocked());
		}
	}
}