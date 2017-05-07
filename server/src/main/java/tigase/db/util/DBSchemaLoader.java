/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013, "Tigase, Inc." <office@tigase.com>
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
 */
package tigase.db.util;

import tigase.db.AuthRepository;
import tigase.db.RepositoryFactory;
import tigase.db.Schema;
import tigase.db.TigaseDBException;
import tigase.server.XMPPServer;
import tigase.util.LogFormatter;
import tigase.util.ui.console.CommandlineParameter;
import tigase.util.ui.console.ParameterParser;
import tigase.util.ui.console.SystemConsole;
import tigase.xmpp.BareJID;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Simple utility class allowing various Database operations, including
 * executing simple queries, loading specific schema files or performing
 * complete load of all Tigase schema required to run the server.
 *
 * Following set of {@link Properties} is accepted:
 * <ul>
 * <li>{@code dbType} - type of the database, possible values are:
 * {@code mysql}, {@code postgresql}, {@code derby}, {@code sqlserver};
 * <li>{@code schemaVersion} - schema version to be loaded, , possible values
 * are: {@code 7-2}, {@code 7-1}, {@code 5-1}, {@code 5}, {@code 4};
 * <li>{@code dbName} - name of the database to be created/used;
 * <li>{@code dbHostname} - hostname of the database;
 * <li>{@code dbUser} - username of the regular user;
 * <li>{@code dbPass} - password of the regular user;
 * <li>{@code rootUser} - username of the database administrator user;
 * <li>{@code rootPass} - password of the database administrator user;
 * <li>{@code query} - simple, single query to be executed;
 * <li>{@code file} - path to the single schema file to be loaded to the
 * database;
 * <li>{@code adminJID} - JID address of the XMPP administrator account;
 * <li>{@code adminJIDpass} - password of the XMPP administrator account.
 *
 * @author wojtek
 */
public class DBSchemaLoader extends SchemaLoader<DBSchemaLoader.Parameters> {

	/** Denotes whether there wasn't any problem establishing connection to the
	 * database */
	private boolean connection_ok = false;
	/** Denotes whether database exists */
	private boolean db_ok = false;
	/** Denotes whether schema has proper version */
	private boolean schema_ok = false;
	/** Holds map of all replacement variables */
	private Map<String, String> replacementMap = new HashMap<String, String>();
	private Parameters params;
	private static final Logger log = Logger.getLogger( DBSchemaLoader.class.getCanonicalName() );
	
	enum SQL_LOAD_STATE {

		INIT, IN_SQL;
	}

	public enum PARAMETERS_ENUM {
		DATABASE_TYPE("dbType","mysql"),
		SCHEMA_VERSION("schemaVersion","7-2"),
//		COMPONENTS("components","message-archiving,pubsub,muc,sock5"),
		DATABASE_NAME("dbName","tigasedb"),
		DATABASE_HOSTNAME("dbHostname","localhost"),
		TIGASE_USERNAME("dbUser","tigase_user"),
		TIGASE_PASSWORD("dbPass","tigase_pass"),
		ROOT_USERNAME("rootUser","root"),
		ROOT_PASSWORD("rootPass","root"),
		LOG_LEVEL("logLevel","CONFIG"),
		USE_SSL("useSSL","false"),
		GET_URI("getURI","false"),
		QUERY("query",null),
		FILE("file",null),
		ADMIN_JID("adminJID",null),
		ADMIN_JID_PASS("adminJIDpass",null),
		IGNORE_MISSING_FILES("ignoreMissingFiles", "false"),
		DATABASE_OPTIONS("dbOptions", null);

		private String name = null;
		private String defaultValue = null;

		PARAMETERS_ENUM(String name, String defaultValue) {
			this.name = name;
			this.defaultValue = defaultValue;
		}

		public String getName() {
			return name;
		}

		public String getDefaultValue() {
			return defaultValue;
		}
	}

	public DBSchemaLoader() {

	}

	@Override
	public void init( Parameters params ) {
		params.init();
		for (PARAMETERS_ENUM p : PARAMETERS_ENUM.values()) {
			String value = null;
			switch (p) {
				case DATABASE_TYPE:
					value = params.getDbType();
					break;
				case DATABASE_HOSTNAME:
					value = params.getDbHostname();
					break;
				case DATABASE_NAME:
					value = params.getDbName();
					break;
				case TIGASE_USERNAME:
					value = params.getDbUser();
					break;
				case TIGASE_PASSWORD:
					value = params.getDbPass();
					break;
				case ROOT_USERNAME:
					value = params.getDbRootUser();
					break;
				case ROOT_PASSWORD:
					value = params.getDbRootPass();
					break;
				default:
					break;
			}
			if (value != null) {
				replacementMap.put("${" + p.getName() + "}", value);
			}
		}

		// configure logger
		Level lvl = params.logLevel;

		System.out.println( "LogLevel: " + lvl );

		Handler handler = new ConsoleHandler();
		handler.setLevel( lvl );
		handler.setFormatter( new LogFormatter() );
		log.setUseParentHandlers( false );
		log.addHandler( handler );
		log.setLevel( lvl );

		log.log(Level.CONFIG, "Parameters: {0}", new Object[]{params});
		this.params = params;
	}
	
