/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

package tigase.conf;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.TigaseDBException;
import tigase.util.DataTypes;
import tigase.util.JDBCAbstract;
import static tigase.conf.Configurable.*;

/**
 * Created: Dec 15, 2009 10:44:00 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ConfigSQLRepository extends ConfigurationCache {

	public static final String CONFIG_REPO_URI_PROP_KEY = "tigase-config-repo-uri";
	public static final String CONFIG_REPO_URI_INIT_KEY = "--tigase-config-repo-uri";

	/**
	 * Private logger for class instancess.
   */
  private static final Logger log =
			Logger.getLogger(ConfigSQLRepository.class.getName());

	private JDBCAccess dbAccess = new JDBCAccess();

	@Override
	public void init(Map<String, Object> params) throws ConfigurationException {
		String config_db_uri = System.getProperty(CONFIG_REPO_URI_PROP_KEY);
		if (config_db_uri == null) {
			config_db_uri = (String)params.get(CONFIG_REPO_URI_INIT_KEY);
		}
		if (config_db_uri == null) {
			config_db_uri = (String)params.get(GEN_USER_DB_URI);
		}
		if (config_db_uri == null) {
			log.severe("Missing configuration database connection string.");
			log.severe("Tigase needs a database connection string to load configuration.");
			log.severe("You can provide it in a few ways and the Tigase server checks");
			log.severe("following parameters in the order below:");
			log.severe("1. System property: -Dtigase-config-repo-uri=db-connection-string");
			log.severe("2. init.properties file or command line parameter: --tigase-config-repo-uri=db-connection-string");
			log.severe("3. init.properties file or command line parameter: --user-db-uri=db-connection-string");
			log.severe("Please correct the error and restart the server.");
			System.exit(1);
		}
		try {
			dbAccess.initRepository(config_db_uri, null);
		} catch (SQLException ex) {
			log.log(Level.SEVERE, "Problem connecting to configuration database: ", ex);
			log.severe("Please check whether the database connection string is correct: " +
					config_db_uri);
			System.exit(1);
		}
	}

	@Override
	public Set<ConfigItem> getItemsForComponent(String compName) {
		return dbAccess.getCompItems(compName);
	}

	@Override
	public ConfigItem getItem(String compName, String node, String key) {
		return dbAccess.getItem(compName, node, key);
	}

	@Override
	public void addItem(String compName, ConfigItem item) {
		dbAccess.addItem(item);
	}

	@Override
	public void removeItem(String compName, ConfigItem item) {
		dbAccess.removeItem(item);
	}

	@Override
	public String[] getCompNames() {
		return dbAccess.getComponentNames();
	}

	@Override
	public String[] getKeys(String compName, String node) {
		return dbAccess.getKeys(compName, node);
	}

	@Override
	public int size() {
		return dbAccess.getPropertiesCount();
	}

	@Override
	public Collection<ConfigItem> allItems() throws TigaseDBException {
		return dbAccess.getAllItems();
	}

	private class JDBCAccess extends JDBCAbstract {

		public static final String TABLE_NAME = "tigase_configuration";
		private static final String CLUSTER_NODE_COLUMN = "cluster_node";
		private static final String COMPONENT_NAME_COLUMN = "component_name";
		private static final String NODE_NAME_COLUMN = "key_node";
		private static final String KEY_NAME_COLUMN = "key_name";
		private static final String VALUE_COLUMN = "value";
		private static final String FLAG_COLUMN = "flag";
		private static final String VALUE_TYPE_COLUMN = "value_type";
		private static final String LAST_UPDATE_COLUMN = "last_update";
		private static final String CREATE_TABLE_QUERY =
				"create table " + TABLE_NAME + " ("
				+ "  " + COMPONENT_NAME_COLUMN + " varchar(127) NOT NULL,"
				+ "  " + KEY_NAME_COLUMN + " varchar(127) NOT NULL,"
				+ "  " + VALUE_COLUMN + " varchar(8191) NOT NULL,"
				+ "  " + CLUSTER_NODE_COLUMN + " varchar(255) NOT NULL DEFAULT '',"
				+ "  " + NODE_NAME_COLUMN + " varchar(127) NOT NULL DEFAULT '',"
				+ "  " + FLAG_COLUMN + " varchar(32) NOT NULL DEFAULT 'DEFAULT',"
				+ "  " + VALUE_TYPE_COLUMN + " varchar(8) NOT NULL DEFAULT 'S',"
				+ "  " + LAST_UPDATE_COLUMN + " timestamp,"
				+ "  primary key("
				+ CLUSTER_NODE_COLUMN + ", "
				+ COMPONENT_NAME_COLUMN + ", "
				+ NODE_NAME_COLUMN + ", "
				+ KEY_NAME_COLUMN + ", "
				+ FLAG_COLUMN + "))";
		private static final String CLUSTER_NODE_WHERE_PART =
				" (" + CLUSTER_NODE_COLUMN + " = '' "
				+ " OR " + CLUSTER_NODE_COLUMN + " = ?) ";
		private static final String ITEM_WHERE_PART =
				" where " + CLUSTER_NODE_WHERE_PART
				+ " AND (" + COMPONENT_NAME_COLUMN + " = ?) "
				+ " AND (" + NODE_NAME_COLUMN + " = ?) "
				+ " AND (" + KEY_NAME_COLUMN + " = ?) ";
				//+ " AND (" + FLAG_COLUMN + " = ?)";
		private static final String CHECK_TABLE_QUERY =
				"select count(*) from " + TABLE_NAME;
		private static final String GET_ITEM_QUERY =
				"select * from " + TABLE_NAME + ITEM_WHERE_PART;
		private static final String ADD_ITEM_QUERY =
				"insert into " + TABLE_NAME + " ("
				+ CLUSTER_NODE_COLUMN + ", "
				+ COMPONENT_NAME_COLUMN + ", "
				+ NODE_NAME_COLUMN + ", "
				+ KEY_NAME_COLUMN + ", "
				+ VALUE_COLUMN + ", "
				+ VALUE_TYPE_COLUMN + ", "
				+ FLAG_COLUMN + ") "
				+ " values (?, ?, ?, ?, ?, ?, ?)";
		private static final String UPDATE_ITEM_QUERY =
				"update " + TABLE_NAME + " set " + VALUE_COLUMN + " = ? "
				+ " where (" + CLUSTER_NODE_COLUMN + " = ?) "
				+ " AND (" + COMPONENT_NAME_COLUMN + " = ?) "
				+ " AND (" + NODE_NAME_COLUMN + " = ?) "
				+ " AND (" + KEY_NAME_COLUMN + " = ?)";
		private static final String DELETE_ITEM_QUERY =
				"delete from " + TABLE_NAME + ITEM_WHERE_PART;
		private static final String GET_ALL_ITEMS_QUERY =
				"select * from " + TABLE_NAME + " where " + CLUSTER_NODE_WHERE_PART;
		private static final String GET_COMPONENT_ITEMS_QUERY =
				"select * from " + TABLE_NAME + " where " + CLUSTER_NODE_WHERE_PART
				+ " AND (" + COMPONENT_NAME_COLUMN + " = ?)";
		private static final String GET_UPDATED_ITEMS_QUERY =
				"select * from " + TABLE_NAME + " where " + CLUSTER_NODE_WHERE_PART
				+ " AND (" + FLAG_COLUMN + " <> 'INITIAL')"
				+ " AND (" + LAST_UPDATE_COLUMN + " > ?)";
		private static final String GET_COMPONENT_NAMES_QUERY =
				"select distinct(" + COMPONENT_NAME_COLUMN + ") from " + TABLE_NAME
				+ " where " + CLUSTER_NODE_COLUMN;
		private static final String GET_PROPERTIES_COUNT_QUERY =
				"select count(*) as count from " + TABLE_NAME
				+ " where " + CLUSTER_NODE_COLUMN;
		private static final String GET_KEYS_QUERY =
				"select " + KEY_NAME_COLUMN + " from " + TABLE_NAME
				+ " where " + CLUSTER_NODE_WHERE_PART
				+ " AND (" + COMPONENT_NAME_COLUMN + " = ?)"
				+ " AND (" + NODE_NAME_COLUMN + " = ?)";

		private PreparedStatement createTableSt = null;
		private PreparedStatement checkTableSt = null;
		private PreparedStatement getItemSt = null;
		private PreparedStatement getAllItemsSt = null;
		private PreparedStatement getCompItemsSt = null;
		private PreparedStatement getUpdatedItemsSt = null;
		private PreparedStatement addItemSt = null;
		private PreparedStatement updateItemSt = null;
		private PreparedStatement deleteItemSt = null;
		private PreparedStatement getCompNamesSt = null;
		private PreparedStatement getPropertiesCountSt = null;
		private PreparedStatement getKeysSt = null;

		@Override
		public void initRepository(String conn_str, Map<String, String> params)
				throws SQLException {
			setResourceUri(conn_str);
			checkConnection();
			// Check if DB is correctly setup and contains all required tables.
			checkDB();
		}

		@Override
		protected void initPreparedStatements() throws SQLException {
			super.initPreparedStatements();
			checkTableSt = prepareStatement(CHECK_TABLE_QUERY);
			getItemSt = prepareStatement(GET_ITEM_QUERY);
			getAllItemsSt = prepareStatement(GET_ALL_ITEMS_QUERY);
			getCompItemsSt = prepareStatement(GET_COMPONENT_ITEMS_QUERY);
			addItemSt = prepareStatement(ADD_ITEM_QUERY);
			updateItemSt = prepareStatement(UPDATE_ITEM_QUERY);
			deleteItemSt = prepareStatement(DELETE_ITEM_QUERY);
			getUpdatedItemsSt = prepareStatement(GET_UPDATED_ITEMS_QUERY);
			getCompNamesSt = prepareStatement(GET_COMPONENT_NAMES_QUERY);
			getPropertiesCountSt = prepareStatement(GET_PROPERTIES_COUNT_QUERY);
			getKeysSt = prepareStatement(GET_KEYS_QUERY);
		}
		
		private void checkDB() throws SQLException {
			ResultSet rs = null;
			try {
				rs = checkTableSt.executeQuery();
				if (rs.next()) {
					long count = rs.getLong(1);
					log.info("DB for external component OK, items: " + count);
				}
			} catch (Exception e) {
				initializeDB();
			} finally {
				release(null, rs);
				rs = null;
			}
		}

		private void initializeDB()	throws SQLException {
			createTableSt = prepareStatement(CREATE_TABLE_QUERY);
			log.info("DB for external component is not OK, creating missing tables...");
			createTableSt.executeUpdate();
			log.info("DB for external component created OK");
		}

		private Collection<ConfigItem> getAllItems() {
			List<ConfigItem> result = new ArrayList<ConfigItem>();
			ResultSet rs = null;
			try {
				checkConnection();
				synchronized (getAllItemsSt) {
					getAllItemsSt.setString(1, getDefHostname());
					rs = getAllItemsSt.executeQuery();
					while (rs.next()) {
						ConfigItem item = createItemFromRS(rs);
						if (item.getFlag() != ConfigItem.FLAGS.INITIAL) {
							result.add(item);
						}
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting elements from DB: ", e);
			} finally {
				release(null, rs);
			}
			return result;
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
			result.set(clusterNode, compName, nodeName, keyName, value_str,
					value_type.charAt(0), flag_str);
			return result;
		}

		private Set<ConfigItem> getCompItems(String compName) {
			Set<ConfigItem> result = new LinkedHashSet<ConfigItem>();
			ResultSet rs = null;
			try {
				checkConnection();
				synchronized (getCompItemsSt) {
					getCompItemsSt.setString(1, getDefHostname());
					getCompItemsSt.setString(2, compName);
					rs = getCompItemsSt.executeQuery();
					while (rs.next()) {
						ConfigItem item = createItemFromRS(rs);
						if (item.getFlag() != ConfigItem.FLAGS.INITIAL) {
							result.add(item);
						}
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting elements from DB: ", e);
			} finally {
				release(null, rs);
			}
			return result;
		}

		private ConfigItem getItem(String compName, String node, String key) {
			ConfigItem result = null;
			ResultSet rs = null;
			try {
				checkConnection();
				synchronized (getItemSt) {
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
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting elements from DB: ", e);
			} finally {
				release(null, rs);
			}
			return result;
		}

		private void addItem(ConfigItem item) {
			try {
				checkConnection();
				synchronized (addItemSt) {
					addItemSt.setString(1,
							(item.getClusterNode() != null ? item.getClusterNode() : ""));
					addItemSt.setString(2, item.getCompName());
					addItemSt.setString(3,
							(item.getNodeName() != null ? item.getNodeName() : ""));
					addItemSt.setString(4, item.getKeyName());
					addItemSt.setString(5, item.getConfigValToString());
					addItemSt.setString(6, "" + DataTypes.getTypeId(item.getConfigVal()));
					addItemSt.setString(7, item.getFlag().name());
					addItemSt.executeUpdate();
				}
			} catch (SQLException e) {
				// Maybe the configuration item is already there, let's try to update it then
				try {
					checkConnection();
					synchronized (updateItemSt) {
						updateItemSt.setString(1, item.getConfigValToString());
						updateItemSt.setString(2,
								(item.getClusterNode() != null ? item.getClusterNode() : ""));
						updateItemSt.setString(3, item.getCompName());
						updateItemSt.setString(4,
								(item.getNodeName() != null ? item.getNodeName() : ""));
						updateItemSt.setString(5, item.getKeyName());
						updateItemSt.executeUpdate();
					}
				} catch (SQLException ex) {
					try {
						// Maybe the configuration item is already there, let's try to update it then
						log.log(Level.WARNING,
								"Problem adding/updating an item to DB: " + item.toElement() +
								"\n", ex);
						log.log(Level.WARNING,
								"SQLWarning: " + updateItemSt.getWarnings().getMessage() +
								", state: " + updateItemSt.getWarnings().getSQLState());
					} catch (SQLException ex1) {
						Logger.getLogger(ConfigSQLRepository.class.getName()).
								log(Level.SEVERE, null, ex1);
					}
				}
			} catch (Exception e) {
				log.warning(e + "Exception while adding config item: " + item.toString());
			}
		}

		private String[] getComponentNames() {
			List<String> result = new ArrayList<String>();
			ResultSet rs = null;
			try {
				checkConnection();
				synchronized (getCompNamesSt) {
					getCompNamesSt.setString(1, getDefHostname());
					rs = getCompNamesSt.executeQuery();
					while (rs.next()) {
						result.add(rs.getString(COMPONENT_NAME_COLUMN));
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting component names from DB: ", e);
			} finally {
				release(null, rs);
			}
			return result.toArray(new String[result.size()]);
		}

		private int getPropertiesCount() {
			int result = 0;
			ResultSet rs = null;
			try {
				checkConnection();
				synchronized (getPropertiesCountSt) {
					getPropertiesCountSt.setString(1, getDefHostname());
					rs = getPropertiesCountSt.executeQuery();
					while (rs.next()) {
						result = rs.getInt("count");
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting elements count from DB: ", e);
			} finally {
				release(null, rs);
			}
			return result;
		}

		private String[] getKeys(String compName, String node) {
			List<String> result = new ArrayList<String>();
			ResultSet rs = null;
			try {
				checkConnection();
				synchronized (getKeysSt) {
					getKeysSt.setString(1, getDefHostname());
					getKeysSt.setString(2, compName);
					getKeysSt.setString(3, node);
					rs = getKeysSt.executeQuery();
					while (rs.next()) {
						result.add(rs.getString(KEY_NAME_COLUMN));
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting keys from DB: ", e);
			} finally {
				release(null, rs);
			}
			return result.toArray(new String[result.size()]);
		}

		private void removeItem(ConfigItem item) {
			try {
				checkConnection();
				synchronized (deleteItemSt) {
					deleteItemSt.setString(1,
							(item.getClusterNode() != null ? item.getClusterNode() : ""));
					deleteItemSt.setString(2, item.getCompName());
					deleteItemSt.setString(3,
							(item.getNodeName() != null ? item.getNodeName() : ""));
					deleteItemSt.setString(4, item.getKeyName());
					deleteItemSt.executeUpdate();
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem removing an item from DB: "
						+ item.toElement(), e);
			}
		}

	}

}
