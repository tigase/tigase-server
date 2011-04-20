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
package tigase.db;

//~--- JDK imports ------------------------------------------------------------

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Map;

import tigase.xmpp.BareJID;

//~--- interfaces -------------------------------------------------------------

/**
 * The interface defines a generic data repository for storing arbitrary data in
 * any application specific form. This interface unifies database (repository)
 * access allowing for easier way to create database connections pools or
 * database fail-over mechanisms.
 * 
 * Created: Jun 16, 2010 3:34:32 PM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface DataRepository {

	/**
	 * The method checks whether a table for the given name exists in the
	 * database.
	 * 
	 * @param tableName
	 *          is a <code>String</code> value of the table name to check
	 * @return <code>true</code> <code>boolean</code> value if the table exist in
	 *         the database and <code>false</code> if the table was not found.
	 * @throws SQLException
	 *           if there was a problem accessing database.
	 */
	boolean checkTable(String tableName) throws SQLException;

	/**
	 * Creates a SQL statement on which SQL queries can be executed later by the
	 * higher repository layer.
	 * 
	 * @param A user id for which the statement has to be created. This is an optional
	 * parameter and null can be provided. It is used mainly to group queries for the
	 * same user on the same DB connection.
	 * @return a newly created <code>Statement</code>
	 * @throws SQLException
	 *           if a JDBC error occurs.
	 */
	Statement createStatement(BareJID user_id) throws SQLException;

	// ~--- get methods ----------------------------------------------------------

	/**
	 * Returns a prepared statement for a given key.
	 * 
	 * @param A user id for which the statement has to be created. This is an optional
	 * parameter and null can be provided. It is used mainly to group queries for the
	 * same user on the same DB connection.
	 * @param stIdKey
	 *          is a statement identification key.
	 * @return a <code>PreparedStatement</code> for the given id key or null if
	 *         such a statement does not exist.
	 * @throws SQLException
	 */
	PreparedStatement getPreparedStatement(BareJID user_id, String stIdKey)
			throws SQLException;

	/**
	 * Returns a DB connection string or DB connection URI.
	 * 
	 * @return a <code>String</code> value representing database connection
	 *         string.
	 */
	String getResourceUri();

	// ~--- methods --------------------------------------------------------------

	/**
	 * Initializes a prepared statement for a given query and stores it internally
	 * under the given id key. It can be retrieved later using
	 * <code>getPreparedStatement(stIdKey)</code> method.
	 * 
	 * @param stIdKey
	 *          is a statement identification key.
	 * @param query
	 *          is a query for the prepared statement.
	 * @throws SQLException
	 */
	void initPreparedStatement(String stIdKey, String query) throws SQLException;

	/**
	 * The method is called to initialize the data repository. Depending on the
	 * implementation all the initialization parameters can be passed either via
	 * <code>resource_uri</code> parameter as the database connection string or
	 * via <code>params</code> map if the required repository parameters are more
	 * complex or both.
	 * 
	 * @param resource_uri
	 *          value in most cases representing the database connection string.
	 * @param params
	 *          is a <code>Map</code> with repository properties necessary to
	 *          initialize and perform all the functions. The initialization
	 *          parameters are implementation dependent.
	 * @throws SQLException
	 *           if there was an error during repository initialization. Some
	 *           implementations, though, perform so called lazy initialization so
	 *           even though there is a problem with the underlying repository it
	 *           may not be signaled through this method call.
	 */
	void initRepository(String resource_uri, Map<String, String> params)
			throws SQLException;

	/**
	 * A helper method to release resources from the statement and result set.
	 * This is most common operation for all database calls, therefore it does
	 * make sense to add such a utility method to the API.
	 * 
	 * @param stmt
	 *          a <code>Statement</code> variable to release resources for. Might
	 *          be null.
	 * @param rs
	 *          a <code>ResultSet</code> variable to release resources for. Might
	 *          be null.
	 */
	void release(Statement stmt, ResultSet rs);

	/**
	 * Returns <code>DataRepository</code> instance. If this is a repository pool
	 * then it returns particular instance from the pool. It this is a real
	 * repository instance it returns itself. This is exclusive take, no other
	 * thread may use this handle until it is returned to the pool.
	 * 
	 * @param user_id
	 *          is user account ID for which we acquire the handle.
	 * @return DataRepository instance.
	 */
	DataRepository takeRepoHandle(BareJID user_id);

	void releaseRepoHandle(DataRepository repo);

	/**
	 * Starts transaction on the DataRepository connection. Please note that
	 * calling this method on the repository pool has no effect. You have to
	 * obtain particular repository handle first, before you can start
	 * transaction.
	 */
	void startTransaction() throws SQLException;

	/**
	 * Commits current transaction on the DataRepository connection. Please note
	 * that calling this method on the repository pool has no effect. You have to
	 * obtain particular repository handle first, before you can start
	 * transaction.
	 */
	void commit() throws SQLException;

	/**
	 * Rolls back started transaction on the DataRepository connection. Please
	 * note that calling this method on the repository pool has no effect. You
	 * have to obtain particular repository handle first, before you can start
	 * transaction.
	 */
	void rollback() throws SQLException;

	/**
	 * Ends current transaction on the DataRepository connection. Please note that
	 * calling this method on the repository pool has no effect. You have to
	 * obtain particular repository handle first, before you can start
	 * transaction.
	 */
	void endTransaction() throws SQLException;

}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