	public List<String> getSupportedTypes() {
		return Arrays.asList("derby", "mysql", "postgresql", "sqlserver", "jdbc");
	}

	public String getSchemaFileName(String schemaId, String version) {
		String path = "database/";
		String dbType = params.getDbType();
		switch (schemaId) {
			case "":
				String[] parts = version.split("\\.");
				return path + dbType + "-schema-" + parts[0] + "-" + parts[1] + ".sql";
			default:
				return path + dbType + "-" + schemaId + "-schema-" + version + ".sql";
		}
	}

	/**
	 * Main method allowing pass arguments to the class and setting all logging to
	 * be printed to console.
	 *
	 * @param args key-value (in the form of {@code "-<variable> value"})
	 *             parameters.
	 */
	public static void main( String[] args ) {
		SchemaLoader.main(args);
	}

	@Override
	public Parameters createParameters() {
		return new Parameters();
	}

	/**
	 * Executes set of {@link TigaseDBTask} tasks selected based on set on passed
	 * properties
	 *
	 * @param params  set of configuration parameters.
	 */
	public void execute(SchemaLoader.Parameters params) {
		if (params instanceof Parameters) {
			// Get list of appropriate task and execute them;
			TigaseDBTask[] tasks;
			Parameters p = (Parameters) params;
			if (p.getQuery() != null) {
				tasks = Tasks.getQueryTasks();
			} else if (p.getFile() != null) {
				tasks = Tasks.getSchemaTasks();
			} else {
				tasks = Tasks.getTasksInOrder();
			}
			for (TigaseDBTask task : tasks) {
				task.execute(this, p);
			}
		} else {
			throw new RuntimeException("Invalid parameters type!");
		}
	}

	/**
	 * Performs actual read and parsing of queries from resource file. It skips
	 * comments and require proper syntax of the file, i.e. each query enclosed in
	 * {@code "-- QUERY START:"} and {@code "-- QUERY END:"}. If the file
	 * references/sources/import another schema file then it will also be read and
	 * parsed.
	 *
	 * @param resource   name of the resource for which an {@link InputStream}
	 *                   should be created, either excerpt of the name or a
	 *                   full/relative path to the schema {@code .sql} file.
	 * @return
	 * @throws IOException
	 */
	private ArrayList<String> loadSQLQueries( String resource)
			throws IOException {
		log.log(Level.FINER, "Loading queries, resource: {0}", new Object[]{resource});
		ArrayList<String> results = new ArrayList<>();
		final Path p = Paths.get(resource);

		if (!Files.exists(p)) {
			if (params.isIgnoreMissingFiles()) {
				log.log(Level.WARNING, "Provided path: {0} doesn't exist, skipping!", new Object[]{p.toString()});
				return results;
			} else {
				throw new IOException("Required file at " + p.toString() + " doesn't exist!");
			}
		}

		String sql_query = "";
		SQL_LOAD_STATE state = SQL_LOAD_STATE.INIT;
		for (String line : Files.readAllLines(p)) {
			switch ( state ) {
				case INIT:
					if ( line.startsWith( "-- QUERY START:" ) ){
						sql_query = "";
						state = SQL_LOAD_STATE.IN_SQL;
					}
//					if ( line.startsWith( "-- LOAD SCHEMA:" ) ){
//						results.addAll( loadSchemaQueries( variables ) );
//					}
					if (  line.startsWith( "-- LOAD FILE:" )  && line.trim().contains( "sql" ) )
					{
						Matcher matcher = Pattern.compile( "-- LOAD FILE:\\s*(.*\\.sql)" ).matcher( line );
						if ( matcher.find() ){
							results.addAll( loadSQLQueries(  matcher.group( 1 ) ) );
						}
					}
					break;
				case IN_SQL:
					if ( line.startsWith( "-- QUERY END:" ) ){
						state = SQL_LOAD_STATE.INIT;
						sql_query = sql_query.trim();
						if ( sql_query.endsWith( ";" ) ){
							sql_query = sql_query.substring( 0, sql_query.length() - 1 );
						}
						if ( sql_query.endsWith( "//" ) ){
							sql_query = sql_query.substring( 0, sql_query.length() - 2 );
						}
						for ( Map.Entry<String, String> entry : replacementMap.entrySet() ) {
							sql_query = sql_query.replace( entry.getKey(), entry.getValue() );
						}

						results.add( sql_query );
					}
					if ( line.isEmpty() || line.trim().startsWith( "--" ) ){
						continue;
					} else {
						sql_query += " " + line.trim();
					}
					break;
				default:
					break;
			}
		}
		return results;
	}

