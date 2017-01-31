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
import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.util.Debug;
import com.izforge.izpack.util.VariableSubstitutor;
import java.io.*;
import java.net.URLEncoder;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



class TigaseInstallerDBHelper {

	private boolean schema_ok = false;
	private boolean connection_ok = false;
	private boolean db_ok = false;
	private String schema_ver_query = TigaseConfigConst.DERBY_GETSCHEMAVER_QUERY;
	private String res_prefix = "";
	private boolean schema_exists = false;
	private String schema_version;

	private AutomatedInstallData iData;

	private VariablesSource variablesSource;



	static final int DB_CONNEC_POS = 0;
	static final int DB_EXISTS_POS = 1;
	static final int DB_SCHEMA_POS = 2;
	static final int DB_CONVER_POS = 3;


	enum SQL_LOAD_STATE {
		INIT, IN_SQL;
	}

	public TigaseInstallerDBHelper() {
		iData = TigaseInstallerCommon.getInstallData();
		variablesSource = new IzPackInstallDataVariablesSource(iData);
	}




	private ArrayList<String> loadSQLQueries(
			String resource,
			String res_prefix,
			Properties variables)
			throws Exception
			{
		ArrayList<String> results = new ArrayList<String>();
		VariableSubstitutor vs = new VariableSubstitutor(variables);
		BufferedReader br;

		if ( res_prefix == null ){
			br = new BufferedReader( new InputStreamReader( getResourcePath( resource ) ) );
		} else {
			br = new BufferedReader( new InputStreamReader( getResource( resource ) ) );
		}

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
				if (  line.startsWith( "-- LOAD FILE:" )  && line.trim().contains( "sql" ) )
				{
					Matcher matcher = Pattern.compile( "-- LOAD FILE:.*" + res_prefix + "-(.*).sql" ).matcher( line );
					if ( matcher.find() ){
						Debug.trace( String.format( "\n\n *** trying to load schema: %1$s \t", matcher.group( 1 ) ) );
						results.addAll( loadSQLQueries( res_prefix + "-" + matcher.group( 1 ), res_prefix, variables ) );
					}
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
                                if ((line.trim().startsWith("source") || line.trim().startsWith("run") || line.trim().startsWith("\\i"))
                                        && line.trim().contains("sql")) {

                                        Matcher matcher = Pattern.compile(res_prefix + "-(.*).sql").matcher(line);
                                        if (matcher.find()) {
                                            Debug.trace(String.format("\n\n *** trying to load schema: %1$s \t", matcher.group(1)));
                                            results.addAll(loadSQLQueries(res_prefix + "-" + matcher.group(1), res_prefix, variables));
                                        }
                                        continue;
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


	protected InputStream getResourcePath(String resource)
	throws ResourceNotFoundException {


		File f = new File( resource );
		InputStream is = null;
		Debug.trace(String.format( "Getting resource: %1$s @ filename: %2$s",resource, f.getAbsolutePath() ));
		try {
			is = new FileInputStream( f );
		} catch ( FileNotFoundException ex ) {
			throw new ResourceNotFoundException("could not find: " + resource );
		}
		return is;
	}

	protected InputStream getResource(String resource)
	throws ResourceNotFoundException {
		return ResourceManager.getInstance().getInputStream(resource);
	}

        private ArrayList<String> loadSchemaQueries(
                String res_prefix,
                Properties variables)
                throws Exception {

            ArrayList<String> queries = new ArrayList<String>();
            queries.addAll(loadSQLQueries(res_prefix + "-schema-7-1-schema", res_prefix, variables));
            queries.addAll(loadSQLQueries(res_prefix + "-schema-7-1-sp", res_prefix, variables));
            queries.addAll(loadSQLQueries(res_prefix + "-schema-7-1-props", res_prefix, variables));

//			System.out.println( "Socks5component variable: " + variablesSource.getVariable(TigaseConfigConst.SOCKS5_COMP) );

            return queries;
        }

	public void validateDBConnection(TigaseInstallerDBHelper.MsgTarget msgTarget) {
		connection_ok = false;
		String db_conn = TigaseConfigConst.props.getProperty("root-db-uri");
		Debug.trace("validateDBConnection (root-db-uri): " + db_conn);
		if (db_conn == null) {
			msgTarget.addResultMessage().append("Missing DB connection URL");
			return;
		} else {
			selectDatabase(db_conn);
			try {
				Enumeration<Driver> drivers = DriverManager.getDrivers();
				while (drivers.hasMoreElements()) {
					Debug.trace("DriverManager (drivers): " + drivers.nextElement().toString());
				}
				Debug.trace("DriverManager (drivers end):");
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
		Debug.trace("selectDatabase (db_uri): " + db_uri);
		schema_ver_query = TigaseConfigConst.JDBC_GETSCHEMAVER_QUERY;
		String driverClass = null;
		if (db_uri.startsWith("jdbc:postgresql")) {
			driverClass = TigaseConfigConst.PGSQL_DRIVER;
			System.setProperty("jdbc.drivers", driverClass);
			res_prefix = "postgresql";
		}
		if (db_uri.startsWith("jdbc:sqlserver") || db_uri.startsWith("jdbc:jtds:sqlserver")) {
			driverClass = TigaseConfigConst.SQLSERVER_DRIVER;
			System.setProperty("jdbc.drivers", driverClass);
			res_prefix = "sqlserver";
			schema_ver_query = TigaseConfigConst.SQLSERVER_GETSCHEMAVER_QUERY;
		}
		if (db_uri.startsWith("jdbc:mysql")) {
			driverClass = TigaseConfigConst.MYSQL_DRIVER;
			System.setProperty("jdbc.drivers", driverClass);
			res_prefix = "mysql";
		}
		if (db_uri.startsWith("jdbc:derby")) {
			driverClass = TigaseConfigConst.DERBY_DRIVER;
			System.setProperty("jdbc.drivers", driverClass);
			res_prefix = "derby";
			schema_ver_query = TigaseConfigConst.DERBY_GETSCHEMAVER_QUERY;
		}
		Driver driver;
		try {

			Debug.trace("selectDatabase (jdbc.drivers): " + System.getProperty( "jdbc.drivers" ));
			Debug.trace("selectDatabase (res_prefix): " + res_prefix);
			Debug.trace("selectDatabase (schema_ver_query): " + schema_ver_query);

			driver = (Driver) Class.forName(driverClass).newInstance();
			Debug.trace("selectDatabase (driver): " + driver.toString());
			Debug.trace("selectDatabase (driver.acceptsURL): " + driver.acceptsURL( db_uri));

			DriverManager.registerDriver(driver);
		} catch ( Exception ex ) {
			Logger.getLogger( TigaseInstallerDBHelper.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}

	public void validateDBExists(Properties variables, MsgTarget msgTarget) {
		if (!connection_ok) {
			msgTarget.addResultMessage().append("Connection not validated");
			return;
		}

		db_ok = false;
		String db_conn = TigaseConfigConst.props.getProperty("--user-db-uri");
		Debug.trace("validateDBExists (--user-db-uri): " + db_conn);
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
                                    ArrayList<String> queries = loadSQLQueries(res_prefix + "-installer-create-db", res_prefix, variables);
                                    queries.add("commit");
                                    for (String query : queries) {
                                        Debug.trace("validateDBExists -- Executing query: " + query);
                                        if (!query.isEmpty()) {
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

	public void validateDBConversion(Properties variables, TigaseInstallerDBHelper.MsgTarget msgTarget) {
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
		Debug.trace("validateDBConversion (root-tigase-db-uri): " + db_conn);
		try {
			//conn.close();
                        ResultMessage resultMessage = msgTarget.addResultMessage();
                        resultMessage.append("Converting...");

                        Connection conn = DriverManager.getConnection(db_conn);
                        Statement stmt = conn.createStatement();

                        ArrayList<String> queries = null;

                        if (schema_version == null) {
                            queries = loadSQLQueries(res_prefix + "-schema-upgrade-to-4", res_prefix, variables);
                        }

                        if ("4.0".equals(schema_version)) {
                            queries = loadSQLQueries(res_prefix + "-schema-upgrade-to-5-1", res_prefix, variables);
                        }
                        if ("5.1".equals(schema_version)) {
                            queries = loadSQLQueries(res_prefix + "-schema-upgrade-to-7-1", res_prefix, variables);
                        }
                        for (String query : queries) {
                            if (!query.isEmpty()) {
                                Debug.trace("validateDBConversion :: Executing query: " + query);
                                stmt.execute(query);
                            }
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
		Debug.trace("validateDBSchema (--user-db-uri): " + db_conn);
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
				schema_version = rs.getString(1);
				if ("5.1".equals(schema_version)) {
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
			Debug.trace("validateDBSchema (root-tigase-db-uri): " + db_conn);
			try {
                            //conn.close();
                            conn = DriverManager.getConnection(db_conn);
                            Statement stmt = conn.createStatement();
                            ArrayList<String> queries = loadSchemaQueries(res_prefix, variables);

                            for (String query : queries) {
                                if (!query.isEmpty()) {
                                    Debug.trace("validateDBSchema -- Executing query: " + query);
                                    stmt.execute(query);
                                }
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

	public void postInstallation(Properties variables, TigaseInstallerDBHelper.MsgTarget msgTarget) {

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
                String db_conn = TigaseConfigConst.props.getProperty("root-tigase-db-uri");
				Debug.trace("postInstallation (root-tigase-db-uri): " + db_conn);
                try {
                    //conn.close();
                    TigaseInstallerDBHelper.ResultMessage resultMessage = msgTarget.addResultMessage();
                    resultMessage.append("Finalizing...");

                    Connection conn = DriverManager.getConnection(db_conn);
                    Statement stmt = conn.createStatement();

                    ArrayList<String> queries = loadSQLQueries(res_prefix + "-installer-post", res_prefix, variables);
                    for (String query : queries) {
                        if (!query.isEmpty()) {
                            Debug.trace("postInstallation :: Executing query: " + query);
                            stmt.execute(query);
                        }
                    }

                    stmt.close();
                    conn.close();
                    schema_ok = true;
                    resultMessage.append(" completed OK");
                } catch (Exception ex) {
                    msgTarget.addResultMessage().append("Can't finalize: " + ex.getMessage());
                    return;
                }
	}

	public void socks5SchemaLoad(Properties variables, TigaseInstallerDBHelper.MsgTarget msgTarget) {

		String socks5 = variablesSource.getVariable(TigaseConfigConst.SOCKS5_COMP);
		boolean installSocks5 = socks5.equals("on") ? true : false ;

		// part 1, check db preconditions
		if (!connection_ok) {
			msgTarget.addResultMessage().append("Connection not validated");
			return;
		}
		if (!db_ok) {
			msgTarget.addResultMessage().append("Database not validated");
			return;
		}
		Debug.trace("socks5Component variable state: " + socks5 + " " + installSocks5);
		if ( !installSocks5 ) {
			msgTarget.addResultMessage().append("Socks5 component not selected, skipping schema load");
			return;
		}
                String db_conn = TigaseConfigConst.props.getProperty("root-tigase-db-uri");
				Debug.trace("socks5SchemaLoad (root-tigase-db-uri): " + db_conn);
                try {
                    //conn.close();
                    TigaseInstallerDBHelper.ResultMessage resultMessage = msgTarget.addResultMessage();
                    resultMessage.append("Loading socks5 schema...");

                    Connection conn = DriverManager.getConnection(db_conn);

					DatabaseMetaData dbm = conn.getMetaData();
					ResultSet tables = dbm.getTables(null, null, "tig_socks5_users", null);
					ResultSet tables_derby = dbm.getTables(null, null, "TIG_SOCKS5_USERS", null);
					if (tables.next()) {
						// Table exists
						msgTarget.addResultMessage().append("Socks5 schema exists, skipping schema load");
					} else if (tables_derby.next()) {
						// Table exists
						msgTarget.addResultMessage().append("Socks5 schema exists, skipping schema load");
					} else {
						// Table does not exist
						Statement stmt = conn.createStatement();

						ArrayList<String> queries = loadSQLQueries(res_prefix + "-socks5-schema", res_prefix, variables);
						for (String query : queries) {
							if (!query.isEmpty()) {
								Debug.trace("socks5 schema :: Executing query: " + query);
								stmt.execute(query);
							}
						}

						resultMessage.append(" completed OK");
						stmt.close();
					}

                    conn.close();
                } catch (Exception ex) {
                    msgTarget.addResultMessage().append("Can't load socks5 schema: " + ex.getMessage());
					Debug.trace("Can't load socks5 schema: " + ex.getMessage());
                    return;
                }
	}

	public void pubsubSchemaLoad(Properties variables, TigaseInstallerDBHelper.MsgTarget msgTarget) {

		String pubsub = variablesSource.getVariable(TigaseConfigConst.PUBSUB_COMP);
		Debug.trace("PubSub variable state: " + pubsub);
		boolean installPubSub = (pubsub != null && pubsub.equals("on")) ? true : false ;

		// part 1, check db preconditions
		if (!connection_ok) {
			msgTarget.addResultMessage().append("Connection not validated");
			return;
		}
		if (!db_ok) {
			msgTarget.addResultMessage().append("Database not validated");
			return;
		}
		Debug.trace("PubSub variable state: " + pubsub + " " + installPubSub);
		if ( !installPubSub ) {
			msgTarget.addResultMessage().append("PubSub component not selected, skipping schema load");
			return;
		}
                String db_conn = TigaseConfigConst.props.getProperty("root-tigase-db-uri");
				Debug.trace("PubSubSchemaLoad (root-tigase-db-uri): " + db_conn);
                try {
                    //conn.close();
                    TigaseInstallerDBHelper.ResultMessage resultMessage = msgTarget.addResultMessage();
                    resultMessage.append("Loading PubSub schema...");

                    Connection conn = DriverManager.getConnection(db_conn);

					DatabaseMetaData dbm = conn.getMetaData();
					ResultSet tables = dbm.getTables(null, null, "tig_pubsub_nodes", null);
					ResultSet tables_derby = dbm.getTables(null, null, "tig_pubsub_nodes".toUpperCase(), null);
					if (tables.next()) {
						// Table exists
						msgTarget.addResultMessage().append("PubSub schema exists, skipping schema load");
					} else if (tables_derby.next()) {
						// Table exists
						msgTarget.addResultMessage().append("PubSub schema exists, skipping schema load");
					} else {
						// Table does not exist
						Statement stmt = conn.createStatement();

						ArrayList<String> queries;
											queries = loadSQLQueries(res_prefix + "-pubsub-schema-3-2", res_prefix, variables);
//						queries.addAll( loadSQLQueries(res_prefix + "-pubsub-schema-3-1", res_prefix, variables) );
						for (String query : queries) {
							if (!query.isEmpty()) {
								Debug.trace("pubsub schema :: Executing query: " + query);
								stmt.execute(query);
							}
						}

						resultMessage.append(" completed OK");
						stmt.close();
					}

                    conn.close();
                } catch (Exception ex) {
                    msgTarget.addResultMessage().append("Can't load PubSub schema: " + ex.getMessage());
					Debug.trace("Can't load PubSub schema: " + ex.getMessage());
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
		Debug.trace("addXmppAdminAccount (--auth-db-uri): " + resource);
		if (resource == null)
			resource = TigaseConfigConst.props.getProperty("root-tigase-db-uri");
			Debug.trace("addXmppAdminAccount (root-tigase-db-uri): " + resource);

		try {
			Debug.trace("RepositoryFactory.getAuthRepository(" + className + ", " + resource + ",  + null)" );
			System.setProperty( RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY, "1" );
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
			Debug.trace("DBInitException: " + e);
		} catch (TigaseDBException e) {
			msgTarget.addResultMessage().append("DB error: " + e.getMessage());
			Debug.trace("TigaseDBException: " + e);
		} catch (ClassNotFoundException e) {
			msgTarget.addResultMessage().append("Error locating connector");
			Debug.trace("ClassNotFoundException: " + e);
		} catch (InstantiationException e) {
			msgTarget.addResultMessage().append("Error initializing connector");
			Debug.trace("InstantiationException: " + e);
		} catch (IllegalAccessException e) {
			msgTarget.addResultMessage().append("Illegal access");
			Debug.trace("IllegalAccessException: " + e);
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
		},
		SOCKS5_COMPONENT("Loading socks5 component schema") {
			public void execute(TigaseInstallerDBHelper helper, Properties variables, MsgTarget msgTarget) {
				helper.socks5SchemaLoad(variables, msgTarget);
			}
		},
		PUBSUB_COMPONENT("Loading PubSub component schema") {
			public void execute(TigaseInstallerDBHelper helper, Properties variables, MsgTarget msgTarget) {
				helper.pubsubSchemaLoad(variables, msgTarget);
			}
		},
		POST_INSTALLATION("Post installation actions") {
			public void execute(TigaseInstallerDBHelper helper, Properties variables, MsgTarget msgTarget) {
				helper.postInstallation(variables, msgTarget);
			}
		}


//				System.out.println( "Socks5component variable: " + variablesSource.getVariable(TigaseConfigConst.SOCKS5_COMP) );
//		Debug.trace( "Socks5component variable: " + variablesSource.getVariable(TigaseConfigConst.SOCKS5_COMP) );

		;

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


abstract class VariablesSource {
	abstract String getVariable(String key);
	abstract String getEncodedVariable(String key);
}

class IzPackInstallDataVariablesSource extends VariablesSource {
	private final AutomatedInstallData idata;

	public IzPackInstallDataVariablesSource(AutomatedInstallData idata) {
		this.idata = idata;

	}

	@Override
	String getVariable(String key) {
		return idata.getVariable(key);
	}

	@Override
	String getEncodedVariable(String key) {

		String variable = idata.getVariable(key);
		String value = null;
		try {
			value = URLEncoder.encode(variable, "UTF-8");
		} catch ( Exception ex ) {
			Logger.getLogger( TigaseInstallerDBHelper.class.getName() ).log( Level.SEVERE, null, ex );
		}
		return value;
	}
}