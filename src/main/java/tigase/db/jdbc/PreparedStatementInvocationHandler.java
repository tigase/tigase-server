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
package tigase.db.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PreparedStatementInvocationHandler
		implements InvocationHandler {

	private final static Logger log = Logger.getLogger(PreparedStatementInvocationHandler.class.getName());

	private final PreparedStatement ps;

	public PreparedStatementInvocationHandler(PreparedStatement ps) {
		this.ps = ps;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			return method.invoke(ps, args);
		} catch (Throwable ex) {
			if (isReadOnlyException(ex)) {
				log.log(Level.FINEST, "Database connection threw read-only exception, marking it as such co reinitialise it: " + ps.getConnection());
				ps.getConnection().setReadOnly(true);
			}
			if (ex instanceof UndeclaredThrowableException) {
				ex = ((UndeclaredThrowableException) ex).getUndeclaredThrowable();
			}
			if (ex instanceof InvocationTargetException) {
				throw ((InvocationTargetException) ex).getTargetException();
			} else {
				throw ex;
			}
		}
	}

	private boolean isMySqlReadOnly(SQLException sqlex) {
		return sqlex.getErrorCode() == 1290 && "HY000".equals(sqlex.getSQLState());
	}

	private boolean isPostgresReadOnly(SQLException sqlex) {
		return sqlex.getErrorCode() == 0 && "25006".equals(sqlex.getSQLState());
	}

	private boolean isReadOnlyException(Throwable ex) {
		if (ex instanceof InvocationTargetException) {
			ex = ((InvocationTargetException) ex).getTargetException();
		}
		if (ex instanceof SQLException) {
			SQLException sqlex = (SQLException) ex;
			return (isMySqlReadOnly(sqlex) || isPostgresReadOnly(sqlex));
		}
		return false;
	}
}