	@Override
	public Result validateDBConnection() {
		connection_ok = false;
		String db_conn = getDBUri(false, true );
		log.log( Level.INFO, "Validating DBConnection, URI: " + db_conn );
		if ( db_conn == null ){
			log.log( Level.WARNING, "Missing DB connection URL" );
			return Result.ok;
		} else {
			try ( Connection conn = DriverManager.getConnection( db_conn ) ) {
				Enumeration<Driver> drivers = DriverManager.getDrivers();
				ArrayList<String> availableDrivers = new ArrayList<>();
				while ( drivers.hasMoreElements() ) {
					availableDrivers.add( drivers.nextElement().toString() );
				}
				log.log( Level.CONFIG, "DriverManager (available drivers): " +  availableDrivers );
				conn.close();
				connection_ok = true;
				log.log( Level.INFO, "Connection OK" );
				return Result.ok;
			} catch ( SQLException e ) {
				log.log( Level.WARNING, e.getMessage() );
				return Result.error;
			}
		}
	}

	private Result withConnection(String db_conn, SQLCommand<Connection, Result> cmd) {
		return withConnection(db_conn, cmd, null);
	}

	private Result withConnection(String db_conn, SQLCommand<Connection, Result> cmd, ExceptionHandler<Exception, Result> exceptionHandler) {
		Result result = null;
		try ( Connection conn = DriverManager.getConnection( db_conn ) ) {
			Enumeration<Driver> drivers = DriverManager.getDrivers();
			ArrayList<String> availableDrivers = new ArrayList<>();
			while ( drivers.hasMoreElements() ) {
				availableDrivers.add( drivers.nextElement().toString() );
			}
			log.log( Level.CONFIG, "DriverManager (available drivers): " +  availableDrivers ) ;
			result = cmd.execute(conn);
			conn.close();
		} catch (SQLException | IOException e) {
			if (exceptionHandler != null) {
				return exceptionHandler.handleException(e);
			} else {
				log.log( Level.SEVERE, "\n\n\n=====\nFailure: " + e.getMessage() + "\n=====\n\n" );
				return Result.error;
			}
		}
		return result;
	}

	private Result withStatement(String dbConn, SQLCommand<Statement, Result> cmd) {
		return withConnection(dbConn, conn -> {
			try (Statement stmt = conn.createStatement()) {
				return cmd.execute(stmt);
			}
		});
	}

	@Override
	public Result shutdown() {
		return shutdownDerby();
	}
	
	public Result shutdownDerby() {
		String db_conn = getDBUri( false, true );
		if ( "derby".equals( params.getDbType() ) ){
			log.log( Level.INFO, "Validating DBConnection, URI: " + db_conn );
			if ( db_conn == null ){
				log.log( Level.WARNING, "Missing DB connection URL" );
			} else {
				db_conn += ";shutdown=true";
				return withConnection(db_conn, conn -> {
					connection_ok = true;
					log.log( Level.INFO, "Connection OK" );
					return Result.ok;
				});
			}
		}
		return Result.ok;
	}

	@Override
	public Result validateDBExists() {
		if ( !connection_ok ){
			log.log( Level.WARNING, "Connection not validated" );
			return Result.error;
		}

		db_ok = false;
		String db_conn1 = getDBUri(true, false );
		log.log( Level.INFO, "Validating whether DB Exists, URI: " + db_conn1 );
		if ( db_conn1 == null ){
			log.log( Level.WARNING, "Missing DB connection URL" );
			return Result.error;
		} else {
			return withConnection(db_conn1, conn -> {
				db_ok = true;
				log.log( Level.INFO, "Exists OK" );
				return Result.ok;
			}, e -> withConnection(getDBUri(false, true), conn -> {
						Result result = Result.ok;
						try {
							ArrayList<String> queries = loadSQLQueries(
									"database/" + params.getDbType() + "-installer-create-db.sql");
							for (String query : queries) {
								log.log(Level.FINE, "Executing query: " + query);
								if (!query.isEmpty()) {
									// Some queries may fail and this is still fine
									// the user or the database may already exist
									try (Statement stmt = conn.createStatement()) {
										stmt.execute(query);
										stmt.close();
									} catch (SQLException ex) {
										result = Result.warning;
										log.log(Level.WARNING, "Query failed: " + ex.getMessage());
									}
								}
							}
							log.log( Level.INFO, " OK" );
							db_ok = true;
						} catch (IOException ex) {
							log.log( Level.WARNING, ex.getMessage() );
							result = Result.error;
						}
						return result;
					}));
		}
	}

