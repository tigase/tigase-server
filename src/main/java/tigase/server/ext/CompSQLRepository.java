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

package tigase.server.ext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.ComponentRepository;
import tigase.util.JDBCAbstract;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import static tigase.conf.Configurable.*;

/**
 * Created: Nov 7, 2009 11:26:10 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CompSQLRepository extends JDBCAbstract
		implements ComponentRepository<CompRepoItem> {

	/**
	 * Private logger for class instancess.
   */
  private static final Logger log = Logger.getLogger(CompSQLRepository.class.getName());
	public static final String REPO_URI_PROP_KEY = "repo-uri";
	public static final String TABLE_NAME = "external_component";
	private static final String DOMAIN_COLUMN = "domain";
	private static final String PASSWORD_COLUMN = "password";
	private static final String CONNECTION_TYPE_COLUMN = "connection_type";
	private static final String PORT_COLUMN = "port";
	private static final String REMOTE_DOMAIN_COLUMN = "remote_domain";
	private static final String PROTOCOL_COLUMN = "protocol";
	private static final String OTHER_DATA_COLUMN = "other_data";
	private static final String CREATE_TABLE_QUERY =
			"create table " + TABLE_NAME + " (" +
			"  " + DOMAIN_COLUMN + " varchar(512) NOT NULL," +
			"  " + PASSWORD_COLUMN + " varchar(255) NOT NULL," +
			"  " + CONNECTION_TYPE_COLUMN + " varchar(127)," +
			"  " + PORT_COLUMN + " int," +
			"  " + REMOTE_DOMAIN_COLUMN + " varchar(1023)," +
			"  " + PROTOCOL_COLUMN + " varchar(127)," +
			"  " + OTHER_DATA_COLUMN + " varchar(32672)," +
			"  primary key(" + DOMAIN_COLUMN + "))";
	private static final String CHECK_TABLE_QUERY =
			"select count(*) from " + TABLE_NAME;
	private static final String GET_ITEM_QUERY =
			"select * from " + TABLE_NAME + " where domain = ?";
	private static final String ADD_ITEM_QUERY =
			"insert into " + TABLE_NAME +
			" (" + DOMAIN_COLUMN + ", " + PASSWORD_COLUMN + ", " +
			CONNECTION_TYPE_COLUMN + ", " + PORT_COLUMN + ", " +
			REMOTE_DOMAIN_COLUMN + ", " + PROTOCOL_COLUMN + ", " +
			OTHER_DATA_COLUMN +	") " +
			" values (?, ?, ?, ?, ?, ?, ?)";
	private static final String DELETE_ITEM_QUERY =
			"delete from " + TABLE_NAME + " where (domain = ?)";
	private static final String GET_ALL_ITEMS_QUERY =
			"select * from " + TABLE_NAME;

	private String tableName = TABLE_NAME;

	private PreparedStatement createTableSt = null;
	private PreparedStatement checkTableSt = null;
	private PreparedStatement getItemSt = null;
	private PreparedStatement getAllItemsSt = null;
	private PreparedStatement addItemSt = null;
	private PreparedStatement deleteItemSt = null;

	private CompConfigRepository configRepo = new CompConfigRepository();

	@Override
	public void initRepository(String conn_str,
			Map<String, String> params)
			throws SQLException {
		// Don't need to do anything special here, initRepo() is called authomatically
		// if needed on each DB query, but then if there a problem with a connection
		// to database we would know that late
		setResourceUri(conn_str);
		checkConnection();
		// Check if DB is correctly setup and contains all required tables.
		checkDB();
	}

	@Override
	public void getDefaults(Map<String, Object> defs,
			Map<String, Object> params) {
		configRepo.getDefaults(defs, params);
		String repo_uri = DERBY_REPO_URL_PROP_VAL;
		if (params.get(GEN_USER_DB_URI) != null) {
			repo_uri = (String)params.get(GEN_USER_DB_URI);
		}
		defs.put(REPO_URI_PROP_KEY, repo_uri);
	}

	@Override
	public void setProperties(Map<String, Object> properties) {
		configRepo.setProperties(properties);
		String repo_uri = (String)properties.get(REPO_URI_PROP_KEY);
		try {
			initRepository(repo_uri, null);
		} catch (SQLException ex) {
			// This might be related to missing tables, let's try to create them
			try {
				initializeDB();
			} catch (Exception e) {
				log.log(Level.WARNING, "Propblem creating database.", e);
			}
			log.log(Level.WARNING, "Problem initializing database.", ex);
		}
	}

	@Override
	public void removeItem(String key) {
		configRepo.removeItem(key);
		try {
			checkConnection();
			synchronized (deleteItemSt) {
				deleteItemSt.setString(1, key);
				deleteItemSt.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Can't remove item: " + key, e);
		}
	}

//	private static final String ADD_ITEM_QUERY =
//			"insert into " + TABLE_NAME +
//			" (" + DOMAIN_COLUMN + ", " + PASSWORD_COLUMN + ", " +
//			CONNECTION_TYPE_COLUMN + ", " + PORT_COLUMN + ", " +
//			REMOTE_DOMAIN_COLUMN + ", " + PROTOCOL_COLUMN + ", " +
//			OTHER_DATA_COLUMN +	") " +
//			" values (?, ?, ?, ?, ?, ?, ?)";

	@Override
	public void addItem(CompRepoItem item) {
		try {
			checkConnection();
			synchronized (addItemSt) {
				if (item.getDomain() != null && !item.getDomain().isEmpty()) {
					addItemSt.setString(1, item.getDomain());
				} else {
					throw new NullPointerException("Null or empty domain name is not allowed");
				}
				if (item.getAuthPasswd() != null) {
					addItemSt.setString(2, item.getAuthPasswd());
				} else {
					throw new NullPointerException("Null password is not allowed");
				}
				if (item.getConnectionType() != null) {
					addItemSt.setString(3, item.getConnectionType().name());
				} else {
					addItemSt.setNull(3, Types.VARCHAR);
				}
				if (item.getPort() > 0) {
					addItemSt.setInt(4, item.getPort());
				} else {
					addItemSt.setNull(4, Types.INTEGER);
				}
				if (item.getRemoteHost() != null && !item.getRemoteHost().isEmpty()) {
					addItemSt.setString(5, item.getRemoteHost());
				} else {
					addItemSt.setNull(5, Types.VARCHAR);
				}
				if (item.getXMLNS() != null) {
					addItemSt.setString(6, item.getXMLNS());
				} else {
					addItemSt.setNull(6, Types.VARCHAR);
				}
				String other_data = item.toElement().toString();
				if (other_data != null) {
					addItemSt.setString(7, other_data);
				} else {
					addItemSt.setNull(7, Types.VARCHAR);
				}
				addItemSt.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem adding a new item to DB: " +
					item.toElement(), e);
		}
	}

	@Override
	public CompRepoItem getItem(String key) {
		CompRepoItem result = configRepo.getItem(key);
		if (result == null) {
			ResultSet rs = null;
			try {
				checkConnection();
				synchronized (getItemSt) {
					getItemSt.setString(1, key);
					rs = getItemSt.executeQuery();
					if (rs.next()) {
						result = createItemFromRS(rs);
					}
				}
			} catch (SQLException e) {
				log.log(Level.WARNING, "Problem getting element from DB for domain: " +
						key, e);
			} finally {
				release(null, rs);
			}
		}
		return result;
	}

	private CompRepoItem createItemFromRS(ResultSet rs) throws SQLException {
		CompRepoItem result = getItemInstance();
		// First init from other parameters, some fixed fields may
		// overwrite fields initialized from other parametrs
		String other = rs.getString(OTHER_DATA_COLUMN);
		if (other != null && !other.isEmpty()) {
			Element elem_item = parseElement(other);
			if (elem_item != null) {
				result.initFromElement(elem_item);
			}
		}
		String domain = rs.getString(DOMAIN_COLUMN);
		if (domain != null && !domain.isEmpty()) {
			result.setDomain(domain);
		}
		String password = rs.getString(PASSWORD_COLUMN);
		if (password != null && !password.isEmpty()) {
			result.setPassword(password);
		}
		int port = rs.getInt(PORT_COLUMN);
		if (port > 0) {
			result.setPort(port);
		}
		String remote_domain = rs.getString(REMOTE_DOMAIN_COLUMN);
		if (remote_domain != null && !remote_domain.isEmpty()) {
			result.setRemoteDomain(remote_domain);
		}
		String protocol = rs.getString(PROTOCOL_COLUMN);
		if (protocol != null && !protocol.isEmpty()) {
			result.setProtocol(protocol);
		}
		String connection_type = rs.getString(CONNECTION_TYPE_COLUMN);
		if (connection_type != null && !connection_type.isEmpty()) {
			result.setConnectionType(connection_type);
		}
		return result;
	}

	private Element parseElement(String data) {
		DomBuilderHandler domHandler = new DomBuilderHandler();
		SimpleParser parser = SingletonFactory.getParserInstance();
		parser.parse(domHandler, data.toCharArray(), 0, data.length());
		Queue<Element> elems = domHandler.getParsedElements();
		if (elems != null && elems.size() > 0) {
			return elems.poll();
		}
		return null;
	}

	@Override
	public boolean contains(String key) {
		boolean result = configRepo.contains(key);
		return result;
	}

	@Override
	public void reload() {
		// Do nothing, no caching, everything is read on demand from DB
	}

	@Override
	public void store() {
		// Do nothing everything is written on demand to DB
	}

	@Override
	public int size() {
		int result = configRepo.size();
		return result;
	}

	@Override
	public Collection<CompRepoItem> allItems() {
		List<CompRepoItem> result = new ArrayList<CompRepoItem>();
		result.addAll(configRepo.allItems());
		ResultSet rs = null;
		try {
			checkConnection();
			synchronized (getAllItemsSt) {
				rs = getAllItemsSt.executeQuery();
				while(rs.next()) {
					result.add(createItemFromRS(rs));
				}
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem getting elements from DB: ", e);
		} finally {
			release(null, rs);
		}
		return result;
	}

	@Override
	public CompRepoItem getItemInstance() {
		return configRepo.getItemInstance();
	}

	@Override
	public Iterator<CompRepoItem> iterator() {
		return allItems().iterator();
	}

	@Override
	protected void initPreparedStatements() throws SQLException {
		super.initPreparedStatements();
		checkTableSt = prepareStatement(CHECK_TABLE_QUERY);
		getItemSt = prepareStatement(GET_ITEM_QUERY);
		getAllItemsSt = prepareStatement(GET_ALL_ITEMS_QUERY);
		addItemSt = prepareStatement(ADD_ITEM_QUERY);
		deleteItemSt = prepareStatement(DELETE_ITEM_QUERY);
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

	private void initializeDB() throws SQLException {
		createTableSt = prepareStatement(CREATE_TABLE_QUERY);
		log.info("DB for external component is not OK, creating missing tables...");
		createTableSt.executeUpdate();
		log.info("DB for external component created OK");
	}

}
