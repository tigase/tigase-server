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

package tigase.conf;

//~--- non-JDK imports --------------------------------------------------------

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.DBInitException;
import tigase.db.DataRepository;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.util.DataTypes;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Dec 15, 2009 10:44:00 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ConfigSQLRepository extends ConfigurationCache {

	/** Field description */
	public static final String CONFIG_REPO_URI_PROP_KEY = "tigase-config-repo-uri";

	/** Field description */
	public static final String CONFIG_REPO_URI_INIT_KEY = "--tigase-config-repo-uri";

	/**
	 * Private logger for class instancess.
	 */
	private static final Logger log = Logger.getLogger(ConfigSQLRepository.class.getName());

	//~--- fields ---------------------------------------------------------------

	private JDBCAccess dbAccess = new JDBCAccess();

	//~--- methods --------------------------------------------------------------

	@Override
	public void addItem(String compName, ConfigItem item) {
		dbAccess.addItem(item);
	}

	@Override
	public Collection<ConfigItem> allItems() throws TigaseDBException {
		return dbAccess.getAllItems();
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String[] getCompNames() {
		return dbAccess.getComponentNames();
	}

	@Override
	public ConfigItem getItem(String compName, String node, String key) {
		return dbAccess.getItem(compName, node, key);
	}

	@Override
	public Set<ConfigItem> getItemsForComponent(String compName) {
		return dbAccess.getCompItems(compName);
	}

	@Override
	public String[] getKeys(String compName, String node) {
		return dbAccess.getKeys(compName, node);
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(String repo_uri, Map<String, String> params) throws DBInitException {
		String config_db_uri = System.getProperty(CONFIG_REPO_URI_PROP_KEY);

		if (config_db_uri == null) {
			config_db_uri = (String) params.get(CONFIG_REPO_URI_INIT_KEY);
		}

		if (config_db_uri == null) {
			config_db_uri = (String) params.get(RepositoryFactory.GEN_USER_DB_URI);
		}

		if (config_db_uri == null) {
			log.severe("Missing configuration database connection string.");
			log.severe("Tigase needs a database connection string to load configuration.");
			log.severe("You can provide it in a few ways and the Tigase server checks");
			log.severe("following parameters in the order below:");
			log.severe("1. System property: -Dtigase-config-repo-uri=db-connection-string");
			log.severe("2. init.properties file or command line parameter: "
					+ "--tigase-config-repo-uri=db-connection-string");
			log.severe("3. init.properties file or command line parameter: "
					+ "--user-db-uri=db-connection-string");
			log.severe("Please correct the error and restart the server.");
			System.exit(1);
		}

		try {
			dbAccess.initRepository(config_db_uri, null);
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "Problem connecting to configuration database: ", ex);
			log.severe("Please check whether the database connection string is correct: "
					+ config_db_uri);
			System.exit(1);
		}
	}

	@Override
	public void removeItem(String compName, ConfigItem item) {
		dbAccess.removeItem(item);
	}

	@Override
	public int size() {
		return dbAccess.getPropertiesCount();
	}

	//~--- inner classes --------------------------------------------------------

	private class JDBCAccess {

		/** Field description */
		public static final String TABLE_NAME = "tigase_configuration";
		private static final String CLUSTER_NODE_COLUMN = "cluster_node";
		private static final String COMPONENT_NAME_COLUMN = "component_name";
		private static final String NODE_NAME_COLUMN = "key_node";
		private static final String KEY_NAME_COLUMN = "key_name";
		private static final String VALUE_COLUMN = "value";
		private static final String FLAG_COLUMN = "flag";
		private static final String VALUE_TYPE_COLUMN = "value_type";
		private static final String LAST_UPDATE_COLUMN = "last_update";
		private static final String CREATE_TABLE_QUERY = "create table " + TABLE_NAME + " (" + "  "
			+ COMPONENT_NAME_COLUMN + " varchar(127) NOT NULL," + "  " + KEY_NAME_COLUMN
			+ " varchar(127) NOT NULL," + "  " + VALUE_COLUMN + " varchar(8191) NOT NULL," + "  "
			+ CLUSTER_NODE_COLUMN + " varchar(255) NOT NULL DEFAULT ''," + "  " + NODE_NAME_COLUMN
			+ " varchar(127) NOT NULL DEFAULT ''," + "  " + FLAG_COLUMN
			+ " varchar(32) NOT NULL DEFAULT 'DEFAULT'," + "  " + VALUE_TYPE_COLUMN
			+ " varchar(8) NOT NULL DEFAULT 'S'," + "  " + LAST_UPDATE_COLUMN + " timestamp,"
			+ "  primary key(" + CLUSTER_NODE_COLUMN + ", " + COMPONENT_NAME_COLUMN + ", "
			+ NODE_NAME_COLUMN + ", " + KEY_NAME_COLUMN + ", " + FLAG_COLUMN + "))";
		private static final String CLUSTER_NODE_WHERE_PART = " (" + CLUSTER_NODE_COLUMN + " = '' "
			+ " OR " + CLUSTER_NODE_COLUMN + " = ?) ";
		private static final String ITEM_WHERE_PART = " where " + CLUSTER_NODE_WHERE_PART + " AND ("
			+ COMPONENT_NAME_COLUMN + " = ?) " + " AND (" + NODE_NAME_COLUMN + " = ?) " + " AND ("
			+ KEY_NAME_COLUMN + " = ?) ";

		private static final String GET_ITEM_QUERY = "select * from " + TABLE_NAME + ITEM_WHERE_PART;
		private static final String ADD_ITEM_QUERY = "insert into " + TABLE_NAME + " ("
			+ CLUSTER_NODE_COLUMN + ", " + COMPONENT_NAME_COLUMN + ", " + NODE_NAME_COLUMN + ", "
			+ KEY_NAME_COLUMN + ", " + VALUE_COLUMN + ", " + VALUE_TYPE_COLUMN + ", " + FLAG_COLUMN
			+ ") " + " values (?, ?, ?, ?, ?, ?, ?)";
		private static final String UPDATE_ITEM_QUERY = "update " + TABLE_NAME + " set " + VALUE_COLUMN
			+ " = ? " + " where (" + CLUSTER_NODE_COLUMN + " = ?) " + " AND (" + COMPONENT_NAME_COLUMN
			+ " = ?) " + " AND (" + NODE_NAME_COLUMN + " = ?) " + " AND (" + KEY_NAME_COLUMN + " = ?)";
		private static final String DELETE_ITEM_QUERY = "delete from " + TABLE_NAME + ITEM_WHERE_PART;
		private static final String GET_ALL_ITEMS_QUERY = "select * from " + TABLE_NAME + " where "
			+ CLUSTER_NODE_WHERE_PART;
		private static final String GET_COMPONENT_ITEMS_QUERY = "select * from " + TABLE_NAME
			+ " where " + CLUSTER_NODE_WHERE_PART + " AND (" + COMPONENT_NAME_COLUMN + " = ?)";
		private static final String GET_UPDATED_ITEMS_QUERY = "select * from " + TABLE_NAME + " where "
			+ CLUSTER_NODE_WHERE_PART + " AND (" + FLAG_COLUMN + " <> 'INITIAL')" + " AND ("
			+ LAST_UPDATE_COLUMN + " > ?)";
		private static final String GET_COMPONENT_NAMES_QUERY = "select distinct("
			+ COMPONENT_NAME_COLUMN + ") from " + TABLE_NAME + " where " + CLUSTER_NODE_COLUMN;
		private static final String GET_PROPERTIES_COUNT_QUERY = "select count(*) as count from "
			+ TABLE_NAME + " where " + CLUSTER_NODE_COLUMN;
		private static final String GET_KEYS_QUERY = "select " + KEY_NAME_COLUMN + " from "
			+ TABLE_NAME + " where " + CLUSTER_NODE_WHERE_PART + " AND (" + COMPONENT_NAME_COLUMN
			+ " = ?)" + " AND (" + NODE_NAME_COLUMN + " = ?)";

		//~--- fields -------------------------------------------------------------

		private DataRepository data_repo = null;

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @param conn_str
		 * @param params
		 *
		 * @throws SQLException
		 */
		public void initRepository(String conn_str, Map<String, String> params) throws SQLException {
			try {
				data_repo = RepositoryFactory.getDataRepository(null, conn_str, params);

				// Check if DB is correctly setup and contains all required tables.
				checkDB();
				data_repo.initPreparedStatement(GET_ITEM_QUERY, GET_ITEM_QUERY);
				data_repo.initPreparedStatement(GET_ALL_ITEMS_QUERY, GET_ALL_ITEMS_QUERY);
				data_repo.initPreparedStatement(GET_COMPONENT_ITEMS_QUERY, GET_COMPONENT_ITEMS_QUERY);
				data_repo.initPreparedStatement(ADD_ITEM_QUERY, ADD_ITEM_QUERY);
				data_repo.initPreparedStatement(UPDATE_ITEM_QUERY, UPDATE_ITEM_QUERY);
				data_repo.initPreparedStatement(DELETE_ITEM_QUERY, DELETE_ITEM_QUERY);
				data_repo.initPreparedStatement(GET_UPDATED_ITEMS_QUERY, GET_UPDATED_ITEMS_QUERY);
				data_repo.initPreparedStatement(GET_COMPONENT_NAMES_QUERY, GET_COMPONENT_NAMES_QUERY);
				data_repo.initPreparedStatement(GET_PROPERTIES_COUNT_QUERY, GET_PROPERTIES_COUNT_QUERY);
				data_repo.initPreparedStatement(GET_KEYS_QUERY, GET_KEYS_QUERY);
			} catch (Exception e) {}
		}

		private void addItem(ConfigItem item) {
			try {
				PreparedStatement addItemSt = data_repo.getPreparedStatement(null, ADD_ITEM_QUERY);

				synchronized (addItemSt) {
					addItemSt.setString(1, ((item.getClusterNode() != null) ? item.getClusterNode() : ""));
					addItemSt.setString(2, item.getCompName());
					addItemSt.setString(3, ((item.getNodeName() != null) ? item.getNodeName() : ""));
					addItemSt.setString(4, item.getKeyName());
					addItemSt.setString(5, item.getConfigValToString());
					addItemSt.setString(6, "" + DataTypes.getTypeId(item.getConfigVal()));
					addItemSt.setString(7, item.getFlag().name());
					addItemSt.executeUpdate();
				}
			} catch (SQLException e) {

				// Maybe the configuration item is already there, let's try to update it then
				try {
					PreparedStatement updateItemSt = data_repo.getPreparedStatement(null, UPDATE_ITEM_QUERY);

					synchronized (updateItemSt) {
						updateItemSt.setString(1, item.getConfigValToString());
						updateItemSt.setString(2,
								((item.getClusterNode() != null) ? item.getClusterNode() : ""));
						updateItemSt.setString(3, item.getCompName());
						updateItemSt.setString(4, ((item.getNodeName() != null) ? item.getNodeName() : ""));
						updateItemSt.setString(5, item.getKeyName());
						updateItemSt.executeUpdate();
					}
				} catch (SQLException ex) {
					log.log(Level.WARNING,
							"Problem adding/updating an item to DB: " + item.toElement() + "\n", ex);

				}
			} catch (Exception e) {
				log.warning(e + "Exception while adding config item: " + item.toString());
			}
		}

		private void checkDB() throws SQLException {
			ResultSet rs = null;
			Statement st = null;

			try {
				if ( !data_repo.checkTable(TABLE_NAME)) {
					st = data_repo.createStatement(null);
					st.executeUpdate(CREATE_TABLE_QUERY);
				} else {
					log.info("DB for server configuration OK.");
				}
			} finally {
				data_repo.release(st, rs);
				rs = null;
				st = null;
			}
		}

		private ConfigItem createItemFromRS(ResultSet rs) throws SQLException {
			ConfigItem result = getItemInstance();
			String clusterNode = rs.getString(CLUSTER_NODE_COLUMN);
			String compName = rs.getString(COMPONENT_NAME_COLUMN);
			String nodeName = rs.getString(NODE_NAME_COLUMN);
			String keyName = rs.getString(KEY_NAME_COLUMN);
			String value_str = rs.getString(VALUE_COLUMN);
			String value_type = rs.getString(VALUE_TYPE_COLUMN);
			String flag_str = rs.getString(FLAG_COLUMN);

			result.set(clusterNode, compName, nodeName, keyName, value_str, value_type.charAt(0),
					flag_str);

			return result;
		}

		//~--- get methods --------------------------------------------------------

		private Collection<ConfigItem> getAllItems() {
			List<ConfigItem> result = new ArrayList<ConfigItem>();

			try {
				ResultSet rs = null;
				PreparedStatement getAllItemsSt = data_repo.getPreparedStatement(null, GET_ALL_ITEMS_QUERY);

				synchronized (getAllItemsSt) {
					try {
						getAllItemsSt.setString(1, getDefHostname());
						rs = getAllItemsSt.executeQuery();

						while (rs.next()) {
							ConfigItem item = createItemFromRS(rs);

							if (item.getFlag() != ConfigItem.FLAGS.INITIAL) {
								result.add(item);
							}
						}
					} finally {
						data_repo.release(null, rs);
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting elements from DB: ", e);
			}

			return result;
		}

		private Set<ConfigItem> getCompItems(String compName) {
			Set<ConfigItem> result = new LinkedHashSet<ConfigItem>();

			try {
				ResultSet rs = null;
				PreparedStatement getCompItemsSt =
					data_repo.getPreparedStatement(null, GET_COMPONENT_ITEMS_QUERY);

				synchronized (getCompItemsSt) {
					try {
						getCompItemsSt.setString(1, getDefHostname());
						getCompItemsSt.setString(2, compName);
						rs = getCompItemsSt.executeQuery();

						while (rs.next()) {
							ConfigItem item = createItemFromRS(rs);

							if (item.getFlag() != ConfigItem.FLAGS.INITIAL) {
								result.add(item);
							}
						}
					} finally {
						data_repo.release(null, rs);
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting elements from DB: ", e);
			}

			return result;
		}

		private String[] getComponentNames() {
			List<String> result = new ArrayList<String>();

			try {
				ResultSet rs = null;
				PreparedStatement getCompNamesSt =
					data_repo.getPreparedStatement(null, GET_COMPONENT_NAMES_QUERY);

				synchronized (getCompNamesSt) {
					try {
						getCompNamesSt.setString(1, getDefHostname());
						rs = getCompNamesSt.executeQuery();

						while (rs.next()) {
							result.add(rs.getString(COMPONENT_NAME_COLUMN));
						}
					} finally {
						data_repo.release(null, rs);
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting component names from DB: ", e);
			}

			return result.toArray(new String[result.size()]);
		}

		private ConfigItem getItem(String compName, String node, String key) {
			ConfigItem result = null;

			try {
				ResultSet rs = null;
				PreparedStatement getItemSt = data_repo.getPreparedStatement(null, GET_ITEM_QUERY);

				synchronized (getItemSt) {
					try {
						getItemSt.setString(1, getDefHostname());
						getItemSt.setString(2, compName);
						getItemSt.setString(3, node);
						getItemSt.setString(4, key);
						rs = getItemSt.executeQuery();

						while (rs.next()) {
							ConfigItem item = createItemFromRS(rs);

							if (item.getFlag() != ConfigItem.FLAGS.INITIAL) {
								result = item;

								break;
							}
						}
					} finally {
						data_repo.release(null, rs);
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting elements from DB: ", e);
			}

			return result;
		}

		private String[] getKeys(String compName, String node) {
			List<String> result = new ArrayList<String>();

			try {
				ResultSet rs = null;
				PreparedStatement getKeysSt = data_repo.getPreparedStatement(null, GET_KEYS_QUERY);

				synchronized (getKeysSt) {
					try {
						getKeysSt.setString(1, getDefHostname());
						getKeysSt.setString(2, compName);
						getKeysSt.setString(3, node);
						rs = getKeysSt.executeQuery();

						while (rs.next()) {
							result.add(rs.getString(KEY_NAME_COLUMN));
						}
					} finally {
						data_repo.release(null, rs);
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting keys from DB: ", e);
			}

			return result.toArray(new String[result.size()]);
		}

		private int getPropertiesCount() {
			int result = 0;

			try {
				ResultSet rs = null;
				PreparedStatement getPropertiesCountSt =
					data_repo.getPreparedStatement(null, GET_PROPERTIES_COUNT_QUERY);

				synchronized (getPropertiesCountSt) {
					try {
						getPropertiesCountSt.setString(1, getDefHostname());
						rs = getPropertiesCountSt.executeQuery();

						while (rs.next()) {
							result = rs.getInt("count");
						}
					} finally {
						data_repo.release(null, rs);
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting elements count from DB: ", e);
			}

			return result;
		}

		//~--- methods ------------------------------------------------------------

		private void removeItem(ConfigItem item) {
			try {
				PreparedStatement deleteItemSt = data_repo.getPreparedStatement(null, DELETE_ITEM_QUERY);

				synchronized (deleteItemSt) {
					deleteItemSt.setString(1, ((item.getClusterNode() != null) ? item.getClusterNode() : ""));
					deleteItemSt.setString(2, item.getCompName());
					deleteItemSt.setString(3, ((item.getNodeName() != null) ? item.getNodeName() : ""));
					deleteItemSt.setString(4, item.getKeyName());
					deleteItemSt.executeUpdate();
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem removing an item from DB: " + item.toElement(), e);
			}
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