	/**
	 * Method performs post-installation action using using
	 * {@code *-installer-post.sql} schema file substituting it's variables with
	 * ones provided.
	 */
	@Override
	public Result postInstallation() {
		// part 1, check db preconditions
		if ( !connection_ok ){
			log.log( Level.WARNING, "Connection not validated" );
			return Result.error;
		}
		if ( !db_ok ){
			log.log( Level.WARNING, "Database not validated" );
			return Result.error;
		}

		if ( !schema_ok ){
			log.log( Level.WARNING, "Database schema is invalid" );
			return Result.error;
		}

		// part 2, acquire reqired fields and validate them
		String db_conn = getDBUri(true, true );
		log.log( Level.INFO, "Post Installation, URI: " + db_conn );
		return withStatement(db_conn, stmt -> {
			log.log( Level.INFO, "Finalizing..." );
			ArrayList<String> queries = loadSQLQueries( "database/" + params.getDbType() + "-installer-post.sql" );
			for ( String query : queries ) {
				if ( !query.isEmpty() ){
					log.log( Level.FINEST, "Executing query: " + query );
					try {
						stmt.execute( query );
					} catch (SQLException ex) {
						log.log(Level.SEVERE, "Failed to execute query: " + query);
						throw ex;
					}
				}
			}
			log.log( Level.INFO, " completed OK" );
			return Result.ok;
		});
	}

	/**
	 * Method performs post-installation action using using
	 * {@code *-installer-post.sql} schema file substituting it's variables with
	 * ones provided.
	 */
	@Override
	public Result printInfo() {
		// part 1, check db preconditions
		if ( !connection_ok ){
			log.log( Level.WARNING, "Connection not validated" );
			return Result.error;
		}
		if ( !db_ok ){
			log.log( Level.WARNING, "Database not validated" );
			return Result.error;
		}

		if ( !schema_ok ){
			log.log( Level.WARNING, "Database schema is invalid" );
			return Result.error;
		}

		String db_conn = getDBUri(true, false );

		switch ( params.getDbType() ) {
			case "mysql":
				if (!db_conn.contains("unicode=true")) {
					db_conn += "&useUnicode=true&characterEncoding=UTF-8";
				}
				break;
		}

		log.log( Level.INFO,"\n\nDatabase init.properties configuration:\n"
		        + "dataSource {\n"
				+ "    default {\n"
				+ "        uri = '" + db_conn + "'\n"
				+ "    }\n"
				+ "}\n");

		return Result.ok;
	}

	@Override
	public Result addXmppAdminAccount() {
		// part 1, check db preconditions
		if ( !connection_ok ){
			log.log( Level.WARNING, "Connection not validated" );
			return Result.error;
		}
		if ( !db_ok ){
			log.log( Level.WARNING, "Database not validated" );
			return Result.error;
		}

		if ( !schema_ok ){
			log.log( Level.WARNING, "Database schema is invalid" );
			return Result.error;
		}

		// part 2, acquire required fields and validate them
		List<BareJID> jids = params.getAdmins();
		if ( jids.size() < 1 ){
			log.log( Level.WARNING, "Error: No admin users entered" );
			return Result.warning;
		}

		String pwd = params.getAdminPassword();
		if ( pwd == null ){
			log.log( Level.WARNING, "Error: No admin password entered" );
			return Result.warning;
		}

		String dbUri = getDBUri( true, true );
		log.log( Level.INFO, "Adding XMPP Admin Account, URI: " + dbUri );

		try {
			Map<String, String> params = new HashMap<>();
			params.put( RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY, String.valueOf( 1 ) );

			log.log( Level.CONFIG, "RepositoryFactory.getAuthRepository(" + null + ", "
														 + dbUri + "," +  params + ")" );
			AuthRepository repo = RepositoryFactory.getAuthRepository( null, dbUri, params );
			for ( BareJID jid : jids ) {
				repo.addUser( jid, pwd );
			}

			log.log( Level.INFO, "All users added" );
			return Result.ok;
		} catch ( TigaseDBException | ClassNotFoundException | InstantiationException | IllegalAccessException e ) {
			log.log( Level.WARNING, "Error initializing DB" + e );
			return Result.error;
		}
	}

	@Override
	public Result loadSchema(String schemaId, String version) {
		if ( !connection_ok ){
			log.log( Level.INFO, "Connection not validated" );
			return Result.error;
		}

		String fileName = getSchemaFileName(schemaId, version);
		return loadSchemaFile(fileName);
	}

