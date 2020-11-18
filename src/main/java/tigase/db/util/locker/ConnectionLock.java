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

import tigase.db.DataRepository;
import tigase.db.jdbc.DataRepositoryImpl;

import java.sql.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract public class ConnectionLock {

	//	protected final Logger log = Logger.getLogger(ConnectionLock.class.getCanonicalName());
	static final Logger log = Logger.getLogger(ConnectionLock.class.getCanonicalName());
	protected boolean isLocked = false;
	protected String jdbcConnection;
	protected int lockAttemptDelay = 1000;
	protected int lockAttemptsLimit = 10;
	String lockName = "tigase";
	private Connection connection;

	static public Optional<ConnectionLock> getConnectionLocker(String db_conn) throws IllegalArgumentException {
		DataRepository.dbTypes dbType = DataRepositoryImpl.parseDatabaseType(db_conn);
		if (dbType == null) {
			return Optional.empty();
		}
		ConnectionLock connectionLock = null;
		switch (dbType) {
			case postgresql:
				connectionLock = new PostgresqlConnectionLock(db_conn);
				break;
			case mysql:
				connectionLock = new MysqlConnectionLock(db_conn);
				break;
			case jtds:
			case sqlserver:
				connectionLock = new MssqlConnectionLock(db_conn);
				break;
			case derby:
			default:
				break;
		}
		return Optional.ofNullable(connectionLock);
	}

	protected ConnectionLock(String db_conn) {
		this.jdbcConnection = db_conn;
		try {
			connection = DriverManager.getConnection(db_conn);
			log.log(Level.INFO, "Prepared connection for locking");
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed preparing connection for locking: " + e.getMessage());
		}
	}

	public boolean lock() {
		log.log(Level.INFO, "Trying to get lock for database, current state: " + isLocked);

		if (connection == null) {
			log.log(Level.WARNING, "No connection available!");
			return false;
		}

		if (isLocked) {
			log.log(Level.WARNING, "Connection already locked!");
			return false;
		}

		boolean gotLocked = false;
		int attempt = 0;
		while (!gotLocked && attempt < lockAttemptsLimit) {
			log.log(Level.FINEST, "Trying to get lock for database, attempt {0} of {1}",
					new Object[]{attempt, lockAttemptsLimit});
			gotLocked = lockDatabase(connection);
			if (!isLocked && gotLocked) {
				isLocked = true;
				break;
			}
			attempt++;
			wait(lockAttemptDelay);
		}

		if (isLocked) {
			log.log(Level.INFO, "Obtained lock for connection: " + jdbcConnection);
		} else {
			log.log(Level.WARNING, "FAILED to obtain lock for connection: " + jdbcConnection);
		}
		return isLocked;
	}

	public boolean unlock() {
		boolean gotUnlocked = false;

		if (connection == null) {
			log.log(Level.WARNING, "No connection available!");
			return false;
		}

		if (isLocked) {

			log.log(Level.INFO, "Unlocking database");

			int attempt = 0;
			while (!gotUnlocked && attempt < lockAttemptsLimit) {
				log.log(Level.FINEST, "Trying to get unlock the database, attempt {0} of {1}",
						new Object[]{attempt, lockAttemptsLimit});
				gotUnlocked = unlockDatabase(connection);
				if (isLocked && gotUnlocked) {
					isLocked = false;
					break;
				}
				attempt++;
				wait(lockAttemptDelay);
			}
		} else {
			log.log(Level.INFO, "Connection was not locked, skipping unlocking ");
		}
		return gotUnlocked;
	}

	public void cleanup() {
		if (!isLocked && connection != null) {
			try {
				log.log(Level.INFO, "Closing lock connection");
				connection.close();
			} catch (SQLException e) {
				log.log(Level.WARNING, "Failed closing connection", e);
			}
		}
	}

	public boolean isLocked() {
		log.log(Level.FINE, "Lock state: " + isLocked);
		return isLocked;
	}

	/**
	 * @param connection {@link java.sql.Connection} which should hold the lock
	 *
	 * @return {@code true} if the locking was successful or {@code false} if it wasn't
	 */
	protected abstract boolean lockDatabase(Connection connection);

	protected void release(Statement statement, ResultSet resultSet) {
		try {
			if (resultSet != null) {
				resultSet.close();
			}
			if (statement != null) {
				statement.close();
			}
		} catch (SQLException e) {
			log.log(Level.FINEST, "Failed to release ResultSet or Statement");
		}
	}

	/**
	 * @param connection {@link java.sql.Connection} which holds the lock and which should be unlocked
	 *
	 * @return {@code true} if the unlocking was successful or {@code false} if it wasn't and the lock is still hold
	 */
	protected abstract boolean unlockDatabase(Connection connection);

	protected boolean executeQuery(Connection connection, String query) {
		boolean isLocked = false;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			statement = connection.prepareStatement(query);
			resultSet = statement.executeQuery();
			if (resultSet.next()) {
				final ResultSetMetaData metaData = resultSet.getMetaData();
				isLocked = resultSet.getBoolean(1);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		} finally {
			release(statement, resultSet);
		}
		return isLocked;
	}

	private void wait(int delay) {
		try {
			TimeUnit.MILLISECONDS.sleep(delay);
		} catch (InterruptedException e) {
			//ignore
		}
	}
}
