/*
 * ClConSQLRepository.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
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
 *
 */



package tigase.cluster.repo;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.DBInitException;
import tigase.db.DataRepository;
import tigase.db.Repository;
import tigase.db.RepositoryFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class description
 *
 *
 * @version        5.2.0, 13/03/09
 * @author         <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Repository.Meta( supportedUris = { "jdbc:[^:]+:.*" } )
public class ClConSQLRepository
				extends ClConConfigRepository
				implements ClusterRepoConstants {
	/**
	 * Private logger for class instances.
	 */
	private static final Logger log = Logger.getLogger(ClConSQLRepository.class.getName());
	//J-
	/* @formatter:off */
	private static final String GET_ITEM_QUERY =
					"select "
					+ HOSTNAME_COLUMN + ", "
					+ SECONDARY_HOSTNAME_COLUMN + ", "
					+ PASSWORD_COLUMN + ", "
					+ LASTUPDATE_COLUMN + ", "
					+ PORT_COLUMN + ", "
					+ CPU_USAGE_COLUMN + ", "
					+ MEM_USAGE_COLUMN
					+ " from " + TABLE_NAME + " where " + HOSTNAME_COLUMN + " = ?";
	private static final String GET_ALL_ITEMS_QUERY =
					"select "
					+ HOSTNAME_COLUMN + ", "
					+ SECONDARY_HOSTNAME_COLUMN + ", "
					+ PASSWORD_COLUMN + ", "
					+ LASTUPDATE_COLUMN + ", "
					+ PORT_COLUMN + ", "
					+ CPU_USAGE_COLUMN + ", "
					+ MEM_USAGE_COLUMN
					+ " from " + TABLE_NAME;
	private static final String DELETE_ITEM_QUERY =
					"delete from " + TABLE_NAME + " where (" + HOSTNAME_COLUMN + " = ?)";
	private static final String CREATE_TABLE_QUERY_MYSQL =
					"create table " + TABLE_NAME + " ("
					+ "  " + HOSTNAME_COLUMN + " varchar(255) not null,"
					+ "  " + SECONDARY_HOSTNAME_COLUMN + " varchar(255),"
					+ "  " + PASSWORD_COLUMN + " varchar(255) not null,"
					+ "  " + LASTUPDATE_COLUMN
					+ " TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
					+ "  " + PORT_COLUMN + " int,"
					+ "  " + CPU_USAGE_COLUMN + " double precision unsigned not null,"
					+ "  " + MEM_USAGE_COLUMN + " double precision unsigned not null,"
					+ "  primary key(" + HOSTNAME_COLUMN + "))"
					+ " ENGINE=InnoDB default character set utf8 ROW_FORMAT=DYNAMIC";
	private static final String CREATE_TABLE_QUERY =
					"create table " + TABLE_NAME + " ("
					+ "  " + HOSTNAME_COLUMN + " varchar(512) not null,"
					+ "  " + SECONDARY_HOSTNAME_COLUMN + " varchar(512),"
					+ "  " + PASSWORD_COLUMN + " varchar(255) not null,"
					+ "  " + LASTUPDATE_COLUMN
					+ " TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
					+ "  " + PORT_COLUMN + " int,"
					+ "  " + CPU_USAGE_COLUMN + " double precision not null,"
					+ "  " + MEM_USAGE_COLUMN + " double precision not null,"
					+ "  primary key(" + HOSTNAME_COLUMN + "))";
	private static final String CREATE_TABLE_QUERY_SQLSERVER =
					"create table [dbo].[" + TABLE_NAME + "] ("
					+ "  " + HOSTNAME_COLUMN + " nvarchar(512) not null,"
					+ "  " + SECONDARY_HOSTNAME_COLUMN + " nvarchar(512),"
					+ "  " + PASSWORD_COLUMN + " nvarchar(255) not null,"
					+ "  " + LASTUPDATE_COLUMN
					+ " [datetime] NULL,"
					+ "  " + PORT_COLUMN + " int,"
					+ "  " + CPU_USAGE_COLUMN + " double precision not null,"
					+ "  " + MEM_USAGE_COLUMN + " double precision not null,"
					+ " CONSTRAINT [PK_" + TABLE_NAME + "] PRIMARY KEY CLUSTERED ( [" + HOSTNAME_COLUMN + "] ASC ) ON [PRIMARY], "
					+ " CONSTRAINT [IX_" + TABLE_NAME + "_" + HOSTNAME_COLUMN + "] UNIQUE NONCLUSTERED ( [" + HOSTNAME_COLUMN + "] ASC ) ON [PRIMARY] "
					+ ") ON [PRIMARY]"
					+ "ALTER TABLE [dbo].[" + TABLE_NAME + "] ADD  CONSTRAINT "
					+ "[DF_" + TABLE_NAME + "_" + LASTUPDATE_COLUMN + "]  DEFAULT (getdate()) FOR [" + LASTUPDATE_COLUMN + "] ";
	private static final String INSERT_ITEM_QUERY =
					"insert into " + TABLE_NAME + " ("
					+ HOSTNAME_COLUMN + ", "
					+ SECONDARY_HOSTNAME_COLUMN + ", "
					+ PASSWORD_COLUMN + ", "
					+ LASTUPDATE_COLUMN + ", "
					+ PORT_COLUMN + ", "
					+ CPU_USAGE_COLUMN + ", "
					+ MEM_USAGE_COLUMN
					+ ") "
					+ " (select ?, ?, ?, ?, ?, ?, ? from " + TABLE_NAME
					+ " WHERE " + HOSTNAME_COLUMN + "=? HAVING count(*)=0)";
	private static final String UPDATE_ITEM_QUERY =
					"update " + TABLE_NAME + " set "
					+ HOSTNAME_COLUMN + "= ?, "
					+ SECONDARY_HOSTNAME_COLUMN + "= ?, "
					+ PASSWORD_COLUMN + "= ?, "
					+ LASTUPDATE_COLUMN + " = ?,"
					+ PORT_COLUMN + "= ?, "
					+ CPU_USAGE_COLUMN + "= ?, "
					+ MEM_USAGE_COLUMN + "= ? "
					+ " where " + HOSTNAME_COLUMN + "= ?"	;
	/* @formatter:on */
	//J+

	//~--- fields ---------------------------------------------------------------

	private DataRepository data_repo = null;

	@Override
	public void destroy() {
		// This implementation of ClConConfigRepository is using shared connection
		// pool to database which is cached by RepositoryFactory and maybe be used
		// in other places, so we can not destroy it.
		super.destroy();
	}
	
	//~--- get methods ----------------------------------------------------------

	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		super.getDefaults(defs, params);

		String repo_uri = RepositoryFactory.DERBY_REPO_URL_PROP_VAL;

		if (params.get(RepositoryFactory.GEN_USER_DB_URI) != null) {
			repo_uri = (String) params.get(RepositoryFactory.GEN_USER_DB_URI);
		}
		defs.put(REPO_URI_PROP_KEY, repo_uri);
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(String conn_str, Map<String, String> params)
					throws DBInitException {
		super.initRepository(conn_str, params);
		try {
			data_repo = RepositoryFactory.getDataRepository(null, conn_str, params);
			checkDB();

			// data_repo.initPreparedStatement(CHECK_TABLE_QUERY, CHECK_TABLE_QUERY);
			data_repo.initPreparedStatement(GET_ITEM_QUERY, GET_ITEM_QUERY);
			data_repo.initPreparedStatement(GET_ALL_ITEMS_QUERY, GET_ALL_ITEMS_QUERY);
			data_repo.initPreparedStatement(INSERT_ITEM_QUERY, INSERT_ITEM_QUERY);
			data_repo.initPreparedStatement(UPDATE_ITEM_QUERY, UPDATE_ITEM_QUERY);
			data_repo.initPreparedStatement(DELETE_ITEM_QUERY, DELETE_ITEM_QUERY);
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem initializing database: ", e);
		}
	}

	@Override
	public void removeItem( String key ) {
		super.removeItem( key );

		if ( log.isLoggable( Level.FINEST ) ){
			log.log( Level.FINEST, "Removing item form database: {0}", key );
		}
		try {
			PreparedStatement removeItem = data_repo.getPreparedStatement( null, DELETE_ITEM_QUERY );
			synchronized ( removeItem ) {
				removeItem.setString( 1, key );
				removeItem.executeUpdate();
			}

		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem removing elements from DB: ", e);
		}

	}

	@Override
	public void storeItem(ClusterRepoItem item) {
		if ( log.isLoggable( Level.FINEST ) ){
			log.log( Level.FINEST, "Storing item to repository: {0}", item );
		}
		try {
			PreparedStatement updateItemSt = data_repo.getPreparedStatement(null,	UPDATE_ITEM_QUERY);
			PreparedStatement insertItemSt = data_repo.getPreparedStatement(null, INSERT_ITEM_QUERY);

			// relatively most DB compliant UPSERT
			Date date = new Date();

			synchronized (updateItemSt) {
				updateItemSt.setString(1, item.getHostname());
				updateItemSt.setString(2, item.getSecondaryHostname());
				updateItemSt.setString(3, item.getPassword());
				updateItemSt.setTimestamp(4, new Timestamp(date.getTime()));
				updateItemSt.setInt(5, item.getPortNo());
				updateItemSt.setFloat(6, item.getCpuUsage());
				updateItemSt.setFloat(7, item.getMemUsage());
				updateItemSt.setString(8, item.getHostname());
				updateItemSt.executeUpdate();
			}

			synchronized (insertItemSt) {
				insertItemSt.setString(1, item.getHostname());
				insertItemSt.setString(2, item.getSecondaryHostname());
				insertItemSt.setString(3, item.getPassword());
				insertItemSt.setTimestamp(4, new Timestamp(date.getTime()));
				insertItemSt.setInt(5, item.getPortNo());
				insertItemSt.setFloat(6, item.getCpuUsage());
				insertItemSt.setFloat(7, item.getMemUsage());
				insertItemSt.setString(8, item.getHostname());
				insertItemSt.executeUpdate();
			}

		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting elements from DB: ", e);
		}
	}

	@Override
	public void reload() {
		if ( log.isLoggable( Level.FINEST ) ){
			log.log( Level.FINEST, "Reloading items" );
		}

		if ( ( System.currentTimeMillis() - lastReloadTime ) <= ( autoreload_interval * lastReloadTimeFactor ) ){
			if ( log.isLoggable( Level.FINE ) ){
				log.log( Level.FINE, "Last reload performed in {0}, skipping: ", ( System.currentTimeMillis() - lastReloadTime ) );
			}
			return;
		}
		lastReloadTime = System.currentTimeMillis();

		super.reload();

		try {
			ResultSet rs = null;
			PreparedStatement getAllItemsSt = data_repo.getPreparedStatement(null,
					GET_ALL_ITEMS_QUERY);
			
			synchronized (getAllItemsSt) {
				try {
					rs = getAllItemsSt.executeQuery();
					while (rs.next()) {
						ClusterRepoItem item = getItemInstance();

						item.setHostname(rs.getString(HOSTNAME_COLUMN));
						item.setSecondaryHostname(rs.getString(SECONDARY_HOSTNAME_COLUMN));
						item.setPassword(rs.getString(PASSWORD_COLUMN));
						item.setLastUpdate(rs.getTimestamp(LASTUPDATE_COLUMN).getTime());
						item.setPort(rs.getInt(PORT_COLUMN));
						item.setCpuUsage(rs.getFloat(CPU_USAGE_COLUMN));
						item.setMemUsage(rs.getFloat(MEM_USAGE_COLUMN));
						itemLoaded(item);
					}
				} finally {
					data_repo.release(null, rs);
				}
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting elements from DB: ", e);
		}
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setProperties(Map<String, Object> properties) {
		super.setProperties(properties);
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void store() {

		// Do nothing everything is written on demand to DB
	}

	/**
	 * Performs database check, creates missing schema if necessary
	 *
	 * @throws SQLException
	 */
	private void checkDB() throws SQLException {
		Statement st = null;

		DataRepository.dbTypes databaseType = data_repo.getDatabaseType();
		String createTableQuery;

		switch ( databaseType ) {
			case mysql:
				createTableQuery = CREATE_TABLE_QUERY_MYSQL;
				break;
			case jtds:
			case sqlserver:
				createTableQuery = CREATE_TABLE_QUERY_SQLSERVER;
				break;
			default:
				createTableQuery = CREATE_TABLE_QUERY;
				break;
		}

		Statement stmt = null;
		
		try {
			if (!data_repo.checkTable(TABLE_NAME)) {
				log.info("DB for external component is not OK, creating missing tables...");

				st = data_repo.createStatement(null);
				st.executeUpdate(createTableQuery);
				log.info("DB for external component created OK");
			}

			try {
				stmt = data_repo.createStatement(null);
				stmt.executeQuery("select " + SECONDARY_HOSTNAME_COLUMN + " from " + TABLE_NAME + " where " + HOSTNAME_COLUMN + " IS NOT NULL");
			} catch (SQLException ex) {
				// if this happens then we have issue with old database schema and missing body columns in MSGS_TABLE
				String alterTable = null;
				switch (data_repo.getDatabaseType()) {
					case derby:
					case mysql:
					case postgresql:
						alterTable = "alter table " + TABLE_NAME + " add " + SECONDARY_HOSTNAME_COLUMN + " varchar(512)";
						break;
					case jtds:
					case sqlserver:
						alterTable = "alter table " + TABLE_NAME + " add " + SECONDARY_HOSTNAME_COLUMN + " nvarchar(512)";
						break;
				}
				try {
					stmt.execute(alterTable);
				} catch (SQLException ex1) {
					log.log(Level.SEVERE, "could not alter table " + TABLE_NAME + " to add missing column by SQL:\n" + alterTable, ex1);
				}
			}


		} finally {
			data_repo.release(st, null);
			st = null;
		}
	}
}
