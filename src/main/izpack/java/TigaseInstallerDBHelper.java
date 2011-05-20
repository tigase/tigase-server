package com.izforge.izpack.panels;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import tigase.db.DBInitException;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.AuthRepository;
import tigase.db.UserExistsException;
import tigase.xmpp.BareJID;

import com.izforge.izpack.installer.ResourceManager;
import com.izforge.izpack.installer.ResourceNotFoundException;
import com.izforge.izpack.util.Debug;
import com.izforge.izpack.util.VariableSubstitutor;

class TigaseInstallerDBHelper {

	private boolean schema_ok = false;
	private boolean connection_ok = false;
	private boolean db_ok = false;
	private String schema_ver_query = TigaseConfigConst.DERBY_GETSCHEMAVER_QUERY;
	private String res_prefix = "";
	private boolean schema_exists = false;


	static final int DB_CONNEC_POS = 0;
	static final int DB_EXISTS_POS = 1;
	static final int DB_SCHEMA_POS = 2;
	static final int DB_CONVER_POS = 3;


	enum SQL_LOAD_STATE {
		INIT, IN_SQL;
	}


	private ArrayList<String> loadSQLQueries(
			String resource, 
			String res_prefix, 
			Properties variables) 
			throws Exception 
			{
		ArrayList<String> results = new ArrayList<String>();
		VariableSubstitutor vs = new VariableSubstitutor(variables);
		BufferedReader br =
			new BufferedReader(new
					InputStreamReader(getResource(resource)));
		String line = null;
		String sql_query = "";
		SQL_LOAD_STATE state = SQL_LOAD_STATE.INIT;
		while ((line = br.readLine()) != null) {
			switch (state) {
			case INIT:
				if (line.startsWith("-- QUERY START:")) {
					sql_query = "";
					state = SQL_LOAD_STATE.IN_SQL;
				}
				if (line.startsWith("-- LOAD SCHEMA:")) {
					results.addAll(loadSchemaQueries(res_prefix, variables));
				}
				break;
			case IN_SQL:
				if (line.startsWith("-- QUERY END:")) {
					state = SQL_LOAD_STATE.INIT;
					sql_query = sql_query.trim();
					if (sql_query.endsWith(";")) {
						sql_query = sql_query.substring(0, sql_query.length()-1);
					}
					if (sql_query.endsWith("//")) {
						sql_query = sql_query.substring(0, sql_query.length()-2);
					}
					results.add(vs.substitute(sql_query, null));
				}
				if (line.isEmpty() || line.trim().startsWith("--")) {
					continue;
				} else {
					sql_query += " " + line.trim();
				}
				break;
			default:
				break;
			}
		}
		br.close();
		return results;
			}


	protected InputStream getResource(String resource) 
	throws ResourceNotFoundException {
		return ResourceManager.getInstance().getInputStream(resource);
	}

	private ArrayList<String> loadSchemaQueries(
			String res_prefix, 
			Properties variables) 
			throws Exception 
			{
		ArrayList<String> queries = loadSQLQueries(res_prefix + ".schema", res_prefix, variables);
		queries.addAll(loadSQLQueries(res_prefix + ".sp", res_prefix, variables));
		queries.addAll(loadSQLQueries(res_prefix + ".props", res_prefix, variables));
		return queries;
			}	

	public void validateDBConnection(MsgTarget msgTarget) {
		connection_ok = false;
		String db_conn = TigaseConfigConst.props.getProperty("root-db-uri");
		if (db_conn == null) {
			msgTarget.addResultMessage().append("Missing DB connection URL");
			return;
		} else {
			selectDatabase(db_conn);
			try {
				Connection conn = DriverManager.getConnection(db_conn);
				conn.close();
				connection_ok = true;
				msgTarget.addResultMessage().append("Connection OK");
				return;
			} catch (Exception e) {
				//e.printStackTrace();
				msgTarget.addResultMessage().append(e.getMessage());
				return;
			}
		}
	}

	private void selectDatabase(String db_uri) {
		schema_ver_query = TigaseConfigConst.JDBC_GETSCHEMAVER_QUERY;
		if (db_uri.startsWith("jdbc:postgresql")) {
			System.setProperty("jdbc.drivers", TigaseConfigConst.PGSQL_DRIVER);
			res_prefix = "pgsql";
		}
		if (db_uri.startsWith("jdbc:mysql")) {
			System.setProperty("jdbc.drivers", TigaseConfigConst.MYSQL_DRIVER);
			res_prefix = "mysql";
		}
		if (db_uri.startsWith("jdbc:derby")) {
			System.setProperty("jdbc.drivers", TigaseConfigConst.DERBY_DRIVER);
			res_prefix = "derby";
			schema_ver_query = TigaseConfigConst.DERBY_GETSCHEMAVER_QUERY;
		}
	}

