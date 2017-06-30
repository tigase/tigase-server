/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.db.jdbc;

import tigase.db.DBInitException;
import tigase.db.DataRepository;
import tigase.db.Repository;
import tigase.stats.CounterValue;
import tigase.stats.StatisticsProviderIfc;
import tigase.stats.StatisticsList;
import tigase.xmpp.BareJID;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Sep 3, 2010 5:55:41 PM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Repository.Meta( isDefault=true, supportedUris = { "jdbc:[^:]+:.*" } )
public class DataRepositoryImpl implements DataRepository, StatisticsProviderIfc {
	private static final Logger log = Logger.getLogger(DataRepositoryImpl.class.getName());

	/** Field description */
	public static final String DERBY_CONNVALID_QUERY = "values 1";

	/** Field description */
	public static final String JDBC_CONNVALID_QUERY = "select 1";

	/** Field description */
	public static final String MYSQL_CHECK_TABLE_QUERY =
			"select * from information_schema.tables where table_name = ? and table_schema = ?";

	/** Field description */
	public static final String PGSQL_CHECK_TABLE_QUERY =
			"select * from pg_tables where tablename = ? and schemaname = ?";

	/** Field description */
	public static final String DERBY_CHECK_TABLE_QUERY =
			"select * from SYS.SYSTABLES where tablename = UPPER(?) and ? is not null";

	/** Field description */
	public static final String SQLSERVER_CHECK_TABLE_QUERY =
			"SELECT * FROM INFORMATION_SCHEMA.TABLES where TABLE_TYPE = 'BASE TABLE' AND  TABLE_NAME = ? and TABLE_SCHEMA = ?";

	/** Field description */
	public static final String OTHER_CHECK_TABLE_QUERY = "";

	/** Field description */
	public static final String SP_STARTS_WITH = "{ call";

	public static final String QUERY_TIMEOUT_PROP_KEY = "sql-query-timeout";
	/** Field description */
	public static final int QUERY_TIMEOUT = 10;

	public static final String DB_CONN_TIMEOUT_PROP_KEY = "db-conn-timeout";
	/** Field description */
	public static final int DB_CONN_TIMEOUT = 15;

	private dbTypes database = null;

	private Connection conn = null;
	private PreparedStatement conn_valid_st = null;
	private long connectionValidateInterval = 1000 * 60;
	private String db_conn = null;
	private long lastConnectionValidated = 0;
	private boolean derby_mode = false;
	private Map<String, PreparedStatement> db_statements =
			new ConcurrentSkipListMap<String, PreparedStatement>();
	private Map<String, DBQuery> db_queries = new ConcurrentSkipListMap<String, DBQuery>();
	private String check_table_query = OTHER_CHECK_TABLE_QUERY;
	private String table_schema = null;
	private int query_timeout = QUERY_TIMEOUT;
	private int db_conn_timeout = DB_CONN_TIMEOUT;

	private CounterValue reconnectionCounter = null;
	private CounterValue reconnectionFailedCounter = null;