	@Override
	public Result loadSchemaFile( String fileName ) {
		// part 1, check db preconditions
		if ( !connection_ok ){
			log.log( Level.INFO, "Connection not validated" );
			return Result.error;
		}
		if ( !db_ok ){
			log.log( Level.INFO, "Database not validated" );
			return Result.error;
		}

		if ( fileName == null ){
			log.log( Level.WARNING, "Error: empty query" );
			return Result.error;
		}

		String db_conn = getDBUri( true, true );
		log.log(Level.INFO,
		        String.format("Loading schema from file(s): %1$s, URI: %2$s", fileName, db_conn));

		return withStatement(db_conn, stmt -> {
			ArrayList<String> queries = new ArrayList<>();
			queries.addAll(loadSQLQueries(fileName));
			for ( String query : queries ) {
				if ( !query.isEmpty() ){
					log.log( Level.FINEST, "Executing query: " + query );
					try {
						stmt.execute(query);
					} catch (SQLException ex) {
						log.log(Level.SEVERE, "Failed to execute query: " + query);
						throw ex;
					}
				}
			}
			schema_ok = true;
			log.log( Level.INFO, " completed OK" );
			return Result.ok;
		});
	}

	/**
	 * Method checks whether the connection to the database is possible and that
	 * database of specified name exists. If yes then a single query is executed.
	 *
	 * @param query to execute
	 */
	protected Result executeSingleQuery( String query ) {
		// part 1, check db preconditions
		if ( !connection_ok ){
			log.log( Level.INFO, "Connection not validated" );
			return Result.error;
		}

		if ( query == null ) {
			log.log(Level.WARNING, "Error: empty query");
			return Result.error;
		}

		String db_conn = getDBUri( false, false );
		log.log( Level.INFO, "Executing Simple Query, URI: " + db_conn );
		if ( db_conn == null ){
			log.log( Level.WARNING, "Missing DB connection URL" );
			return Result.error;
		} else {
			return withStatement(db_conn, stmt -> {
				log.log(Level.FINEST, "Executing query: " + query);
				if (!query.isEmpty()) {
					try {
						stmt.execute(query);
						stmt.close();
					} catch (SQLException ex) {
						log.log(Level.WARNING, "Query failed: " + query + ", " + ex.getMessage());
					}
				}
				return Result.ok;
			});
		}
	}

	public String getDBUri() {
		return getDBUri(true, false);
	}

	/**
	 * Helper method used to generate proper database URI depending on properties.
	 *
	 * @param includeDbName      configure whether to include database name in the
	 *                           URI
	 * @param useRootCredentials whether to put in the URI credentials of database
	 *                           administrator ({@code true}) or regular user.
	 * @return
	 */
	private String getDBUri(boolean includeDbName, boolean useRootCredentials ) {
		String db_uri = "jdbc:";
		String database = params.getDbType();
		String USERNAME = useRootCredentials ? params.getDbRootUser() : params.getDbUser();
		String PASSWORD = useRootCredentials ? params.getDbRootPass() : params.getDbPass();

		switch ( database ) {
			case "sqlserver":
				db_uri += "jtds:sqlserver:";
				break;
			default:
				db_uri += database + ":";
				break;
		}
		switch ( database ) {
			case "derby":
				db_uri += params.getDbName() + ";create=true";
				break;
			case "sqlserver":
				db_uri += "//" + params.getDbHostname();
				if ( includeDbName ){
					db_uri += ";databaseName=" + params.getDbName();
				}
				db_uri += ";user=" + USERNAME;
				if ( PASSWORD != null
						 && !PASSWORD.isEmpty() ){
					db_uri += ";password=" + PASSWORD;
				}
				db_uri += ";schema=dbo";
				db_uri += ";lastUpdateCount=false";
				db_uri += ";cacheMetaData=false";
				if ( Boolean.TRUE.equals(params.isUseSSL()) ) {
					db_uri += ";encrypt=true";
				}
				break;
			default:
				db_uri += "//" + params.getDbHostname() + "/";
				if ( includeDbName ){
					db_uri += params.getDbName();
				}
				db_uri += "?user=" + USERNAME;
				if ( PASSWORD != null
						 && !PASSWORD.isEmpty() ){
					db_uri += "&password=" + PASSWORD;
				}
				if ( Boolean.TRUE.equals(params.isUseSSL()) ) {
					db_uri += "&useSSL=true";
				}
				else if ( Boolean.FALSE.equals(params.isUseSSL()) ) {
					// explicitly disable SSL to avoid a warning by the driver
					db_uri += "&useSSL=false";
				}
				break;
		}
		return db_uri;
	}

	enum Tasks implements TigaseDBTask {