	public void validateDBExists(Properties variables, MsgTarget msgTarget) {
		if (!connection_ok) {
			msgTarget.addResultMessage().append("Connection not validated");
			return;
		}

		db_ok = false;
		String db_conn = TigaseConfigConst.props.getProperty("--user-db-uri");
		if (db_conn == null) {
			msgTarget.addResultMessage().append("Missing DB connection URL");
			return;
		} else {
			Connection conn = null;
			try {
				conn = DriverManager.getConnection(db_conn);
				conn.close();
				db_ok = true;
				msgTarget.addResultMessage().append("Exists OK");
				return;
			} catch (Exception e) {
				ResultMessage resulMessage = msgTarget.addResultMessage();
				resulMessage.append("Doesn't exist");
				resulMessage.append(", creating...");				

				db_conn = TigaseConfigConst.props.getProperty("root-db-uri");
				try {
					conn = DriverManager.getConnection(db_conn);
					ArrayList<String> queries = loadSQLQueries(res_prefix + ".create", res_prefix, variables);
					queries.add("commit");
					for (String query: queries) {
						Debug.trace("Executing query: " + query);
						Statement stmt = conn.createStatement();
						// Some queries may fail and this is still fine
						// the user or the database may already exist
						try {
							stmt.execute(query);
							stmt.close();
						} catch (Exception ex) {
							Debug.trace("Query failed: " + ex.getMessage());
						}
					}
					conn.close();
					resulMessage.append(" OK");
					db_ok = true;
				} catch (Exception ex) {
					resulMessage.append(ex.getMessage());
				}
			}
		}
	}

	public void validateDBConversion(Properties variables, MsgTarget msgTarget) {
		if (!connection_ok) {
			msgTarget.addResultMessage().append("Connection not validated");
			return;
		}
		if (!db_ok) {
			msgTarget.addResultMessage().append("Database not validated");
			return;
		}
		if (schema_ok) {
			msgTarget.addResultMessage().append("Conversion not needed");
			return;
		}
		if (!schema_exists) {
			msgTarget.addResultMessage().append("Something wrong, the schema still is not loaded...");
			return;
		}
		String db_conn = TigaseConfigConst.props.getProperty("root-tigase-db-uri");
		try {
			//conn.close();
			ResultMessage resultMessage = msgTarget.addResultMessage();
			resultMessage.append("Converting...");

			Connection conn = DriverManager.getConnection(db_conn);
			Statement stmt = conn.createStatement();
			ArrayList<String> queries = loadSQLQueries(res_prefix + ".upgrade", res_prefix, variables);
			for (String query: queries) {
				Debug.trace("Executing query: " + query);
				stmt.execute(query);
			}
			stmt.close();
			conn.close();
			schema_ok = true;
			resultMessage.append(" completed OK");
		} catch (Exception ex) {
			msgTarget.addResultMessage().append("Can't upgrade schema: " + ex.getMessage());
			return;
		}
	}

