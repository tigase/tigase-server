/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.db;

//~--- JDK imports ------------------------------------------------------------

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.xmpp.BareJID;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Sep 4, 2010 2:13:22 PM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DataRepositoryPool implements DataRepository {
	private static final Logger log = Logger.getLogger(DataRepositoryPool.class.getName());

	// ~--- fields ---------------------------------------------------------------

	private CopyOnWriteArrayList<DataRepository> repoPool =
			new CopyOnWriteArrayList<DataRepository>();
	private String resource_uri = null;

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param repo
	 */
	public void addRepo(DataRepository repo) {
		repoPool.addIfAbsent(repo);
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	public DataRepository takeRepo(BareJID user_id) {
		int idx = user_id != null ? Math.abs(user_id.hashCode() % repoPool.size()) : 0;
		DataRepository result = null;
		try {
			result = repoPool.get(idx);
		} catch (IndexOutOfBoundsException ioobe) {
			result = repoPool.get(0);
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.db.DataRepository#takeRepoHandle()
	 */
	@Override
	public DataRepository takeRepoHandle(BareJID user_id) {
		return takeRepo(user_id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.db.DataRepository#releaseRepoHandle()
	 */
	@Override
	public void releaseRepoHandle(DataRepository repo) {
		// addRepo(repo);
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param tableName
	 * 
	 * @return
	 * 
	 * @throws SQLException
	 */
	@Override
	public boolean checkTable(String tableName) throws SQLException {
		DataRepository repo = takeRepo(null);

		if (repo != null) {
			return repo.checkTable(tableName);
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return false;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 * 
	 * @throws SQLException
	 */
	@Override
	public Statement createStatement(BareJID user_id) throws SQLException {
		DataRepository repo = takeRepo(user_id);

		if (repo != null) {
			return repo.createStatement(user_id);
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	// ~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param stIdKey
	 * 
	 * @return
	 * 
	 * @throws SQLException
	 */
	@Override
	public PreparedStatement getPreparedStatement(BareJID user_id, String stIdKey) throws SQLException {
		DataRepository repo = takeRepo(user_id);

		if (repo != null) {
			return repo.getPreparedStatement(user_id, stIdKey);
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	@Override
	public String getResourceUri() {
		return resource_uri;
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param stIdKey
	 * @param query
	 * 
	 * @throws SQLException
	 */
	@Override
	public void initPreparedStatement(String stIdKey, String query) throws SQLException {
		for (DataRepository dataRepository : repoPool) {
			dataRepository.initPreparedStatement(stIdKey, query);
		}
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param resource_uri
	 * @param params
	 * 
	 * @throws SQLException
	 */
	@Override
	public void initRepository(String resource_uri, Map<String, String> params)
			throws SQLException {
		this.resource_uri = resource_uri;

		for (DataRepository dataRepository : repoPool) {
			dataRepository.initRepository(resource_uri, params);
		}
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param stmt
	 * @param rs
	 */
	@Override
	public void release(Statement stmt, ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException sqlEx) {
			}
		}

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException sqlEx) {
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.db.DataRepository#startTransaction()
	 */
	@Override
	public void startTransaction() throws SQLException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.db.DataRepository#commit()
	 */
	@Override
	public void commit() throws SQLException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.db.DataRepository#rollback()
	 */
	@Override
	public void rollback() throws SQLException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tigase.db.DataRepository#endTransaction()
	 */
	@Override
	public void endTransaction() throws SQLException {
		// TODO Auto-generated method stub

	}

}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