		VALIDATE_CONNECTION( "Checking connection to the database" ) {
			@Override
			public void execute( DBSchemaLoader helper, Parameters params ) {
				helper.validateDBConnection();
			}
		},
		VALIDATE_DB_EXISTS( "Checking if the database exists" ) {
			@Override
			public void execute( DBSchemaLoader helper, Parameters params ) {
				helper.validateDBExists();
			}
		},
		VALIDATE_DB_SCHEMA( "Checking the database schema" ) {
			@Override
			public void execute( DBSchemaLoader helper, Parameters params ) {
				helper.loadSchema(Schema.SERVER_SCHEMA_ID, XMPPServer.class.getPackage().getImplementationVersion());
			}
		},
		ADD_ADMIN_XMPP_ACCOUNT( "Adding XMPP admin accounts" ) {
			@Override
			public void execute( DBSchemaLoader helper, Parameters params ) {
				helper.addXmppAdminAccount();
			}
		},
		EXECUTE_SIMPLE_QUERY( "Executing simple single query" ) {
			@Override
			public void execute( DBSchemaLoader helper, Parameters params ) {
				helper.executeSingleQuery( params.getQuery() );
			}
		},
		LOAD_SCHEMA_FILE( "Loading schema file from provided file" ) {
			@Override
			public void execute( DBSchemaLoader helper, Parameters params ) {
				helper.loadSchemaFile( params.getFile() );
			}
		},
		POST_INSTALLATION( "Post installation actions" ) {
			@Override
			public void execute( DBSchemaLoader helper, Parameters params ) {
				helper.postInstallation( );
			}
		},
		SHUTDOWN_DATABASE( "Shutting Down Database" ) {
			@Override
			public void execute( DBSchemaLoader helper, Parameters params ) {
				helper.shutdownDerby( );
			}
		},
		PRINT_INFO_TASK( "Database Configuration Details" ) {
			@Override
			public void execute( DBSchemaLoader helper, Parameters params ) {
				helper.printInfo( );
			}
		};
		private final String description;

		private Tasks( String description ) {
			this.description = description;
		}

		@Override
		public String getDescription() {
			return description;
		}

		public static TigaseDBTask[] getTasksInOrder() {
			return new TigaseDBTask[] {
				VALIDATE_CONNECTION
					,VALIDATE_DB_EXISTS
					,VALIDATE_DB_SCHEMA
					,ADD_ADMIN_XMPP_ACCOUNT
					,POST_INSTALLATION
					,SHUTDOWN_DATABASE
					,PRINT_INFO_TASK
			};
		}

		public static TigaseDBTask[] getSchemaTasks() {
			return new TigaseDBTask[]{
					VALIDATE_CONNECTION,
					VALIDATE_DB_EXISTS,
					LOAD_SCHEMA_FILE,
					POST_INSTALLATION,
					SHUTDOWN_DATABASE,
					PRINT_INFO_TASK};
		}

		public static TigaseDBTask[] getQueryTasks() {
			return new TigaseDBTask[] {VALIDATE_CONNECTION, EXECUTE_SIMPLE_QUERY, SHUTDOWN_DATABASE};
		}
	}

	static interface TigaseDBTask {

		String getDescription();

		abstract void execute( DBSchemaLoader helper, Parameters variables );
	}

	public static class Parameters implements SchemaLoader.Parameters {

		private Boolean ingoreMissingFiles = false;
		private Level logLevel = Level.CONFIG;

		private String adminPassword;
		private List<BareJID> admins;
		private String dbRootPass;
		private String dbRootUser;
		private String dbType;
		private String dbName = null;
		private String dbHostname = null;
		private String dbUser = null;
		private String dbPass = null;
		private Boolean useSSL = null;

		private String file;
		private String query;

		private String getFile() {
			return file;
		}

		private String getQuery() {
			return query;
		}

		public String getAdminPassword() {
			return adminPassword;
		}

		public List<BareJID> getAdmins() {
			return admins == null ? Collections.emptyList() : admins;
		}

		public String getDbRootPass() {
			return dbRootPass;
		}

		public String getDbRootUser() {
			return dbRootUser;
		}

		public String getDbType() {
			return dbType;
		}

		public String getDbName() {
			return dbName;
		}

		public String getDbHostname() {
			return dbHostname;
		}

		public String getDbUser() {
			return dbUser;
		}

		public String getDbPass() {
			return dbPass;
		}

		public boolean isIgnoreMissingFiles() {
			return ingoreMissingFiles;
		}

		public Boolean isUseSSL() {
			return useSSL;
		}

		@Override
		public void parseUri(String uri) {
			int idx = uri.indexOf(":", 5);
			dbType = uri.substring(5, idx);
			if ("jtds".equals(dbType)) dbType = "sqlserver";

			String rest = null;
			switch (dbType) {
				case "derby":
					dbName = uri.substring(idx+1, uri.indexOf(";"));
					break;
				case "sqlserver":
					idx = uri.indexOf("//", idx) + 2;
					rest = uri.substring(idx);
					for (String x : rest.split(";")) {
						if (!x.contains("=")) {
							dbHostname = x;
						} else {
							String p[] = x.split("=");
							switch (p[0]) {
								case "databaseName":
									dbName = p[1];
									break;
								case "user":
									dbUser = p[1];
									break;
								case "password":
									dbPass = p[1];
									break;
								case "encrypt":
									useSSL = Boolean.valueOf(p[1]);
								default:
									// unknown setting
									break;
							}
						}
					}
					break;
				default:
					idx = uri.indexOf("//", idx) + 2;
					rest = uri.substring(idx);
					idx = rest.indexOf("/");
					dbHostname = rest.substring(0, idx);
					rest = rest.substring(idx+1);
					idx = rest.indexOf("?");
					dbName = rest.substring(0, idx);
					rest = rest.substring(idx + 1);
					for (String x : rest.split("&")) {
						String p[] = x.split("=");
						if (p.length < 2)
							continue;
						switch (p[0]) {
							case "user":
								dbUser = p[1];
								break;
							case "password":
								dbPass = p[1];
								break;
							case "useSSL":
								useSSL = Boolean.valueOf(p[1]);
							default:
								break;
						}
					}
					break;
			}
		}

