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
package tigase.cluster.repo;

import tigase.annotations.TigaseDeprecated;
import tigase.db.*;
import tigase.db.comp.ComponentRepositoryDataSourceAware;
import tigase.db.util.RepositoryVersionAware;
import tigase.sys.TigaseRuntime;
import tigase.xmpp.jid.BareJID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version 5.2.0, 13/03/09
 */
@Repository.Meta(supportedUris = {"jdbc:[^:]+:.*"})
@Repository.SchemaId(id = Schema.SERVER_SCHEMA_ID, name = Schema.SERVER_SCHEMA_NAME)
public class ClConSQLRepository
		extends ClConConfigRepository
		implements ClusterRepoConstants, ComponentRepositoryDataSourceAware<ClusterRepoItem, DataRepository>,
		           RepositoryVersionAware {

	private static final Logger log = Logger.getLogger(ClConSQLRepository.class.getName());
	private static final BareJID repoUser =  BareJID.bareJIDInstanceNS(ClConRepoDefaults.getConfigKey());

	//J-
	/* @formatter:off */
	private static final String GET_ITEM_QUERY =
			"select " + HOSTNAME_COLUMN + ", " + SECONDARY_HOSTNAME_COLUMN + ", " + PASSWORD_COLUMN + ", " +
					LASTUPDATE_COLUMN + ", " + PORT_COLUMN + ", " + CPU_USAGE_COLUMN + ", " + MEM_USAGE_COLUMN +
					" from " + TABLE_NAME + " where " + HOSTNAME_COLUMN + " = ?";
	private static final String GET_ALL_ITEMS_QUERY =
			"select " + HOSTNAME_COLUMN + ", " + SECONDARY_HOSTNAME_COLUMN + ", " + PASSWORD_COLUMN + ", " +
					LASTUPDATE_COLUMN + ", " + PORT_COLUMN + ", " + CPU_USAGE_COLUMN + ", " + MEM_USAGE_COLUMN +
					" from " + TABLE_NAME;
	private static final String DELETE_ITEM_QUERY =
			"delete from " + TABLE_NAME + " where (" + HOSTNAME_COLUMN + " = ?)";
	private static final String INSERT_ITEM_QUERY =
			"insert into " + TABLE_NAME + " (" + HOSTNAME_COLUMN + ", " + SECONDARY_HOSTNAME_COLUMN + ", " +
					PASSWORD_COLUMN + ", " + LASTUPDATE_COLUMN + ", " + PORT_COLUMN + ", " + CPU_USAGE_COLUMN + ", " +
					MEM_USAGE_COLUMN + ") " + " (select ?, ?, ?, ?, ?, ?, ? from " + TABLE_NAME + " WHERE " +
					HOSTNAME_COLUMN + "=? HAVING count(*)=0)";
	private static final String UPDATE_ITEM_QUERY =
			"update " + TABLE_NAME + " set " + HOSTNAME_COLUMN + "= ?, " + SECONDARY_HOSTNAME_COLUMN + "= ?, " +
					PASSWORD_COLUMN + "= ?, " + LASTUPDATE_COLUMN + " = ?," + PORT_COLUMN + "= ?, " + CPU_USAGE_COLUMN +
					"= ?, " + MEM_USAGE_COLUMN + "= ? " + " where " + HOSTNAME_COLUMN + "= ?";
	/* @formatter:on */
	//J+

	private DataRepository data_repo = null;

	@Override
	public void destroy() {
		// This implementation of ClConConfigRepository is using shared connection
		// pool to database which is cached by RepositoryFactory and maybe be used
		// in other places, so we can not destroy it.
		super.destroy();
	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		super.getDefaults(defs, params);

		String repo_uri = RepositoryFactory.DERBY_REPO_URL_PROP_VAL;

		if (params.get(RepositoryFactory.GEN_USER_DB_URI) != null) {
			repo_uri = (String) params.get(RepositoryFactory.GEN_USER_DB_URI);
		}
		defs.put(REPO_URI_PROP_KEY, repo_uri);
	}

	@Override
	public void setDataSource(DataRepository data_repo) {
		try {
			checkDB(data_repo);

			data_repo.initPreparedStatement(GET_ITEM_QUERY, GET_ITEM_QUERY);
			data_repo.initPreparedStatement(GET_ALL_ITEMS_QUERY, GET_ALL_ITEMS_QUERY);
			data_repo.initPreparedStatement(INSERT_ITEM_QUERY, INSERT_ITEM_QUERY);
			data_repo.initPreparedStatement(UPDATE_ITEM_QUERY, UPDATE_ITEM_QUERY);
			data_repo.initPreparedStatement(DELETE_ITEM_QUERY, DELETE_ITEM_QUERY);
			this.data_repo = data_repo;
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem initializing database: ", e);
		}
	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	@Override
	public void initRepository(String conn_str, Map<String, String> params) throws DBInitException {
		super.initRepository(conn_str, params);
		try {
			data_repo = RepositoryFactory.getDataRepository(null, conn_str, params);
			data_repo.checkSchemaVersion(this, true);
			setDataSource(data_repo);
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem initializing database: ", e);
		}
	}

	@Override
	public void removeItem(String key) {
		super.removeItem(key);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Removing item form database: {0}", key);
		}
		try {
			PreparedStatement removeItem = data_repo.getPreparedStatement(repoUser, DELETE_ITEM_QUERY);
			synchronized (removeItem) {
				removeItem.setString(1, key);
				removeItem.executeUpdate();
			}

		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem removing element: " + key + " from DB: ", e);
		}

	}

	@Override
	public void storeItem(ClusterRepoItem item) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Storing item to repository: {0}", item);
		}
		try {
			PreparedStatement updateItemSt = data_repo.getPreparedStatement(repoUser, UPDATE_ITEM_QUERY);
			PreparedStatement insertItemSt = data_repo.getPreparedStatement(repoUser, INSERT_ITEM_QUERY);

			// relatively most DB compliant UPSERT
			Date date = new Date();

			synchronized (updateItemSt) {
				updateItemSt.setString(1, item.getHostname());
				updateItemSt.setString(2, item.getSecondaryHostname());
				updateItemSt.setString(3, item.getPassword());
				data_repo.setTimestamp(updateItemSt, 4, new Timestamp(date.getTime()));
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
				data_repo.setTimestamp(insertItemSt, 4, new Timestamp(date.getTime()));
				insertItemSt.setInt(5, item.getPortNo());
				insertItemSt.setFloat(6, item.getCpuUsage());
				insertItemSt.setFloat(7, item.getMemUsage());
				insertItemSt.setString(8, item.getHostname());
				insertItemSt.executeUpdate();
			}

		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem storing element to DB: " + item, e);
		}
	}

	@Override
	public void reload() {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Reloading items");
		}

		if ((System.currentTimeMillis() - lastReloadTime) <= (autoReloadInterval * lastReloadTimeFactor)) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Last reload performed in {0}, skipping: ",
						(System.currentTimeMillis() - lastReloadTime));
			}
			return;
		}
		lastReloadTime = System.currentTimeMillis();

		super.reload();

		try {
			ResultSet rs = null;
			PreparedStatement getAllItemsSt = data_repo.getPreparedStatement(repoUser, GET_ALL_ITEMS_QUERY);

			synchronized (getAllItemsSt) {
				try {
					rs = getAllItemsSt.executeQuery();
					while (rs.next()) {
						ClusterRepoItem item = getItemInstance();

						item.setHostname(rs.getString(HOSTNAME_COLUMN));
						item.setSecondaryHostname(rs.getString(SECONDARY_HOSTNAME_COLUMN));
						item.setPassword(rs.getString(PASSWORD_COLUMN));
						item.setLastUpdate(data_repo.getTimestamp(rs, LASTUPDATE_COLUMN).getTime());
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

			//if there is a SQLException we should return to avoid triggering `removeObsoloteItems()` method as
			// possibly not items have been properly reloaded and some may have stale update-date information.
			return;
		}
		// make sure we remove items which are gone from the database after timeout (those have last update not updated)
		// and are not removed from in-memory cache by above query
		if (auto_remove_obsolete_items) {
			removeObsoloteItems(5000);
		}
	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	@Override
	public void setProperties(Map<String, Object> properties) {
		super.setProperties(properties);
	}

	@Override
	public void store() {

		// Do nothing everything is written on demand to DB
	}

	/**
	 * Performs database check, creates missing schema if necessary
	 *
	 */
	private void checkDB(DataRepository data_repo) throws SQLException {
		if (!data_repo.checkTable(TABLE_NAME)) {
			log.info("DB for external component is not OK, stopping server...");

			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{"ERROR! Terminating the server process.",
												 "Problem initializing the server: missing tig_cluster_nodes table on " +
														 data_repo.getResourceUri(),
												 "Please fix the problem and start the server again."}, 1);
		}
	}
}
