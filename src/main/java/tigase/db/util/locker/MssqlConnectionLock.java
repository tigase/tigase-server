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

import java.sql.*;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * https://docs.microsoft.com/en-us/sql/relational-databases/system-stored-procedures/sp-getapplock-transact-sql
 * https://docs.microsoft.com/en-us/sql/relational-databases/system-stored-procedures/sp-releaseapplock-transact-sql
 * https://docs.microsoft.com/en-us/sql/connect/jdbc/using-a-stored-procedure-with-a-return-status
 */
class MssqlConnectionLock
		extends ConnectionLock {

	public MssqlConnectionLock(String db_conn) {
		super(db_conn);
	}

	@Override
	protected boolean lockDatabase(Connection connection) {
		String query = "{ ? = call dbo.sp_getapplock(?, ?, ?, ?) }";
		return executeProcedure(connection, query, statement -> {
			try {
				statement.setString(2, lockName);
				statement.setString(3, "Exclusive");
				statement.setString(4, "Session");
				statement.setInt(5, 1000);
			} catch (SQLException e) {
				log.log(Level.WARNING, "Failed to set statement parameters", e);
			}
		});
	}

	@Override
	protected boolean unlockDatabase(Connection connection) {
		String query = "{ ? = call dbo.sp_releaseapplock(?, ?) }";
//		String query = "exec sp_getapplock @Resource = '" + lockName + "', @LockMode = 'Exclusive', @LockOwner='Session'";
		return executeProcedure(connection, query, statement -> {
			try {
				statement.setString(2, lockName);
				statement.setString(3, "Session");
			} catch (SQLException e) {
				log.log(Level.WARNING, "Failed to set statement parameters", e);
			}
		});
	}

	protected boolean executeProcedure(Connection connection, String query, Consumer<CallableStatement> consumer) {
		boolean isLocked = false;
		CallableStatement statement = null;
		ResultSet resultSet = null;
		try {
			statement = connection.prepareCall(query);
			statement.registerOutParameter(1, Types.INTEGER);
			consumer.accept(statement);
			statement.execute();
			final int returnCode = statement.getInt(1);
			log.log(Level.FINEST, "Stored procedure return code: " + returnCode);
			switch (returnCode) {
				case 0:
				case 1:
					isLocked = true;
					break;
				case -1:
				case -2:
				case -3:
				case -999:
				default:
					isLocked = false;
					break;

			}
		} catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		} finally {
			release(statement, resultSet);
		}
		return isLocked;
	}
}