	public void validateDBSchema(Properties variables, MsgTarget msgTarget) {
		if (!connection_ok) {
			msgTarget.addResultMessage().append("Connection not validated");
			return;
		}
		if (!db_ok) {
			msgTarget.addResultMessage().append("Database not validated");
			return;
		}
		schema_exists = false;
		schema_ok = false;
		Connection conn = null;
		String db_conn = TigaseConfigConst.props.getProperty("--user-db-uri");
		long users = 0;
		try {
			conn = DriverManager.getConnection(db_conn);
			Statement stmt = conn.createStatement();

			String query = TigaseConfigConst.JDBC_CHECKUSERTABLE_QUERY;
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				users = rs.getLong(1);
				schema_exists = true;
				Debug.trace("Schema exists, users: " + users);
			}

			query = schema_ver_query;
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				String schema_version = rs.getString(1);
				if ("4.0".equals(schema_version)) {
					schema_ok = true;
				}
			}
		} catch (Exception e) {
			Debug.trace("Exception, posibly schema hasn't been loaded yet: " + e);
		}
		if (schema_ok) {
			msgTarget.addResultMessage().append("Schema OK, accounts number: " + users);
			return;
		}
		if (!schema_exists) {
			Debug.trace("DB schema doesn't exists, creating one...");
			db_conn = TigaseConfigConst.props.getProperty("root-tigase-db-uri");
			try {
				//conn.close();
				conn = DriverManager.getConnection(db_conn);
				Statement stmt = conn.createStatement();
				ArrayList<String> queries = loadSchemaQueries(res_prefix, variables);
				for (String query: queries) {
					Debug.trace("Executing query: " + query);
					stmt.execute(query);
				}
				stmt.close();
				conn.close();
				schema_ok = true;
				msgTarget.addResultMessage().append("New schema loaded OK");
				return;
			} catch (Exception ex) {
				msgTarget.addResultMessage().append("Can't load schema: " + ex.getMessage());
				return;
			}
		} else {
			msgTarget.addResultMessage().append("Old schema, accounts number: " + users);
			return;
		}
	}

	protected void addXmppAdminAccount(Properties variables,
			MsgTarget msgTarget) {

		
		// part 1, check db preconditions
		if (!connection_ok) {
			msgTarget.addResultMessage().append("Connection not validated");
			return;
		}
		if (!db_ok) {
			msgTarget.addResultMessage().append("Database not validated");
			return;
		}

		if (!schema_ok) {
			msgTarget.addResultMessage().append("Database schema is invalid");
			return;
		}

		// part 2, acquire reqired fields and validate them
		Object admins = variables.get("admins");
		Set<BareJID> jids = new LinkedHashSet<BareJID>();
		if (admins != null) {
			String[] adminsStr = admins.toString().split(",");
			for (String adminStr : adminsStr) {
				String jid = adminStr.trim();
				if (jid != null && !jid.equals("")) {
					jids.add(BareJID.bareJIDInstanceNS(jid));
				}
			}
		}
		if (jids.size() < 1) {
			msgTarget.addResultMessage().append("Error: No admin users entered");
			return;
		}
		
		Object pwdObj = variables.get("adminsPwd");
		if (pwdObj == null) {
			msgTarget.addResultMessage().append("Error: No admin password enetered");
			return;
		}
		String pwd = pwdObj.toString();
		
		String className = TigaseConfigConst.props.getProperty("--auth-db");
		// currently Tigase use "tigase.db.jdbc.TigaseCustomAuth" if no --auth-db was configured
		// if (className == null) 
		//	className = TigaseConfigConst.props.getProperty("--user-db");
		String resource = TigaseConfigConst.props.getProperty("--auth-db-uri");
		if (resource == null) 
			resource = TigaseConfigConst.props.getProperty("root-tigase-db-uri");

		try {
			AuthRepository repo = RepositoryFactory.getAuthRepository(
					className, resource, null);
			
			for (BareJID jid : jids) {
				try {
					repo.addUser(jid, pwd);
				} catch (UserExistsException e) {
					// user is already there, we swallow the exception
				} 
			}
			
			msgTarget.addResultMessage().append("All users added");
			
		} catch (DBInitException e) {
			msgTarget.addResultMessage().append("Error initializing DB");
		} catch (TigaseDBException e) {
			msgTarget.addResultMessage().append("DB error: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			msgTarget.addResultMessage().append("Error locating connector");
		} catch (InstantiationException e) {
			msgTarget.addResultMessage().append("Error initializing connector");
		} catch (IllegalAccessException e) {
			msgTarget.addResultMessage().append("Illegal access");
		}
	}	

	
	enum Tasks implements TigaseDBTask {
		VALIDATE_CONNECTION("Checking connection to the database") {
			public void execute(TigaseInstallerDBHelper helper, Properties variables, MsgTarget msgTarget) {
				helper.validateDBConnection(msgTarget);
			}
		},
		VALIDATE_DB_EXISTS("Checking if the database exists") {
			public void execute(TigaseInstallerDBHelper helper, Properties variables, MsgTarget msgTarget) {
				helper.validateDBExists(variables, msgTarget);
			}
		},					
		VALIDATE_DB_SCHEMA("Checking the database schema") {
			public void execute(TigaseInstallerDBHelper helper, Properties variables, MsgTarget msgTarget) {
				helper.validateDBSchema(variables, msgTarget);
			}
		},					
		VALIDATE_DB_CONVERSION("Checking whether the database needs conversion") {
			public void execute(TigaseInstallerDBHelper helper, Properties variables, MsgTarget msgTarget) {
				helper.validateDBConversion(variables, msgTarget);
			}
		},
		ADD_ADMIN_XMPP_ACCOUNT("Adding XMPP admin accounts") {
			public void execute(TigaseInstallerDBHelper helper, Properties variables, MsgTarget msgTarget) {
				helper.addXmppAdminAccount(variables, msgTarget);
			}
		};

		private final String description;
		
		private Tasks(String description) {
			this.description = description;
		}
		
		public String getDescription() {
			return description;
		}
		
		// override to change order
		public static TigaseDBTask[] getTasksInOrder() {
			return values();
		}
	}
	
	
	static interface TigaseDBTask {
		String getDescription();
		abstract void execute(TigaseInstallerDBHelper helper, Properties variables, MsgTarget msgTarget);
	}

	static interface MsgTarget {
		abstract ResultMessage addResultMessage();	
	}

	static interface ResultMessage {
		void append(String msg);
	}
	
}
