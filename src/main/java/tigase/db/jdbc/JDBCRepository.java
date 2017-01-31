/*
 * JDBCRepository.java
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



package tigase.db.jdbc;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.*;
import tigase.xmpp.BareJID;
import tigase.util.SimpleCache;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Not synchronized implementation! Musn't be used by more than one thread at
 * the same time.
 * <p>
 * Thanks to Daniele for better unique IDs handling. Created: Thu Oct 26
 * 11:48:53 2006
 * </p>
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @author <a href="mailto:piras@tiscali.com">Daniele</a>
 * @version $Rev$
 */
@Repository.Meta( supportedUris = { "jdbc:[^:]+:.*" } )
public class JDBCRepository
				implements AuthRepository, UserRepository {
	/** Field description */
	public static final String CURRENT_DB_SCHEMA_VER = "7.1";

	/** Field description */
	public static final String DEF_MAXIDS_TBL = "tig_max_ids";

	/** Field description */
	public static final String DEF_NODES_TBL = "tig_nodes";

	/** Field description */
	public static final String DEF_PAIRS_TBL = "tig_pairs";

	/** Field description */
	public static final String DEF_ROOT_NODE = "root";

	/** Field description */
	public static final String DEF_USERS_TBL = "tig_users";

	/** Field description */
	public static final String DERBY_GETSCHEMAVER_QUERY =
		"values TigGetDBProperty('schema-version')";

	/** Stored procedure used to check version of the schema
	 * 
	 * neither MS SQL Server JDBC driver supports default
	 * schema prefix in connection string for stored functions
	 */
	public static final String SQLSERVER_GETSCHEMAVER_QUERY =
		"select dbo.TigGetDBProperty('schema-version')";

	/** Field description */
	public static final String JDBC_GETSCHEMAVER_QUERY =
		"select TigGetDBProperty('schema-version')";

	/** Field description */
	public static final String SCHEMA_UPGRADE_LINK =
		"Administration Guide > Tigase Server Schema v7.1 Updates (available locally in docs directory and online http://docs.tigase.org)";
	private static final String ADD_NODE_QUERY          = "{ call TigAddNode(?, ?, ?) }";
	private static final String ADD_USER_PLAIN_PW_QUERY =
		"{ call TigAddUserPlainPw(?, ?) }";
	private static final String COUNT_USERS_FOR_DOMAIN_QUERY =
		"select count(*) from tig_users where user_id like ?";
	private static final String DEF_GET_USERS_QUERY   = "{ call TigAllUsers() }";
	private static final String GET_USER_DB_UID_QUERY = "{ call TigGetUserDBUid(?) }";
	private static final String GET_USERS_COUNT_QUERY = "{ call TigAllUsersCount() }";
	private static final Logger log                   =
		Logger.getLogger(JDBCRepository.class.getName());
	private static final String PGSQL_GET_USERS_QUERY = "select TigAllUsers()";
	private static final String REMOVE_USER_QUERY     = "{ call TigRemoveUser(?) }";
	private static final String UPDATE_PAIRS_QUERY    =
		"{ call TigUpdatePairs(?, ?, ?, ?) }";
	private static final String USER_STR              = "User: ";
	private static final String REMOVE_KEY_DATA_QUERY = "delete from " + DEF_PAIRS_TBL +
																											" where (nid = ?) AND (pkey = ?)";
	private static final String NODES_FOR_NODE_QUERY = "select nid, node from " +
																										 DEF_NODES_TBL +
																										 " where parent_nid = ?";
	private static final String KEYS_FOR_NODE_QUERY = "select pkey from " + DEF_PAIRS_TBL +
																										" where (nid = ?)";
	private static final String INSERT_KEY_VAL_QUERY = "insert into " + DEF_PAIRS_TBL +
																										 " (nid, uid, pkey, pval) " +
																										 " values (?, ?, ?, ?)";
	private static final String DATA_FOR_NODE_QUERY = "select pval from " + DEF_PAIRS_TBL +
																										" where (nid = ?) AND (pkey = ?)";

	//~--- fields ---------------------------------------------------------------

	// ~--- fields ---------------------------------------------------------------
	private AuthRepository auth = null;

	// Cache moved to connection pool
	private Map<String, Object> cache = null;
	private DataRepository data_repo  = null;
	private String get_users_query    = null;
	private boolean autoCreateUser    = false;

	//~--- methods --------------------------------------------------------------

	// ~--- methods --------------------------------------------------------------
	private void addDataList(DataRepository repo, BareJID user_id, final String subnode,
													 final String key, final String[] list)
					throws UserNotFoundException, SQLException, UserNotFoundException {
		long uid = -2;
		long nid = -2;

		try {

			// OK
			uid = getUserUID(repo, user_id, autoCreateUser);

			// OK
			nid = getNodeNID(repo, uid, subnode);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
								"Saving data adding data list, user_id: {0}, subnode: {1}, key: {2}, " +
								"uid: {3}, nid: {4}, list: {5}", new Object[] {
					user_id, subnode, key, uid, nid, Arrays.toString(list)
				});
			}
			if (nid < 0) {
				try {

					// OK
					nid = createNodePath(repo, user_id, subnode);
				} catch (SQLException e) {

					// This may happen in cluster node, when 2 nodes at the same
					// time write data to the same location, like offline messages....
					// Let's try to get the nid again.
					// OK
					nid = getNodeNID(repo, uid, subnode);
				}
			}

			PreparedStatement insert_key_val_st = null;

			if (repo == null) {
				insert_key_val_st = data_repo.getPreparedStatement(user_id, INSERT_KEY_VAL_QUERY);
			} else {
				insert_key_val_st = repo.getPreparedStatement(user_id, INSERT_KEY_VAL_QUERY);
			}
			synchronized (insert_key_val_st) {
				insert_key_val_st.setLong(1, nid);
				insert_key_val_st.setLong(2, uid);
				insert_key_val_st.setString(3, key);
				for (String val : list) {
					insert_key_val_st.setString(4, val);
					insert_key_val_st.executeUpdate();
				}    // end of for (String val: list)
			}
		} catch (SQLException e) {
			log.log(Level.WARNING,
							"Error adding data list, user_id: " + user_id + ", subnode: " + subnode +
							", key: " + key + ", uid: " + uid + ", nid: " + nid + ", list: " +
							Arrays.toString(list), e);

			throw e;
		}

		// cache.put(user_id+"/"+subnode+"/"+key, list);
	}

	@Override
	public void addDataList(BareJID user_id, final String subnode, final String key,
													final String[] list)
					throws UserNotFoundException, TigaseDBException {
		try {
			addDataList(null, user_id, subnode, key, list);
		} catch (SQLException ex) {
			throw new TigaseDBException("Problem adding data list to repository", ex);
		}
	}

	@Override
	public void addUser(BareJID user_id) throws UserExistsException, TigaseDBException {
		try {
			addUserRepo(null, user_id);
		} catch (SQLException e) {
			throw new TigaseDBException("Error adding user to repository: ", e);
		}
	}

	@Override
	public void addUser(BareJID user, final String password)
					throws UserExistsException, TigaseDBException {
		auth.addUser(user, password);
	}

	@Override
	@Deprecated
	public boolean digestAuth(BareJID user, final String digest, final String id,
														final String alg)
					throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.digestAuth(user, digest, id, alg);
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getData(BareJID user_id, final String subnode, final String key,
												final String def)
					throws UserNotFoundException, TigaseDBException {

		try {
			long nid = getNodeNID(null, user_id, subnode);

			if (log.isLoggable(Level.FINEST)) {
				log.log(
						Level.FINEST,
						"Loading data for key: {0}, user: {1}, node: {2}, def: {3}, found nid: {4}",
						new Object[] { key,
													 user_id, subnode, def, nid });
			}

			if (nid > 0) {
				ResultSet rs = null;

				PreparedStatement data_for_node_st = data_repo.getPreparedStatement(user_id,
						DATA_FOR_NODE_QUERY);

				synchronized (data_for_node_st) {
					try {
						String result = def;

						data_for_node_st.setLong(1, nid);
						data_for_node_st.setString(2, key);
						rs = data_for_node_st.executeQuery();
						if (rs.next()) {
							result = rs.getString(1);
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "Found data: {0}", result);
							}
						}

						// cache.put(user_id+"/"+subnode+"/"+key, new String[] {result});
						return result;
					} finally {
						data_repo.release(null, rs);
					}
				}
			} else {
				return def;
			}    // end of if (nid > 0) else
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting user data for: " + user_id + "/" +
																	subnode + "/" + key, e);
		} 
	}

	@Override
	public String getData(BareJID user_id, final String subnode, final String key)
					throws UserNotFoundException, TigaseDBException {
		return getData(user_id, subnode, key, null);
	}

	@Override
	public String getData(BareJID user_id, final String key)
					throws UserNotFoundException, TigaseDBException {
		return getData(user_id, null, key, null);
	}

	@Override
	public String[] getDataList(BareJID user_id, final String subnode, final String key)
					throws UserNotFoundException, TigaseDBException {

		// String[] cache_res = (String[])cache.get(user_id+"/"+subnode+"/"+key);
		// if (cache_res != null) {
		// return cache_res;
		// } // end of if (result != null)

		try {
			long nid = getNodeNID(null, user_id, subnode);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
								"Loading data for key: {0}, user: {1}, node: {2}, found nid: {3}",
								new Object[] { key,
															 user_id, subnode, nid });
			}

			if (nid > 0) {
				ResultSet rs = null;
				
				PreparedStatement data_for_node_st = data_repo.getPreparedStatement(user_id,
						DATA_FOR_NODE_QUERY);

				synchronized (data_for_node_st) {
					try {
						List<String> results = new ArrayList<String>();

						data_for_node_st.setLong(1, nid);
						data_for_node_st.setString(2, key);
						rs = data_for_node_st.executeQuery();
						while (rs.next()) {
							results.add(rs.getString(1));
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "Found data: {0}", rs.getString(1));
							}
						}

						String[] result = (results.size() == 0)
								? null
								: results.toArray(new String[results.size()]);

						// cache.put(user_id+"/"+subnode+"/"+key, result);
						return result;
					} finally {
						data_repo.release(null, rs);
					}
				}
			} else {
				return null;
			}    // end of if (nid > 0) else			
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting data list for: " + user_id + "/"
					+ subnode + "/" + key, e);
		}
	}

	@Override
	public String[] getKeys(BareJID user_id, final String subnode)
					throws UserNotFoundException, TigaseDBException {

		try {
			long nid = getNodeNID(null, user_id, subnode);

			if (nid > 0) {
				ResultSet rs = null;
				List<String> results               = new ArrayList<String>();
				PreparedStatement keys_for_node_st = data_repo.getPreparedStatement(user_id,
																							 KEYS_FOR_NODE_QUERY);

				synchronized (keys_for_node_st) {
					try {
					keys_for_node_st.setLong(1, nid);
					rs = keys_for_node_st.executeQuery();
					while (rs.next()) {
						results.add(rs.getString(1));
					}

					return (results.size() == 0)
								 ? null
								 : results.toArray(new String[results.size()]);
					} finally {
						data_repo.release(null, rs);
					}
				}
			} else {
				return null;
			}    // end of if (nid > 0) else
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting subnodes list.", e);
		}
	}

	@Override
	public String[] getKeys(BareJID user_id)
					throws UserNotFoundException, TigaseDBException {
		return getKeys(user_id, null);
	}

	@Override
	public String getResourceUri() {
		return data_repo.getResourceUri();
	}

	protected DataRepository getRepository() {
		return data_repo;
	}

	@Override
	public String[] getSubnodes(BareJID user_id, final String subnode)
					throws UserNotFoundException, TigaseDBException {
		try {
			long nid = getNodeNID(null, user_id, subnode);
			if (nid > 0) {
				ResultSet rs = null;
				PreparedStatement nodes_for_node_st = data_repo.getPreparedStatement(user_id,
						NODES_FOR_NODE_QUERY);

				synchronized (nodes_for_node_st) {
					try {
						List<String> results = new ArrayList<String>();

						nodes_for_node_st.setLong(1, nid);
						rs = nodes_for_node_st.executeQuery();
						while (rs.next()) {
							results.add(rs.getString(2));
						}

						return (results.size() == 0)
								? null
								: results.toArray(new String[results.size()]);
					} finally {
						data_repo.release(null, rs);
					}
				}
			} else {
				return null;
			}    // end of if (nid > 0) else

		} catch (SQLException e) {
			throw new TigaseDBException("Error getting subnodes list.", e);
		}
	}

	@Override
	public String[] getSubnodes(BareJID user_id)
					throws UserNotFoundException, TigaseDBException {
		return getSubnodes(user_id, null);
	}

	@Override
	public long getUserUID(BareJID user_id) throws TigaseDBException {
		Long cache_res = (Long) cache.get(user_id.toString());

		if (cache_res != null) {
			return cache_res.longValue();
		}    // end of if (result != null)

		long result = -1;

		try {
			result = getUserUID(null, user_id);
		} catch (SQLException e) {
			throw new TigaseDBException("Error retrieving user UID from repository: ", e);
		}
		cache.put(user_id.toString(), Long.valueOf(result));

		return result;
	}

	public long getUserUID(DataRepository repo, BareJID user_id) throws SQLException {
		long result  = -1;

		ResultSet rs = null;
		PreparedStatement uid_sp = null;

		if (repo == null) {
			uid_sp = data_repo.getPreparedStatement(user_id, GET_USER_DB_UID_QUERY);
		} else {
			uid_sp = repo.getPreparedStatement(user_id, GET_USER_DB_UID_QUERY);
		}
		synchronized (uid_sp) {
			try {
				uid_sp.setString(1, user_id.toString());
				rs = uid_sp.executeQuery();
				if (rs.next()) {
					result = rs.getLong(1);
				} else {
					result = -1;
				}
			} finally {
				data_repo.release(null, rs);
			}
		}

		return result;
	}

	@Override
	public List<BareJID> getUsers() throws TigaseDBException {
		ResultSet rs = null;
		List<BareJID> users = null;

		try {
			PreparedStatement all_users_sp = data_repo.getPreparedStatement(null,
					get_users_query);

			synchronized (all_users_sp) {
				try {
					// Load all user ids from database
					rs = all_users_sp.executeQuery();
					users = new ArrayList<BareJID>(1000);
					while (rs.next()) {
						users.add(BareJID.bareJIDInstanceNS(rs.getString(1)));
					}    // end of while (rs.next())
				} finally {
					data_repo.release(null, rs);
					rs = null;
				}

			}
		} catch (SQLException e) {
			throw new TigaseDBException("Problem loading user list from repository", e);
		}
		return users;
	}

	@Override
	public long getUsersCount() {
		try {
			ResultSet rs = null;
			long users = -1;
			PreparedStatement users_count_sp = data_repo.getPreparedStatement(null,
					GET_USERS_COUNT_QUERY);

			synchronized (users_count_sp) {
				try {
					// Load all user count from database
					rs = users_count_sp.executeQuery();
					if (rs.next()) {
						users = rs.getLong(1);
					}    // end of while (rs.next())
				} finally {
					data_repo.release(null, rs);
					rs = null;

				}

				return users;
			}
		} catch (SQLException e) {
			return -1;

			// throw new
			// TigaseDBException("Problem loading user list from repository", e);
		}
	}

	@Override
	public long getUsersCount(String domain) {
		try {
			ResultSet rs = null;
			long users = -1;
			PreparedStatement users_domain_count_st = data_repo.getPreparedStatement(null,
					COUNT_USERS_FOR_DOMAIN_QUERY);

			synchronized (users_domain_count_st) {
				try {
					// Load all user count from database
					users_domain_count_st.setString(1, "%@" + domain);
					rs = users_domain_count_st.executeQuery();
					if (rs.next()) {
						users = rs.getLong(1);
					}    // end of while (rs.next())
				} finally {
					data_repo.release(null, rs);
					rs = null;
				}
			}

			return users;
		} catch (SQLException e) {
			return -1;

			// throw new
			// TigaseDBException("Problem loading user list from repository", e);
		}
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(final String connection_str, Map<String, String> params)
					throws DBInitException {
		try {
			data_repo  = RepositoryFactory.getDataRepository(null, connection_str, params);
			checkDBSchema();
			if (connection_str.contains("autoCreateUser=true")) {
				autoCreateUser = true;
			}    // end of if (db_conn.contains())
			if (connection_str.contains("cacheRepo=off")) {
				log.fine("Disabling cache.");
				cache = Collections.synchronizedMap(new RepoCache(0, -1000));
			} else {
				cache = Collections.synchronizedMap(new RepoCache(10000, 60 * 1000));
			}
			data_repo.initPreparedStatement(GET_USER_DB_UID_QUERY, GET_USER_DB_UID_QUERY);
			data_repo.initPreparedStatement(GET_USERS_COUNT_QUERY, GET_USERS_COUNT_QUERY);
			if (connection_str.startsWith("jdbc:postgresql")) {
				get_users_query = PGSQL_GET_USERS_QUERY;
			} else {
				get_users_query = DEF_GET_USERS_QUERY;
			}
			data_repo.initPreparedStatement(get_users_query, get_users_query);
			data_repo.initPreparedStatement(ADD_USER_PLAIN_PW_QUERY, ADD_USER_PLAIN_PW_QUERY);
			data_repo.initPreparedStatement(REMOVE_USER_QUERY, REMOVE_USER_QUERY);
			data_repo.initPreparedStatement(ADD_NODE_QUERY, ADD_NODE_QUERY);
			data_repo.initPreparedStatement(COUNT_USERS_FOR_DOMAIN_QUERY,
																			COUNT_USERS_FOR_DOMAIN_QUERY);
			data_repo.initPreparedStatement(DATA_FOR_NODE_QUERY, DATA_FOR_NODE_QUERY);
			data_repo.initPreparedStatement(KEYS_FOR_NODE_QUERY, KEYS_FOR_NODE_QUERY);
			data_repo.initPreparedStatement(NODES_FOR_NODE_QUERY, NODES_FOR_NODE_QUERY);
			data_repo.initPreparedStatement(INSERT_KEY_VAL_QUERY, INSERT_KEY_VAL_QUERY);
			data_repo.initPreparedStatement(REMOVE_KEY_DATA_QUERY, REMOVE_KEY_DATA_QUERY);
			data_repo.initPreparedStatement(UPDATE_PAIRS_QUERY, UPDATE_PAIRS_QUERY);
			auth = new AuthRepositoryImpl(this);

			// initRepo();
			log.log(Level.INFO, "Initialized database connection: {0}", connection_str);
		} catch (Exception e) {
			data_repo = null;

			throw new DBInitException("Problem initializing jdbc connection: " +
																connection_str, e);
		}
	}

	@Override
	public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {
		auth.logout(user);
	}

	@Override
	public boolean otherAuth(final Map<String, Object> props)
					throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.otherAuth(props);
	}

	// Implementation of tigase.db.AuthRepository

	@Override
	@Deprecated
	public boolean plainAuth(BareJID user, final String password)
					throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.plainAuth(user, password);
	}

	@Override
	public void queryAuth(Map<String, Object> authProps) {
		auth.queryAuth(authProps);
	}

	@Override
	public void removeData(BareJID user_id, final String subnode, final String key)
					throws UserNotFoundException, TigaseDBException {
		removeData(null, user_id, subnode, key);
	}

	private void removeData(DataRepository repo, BareJID user_id, final String subnode,
													final String key)
					throws UserNotFoundException, TigaseDBException {

		// cache.remove(user_id+"/"+subnode+"/"+key);
		try {
			long nid = getNodeNID(repo, user_id, subnode);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST,
								"Removing data, user_id: {0}, subnode: {1}, key: {2}, nid: {3}",
								new Object[] { user_id,
															 subnode, key, nid });
			}

			PreparedStatement remove_key_data_st = null;

			if (repo == null) {
				remove_key_data_st = data_repo.getPreparedStatement(user_id,
								REMOVE_KEY_DATA_QUERY);
			} else {
				remove_key_data_st = repo.getPreparedStatement(user_id, REMOVE_KEY_DATA_QUERY);
			}
			if (nid > 0) {
				synchronized (remove_key_data_st) {
					remove_key_data_st.setLong(1, nid);
					remove_key_data_st.setString(2, key);
					remove_key_data_st.executeUpdate();
				}
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting subnodes list.", e);
		}
	}

	@Override
	public void removeData(BareJID user_id, final String key)
					throws UserNotFoundException, TigaseDBException {
		removeData(user_id, null, key);
	}

	@Override
	public void removeSubnode(BareJID user_id, final String subnode)
					throws UserNotFoundException, TigaseDBException {
		if (subnode == null) {
			return;
		}    // end of if (subnode == null)
		try {
			String[] subnodes = getSubnodes( user_id, subnode );
			if (subnodes != null && subnodes.length > 0 ) {
				for ( String innerSubNode : subnodes) {
					removeSubnode( user_id, subnode + "/" + innerSubNode);
				}
			}
			long nid = getNodeNID(null, user_id, subnode);

			if (nid > 0) {
				deleteSubnode(null, nid);
				cache.remove(user_id + "/" + subnode);
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting subnodes list.", e);
		}
	}

	/**
	 * <code>removeUser</code> method is thread safe. It uses local variable for
	 * storing <code>Statement</code>.
	 *
	 * @param user_id
	 *          a <code>String</code> value the user Jabber ID.
	 *
	 * @throws TigaseDBException
	 * @exception UserNotFoundException
	 *              if an error occurs
	 */
	@Override
	public void removeUser(BareJID user_id)
					throws UserNotFoundException, TigaseDBException {
		Statement stmt = null;
		String query   = null;

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Removing user: {0}", user_id);
		}
		try {
			stmt = data_repo.createStatement(user_id);

			// Get user account uid
			long uid = getUserUID(null, user_id, autoCreateUser);

			// Remove all user enrties from pairs table
			query = "delete from " + DEF_PAIRS_TBL + " where uid = " + uid;
			stmt.executeUpdate(query);

			// Remove all user entries from nodes table
			query = "delete from " + DEF_NODES_TBL + " where uid = " + uid;
			stmt.executeUpdate(query);

			PreparedStatement user_del_sp = data_repo.getPreparedStatement(user_id,
																				REMOVE_USER_QUERY);

			// Remove user account from users table
			synchronized (user_del_sp) {
				user_del_sp.setString(1, user_id.toString());
				user_del_sp.executeUpdate();
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Error removing user from repository: " + query, e);
		} finally {
			data_repo.release(stmt, null);
			stmt = null;
			cache.remove(user_id.toString());

			// cache.clear();
		}
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setData(BareJID user_id, final String subnode, final String key,
											final String value)
					throws UserNotFoundException, TigaseDBException {
		long uid            = -2;
		long nid            = -2;
		DataRepository repo = data_repo.takeRepoHandle(user_id);

		synchronized (repo) {
			try {
				uid = getUserUID(repo, user_id, autoCreateUser);
				nid = getNodeNID(repo, uid, subnode);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
									"Saving data setting data, user_id: {0}, subnode: {1}, key: {2}, " +
									"uid: {3}, nid: {4}, value: {5}", new Object[] {
						user_id, subnode, key, uid, nid, value
					});
				}
				if (nid < 0) {
					try {

						// OK
						nid = createNodePath(repo, user_id, subnode);
					} catch (SQLException e) {

						// This may happen in cluster node, when 2 nodes at the same
						// time write data to the same location, like offline messages....
						// Let's try to get the nid again.
						// OK
						nid = getNodeNID(repo, uid, subnode);
					}
				}

				PreparedStatement update_pairs_sp = repo.getPreparedStatement(user_id,
																							UPDATE_PAIRS_QUERY);

				update_pairs_sp.setLong(1, nid);
				update_pairs_sp.setLong(2, uid);
				update_pairs_sp.setString(3, key);
				switch ( data_repo.getDatabaseType() ) {
					case derby:
						Clob c = update_pairs_sp.getConnection().createClob();
						c.setString( 1, value);
						update_pairs_sp.setClob( 4, c);
						break;
					default:
						update_pairs_sp.setString( 4, value );
				}
				update_pairs_sp.executeUpdate();
			} catch ( SQLException e ) {
				log.log(Level.WARNING,
								"Error setting data , user_id: " + user_id + ", subnode: " + subnode +
								", key: " + key + ", uid: " + uid + ", nid: " + nid + ", value: " +
								value, e);
			}
		}
	}

	@Override
	public void setData(BareJID user_id, final String key, final String value)
					throws UserNotFoundException, TigaseDBException {
		setData(user_id, null, key, value);
	}

	@Override
	public void setDataList(BareJID user_id, final String subnode, final String key,
													final String[] list)
					throws UserNotFoundException, TigaseDBException {

		// Transactions may not yet work properly but at least let's make sure
		// both calls below are executed exclusively on the same DB connection
		DataRepository repo = data_repo.takeRepoHandle(user_id);

		synchronized (repo) {
			try {
				removeData(repo, user_id, subnode, key);
				try {
					addDataList(repo, user_id, subnode, key, list);
				} catch (SQLException ex) {
					throw new TigaseDBException("Problem adding data to DB, user_id: " + user_id +
																			", subnode: " + subnode + ", key: " + key +
																			", list: " + Arrays.toString(list), ex);
				}
			} finally {
				data_repo.releaseRepoHandle(repo);
			}
		}

	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void updatePassword(BareJID user, final String password)
					throws TigaseDBException {
		auth.updatePassword(user, password);
	}

	@Override
	public boolean userExists(BareJID user) {
		try {
			getUserUID(null, user, false);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private long addNode(DataRepository repo, long uid, long parent_nid, String node_name)
					throws SQLException {
		ResultSet rs                  = null;
		PreparedStatement node_add_sp = null;

		if (repo == null) {
			node_add_sp = data_repo.getPreparedStatement(null, ADD_NODE_QUERY);
		} else {
			node_add_sp = repo.getPreparedStatement(null, ADD_NODE_QUERY);
		}
		synchronized (node_add_sp) {
			try {
				if (parent_nid < 0) {
					node_add_sp.setNull(1, Types.BIGINT);
				} else {
					node_add_sp.setLong(1, parent_nid);
				}    // end of else
				node_add_sp.setLong(2, uid);
				node_add_sp.setString(3, node_name);

				switch ( data_repo.getDatabaseType() ) {
//					case sqlserver:
//						node_add_sp.executeUpdate();
//						rs = node_add_sp.getGeneratedKeys();
//						break;
					default:
						rs = node_add_sp.executeQuery();
						break;
				}

				if (rs.next()) {
					return rs.getLong(1);
				} else {
					log.warning("Missing NID after adding new node...");

					return -1;

					// throw new TigaseDBException("Propeblem adding new node. "
					// + "The SP should return nid or fail");
				}    // end of if (isnext) else
			} finally {
				data_repo.release(null, rs);
			}
		}

		// return new_nid;
	}

	/**
	 * <code>addUserRepo</code> method is thread safe. It uses local variable for
	 * storing <code>Statement</code>.
	 *
	 * @param user_id
	 *          a <code>String</code> value of the user ID.
	 * @return a <code>long</code> value of <code>uid</code> database user ID.
	 * @exception SQLException if an error occurs
	 * @exception UserExistsException if the user already exists in repository
	 */
	private long addUserRepo( DataRepository repo, BareJID user_id )
			throws SQLException, UserExistsException {
		ResultSet rs                  = null;
		long uid                      = -1;
		PreparedStatement user_add_sp = null;

		if (repo == null) {
			user_add_sp = data_repo.getPreparedStatement(user_id, ADD_USER_PLAIN_PW_QUERY);
		} else {
			user_add_sp = repo.getPreparedStatement(user_id, ADD_USER_PLAIN_PW_QUERY);
		}
		synchronized (user_add_sp) {
			try {
				user_add_sp.setString(1, user_id.toString());
				user_add_sp.setNull(2, Types.VARCHAR);

				log.log(Level.FINEST, "Adding non existing user to user-repository: " + user_id.toString() );

				switch ( data_repo.getDatabaseType() ) {
					default:
						rs = user_add_sp.executeQuery();
						break;
				}

				if (rs != null && rs.next()) {
					uid = rs.getLong(1);

					// addNode(uid, -1, root_node);
				} else {
					log.warning("Missing UID after adding new user...");
				}    // end of if (isnext) else
			} catch ( SQLException ex ) {
				if ( isExceptionKeyViolation( ex ) ){
					throw new UserExistsException(user_id, "User already exist in the database", ex );
				} else {
					throw ex;
				}

			} finally {
				data_repo.release(null, rs);
			}
		}
		cache.put(user_id.toString(), Long.valueOf(uid));

		return uid;
	}

	protected boolean isExceptionKeyViolation( SQLException ex ) {
		String sqlState = ex.getSQLState();
		boolean keyViolation = false;
		switch ( data_repo.getDatabaseType() ) {

			case derby:
				keyViolation = (sqlState.equals( "X0Y78" ));
				break;
			case postgresql:
				keyViolation = (sqlState.equals( "23505" ) || sqlState.equals( "23000"));
				break;
			default:
				keyViolation = sqlState.equals( "23000");
				break;
		}
		return keyViolation;
	}

	private String buildNodeQuery(long uid, String node_path) {
		String query = "select nid as nid1 from " + DEF_NODES_TBL + " where (uid = " + uid +
									 ")" + " AND (parent_nid is null)" + " AND (node = '" + DEF_ROOT_NODE +
									 "')";

		if (node_path == null) {
			return query;
		} else {
			StringTokenizer strtok = new StringTokenizer(node_path, "/", false);
			int cnt                = 1;
			String subquery        = query;

			while (strtok.hasMoreTokens()) {
				String token = strtok.nextToken();

				++cnt;
				subquery = "select nid as nid" + cnt + ", node as node" + cnt + " from " +
									 DEF_NODES_TBL + ", (" + subquery + ") nodes" + (cnt - 1) +
									 " where (parent_nid = nid" + (cnt - 1) + ")" + " AND (node = '" +
									 token + "')";
			}    // end of while (strtok.hasMoreTokens())

			return subquery;
		}      // end of else
	}

	// Implementation of tigase.db.UserRepository
	private void checkDBSchema() throws SQLException {
		String schema_version = "1.0";
		String query = null;

		switch ( data_repo.getDatabaseType() ) {
			case derby:
				query = DERBY_GETSCHEMAVER_QUERY;
				break;
			case jtds:
			case sqlserver:
				query = SQLSERVER_GETSCHEMAVER_QUERY;
				break;
			default:
				query = JDBC_GETSCHEMAVER_QUERY;
				break;
		}
		Statement stmt        = data_repo.createStatement(null);
		ResultSet rs          = stmt.executeQuery(query);

		try {
			if (rs.next()) {
				schema_version = rs.getString(1);
				if (false == CURRENT_DB_SCHEMA_VER.equals(schema_version)) {
					System.err.println("\n\nPlease upgrade database schema now.");
					System.err.println("Current scheme version is: " + schema_version +
														 ", expected: " + CURRENT_DB_SCHEMA_VER);
					System.err.println("Check the schema upgrade guide at the address:");
					System.err.println(SCHEMA_UPGRADE_LINK);
					System.err.println("----");
					System.err.println("If you have upgraded your schema and you are still");
					System.err.println("experiencing this problem please contact support at");
					System.err.println("e-mail address: support@tigase.org");

					// e.printStackTrace();
					System.exit(100);
				}
			}
		} finally {
			data_repo.release(stmt, rs);
		}    // end of try-catch
	}

	private long createNodePath(DataRepository repo, BareJID user_id, String node_path)
					throws SQLException, UserNotFoundException {
		if (node_path == null) {

			// Or should I throw NullPointerException?
			// OK
			return getNodeNID(repo, user_id, null);
		}    // end of if (node_path == null)

		// OK
		long uid = getUserUID(repo, user_id, autoCreateUser);

		// OK
		long nid                 = getNodeNID(repo, uid, null);
		StringTokenizer strtok   = new StringTokenizer(node_path, "/", false);
		StringBuilder built_path = new StringBuilder();

		while (strtok.hasMoreTokens()) {
			String token = strtok.nextToken();

			built_path.append("/").append(token);

			// OK
			long cur_nid = getNodeNID(repo, uid, built_path.toString());

			if (cur_nid > 0) {
				nid = cur_nid;
			} else {

				// OK
				nid = addNode(repo, uid, nid, token);
			}    // end of if (cur_nid > 0) else
		}      // end of while (strtok.hasMoreTokens())

		return nid;
	}

	private void deleteSubnode(DataRepository repo, long nid) throws SQLException {
		Statement stmt = null;
		ResultSet rs   = null;
		String query   = null;

		try {
			if (repo == null) {
				stmt = data_repo.createStatement(null);
			} else {
				stmt = repo.createStatement(null);
			}
			query = "delete from " + DEF_PAIRS_TBL + " where nid = " + nid;
			stmt.executeUpdate(query);
			query = "delete from " + DEF_NODES_TBL + " where nid = " + nid;
			stmt.executeUpdate(query);
		} finally {
			data_repo.release(stmt, rs);
		}
	}

	//~--- get methods ----------------------------------------------------------

	private long getNodeNID(DataRepository repo, long uid, String node_path)
					throws SQLException, UserNotFoundException {
		String query = buildNodeQuery(uid, node_path);

		if (log.isLoggable(Level.FINEST)) {
			log.finest(query);
		}

		Statement stmt = null;
		ResultSet rs   = null;
		long nid       = -1;

		try {
			if (repo == null) {
				stmt = data_repo.createStatement(null);
			} else {
				stmt = repo.createStatement(null);
			}
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				nid = rs.getLong(1);
			} else {
				nid = -1;
			}    // end of if (isnext) else
			if (nid <= 0) {
				if (node_path == null) {
					log.info(
							"Missing root node, database upgrade or bug in the code? Adding missing " +
							"root node now.");

					// OK
					nid = addNode(repo, uid, -1, "root");
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Missing nid for node path: {0} and uid: {1}",
										new Object[] { node_path,
																	 uid });
					}
				}
			}

			return nid;
		} finally {
			data_repo.release(stmt, rs);
			stmt = null;
			rs   = null;
		}
	}

	private long getNodeNID(DataRepository repo, BareJID user_id, String node_path)
					throws SQLException, UserNotFoundException {
		Long cache_res = (Long) cache.get(user_id + "/" + node_path);

		if (cache_res != null) {
			return cache_res.longValue();
		}    // end of if (result != null)

		// OK
		long uid = getUserUID(repo, user_id, autoCreateUser);

		// OK
		long result = getNodeNID(repo, uid, node_path);

		if (result > 0) {
			cache.put(user_id + "/" + node_path, Long.valueOf(result));
		}    // end of if (result > 0)

		return result;
	}

	private long getUserUID(DataRepository repo, BareJID user_id, boolean autoCreate)
					throws SQLException, UserNotFoundException {

		// OK
		long result = getUserUID(repo, user_id);

		if (result <= 0) {
			if (autoCreate) {

				// OK
				try {
					result = addUserRepo(repo, user_id);
				} catch (UserExistsException ex) {
					// there was unique key violation which indicates that the user
					// already exists in the database therefore we can retrieve it's UID
					result = getUserUID( repo, user_id );
				}
			} else {
				throw new UserNotFoundException("User does not exist: " + user_id);
			}    // end of if (autoCreate) else
		}      // end of if (isnext) else

		return result;
	}

	//~--- inner classes --------------------------------------------------------

	// ~--- inner classes --------------------------------------------------------
	private class RepoCache
					extends SimpleCache<String, Object> {
		/**
		 * Constructs ...
		 *
		 *
		 * @param maxsize
		 * @param cache_time
		 */
		public RepoCache(int maxsize, long cache_time) {
			super(maxsize, cache_time);
		}

		//~--- methods ------------------------------------------------------------

		@Override
		public Object remove(Object key) {
			if (cache_off) {
				return null;
			}

			Object val          = super.remove(key);
			String strk         = key.toString();
			Iterator<String> ks = keySet().iterator();

			while (ks.hasNext()) {
				String k = ks.next().toString();

				if (k.startsWith(strk)) {
					ks.remove();
				}    // end of if (k.startsWith(strk))
			}      // end of while (ks.hasNext())

			return val;
		}
	}	
	
	@Override
	public String getPassword(BareJID user) throws UserNotFoundException, TigaseDBException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isUserDisabled(BareJID user) throws UserNotFoundException, TigaseDBException {
		return auth.isUserDisabled(user);
	}
	
	@Override
	public void setUserDisabled(BareJID user, Boolean value) throws UserNotFoundException, TigaseDBException {
		auth.setUserDisabled(user, value);
	}	
}    // JDBCRepository