		@Override
		public void parseArguments(String[] args) {
			ParameterParser parser = new ParameterParser(true);

			parser.addOption(new CommandlineParameter.Builder("T", PARAMETERS_ENUM.DATABASE_TYPE.getName()).description(
					"Database server type")
									 .defaultValue(PARAMETERS_ENUM.DATABASE_TYPE.getDefaultValue())
									 .required(true)
									 .build());
//			parser.addOption(new CommandlineParameter.Builder("V", PARAMETERS_ENUM.SCHEMA_VERSION.getName()).description(
//					"Intended version of the schema to be loaded")
//									 .options("4", "5", "5-1", "7-1", "7-2")
//									 .required(true)
//									 .defaultValue(PARAMETERS_ENUM.SCHEMA_VERSION.getDefaultValue())
//									 .build());
//		parser.addOption(new CommandlineParameter.Builder("V", PARAMETERS_ENUM.COMPONENTS.getName()).description(
//				"Comma-separated list of components for which schema should be loaded")
//				                 .options("message-archiving","pubsub","muc","sock5","unified-archive")
//				                 .defaultValue(PARAMETERS_ENUM.COMPONENTS.getDefaultValue())
//				                 .build());

			getSetupOptions().stream().forEach(option -> parser.addOption(option));

			parser.addOption(new CommandlineParameter.Builder("F", PARAMETERS_ENUM.FILE.getName()).description(
					"Comma separated list of SQL files that will be processed").build());
			parser.addOption(new CommandlineParameter.Builder("Q", PARAMETERS_ENUM.QUERY.getName()).description(
					"Custom query to be executed").build());
			parser.addOption(new CommandlineParameter.Builder("L", PARAMETERS_ENUM.LOG_LEVEL.getName()).description(
					"Java Logger level during loading process")
									 .defaultValue(PARAMETERS_ENUM.LOG_LEVEL.getDefaultValue())
									 .build());
			parser.addOption(new CommandlineParameter.Builder("J", PARAMETERS_ENUM.ADMIN_JID.getName()).description(
					"Comma separated list of administrator JID(s)").build());
			parser.addOption(new CommandlineParameter.Builder("N", PARAMETERS_ENUM.ADMIN_JID_PASS.getName()).description(
					"Password that will be used for the entered JID(s) - one for all configured administrators")
									 .secret()
									 .build());
			parser.addOption(new CommandlineParameter.Builder(null, PARAMETERS_ENUM.GET_URI.getName()).description(
					"Generate database URI")
									 .requireArguments(false)
									 .defaultValue(PARAMETERS_ENUM.GET_URI.getDefaultValue())
									 .build());
			parser.addOption(new CommandlineParameter.Builder(null, PARAMETERS_ENUM.IGNORE_MISSING_FILES.getName()).description(
					"Force ignoring missing files errors")
									 .defaultValue(PARAMETERS_ENUM.IGNORE_MISSING_FILES.getDefaultValue())
									 .build());

			Properties properties = null;

			if (null == args || args.length == 0 || (properties = parser.parseArgs(args)) == null) {
				System.out.println(parser.getHelp());
				System.exit(0);
			} else {
				System.out.println("properties: " + properties);
			}

			setProperties(properties);
		}

		@Override
		public void setProperties(Properties props) {
			logLevel = getProperty(props, PARAMETERS_ENUM.LOG_LEVEL, val -> Level.parse(val));
			ingoreMissingFiles = getProperty(props, PARAMETERS_ENUM.IGNORE_MISSING_FILES, val -> Boolean.valueOf(val));
			admins = getProperty(props, PARAMETERS_ENUM.ADMIN_JID, tmp -> Arrays.stream(tmp.split(","))
					.map(str -> BareJID.bareJIDInstanceNS(str))
					.collect(Collectors.toList()));
			adminPassword = getProperty(props, PARAMETERS_ENUM.ADMIN_JID_PASS);

			dbType = getProperty(props, PARAMETERS_ENUM.DATABASE_TYPE);
			dbName = getProperty(props, PARAMETERS_ENUM.DATABASE_NAME);
			dbHostname = getProperty(props, PARAMETERS_ENUM.DATABASE_HOSTNAME);
			dbUser = getProperty(props, PARAMETERS_ENUM.TIGASE_USERNAME);
			dbPass = getProperty(props, PARAMETERS_ENUM.TIGASE_PASSWORD);
			useSSL = getProperty(props, PARAMETERS_ENUM.USE_SSL, tmp -> Boolean.parseBoolean(tmp));

			dbRootUser = getProperty(props, PARAMETERS_ENUM.ROOT_USERNAME);
			dbRootPass = getProperty(props, PARAMETERS_ENUM.ROOT_PASSWORD);

			file = getProperty(props, PARAMETERS_ENUM.FILE);
			query = getProperty(props, PARAMETERS_ENUM.QUERY);
		}

