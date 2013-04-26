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

import tigase.db.DataRepository;
import tigase.db.RepositoryFactory;

import static tigase.conf.Configurable.*;

//~--- JDK imports ------------------------------------------------------------

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

/**
 * Class description
 *
 *
 * @version        5.2.0, 13/03/09
 * @author         <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
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
					+ PASSWORD_COLUMN + ", "
					+ LASTUPDATE_COLUMN + ", "
					+ PORT_COLUMN + ", "
					+ CPU_USAGE_COLUMN + ", "
					+ MEM_USAGE_COLUMN
					+ " from " + TABLE_NAME + " where " + HOSTNAME_COLUMN + " = ?";
	private static final String GET_ALL_ITEMS_QUERY =
					"select "
					+ HOSTNAME_COLUMN + ", "
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
					+ "  " + PASSWORD_COLUMN + " varchar(255) not null,"
					+ "  " + LASTUPDATE_COLUMN
					+ " TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
					+ "  " + PORT_COLUMN + " int,"
					+ "  " + CPU_USAGE_COLUMN + " float unsigned not null,"
					+ "  " + MEM_USAGE_COLUMN + " float unsigned not null,"
					+ "  primary key(" + HOSTNAME_COLUMN + "))";
	private static final String CREATE_TABLE_QUERY =
					"create table " + TABLE_NAME + " ("
					+ "  " + HOSTNAME_COLUMN + " varchar(512) not null,"
					+ "  " + PASSWORD_COLUMN + " varchar(255) not null,"
					+ "  " + LASTUPDATE_COLUMN
					+ " TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
					+ "  " + PORT_COLUMN + " int,"
					+ "  " + CPU_USAGE_COLUMN + " double precision not null,"
					+ "  " + MEM_USAGE_COLUMN + " double precision not null,"
					+ "  primary key(" + HOSTNAME_COLUMN + "))";
	private static final String INSERT_ITEM_QUERY =
					"insert into " + TABLE_NAME + " ("
					+ HOSTNAME_COLUMN + ", "
					+ PASSWORD_COLUMN + ", "
					+ LASTUPDATE_COLUMN + ", "
					+ PORT_COLUMN + ", "
					+ CPU_USAGE_COLUMN + ", "
					+ MEM_USAGE_COLUMN
					+ ") "
					+ " (select ?, ?, CURRENT_TIMESTAMP, ?, ?, ? from " + TABLE_NAME
					+ " WHERE " + HOSTNAME_COLUMN + "=? HAVING count(*)=0)";
	private static final String UPDATE_ITEM_QUERY =
					"update " + TABLE_NAME + " set "
					+ HOSTNAME_COLUMN + "= ?, "
					+ PASSWORD_COLUMN + "= ?, "
					+ LASTUPDATE_COLUMN + " = CURRENT_TIMESTAMP,"
					+ PORT_COLUMN + "= ?, "
					+ CPU_USAGE_COLUMN + "= ?, "
					+ MEM_USAGE_COLUMN + "= ? "
					+ " where " + HOSTNAME_COLUMN + "= ?"	;
	/* @formatter:on */
	//J+

	//~--- fields ---------------------------------------------------------------

	private DataRepository data_repo = null;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param defs
	 * @param params
	 */
	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		super.getDefaults(defs, params);

		String repo_uri = DERBY_REPO_URL_PROP_VAL;

		if (params.get(GEN_USER_DB_URI) != null) {
			repo_uri = (String) params.get(GEN_USER_DB_URI);
		}
		defs.put(REPO_URI_PROP_KEY, repo_uri);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param conn_str
	 * @param params
	 *
	 * @throws SQLException
	 */
	public void initRepository(String conn_str, Map<String, String> params)
					throws SQLException {
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

	/**
	 * Method description
	 *
	 *
	 * @param item
	 */
	@Override
	public void storeItem(ClusterRepoItem item) {
		try {
			PreparedStatement updateItemSt = data_repo.getPreparedStatement(null,	UPDATE_ITEM_QUERY);
			PreparedStatement insertItemSt = data_repo.getPreparedStatement(null, INSERT_ITEM_QUERY);

			// relatively most DB compliant UPSERT

			synchronized (updateItemSt) {
				updateItemSt.setString(1, item.getHostname());
				updateItemSt.setString(2, item.getPassword());
				updateItemSt.setInt(3, item.getPortNo());
				updateItemSt.setFloat(4, item.getCpuUsage());
				updateItemSt.setFloat(5, item.getMemUsage());
				updateItemSt.setString(6, item.getHostname());
				updateItemSt.executeUpdate();
			}

			synchronized (insertItemSt) {
				insertItemSt.setString(1, item.getHostname());
				insertItemSt.setString(2, item.getPassword());
				insertItemSt.setInt(3, item.getPortNo());
				insertItemSt.setFloat(4, item.getCpuUsage());
				insertItemSt.setFloat(5, item.getMemUsage());
				insertItemSt.setString(6, item.getHostname());
				insertItemSt.executeUpdate();
			}

		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting elements from DB: ", e);
		}
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void reload() {
		super.reload();

		ResultSet rs = null;

		try {
			PreparedStatement getAllItemsSt = data_repo.getPreparedStatement(null,
					GET_ALL_ITEMS_QUERY);

			synchronized (getAllItemsSt) {
				rs = getAllItemsSt.executeQuery();
				while (rs.next()) {
					ClusterRepoItem item = getItemInstance();

					item.setHostname(rs.getString(HOSTNAME_COLUMN));
					item.setPassword(rs.getString(PASSWORD_COLUMN));
					item.setLastUpdate(rs.getTimestamp(LASTUPDATE_COLUMN).getTime());
					item.setPort(rs.getInt(PORT_COLUMN));
					item.setCpuUsage(rs.getFloat(CPU_USAGE_COLUMN));
					item.setMemUsage(rs.getFloat(MEM_USAGE_COLUMN));
					itemLoaded(item);
				}
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting elements from DB: ", e);
		} finally {
			data_repo.release(null, rs);
		}
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param properties
	 */
	@Override
	public void setProperties(Map<String, Object> properties) {
		super.setProperties(properties);

		String repo_uri = (String) properties.get(REPO_URI_PROP_KEY);

		try {
			initRepository(repo_uri, null);
		} catch (SQLException ex) {
			log.log(Level.WARNING, "Problem initializing database.", ex);
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	@Override
	public void store() {

		// Do nothing everything is written on demand to DB
	}

	private void checkDB() throws SQLException {
		ResultSet rs = null;
		Statement st = null;

		String resourceUri = data_repo.getResourceUri();
		String createTableQuery;

		if ( resourceUri.startsWith( "jdbc:mysql:" ) ){
			createTableQuery = CREATE_TABLE_QUERY_MYSQL;
		} else {
			createTableQuery = CREATE_TABLE_QUERY;
		}

		try {
			if (!data_repo.checkTable(TABLE_NAME)) {
				log.info("DB for external component is not OK, creating missing tables...");

				st = data_repo.createStatement(null);
				st.executeUpdate(createTableQuery);
				log.info("DB for external component created OK");
			}
		} finally {
			data_repo.release(st, rs);
			rs = null;
			st = null;
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/03/11
