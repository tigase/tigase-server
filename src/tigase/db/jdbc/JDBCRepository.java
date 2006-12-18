/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.db.jdbc;


import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.AuthorizationException;
import tigase.db.DBInitException;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserAuthRepository;
import tigase.db.UserAuthRepositoryImpl;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.util.SimpleCache;

/**
 * Not synchronized implementation!
 * Mustn't be used by more than one thread a time.
 *
 *
 * Created: Thu Oct 26 11:48:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JDBCRepository implements UserAuthRepository, UserRepository {

  private static final Logger log =
    Logger.getLogger("tigase.db.jdbc.JDBCRepository");

	public static final String DEF_USERS_TBL = "tig_users";
	public static final String DEF_NODES_TBL = "tig_nodes";
	public static final String DEF_PAIRS_TBL = "tig_pairs";
	public static final String DEF_MAXIDS_TBL = "tig_max_ids";
	public static final String DEF_ROOT_NODE = "root";

	private static final String USER_STR = "User: ";

	private String users_tbl = DEF_USERS_TBL;
	private String nodes_tbl = DEF_NODES_TBL;
	private String pairs_tbl = DEF_PAIRS_TBL;
	private String maxids_tbl = DEF_MAXIDS_TBL;
	private String root_node = DEF_ROOT_NODE;

	private UserAuthRepository auth = null;
	private String db_conn = null;
	private Connection conn = null;
	private PreparedStatement uid_st = null;
	private PreparedStatement node_add_st = null;
	private PreparedStatement data_for_node_st = null;
	private PreparedStatement keys_for_node_st = null;
	private PreparedStatement nodes_for_node_st = null;
	private PreparedStatement insert_key_val_st = null;
	private PreparedStatement remove_key_data_st = null;
	private PreparedStatement conn_valid_st = null;

	private Map<String, Object> cache = null;

	private long lastConnectionValidated = 0;
	private long connectionValidateInterval = 1000*60;

	private boolean autoCreateUser = false;

	private long max_uid = 0;
	private long max_nid = 0;

	private void release(Statement stmt, ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException sqlEx) { }
		}
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException sqlEx) { }
		}
	}

	private boolean checkConnection() throws SQLException {
		try {
			long tmp = System.currentTimeMillis();
			if ((tmp - lastConnectionValidated) >= connectionValidateInterval) {
				conn_valid_st.executeQuery();
				lastConnectionValidated = tmp;
			} // end of if ()
		} catch (Exception e) {
			initRepo();
		} // end of try-catch
		return true;
	}

	private long getUserUID(String user_id)
		throws SQLException, UserNotFoundException {
		Long cache_res = (Long)cache.get(user_id);
		if (cache_res != null) {
			return cache_res.longValue();
		} // end of if (result != null)
		ResultSet rs = null;
		long result = -1;
		try {
			uid_st.setString(1, user_id);
			rs = uid_st.executeQuery();
			if (rs.next()) {
				result = rs.getLong(1);
			} else {
				if (autoCreateUser) {
					result = addUserRepo(user_id);
				} else {
					throw new UserNotFoundException("User does not exist: " + user_id);
				} // end of if (autoCreateUser) else
			} // end of if (isnext) else
		} finally {
			release(null, rs);
		}
		cache.put(user_id, new Long(result));
		return result;
	}

	private void incrementMaxUID() throws SQLException {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			// Update max_uid to current value
			stmt.executeUpdate("update " + maxids_tbl + " set max_uid=(max_uid+1);");
		} finally {
			release(stmt, null);
			stmt = null;
		}
	}

	private void incrementMaxNID() throws SQLException {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			// Update max_uid to current value
			stmt.executeUpdate("update " + maxids_tbl + " set max_nid=(max_nid+1);");
		} finally {
			release(stmt, null);
			stmt = null;
		}
	}

	private String buildNodeQuery(long uid, String node_path) {
		String query =
			"select nid as nid1 from " + nodes_tbl
			+ " where (uid = " + uid + ")"
			+ " AND (parent_nid is null)"
			+ " AND (node = '" + root_node + "')";
		if (node_path == null) {
			return query;
		} else {
			StringTokenizer strtok = new StringTokenizer(node_path, "/", false);
			int cnt = 1;
			String subquery = query;
			while (strtok.hasMoreTokens()) {
				String token = strtok.nextToken();
				++cnt;
				subquery = "select nid as nid" + cnt + ", node as node" + cnt
					+ " from " + nodes_tbl + ", (" + subquery + ") nodes" + (cnt-1)
					+ " where (parent_nid = nid" + (cnt-1) + ")"
					+ " AND (node = '" + token + "')";
			} // end of while (strtok.hasMoreTokens())
			return subquery;
		} // end of else
	}

	private long getNodeNID(long uid, String node_path)
		throws SQLException, UserNotFoundException {
		String query = buildNodeQuery(uid, node_path);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				return rs.getLong(1);
			} else {
				return -1;
			} // end of if (isnext) else
		} finally {
			release(stmt, rs);
			stmt = null; rs = null;
		}
	}

	private long getNodeNID(String user_id, String node_path)
		throws SQLException, UserNotFoundException {
		Long cache_res = (Long)cache.get(user_id+"/"+node_path);
		if (cache_res != null) {
			return cache_res.longValue();
		} // end of if (result != null)
		long uid = getUserUID(user_id);
		long result = getNodeNID(uid, node_path);
		if (result > 0) {
			cache.put(user_id+"/"+node_path, new Long(result));
		} // end of if (result > 0)
		return result;
	}

	private long addNode(long uid, long parent_nid, String node_name)
		throws SQLException {
		long new_nid = max_nid++;
		node_add_st.setLong(1, new_nid);
		if (parent_nid < 0) {
			node_add_st.setNull(2, Types.BIGINT);
		} else {
			node_add_st.setLong(2, parent_nid);
		} // end of else
		node_add_st.setLong(3, uid);
		node_add_st.setString(4, node_name);
		node_add_st.executeUpdate();
		incrementMaxNID();
		return new_nid;
	}

	private long createNodePath(String user_id, String node_path)
		throws SQLException, UserNotFoundException {
		if (node_path == null) {
			// Or should I throw NullPointerException?
			return getNodeNID(user_id, null);
		} // end of if (node_path == null)
		long uid = getUserUID(user_id);
		long nid = getNodeNID(uid, null);
		StringTokenizer strtok = new StringTokenizer(node_path, "/", false);
		String built_path = "";
		while (strtok.hasMoreTokens()) {
			String token = strtok.nextToken();
			built_path = built_path + "/" + token;
			long cur_nid = getNodeNID(uid, built_path);
			if (cur_nid > 0) {
				nid = cur_nid;
			} else {
				nid = addNode(uid, nid, token);
			} // end of if (cur_nid > 0) else
		} // end of while (strtok.hasMoreTokens())
		return nid;
	}

	private void initPreparedStatements() throws SQLException {
		String query = "select uid from " + users_tbl
			+ " where user_id = ?;";
		uid_st = conn.prepareStatement(query);

		query = "insert into " + nodes_tbl
			+ " (nid, parent_nid, uid, node)"
			+ " values (?, ?, ?, ?);";
		node_add_st = conn.prepareStatement(query);

		query = "select pval from " + pairs_tbl
			+ " where (nid = ?) AND (pkey = ?);";
		data_for_node_st = conn.prepareStatement(query);

		query = "select pkey from " + pairs_tbl
			+ " where (nid = ?);";
		keys_for_node_st = conn.prepareStatement(query);

		query = "select nid, node from " + nodes_tbl
			+ " where parent_nid = ?;";
		nodes_for_node_st = conn.prepareStatement(query);

		query =  "insert into " + pairs_tbl	+ " (nid, uid, pkey, pval) "
			+ " values (?, ?, ?, ?)";
		insert_key_val_st = conn.prepareStatement(query);

		query =  "delete from " + pairs_tbl
			+ " where (nid = ?) AND (pkey = ?)";
		remove_key_data_st = conn.prepareStatement(query);

		query = "select count(*) from " + users_tbl;
		conn_valid_st = conn.prepareStatement(query);
	}

	// Implementation of tigase.db.UserRepository

	private void initRepo() throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = DriverManager.getConnection(db_conn);
			conn.setAutoCommit(true);
			initPreparedStatements();
			auth = new UserAuthRepositoryImpl(this);
			stmt = conn.createStatement();
			// load maximum ids
			String query = "SELECT max_uid, max_nid FROM " + maxids_tbl;
			rs = stmt.executeQuery(query);
			rs.next();
			max_uid = rs.getLong("max_uid");
			max_nid = rs.getLong("max_nid");
			cache = new SimpleCache<String, Object>(10000);
		} finally {
			release(stmt, rs);
			stmt = null; rs = null;
		}
	}


	/**
	 * Describe <code>initRepository</code> method here.
	 *
	 * @param connection_str a <code>String</code> value
	 */
	public void initRepository(final String connection_str)
		throws DBInitException {
		db_conn = connection_str;
		if (db_conn.contains("autoCreateUser=true")) {
			autoCreateUser=true;
		} // end of if (db_conn.contains())
		try {
			initRepo();
			log.info("Initialized database connection: " + connection_str);
		} catch (SQLException e) {
			conn = null;
			throw	new DBInitException("Problem initializing jdbc connection: "
				+ db_conn, e);
		}
	}

	/**
	 * Describe <code>getUsers</code> method here.
	 *
	 * @return a <code>List</code> value
	 */
	public List<String> getUsers() throws TigaseDBException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			// Load all user ids from database
			rs = stmt.executeQuery("SELECT user_id FROM " + users_tbl);
			List<String> users = new ArrayList<String>();
			while (rs.next()) {
				users.add(rs.getString(1));
			} // end of while (rs.next())
		return users;
		} catch (SQLException e) {
			throw new TigaseDBException("Problem loading user list from repository", e);
		} finally {
			release(stmt, rs);
			stmt = null; rs = null;
		}
	}

	private long addUserRepo(final String user_id) throws SQLException {
		checkConnection();
		Statement stmt = null;
		String query = null;
		long uid = max_uid++;
		try {
			stmt = conn.createStatement();
			// Add user into database.
			query = "insert into " + users_tbl + " (uid, user_id) values ("
				+ uid + ", '" + user_id + "');";
			stmt.executeUpdate(query);
			incrementMaxUID();
			addNode(uid, -1, root_node);
		} finally {
			release(stmt, null);
			stmt = null;
		}
		cache.put(user_id, new Long(uid));
		return uid;
	}

	/**
	 * Describe <code>addUser</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @exception UserExistsException if an error occurs
	 */
	public void addUser(final String user_id)
		throws UserExistsException, TigaseDBException {
		try {
			addUserRepo(user_id);
		} catch (SQLException e) {
			throw new UserExistsException("Error adding user to repository: ", e);
		}
	}

	/**
	 * Describe <code>removeUser</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public void removeUser(final String user_id)
		throws UserNotFoundException, TigaseDBException {
		Statement stmt = null;
		ResultSet rs = null;
		String query = null;
		try {
			checkConnection();
			stmt = conn.createStatement();
			// Get user account uid
			long uid = getUserUID(user_id);
			// Remove user account from users table
			query = "delete from " + users_tbl + " where uid = " + uid;
			stmt.executeUpdate(query);
			// Remove all user entries from nodes table
			query = "delete from " + nodes_tbl + " where uid = " + uid;
			stmt.executeUpdate(query);
			// Remove all user enrties from pairs table
			query = "delete from " + pairs_tbl + " where uid = " + uid;
			stmt.executeUpdate(query);
		} catch (SQLException e) {
			throw new TigaseDBException("Error removing user from repository: "
				+ query, e);
		} finally {
			release(stmt, rs);
			stmt = null;
			cache.remove(user_id);
			//cache.clear();
		}
	}

	/**
	 * Describe <code>getDataList</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param subnode a <code>String</code> value
	 * @param key a <code>String</code> value
	 * @return a <code>String[]</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public String[] getDataList(final String user_id, final String subnode,
		final String key) throws UserNotFoundException, TigaseDBException {
// 		String[] cache_res = (String[])cache.get(user_id+"/"+subnode+"/"+key);
// 		if (cache_res != null) {
// 			return cache_res;
// 		} // end of if (result != null)
		ResultSet rs = null;
		try {
			checkConnection();
			long nid = getNodeNID(user_id, subnode);
			if (nid > 0) {
				List<String> results = new ArrayList<String>();
				data_for_node_st.setLong(1, nid);
				data_for_node_st.setString(2, key);
				rs = data_for_node_st.executeQuery();
				while (rs.next()) {
					results.add(rs.getString(1));
				}
				String[] result = results.size() == 0 ? null :
					results.toArray(new String[results.size()]);
				//				cache.put(user_id+"/"+subnode+"/"+key, result);
				return result;
			} else {
				return null;
			} // end of if (nid > 0) else
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting data list.", e);
		} finally {
			release(null, rs);
		}
	}

	/**
	 * Describe <code>getSubnodes</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param subnode a <code>String</code> value
	 * @return a <code>String[]</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public String[] getSubnodes(final String user_id,	final String subnode)
		throws UserNotFoundException, TigaseDBException {
		ResultSet rs = null;
		try {
			checkConnection();
			long nid = getNodeNID(user_id, subnode);
			if (nid > 0) {
				List<String> results = new ArrayList<String>();
				nodes_for_node_st.setLong(1, nid);
				rs = nodes_for_node_st.executeQuery();
				while (rs.next()) {
					results.add(rs.getString(2));
				}
				return results.size() == 0 ? null :
					results.toArray(new String[results.size()]);
			} else {
				return null;
			} // end of if (nid > 0) else
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting subnodes list.", e);
		} finally {
			release(null, rs);
		}
	}

	/**
	 * Describe <code>getSubnodes</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @return a <code>String[]</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public String[] getSubnodes(final String user_id)
		throws UserNotFoundException, TigaseDBException {
		return getSubnodes(user_id, null);
	}

	private void deleteSubnode(long nid) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		String query = null;
		try {
			stmt = conn.createStatement();
			query = "delete from " + nodes_tbl + " where nid = " + nid;
			stmt.executeUpdate(query);
			query = "delete from " + pairs_tbl + " where nid = " + nid;
			stmt.executeUpdate(query);
			query = "select nid from " + nodes_tbl + " where parent_nid = " + nid;
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				long subnode_nid = rs.getLong(1);
				deleteSubnode(subnode_nid);
			} // end of while (rs.next())
		} finally {
			release(stmt, rs);
		}
	}

	/**
	 * Describe <code>removeSubnode</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param subnode a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public void removeSubnode(final String user_id,	final String subnode)
		throws UserNotFoundException, TigaseDBException {
		if (subnode == null) {
			return;
		} // end of if (subnode == null)
		try {
			checkConnection();
			long nid = getNodeNID(user_id, subnode);
			if (nid > 0) {
				deleteSubnode(nid);
				cache.remove(user_id+"/"+subnode);
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting subnodes list.", e);
		}
	}

	/**
	 * Describe <code>setDataList</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param subnode a <code>String</code> value
	 * @param key a <code>String</code> value
	 * @param list a <code>String[]</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public void setDataList(final String user_id, final String subnode,
		final String key, final String[] list)
		throws UserNotFoundException, TigaseDBException {
		removeData(user_id, subnode, key);
		addDataList(user_id, subnode, key, list);
	}

	/**
	 * Describe <code>addDataList</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param subnode a <code>String</code> value
	 * @param key a <code>String</code> value
	 * @param list a <code>String[]</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public void addDataList(final String user_id, final String subnode,
		final String key, final String[] list)
		throws UserNotFoundException, TigaseDBException {
		try {
			checkConnection();
			long uid = getUserUID(user_id);
			long nid = getNodeNID(uid, subnode);
			if (nid < 0) {
				nid = createNodePath(user_id, subnode);
			}
			insert_key_val_st.setLong(1, nid);
			insert_key_val_st.setLong(2, uid);
			insert_key_val_st.setString(3, key);
			for (String val: list) {
				insert_key_val_st.setString(4, val);
				insert_key_val_st.executeUpdate();
			} // end of for (String val: list)
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting subnodes list.", e);
		}
		//		cache.put(user_id+"/"+subnode+"/"+key, list);
	}

	/**
	 * Describe <code>getKeys</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param subnode a <code>String</code> value
	 * @return a <code>String[]</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public String[] getKeys(final String user_id, final String subnode)
		throws UserNotFoundException, TigaseDBException {
		ResultSet rs = null;
		try {
			checkConnection();
			long nid = getNodeNID(user_id, subnode);
			if (nid > 0) {
				List<String> results = new ArrayList<String>();
				keys_for_node_st.setLong(1, nid);
				rs = keys_for_node_st.executeQuery();
				while (rs.next()) {
					results.add(rs.getString(1));
				}
				return results.size() == 0 ? null :
					results.toArray(new String[results.size()]);
			} else {
				return null;
			} // end of if (nid > 0) else
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting subnodes list.", e);
		} finally {
			release(null, rs);
		}
	}

	/**
	 * Describe <code>getKeys</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @return a <code>String[]</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public String[] getKeys(final String user_id)
		throws UserNotFoundException, TigaseDBException {
		return getKeys(user_id, null);
	}

	/**
	 * Describe <code>getData</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param subnode a <code>String</code> value
	 * @param key a <code>String</code> value
	 * @param def a <code>String</code> value
	 * @return a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public String getData(final String user_id, final String subnode,
		final String key, final String def)
		throws UserNotFoundException, TigaseDBException {
// 		String[] cache_res = (String[])cache.get(user_id+"/"+subnode+"/"+key);
// 		if (cache_res != null) {
// 			return cache_res[0];
// 		} // end of if (result != null)
		ResultSet rs = null;
		try {
			checkConnection();
			long nid = getNodeNID(user_id, subnode);
			if (nid > 0) {
				String result = def;
				data_for_node_st.setLong(1, nid);
				data_for_node_st.setString(2, key);
				rs = data_for_node_st.executeQuery();
				if (rs.next()) {
					result = rs.getString(1);
				}
				//				cache.put(user_id+"/"+subnode+"/"+key, new String[] {result});
				return result;
			} else {
				return def;
			} // end of if (nid > 0) else
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting data list.", e);
		} finally {
			release(null, rs);
		}
	}

	/**
	 * Describe <code>getData</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param subnode a <code>String</code> value
	 * @param key a <code>String</code> value
	 * @return a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public String getData(final String user_id, final String subnode,
		final String key) throws UserNotFoundException, TigaseDBException {
		return getData(user_id, subnode, key, null);
	}

	/**
	 * Describe <code>getData</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param key a <code>String</code> value
	 * @return a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public String getData(final String user_id,	final String key)
		throws UserNotFoundException, TigaseDBException {
		return getData(user_id, null, key, null);
	}

	/**
	 * Describe <code>setData</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param subnode a <code>String</code> value
	 * @param key a <code>String</code> value
	 * @param value a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public void setData(final String user_id, final String subnode,
		final String key, final String value)
		throws UserNotFoundException, TigaseDBException {
		setDataList(user_id, subnode, key, new String[] {value});
	}

	/**
	 * Describe <code>setData</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param key a <code>String</code> value
	 * @param value a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public void setData(final String user_id, final String key,
		final String value)
		throws UserNotFoundException, TigaseDBException {
		setData(user_id, null, key, value);
	}

	/**
	 * Describe <code>removeData</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param subnode a <code>String</code> value
	 * @param key a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public void removeData(final String user_id, final String subnode,
		final String key)
		throws UserNotFoundException, TigaseDBException {
		//		cache.remove(user_id+"/"+subnode+"/"+key);
		try {
			checkConnection();
			long nid = getNodeNID(user_id, subnode);
			if (nid > 0) {
				remove_key_data_st.setLong(1, nid);
				remove_key_data_st.setString(2, key);
				remove_key_data_st.executeUpdate();
			}
		} catch (SQLException e) {
			throw new TigaseDBException("Error getting subnodes list.", e);
		}
	}

	/**
	 * Describe <code>removeData</code> method here.
	 *
	 * @param user_id a <code>String</code> value
	 * @param key a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 */
	public void removeData(final String user_id, final String key)
		throws UserNotFoundException, TigaseDBException {
		removeData(user_id, null, key);
	}

	// Implementation of tigase.db.UserAuthRepository

	/**
	 * Describe <code>plainAuth</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public boolean plainAuth(final String user, final String password)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.plainAuth(user, password);
	}

	/**
	 * Describe <code>digestAuth</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param digest a <code>String</code> value
	 * @param id a <code>String</code> value
	 * @param alg a <code>String</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public boolean digestAuth(final String user, final String digest,
		final String id, final String alg)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.digestAuth(user, digest, id, alg);
	}

	/**
	 * Describe <code>otherAuth</code> method here.
	 *
	 * @param props a <code>Map</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 * @exception AuthorizationException if an error occurs
	 */
	public boolean otherAuth(final Map<String, Object> props)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.otherAuth(props);
	}

	public void updatePassword(final String user, final String password)
		throws TigaseDBException {
		auth.updatePassword(user, password);
	}

	/**
	 * Describe <code>addUser</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @exception UserExistsException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public void addUser(final String user, final String password)
		throws UserExistsException, TigaseDBException {
		auth.addUser(user, password);
	}

	public void queryAuth(Map<String, Object> authProps) {
		auth.queryAuth(authProps);
	}

} // JDBCRepository