		protected void init() {
			if (dbRootUser == null || dbRootPass == null) {
				SystemConsole console = new SystemConsole();
				console.writeLine("");
				if (dbRootUser == null) {
					dbRootUser = console.readLine(
							"Database root account username used to create tigase user and database at " + dbHostname +" : ");
				}
				if (dbRootPass == null) {
					dbRootPass = new String(console.readPassword(
							"Database root account password used to create tigase user and database at " + dbHostname + " : "));
				}
			}
		}

		private static String getProperty(Properties props, PARAMETERS_ENUM param) {
			return props.getProperty(param.getName(), param.getDefaultValue());
		}

		private static <T> T getProperty(Properties props, PARAMETERS_ENUM param, Function<String, T> converter) {
			String tmp = getProperty(props, param);
			if (tmp == null) {
				return null;
			}
			return converter.apply(tmp);
		}

		@Override
		public void setAdmins(List<BareJID> admins, String password) {
			this.admins = admins;
			this.adminPassword = password;
		}

		@Override
		public void setDbRootCredentials(String username, String password) {
			this.dbRootUser = username;
			this.dbRootPass = password;
			if (this.dbRootUser == null && this.dbRootPass == null) {
				this.dbRootUser = this.dbUser;
				this.dbRootPass = this.dbPass;
			}
		}

		public List<CommandlineParameter> getSetupOptions() {
			List<CommandlineParameter> options = new ArrayList<>();
			options.add(new CommandlineParameter.Builder("D", PARAMETERS_ENUM.DATABASE_NAME.getName()).description(
					"Name of the database that will be created and to which schema will be loaded")
									 .defaultValue(PARAMETERS_ENUM.DATABASE_NAME.getDefaultValue())
									 .required(true)
									 .build());
			options.add(new CommandlineParameter.Builder("H", PARAMETERS_ENUM.DATABASE_HOSTNAME.getName()).description(
					"Address of the database instance")
									 .defaultValue(PARAMETERS_ENUM.DATABASE_HOSTNAME.getDefaultValue())
									 .required(true)
									 .build());
			options.add(new CommandlineParameter.Builder("U", PARAMETERS_ENUM.TIGASE_USERNAME.getName()).description(
					"Name of the user that will be created specifically to access Tigase XMPP Server")
									 .defaultValue(PARAMETERS_ENUM.TIGASE_USERNAME.getDefaultValue())
									 .required(true)
									 .build());
			options.add(new CommandlineParameter.Builder("P", PARAMETERS_ENUM.TIGASE_PASSWORD.getName()).description(
					"Password of the user that will be created specifically to access Tigase XMPP Server")
									 .defaultValue(PARAMETERS_ENUM.TIGASE_PASSWORD.getDefaultValue())
									 .required(true)
									 .secret()
									 .build());
			options.add(new CommandlineParameter.Builder("R", PARAMETERS_ENUM.ROOT_USERNAME.getName()).description(
					"Database root account username used to create tigase user and database")
									 .defaultValue(PARAMETERS_ENUM.ROOT_USERNAME.getDefaultValue())
									 .required(true)
									 .build());
			options.add(new CommandlineParameter.Builder("A", PARAMETERS_ENUM.ROOT_PASSWORD.getName()).description(
					"Database root account password used to create tigase user and database")
									 .defaultValue(PARAMETERS_ENUM.ROOT_PASSWORD.getDefaultValue())
									 .secret()
									 .required(true)
									 .build());
			options.add(new CommandlineParameter.Builder("S", PARAMETERS_ENUM.USE_SSL.getName()).description(
					"Enable SSL support for database connection (if database supports it)")
									 .requireArguments(false)
									 .defaultValue(PARAMETERS_ENUM.USE_SSL.getDefaultValue())
									 .type(Boolean.class)
									 .build());
			return options;
		}

		public Boolean getIngoreMissingFiles() {
			return ingoreMissingFiles;
		}

		public void setIngoreMissingFiles(Boolean ingoreMissingFiles) {
			this.ingoreMissingFiles = ingoreMissingFiles;
		}
	}

	public interface SQLCommand<C, R> {

		R execute(C conn) throws SQLException, IOException;

	}

	public interface ExceptionHandler<T extends Exception, R> {
		R handleException(T ex);
	}
}