	@Override
	public boolean checkTable(String tableName) throws SQLException {
		PreparedStatement checkTableSt = getPreparedStatement(null, check_table_query);

		if (checkTableSt == null) {
			return true;
		}

		boolean result = false;
		ResultSet rs = null;

		synchronized (checkTableSt) {
			try {
				checkTableSt.setString(1, tableName);
				checkTableSt.setString(2, table_schema);
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

	@Override
	public boolean checkTable(String tableName, String createTableQuery)
			throws SQLException {
		ResultSet rs = null;
		Statement st = null;
		boolean result = false;

		try {
			log.log(Level.INFO, "Checking if table {0} exists in DB {1}.", new Object[] {
					tableName, table_schema });
			if (!checkTable(tableName)) {
				log.log(Level.INFO, "Table {0} not found in database, creating: {1}",
						new Object[] { tableName, createTableQuery });
				st = createStatement(null);
				if (!db_conn.contains("derby")) {
					st.executeUpdate(createTableQuery);
				} else {
					String[] queries = createTableQuery.split(";");
					for (String query : queries) {
						query = query.trim();
						if (query.isEmpty())
							continue;

						st.executeUpdate(query);
					}
				}
				result = true;
			} else {
				log.log(Level.INFO, "OK table {0} found in database.", tableName);
			}
		} finally {
			release(st, rs);
			rs = null;
			st = null;

			// stmt = null;
		}
		return result;
	}

	@Override
	public Statement createStatement(BareJID user_id) throws SQLException {
		checkConnection();
		// This synchronization is used to prevent call when the connection and
		// all prepared statements are being recreated.
		synchronized (db_statements) {
			return conn.createStatement();
		}
	}

	@Override
	public PreparedStatement getPreparedStatement(BareJID user_id, String stIdKey)
			throws SQLException {
		checkConnection();

		// This synchronization is used to prevent call when the connection and
		// all prepared statements are being recreated.
		synchronized (db_statements) {
			return db_statements.get(stIdKey);
		}
	}

	@Override
	public String getResourceUri() {
		return db_conn;
	}

	@Override
	public dbTypes getDatabaseType() {
		return database;
	}

	@Override
	public void initPreparedStatement(String key, String query) throws SQLException {
		db_queries.put(key, new DBQuery(query, Statement.NO_GENERATED_KEYS));
		initStatement(key);
	}

	@Override
	public void initPreparedStatement(String key, String query, int autoGeneratedKeys) throws SQLException {
		db_queries.put(key, new DBQuery(query, autoGeneratedKeys));
		initStatement(key);
	}	
	
	@Override
	public void initRepository(String resource_uri, Map<String, String> params)
			throws DBInitException {
		String driverClass = null;

		if ( resource_uri.startsWith( "jdbc:postgresql" ) ){
			database = dbTypes.postgresql;
		} else if ( resource_uri.startsWith( "jdbc:mysql" ) ){
			database = dbTypes.mysql;
		} else if ( resource_uri.startsWith( "jdbc:derby" ) ){
			database = dbTypes.derby;
		} else if ( resource_uri.startsWith( "jdbc:jtds:sqlserver" ) ){
			database = dbTypes.jtds;
		} else if ( resource_uri.startsWith( "jdbc:sqlserver" ) ){
			database = dbTypes.sqlserver;
		}
		
		if (database == null) {
			throw new DBInitException("Database not supported");
		}
		
		switch ( database ) {
			case postgresql:
				driverClass = "org.postgresql.Driver";
				check_table_query = PGSQL_CHECK_TABLE_QUERY;
				break;
			case mysql:
				driverClass = "com.mysql.jdbc.Driver";
				check_table_query = MYSQL_CHECK_TABLE_QUERY;
				break;
			case derby:
				driverClass = "org.apache.derby.jdbc.EmbeddedDriver";
				check_table_query = DERBY_CHECK_TABLE_QUERY;
				break;
			case jtds:
				driverClass = "net.sourceforge.jtds.jdbc.Driver";
				check_table_query = SQLSERVER_CHECK_TABLE_QUERY;
				break;
			case sqlserver:
				driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
				check_table_query = SQLSERVER_CHECK_TABLE_QUERY;
				break;
			default:
				driverClass = "net.sf.log4jdbc.sql.jdbcapi.DriverSpy";
				check_table_query = OTHER_CHECK_TABLE_QUERY;
				break;
		}

		try {
			Class.forName( driverClass, true, this.getClass().getClassLoader() );
		} catch ( ClassNotFoundException ex ) {
			Logger.getLogger( DataRepositoryImpl.class.getName() ).log( Level.SEVERE, null, ex );
		}

		db_conn = resource_uri;
		db_conn_timeout = getParam( DB_CONN_TIMEOUT_PROP_KEY, params, DB_CONN_TIMEOUT );
		query_timeout = getParam( QUERY_TIMEOUT_PROP_KEY, params, QUERY_TIMEOUT );

		if ( db_conn != null ){

			switch ( database ) {
				case jtds:
				case sqlserver:
					table_schema = "dbo";
					break;
				case postgresql:
					table_schema = "public";
					break;
				default:
					String[] slashes = db_conn.split( "/" );
					table_schema = slashes[slashes.length - 1].split( "\\?" )[0];
					break;
			}
			log.log( Level.INFO, "Table schema found: {0}, database type: {1}, database driver: {2}",
							 new Object[] { table_schema, database.toString(), driverClass } );
		}
		try {
			reconnectionCounter = new CounterValue("repository " + getResourceUri() + " reconnections", Level.FINER);
			reconnectionFailedCounter = new CounterValue("repository " + getResourceUri() + " failed reconnections", Level.FINER);
			initRepo();

			if (!check_table_query.isEmpty()) {
				initPreparedStatement(check_table_query, check_table_query);
			}
		} catch (SQLException ex) {
			throw new DBInitException("Database initialization failed", ex);
		}

		log.log(Level.INFO, "Initialized database connection: {0}", resource_uri);
	}

	protected int getParam(String key, Map<String, String> params, int def) {
		int result = def;
		String temp = System.getProperty(key);
		if (temp != null) {
			try {
				result = Integer.parseInt(temp);
			} catch (NumberFormatException e) {
				result = def;
			}
		}
		if (params != null) {
			temp = params.get(key);
			if (temp != null) {
				try {
					result = Integer.parseInt(temp);
				} catch (NumberFormatException e) {
					result = def;
				}
			}
		}
		return result;
	}

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

	/**
	 * <code>checkConnection</code> method checks database connection before any
	 * query. For some database servers (or JDBC drivers) it happens the
	 * connection is dropped if not in use for a long time or after certain
	 * timeout passes. This method allows us to detect the problem and
	 * reinitialize database connection. This method must not be called
	 * concurrently, therefore it is synchronized.
	 * 
	 * @return a <code>boolean</code> value if the database connection is working.
	 * @exception SQLException
	 *              if an error occurs on database query.
	 */
	private synchronized boolean checkConnection() throws SQLException {
		ResultSet rs = null;

		try {
			long tmp = System.currentTimeMillis();

			// synchronized (conn_valid_st) {
			if ((tmp - lastConnectionValidated) >= connectionValidateInterval) {
				lastConnectionValidated = tmp;
				rs = conn_valid_st.executeQuery();
			} // end of if ()
			// }

			if (((conn_valid_st == null) || conn_valid_st.isClosed())
					&& ((tmp - lastConnectionValidated) >= 1000)) {
				initRepo();
			} // end of if ()
		} catch (Exception e) {
			initRepo();
		} finally {
			release(null, rs);
		} // end of try-catch

		return true;
	}

	/**
	 * <code>initPreparedStatements</code> method initializes internal database
	 * connection variables such as prepared statements.
	 * 
	 * @exception SQLException
	 *              if an error occurs on database query.
	 */
	private void initPreparedStatements() throws SQLException {
		String query = (derby_mode ? DERBY_CONNVALID_QUERY : JDBC_CONNVALID_QUERY);

		conn_valid_st = prepareQuery(query, Statement.NO_GENERATED_KEYS);
		try {
			conn_valid_st.setQueryTimeout(query_timeout);
		} catch (SQLException ex) {
			// Ignore for now, it seems that PostgreSQL does not support this method
			// call yet
		}

		for (String key : db_queries.keySet()) {
			initStatement(key);
		}
	}

	private void initStatement(String key) throws SQLException {
		DBQuery dbQuery = db_queries.get(key);

		PreparedStatement st = prepareQuery(dbQuery.query, dbQuery.autoGeneratedKeys);

		st = (PreparedStatement) Proxy.newProxyInstance(
				this.getClass().getClassLoader(),
				new Class[]{PreparedStatement.class},
				new PreparedStatementInvocationHandler(st));

		try {
			st.setQueryTimeout(query_timeout);
		} catch (SQLException ex) {
			// Ignore for now, it seems that PostgreSQL does not support this method
			// call yet
		}
		db_statements.put(key, st);
	}

	/**
	 * <code>initRepo</code> method initializes database connection and data
	 * repository.
	 * 
	 * @exception SQLException
	 *              if an error occurs on database query.
	 */
	private void initRepo() throws SQLException {


		boolean failure = true;
		try {
			if (conn != null) {
				reconnectionCounter.inc();
				log.log(Level.INFO, "Reconnecting connection: {0}", reconnectionCounter);
			}
			synchronized (db_statements) {
				db_statements.clear();
				DriverManager.setLoginTimeout(db_conn_timeout);
				conn = DriverManager.getConnection(db_conn);
				conn.setAutoCommit(true);
				derby_mode = db_conn.startsWith("jdbc:derby");
				initPreparedStatements();

				failure = false;
				// stmt = conn.createStatement();
			}
		} finally {
			release(null, null);

			if (failure) {
				reconnectionFailedCounter.inc();
				log.log(Level.INFO, "Reconnecting connection failed: {0}", reconnectionFailedCounter);
			}
			// release(stmt, rs);
			// stmt = null;
//			rs = null;
		}
	}

	private PreparedStatement prepareQuery(String query, int autoGeneratedKeys) throws SQLException {
		if (query.startsWith(SP_STARTS_WITH)) {
			return conn.prepareCall( query );
		} else {
			switch ( database ) {
				case sqlserver:
					return conn.prepareStatement( query, Statement.RETURN_GENERATED_KEYS );
				default:
					return conn.prepareStatement( query, autoGeneratedKeys );
			}
		}
	}

	@Override
	public DataRepository takeRepoHandle(BareJID user_id) {
		return this;
	}

	@Override
	public void startTransaction() throws SQLException {
		conn.setAutoCommit(false);
	}

	@Override
	public void commit() throws SQLException {
		conn.commit();
	}

	@Override
	public void rollback() throws SQLException {
		conn.rollback();
	}

	@Override
	public void endTransaction() throws SQLException {
		conn.setAutoCommit(true);
	}

	@Override
	public void releaseRepoHandle(DataRepository repo) {
	}

	@Override
	public void getStatistics(String compName, StatisticsList list) {
		long reconnections = list.getValue(compName, reconnectionCounter.getName(), 0L) + reconnectionCounter.getValue();
		list.add(compName, reconnectionCounter.getName(), reconnections, Level.FINER);
		long failedReconnections = list.getValue(compName, reconnectionFailedCounter.getName(), 0L) + reconnectionFailedCounter.getValue();
		list.add(compName, reconnectionFailedCounter.getName(),failedReconnections, Level.FINER);
	}
	
	private class DBQuery {
		final String query;
		final int autoGeneratedKeys;
		
		DBQuery(String query, int autoGeneratedKeys) {
			this.query = query;
			this.autoGeneratedKeys = autoGeneratedKeys;
		}
	}
}
