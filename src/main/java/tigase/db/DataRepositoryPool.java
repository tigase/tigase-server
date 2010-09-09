
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
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Sep 4, 2010 2:13:22 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DataRepositoryPool implements DataRepository {
	private static final Logger log = Logger.getLogger(DataRepositoryPool.class.getName());

	//~--- fields ---------------------------------------------------------------

	private int idx = 0;
	private ArrayList<DataRepository> repoPool = new ArrayList<DataRepository>(5);
	private String resource_uri = null;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 */
	public void addRepo(DataRepository repo) {
		synchronized (repoPool) {
			repoPool.add(repo);
		}
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
		DataRepository repo = takeRepo();

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
	public Statement createStatement() throws SQLException {
		DataRepository repo = takeRepo();

		if (repo != null) {
			return repo.createStatement();
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	//~--- get methods ----------------------------------------------------------

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
	public PreparedStatement getPreparedStatement(String stIdKey) throws SQLException {
		DataRepository repo = takeRepo();

		if (repo != null) {
			return repo.getPreparedStatement(stIdKey);
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

	//~--- methods --------------------------------------------------------------

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
	public void initRepository(String resource_uri, Map<String, String> params) throws SQLException {
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
			} catch (SQLException sqlEx) {}
		}

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException sqlEx) {}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public DataRepository takeRepo() {
		synchronized (repoPool) {
			if (idx >= repoPool.size()) {
				idx = 0;
			}

			return repoPool.get(idx++);
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
