
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
package tigase.db.jdbc;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.DataRepository;

//~--- JDK imports ------------------------------------------------------------

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Sep 3, 2010 5:55:41 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DataRepositoryImpl implements DataRepository {
	private static final Logger log = Logger.getLogger(DataRepositoryImpl.class.getName());

	/** Field description */
	public static final String DERBY_CONNVALID_QUERY = "values 1";

	/** Field description */
	public static final String JDBC_CONNVALID_QUERY = "select 1";

	/** Field description */
	public static final String MYSQL_CHECK_TABLE_QUERY =
		"select * from information_schema.tables where table_name = ?";

	/** Field description */
	public static final String OTHER_CHECK_TABLE_QUERY = "";

	/** Field description */
	public static final String SP_STARTS_WITH = "{ call";

	//~--- fields ---------------------------------------------------------------

	private Connection conn = null;
	private PreparedStatement conn_valid_st = null;
	private long connectionValidateInterval = 1000 * 60;
	private String db_conn = null;
	private long lastConnectionValidated = 0;
	private boolean derby_mode = false;
	private Map<String, PreparedStatement> db_statements = new ConcurrentSkipListMap<String,
		PreparedStatement>();
	private Map<String, String> db_queries = new ConcurrentSkipListMap<String, String>();
	private String check_table_query = MYSQL_CHECK_TABLE_QUERY;

	//~--- methods --------------------------------------------------------------

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
		PreparedStatement checkTableSt = getPreparedStatement(check_table_query);

		if (checkTableSt == null) {
			return true;
		}

		boolean result = false;
		ResultSet rs = null;

		synchronized (checkTableSt) {
			try {
				checkTableSt.setString(1, tableName);
				rs = checkTableSt.executeQuery();

				if (rs.next()) {
					result = true;
				}
			} finally {
				release(null, rs);
			}
		}

		return result;
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
		checkConnection();

		return conn.createStatement();
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
		checkConnection();

		return db_statements.get(stIdKey);
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getResourceUri() {
		return db_conn;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param key
	 * @param query
	 *
	 * @throws SQLException
	 */
	@Override
	public void initPreparedStatement(String key, String query) throws SQLException {
		db_queries.put(key, query);

		PreparedStatement st = prepareQuery(query);

		db_statements.put(key, st);
	}

	/**
	 * Method description
	 *
	 *
	 * @param resource_uri
	 * @param params
	 *
	 *
	 * @throws SQLException
	 */
	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws SQLException {
		db_conn = resource_uri;
		initRepo();

		if ( !db_conn.contains("mysql")) {
			check_table_query = OTHER_CHECK_TABLE_QUERY;
		}

		if ( !check_table_query.isEmpty()) {
			initPreparedStatement(check_table_query, check_table_query);
		}

		log.log(Level.INFO, "Initialized database connection: {0}", resource_uri);
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
	 * <code>checkConnection</code> method checks database connection before any
	 * query. For some database servers (or JDBC drivers) it happens the connection
	 * is dropped if not in use for a long time or after certain timeout passes.
	 * This method allows us to detect the problem and reinitialize database
	 * connection.
	 *
	 * @return a <code>boolean</code> value if the database connection is working.
	 * @exception SQLException if an error occurs on database query.
	 */
	private boolean checkConnection() throws SQLException {
		ResultSet rs = null;

		try {
			synchronized (conn_valid_st) {
				long tmp = System.currentTimeMillis();

				if ((tmp - lastConnectionValidated) >= connectionValidateInterval) {
					rs = conn_valid_st.executeQuery();
					lastConnectionValidated = tmp;
				}    // end of if ()
			}
		} catch (Exception e) {
			initRepo();
		} finally {
			release(null, rs);
		}        // end of try-catch

		return true;
	}

	/**
	 * <code>initPreparedStatements</code> method initializes internal
	 * database connection variables such as prepared statements.
	 *
	 * @exception SQLException if an error occurs on database query.
	 */
	private void initPreparedStatements() throws SQLException {
		String query = (derby_mode ? DERBY_CONNVALID_QUERY : JDBC_CONNVALID_QUERY);

		conn_valid_st = prepareQuery(query);

		for (String key : db_queries.keySet()) {
			query = db_queries.get(key);

			PreparedStatement st = prepareQuery(query);

			db_statements.put(key, st);
		}
	}

	/**
	 * <code>initRepo</code> method initializes database connection
	 * and data repository.
	 *
	 * @exception SQLException if an error occurs on database query.
	 */
	private void initRepo() throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;

		try {
			synchronized (db_conn) {
				conn = DriverManager.getConnection(db_conn);
				conn.setAutoCommit(true);
				derby_mode = db_conn.startsWith("jdbc:derby");
				initPreparedStatements();
				stmt = conn.createStatement();
			}
		} finally {
			release(stmt, rs);
			stmt = null;
			rs = null;
		}
	}

	private PreparedStatement prepareQuery(String query) throws SQLException {
		if (query.startsWith(SP_STARTS_WITH)) {
			return conn.prepareCall(query);
		} else {
			return conn.prepareStatement(query);
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
