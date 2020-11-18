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

import java.sql.Connection;
import java.util.Objects;

/**
 * Based on https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADVISORY-LOCKS
 */
class PostgresqlConnectionLock
		extends ConnectionLock {

	int lockName = Math.abs(Objects.hash(super.lockName));

	public PostgresqlConnectionLock(String db_conn) {
		super(db_conn);
	}

	@Override
	protected boolean lockDatabase(Connection connection) {
		String query = "SELECT pg_try_advisory_lock(" + lockName + ")";
		return executeQuery(connection, query);
	}

	@Override
	protected boolean unlockDatabase(Connection connection) {
		String query = "SELECT  pg_advisory_unlock(" + lockName + ")";
		return executeQuery(connection, query);
	}
}
