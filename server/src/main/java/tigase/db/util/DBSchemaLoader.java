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
import tigase.db.TigaseDBException;
import tigase.util.LogFormatter;
import tigase.util.ui.console.CommandlineParameter;
import tigase.util.ui.console.ParameterParser;
import tigase.xmpp.BareJID;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
class DBSchemaLoader extends SchemaLoader {

	/** Denotes whether there wasn't any problem establishing connection to the
	 * database */
	private boolean connection_ok = false;
	/** Denotes whether database exists */
	private boolean db_ok = false;
	/** Denotes whether schema has proper version */
	private boolean schema_ok = false;
	/** Holds map of all replacement variables */
	private Map<String, String> replacementMap = new HashMap<String, String>();
	private static final Logger log = Logger.getLogger( DBSchemaLoader.class.getCanonicalName() );
	// queries
	public static final String JDBC_CHECKUSERTABLE_QUERY = "select count(*) from tig_users";
	public static final String JDBC_GETSCHEMAVER_QUERY = "select TigGetDBProperty('schema-version')";
	public static final String DERBY_GETSCHEMAVER_QUERY = "values TigGetDBProperty('schema-version')";
	public static final String SQLSERVER_GETSCHEMAVER_QUERY = "select dbo.TigGetDBProperty('schema-version')";

	enum SQL_LOAD_STATE {

		INIT, IN_SQL;
	}

	enum PARAMETERS {
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
		QUERY("query",null),
		FILE("file",null),
		ADMIN_JID("adminJID",null),
		ADMIN_JID_PASS("adminJIDpass",null),
		IGNORE_MISSING_FILES("ignoreMissingFiles", "false");

		private String name = null;
		private String defaultValue = null;

		PARAMETERS(String name, String defaultValue) {
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

	/**
	 * Constructs {@link DBSchemaLoader} and set default values for missing
	 * properties - for the complete list see {@link DBSchemaLoader}
	 * documentation.
	 *
	 * @param props user defined {@link Properties}
	 */
	public DBSchemaLoader( Properties props ) {

		for (PARAMETERS p : PARAMETERS.values()) {
			final String property = props.getProperty(p.getName(), p.getDefaultValue());
			if (null != property || null != p.getDefaultValue()) {
				replacementMap.put("${" + p.getName() + "}", property);
			}
		}

		// configure logger
		Level lvl = Level.parse( String.valueOf( props.getProperty( PARAMETERS.LOG_LEVEL.getName(), PARAMETERS.LOG_LEVEL.getDefaultValue() ) ) );

		System.out.println( "LogLevel: " + lvl );

		Handler handler = new ConsoleHandler();
		handler.setLevel( lvl );
		handler.setFormatter( new LogFormatter() );
		log.setUseParentHandlers( false );
		log.addHandler( handler );
		log.setLevel( lvl );

		log.log(Level.CONFIG, "Properties: {0}", new Object[]{props});

	}

	/**
	 * Main method allowing pass arguments to the class and setting all logging to
	 * be printed to console.
	 *
	 * @param args key-value (in the form of {@code "-<variable> value"})
	 *             parameters.
	 */
	public static void main( String[] args ) {
		ParameterParser parser = new ParameterParser(true);

		parser.addOption(new CommandlineParameter.Builder("T", PARAMETERS.DATABASE_TYPE.getName()).description(
				"Database server type")
				                 .options("derby", "mysql", "postgresql", "sqlserver")
				                 .defaultValue(PARAMETERS.DATABASE_TYPE.getDefaultValue())
				                 .required(true)
				                 .build());
		parser.addOption(new CommandlineParameter.Builder("V", PARAMETERS.SCHEMA_VERSION.getName()).description(
				"Intended version of the schema to be loaded")
				                 .options("4", "5", "5-1", "7-1", "7-2")
				                 .required(true)
				                 .defaultValue(PARAMETERS.SCHEMA_VERSION.getDefaultValue())
				                 .build());
//		parser.addOption(new CommandlineParameter.Builder("V", PARAMETERS.COMPONENTS.getName()).description(
//				"Comma-separated list of components for which schema should be loaded")
//				                 .options("message-archiving","pubsub","muc","sock5","unified-archive")
//				                 .defaultValue(PARAMETERS.COMPONENTS.getDefaultValue())
//				                 .build());
		parser.addOption(new CommandlineParameter.Builder("D", PARAMETERS.DATABASE_NAME.getName()).description(
				"Name of the database that will be created and to which schema will be loaded")
				                 .defaultValue(PARAMETERS.DATABASE_NAME.getDefaultValue())
				                 .required(true)
				                 .build());
		parser.addOption(new CommandlineParameter.Builder("H", PARAMETERS.DATABASE_HOSTNAME.getName()).description(
				"Address of the database instance")
				                 .defaultValue(PARAMETERS.DATABASE_HOSTNAME.getDefaultValue())
				                 .required(true)
				                 .build());
		parser.addOption(new CommandlineParameter.Builder("U", PARAMETERS.TIGASE_USERNAME.getName()).description(
				"Name of the user that will be created specifically to access Tigase XMPP Server")
				                 .defaultValue(PARAMETERS.TIGASE_USERNAME.getDefaultValue())
				                 .required(true)
				                 .build());
		parser.addOption(new CommandlineParameter.Builder("P", PARAMETERS.TIGASE_PASSWORD.getName()).description(
				"Password of the user that will be created specifically to access Tigase XMPP Server")
				                 .defaultValue(PARAMETERS.TIGASE_PASSWORD.getDefaultValue())
				                 .required(true)
				                 .secret()
				                 .build());
		parser.addOption(new CommandlineParameter.Builder("R", PARAMETERS.ROOT_USERNAME.getName()).description(
				"Database root account username used to create tigase user and database")
				                 .defaultValue(PARAMETERS.ROOT_USERNAME.getDefaultValue())
				                 .required(true)
				                 .build());
		parser.addOption(new CommandlineParameter.Builder("A", PARAMETERS.ROOT_PASSWORD.getName()).description(
				"Database root account password used to create tigase user and database")
				                 .defaultValue(PARAMETERS.ROOT_PASSWORD.getDefaultValue())
				                 .secret()
				                 .required(true)
				                 .build());
		parser.addOption(new CommandlineParameter.Builder("F", PARAMETERS.FILE.getName()).description(
				"Comma separated list of SQL files that will be processed").build());
		parser.addOption(new CommandlineParameter.Builder("L", PARAMETERS.LOG_LEVEL.getName()).description(
				"Java Logger level during loading process")
				                 .defaultValue(PARAMETERS.LOG_LEVEL.getDefaultValue())
				                 .build());
		parser.addOption(new CommandlineParameter.Builder("S", PARAMETERS.USE_SSL.getName()).description(
				"Enable SSL support for database connection (if database supports it)")
				                 .requireArguments(false)
				                 .defaultValue(PARAMETERS.USE_SSL.getDefaultValue())
				                 .build());
		parser.addOption(new CommandlineParameter.Builder("J", PARAMETERS.ADMIN_JID.getName()).description(
				"Comma separated list of administrator JID(s)").build());
		parser.addOption(new CommandlineParameter.Builder("N", PARAMETERS.ADMIN_JID_PASS.getName()).description(
				"Password that will be used for the entered JID(s) - one for all configured administrators")
				                 .secret()
				                 .build());
		parser.addOption(new CommandlineParameter.Builder(null, PARAMETERS.IGNORE_MISSING_FILES.getName()).description(
				"Force ignoring missing files errors")
								 .defaultValue(PARAMETERS.IGNORE_MISSING_FILES.getDefaultValue())
								 .build());

		Properties properties = null;
		if (null == args || args.length == 0 || (properties = parser.parseArgs(args)) == null) {
			System.out.println(parser.getHelp());
			System.exit(0);
		} else {
			System.out.println("properties: " + properties);
		}

		DBSchemaLoader dbHelper = new DBSchemaLoader(properties);

//		Scanner sc = new Scanner(System.in);
//		log.log(Level.ALL, "Please press any key to continue");
//		sc.nextLine();

		DBSchemaLoader.execute(dbHelper, properties);
	}

	/**
	 * Executes set of {@link TigaseDBTask} tasks selected based on set on passed
	 * properties
	 *
	 * @param helper {@link DBSchemaLoader} for which tasks will be executed.
	 * @param props  set of configuration properties.
	 */
	private static void execute( DBSchemaLoader helper, Properties props ) {

		// Get list of appropriate task and execute them;
		TigaseDBTask[] tasks;
		if ( props.getProperty( PARAMETERS.QUERY.getName() ) != null ){
			tasks = Tasks.getQueryTasks();
		} else if ( props.getProperty( PARAMETERS.FILE.getName() ) != null ){
			tasks = Tasks.getSchemaTasks();
		} else {
			tasks = Tasks.getTasksInOrder();
		}
		for ( TigaseDBTask task : tasks ) {
			task.execute( helper, props );
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
	 * @param res_prefix prefix of the resource denoting type of the database.
	 * @param variables  set of {@code Properties} with all configuration options.
	 * @return
	 * @throws IOException
	 */
	private ArrayList<String> loadSQLQueries( String resource, String res_prefix, Properties variables )
			throws IOException {
		log.log(Level.FINER, "Loading queries, resource: {0}, res_prefix: {1} ", new Object[]{resource, res_prefix});
		ArrayList<String> results = new ArrayList<>();
		boolean path = res_prefix == null;

		final Path p = Paths.get(path ? resource : "database/" + resource + ".sql");

		if (!Files.exists(p)) {
			if ("true".equals(variables.getProperty(PARAMETERS.IGNORE_MISSING_FILES.getName(), PARAMETERS.IGNORE_MISSING_FILES.getDefaultValue()))) {
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
					if ( line.startsWith( "-- LOAD SCHEMA:" ) ){
						results.addAll( loadSchemaQueries( variables ) );
					}
					if (  line.startsWith( "-- LOAD FILE:" )  && line.trim().contains( "sql" ) )
					{
						Matcher matcher = Pattern.compile( "-- LOAD FILE:\\s*(.*\\.sql)" ).matcher( line );
						if ( matcher.find() ){
							log.log(Level.FINE, "\n\n *** trying to load schema: {0} \n",
							        new Object[]{matcher.group(1)});
							results.addAll( loadSQLQueries(  matcher.group( 1 ), null, variables ) );
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

	/**
	 * Load all queries from all schema files necessary to run Tigase XMPP Server
	 * (which includes stored procedures and properties). Version and database
	 * type is determined from properties.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 *
	 * @return An {@code ArrayList} with all queries from all schema files.
	 *
	 * @throws IOException
	 */
	private ArrayList<String> loadSchemaQueries( Properties variables )
			throws IOException {

		String res_prefix = variables.getProperty( PARAMETERS.DATABASE_TYPE.getName() );
		String version = variables.getProperty( PARAMETERS.SCHEMA_VERSION.getName() );
		ArrayList<String> queries = new ArrayList<>();
		queries.addAll( loadSQLQueries( res_prefix + "-schema-" + version + "-schema", res_prefix, variables ) );
		queries.addAll( loadSQLQueries( res_prefix + "-schema-" + version + "-sp", res_prefix, variables ) );
		queries.addAll( loadSQLQueries( res_prefix + "-schema-" + version + "-props", res_prefix, variables ) );
		log.log(Level.FINE, "Loading schema queries: {0} // {1}",
		        new Object[]{queries, queries.toArray()});
		return queries;
	}

	@Override
	public Result validateDBConnection( Properties variables ) {
		connection_ok = false;
		String db_conn = getDBUri( variables, false, true );
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

	@Override
	public Result shutdown( Properties variables ) {
		return shutdownDerby(variables);
	}
	
	public Result shutdownDerby( Properties variables ) {
		String db_conn = getDBUri( variables, false, true );
		String database = variables.getProperty( PARAMETERS.DATABASE_TYPE.getName() );
		if ( "derby".equals( database ) ){
			log.log( Level.INFO, "Validating DBConnection, URI: " + db_conn );
			if ( db_conn == null ){
				log.log( Level.WARNING, "Missing DB connection URL" );
			} else {
				db_conn += ";shutdown=true";
				try ( Connection conn = DriverManager.getConnection( db_conn ) ) {
					Enumeration<Driver> drivers = DriverManager.getDrivers();
					ArrayList<String> availableDrivers = new ArrayList<>();
					while ( drivers.hasMoreElements() ) {
						availableDrivers.add( drivers.nextElement().toString() );
					}
					log.log( Level.CONFIG, "DriverManager (available drivers): " +  availableDrivers ) ;
					conn.close();
					connection_ok = true;
					log.log( Level.INFO, "Connection OK" );
				} catch ( SQLException e ) {
					log.log( Level.WARNING, e.getMessage() );
				}
			}
		}
		return Result.ok;
	}

	@Override
	public Result validateDBExists( Properties variables ) {
		if ( !connection_ok ){
			log.log( Level.WARNING, "Connection not validated" );
			return Result.error;
		}

		String res_prefix = variables.getProperty( PARAMETERS.DATABASE_TYPE.getName() );
		db_ok = false;
		String db_conn = getDBUri( variables, true, false );
		log.log( Level.INFO, "Validating whether DB Exists, URI: " + db_conn );
		if ( db_conn == null ){
			log.log( Level.WARNING, "Missing DB connection URL" );
			return Result.error;
		} else {
			try
				( Connection conn = DriverManager.getConnection( db_conn ) ) {
				conn.close();
				db_ok = true;
				log.log( Level.INFO, "Exists OK" );
				return Result.ok;
			} catch ( SQLException e ) {
				log.log( Level.INFO, "Doesn't exist, creating..." );

				db_conn = getDBUri( variables, false, true );
			
				try
					(Connection conn = DriverManager.getConnection( db_conn ) ) {
					Result result = Result.ok;
					ArrayList<String> queries = loadSQLQueries( res_prefix + "-installer-create-db", res_prefix, variables );
					for ( String query : queries ) {
						log.log( Level.FINE, "Executing query: " + query );
						if ( !query.isEmpty() ){
							// Some queries may fail and this is still fine
							// the user or the database may already exist
							try ( Statement stmt = conn.createStatement() ) {
								stmt.execute( query );
								stmt.close();
							} catch ( SQLException ex ) {
								result = Result.warning;
								log.log( Level.WARNING, "Query failed: " + ex.getMessage() );
							}
						}
					}
					conn.close();
					log.log( Level.INFO, " OK" );
					db_ok = true;
					return result;
				} catch ( SQLException | IOException ex ) {
					log.log( Level.WARNING, ex.getMessage() );
					return Result.error;
				}
			}
		}
	}

	/**
	 * Method, if the connection is validated by {@code validateDBConnection} and
	 * database exists, checks if the schema exists and has correct version (one
	 * which we want to load). If not - performs full schema load.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	public Result validateDBSchema( Properties variables ) {
		if ( !connection_ok ){
			log.log( Level.WARNING, "Connection not validated" );
			return Result.error;
		}
		if ( !db_ok ){
			log.log( Level.WARNING, "Database not validated" );
			return Result.error;
		}
		/* Denotes whether schema exists */
		boolean schema_exists = false;
		schema_ok = false;
		String db_conn = getDBUri( variables, true, false );
		log.log( Level.INFO, "Validating DBSchema, URI: " + db_conn );
		long users = 0;
		try {
			try ( Connection conn = DriverManager.getConnection( db_conn ) ;
						Statement stmt = conn.createStatement() ) {

				String query = JDBC_CHECKUSERTABLE_QUERY;

				ResultSet rs = stmt.executeQuery( query );
				if ( rs.next() ){
					users = rs.getLong( 1 );
					schema_exists = true;
					log.log( Level.INFO, "Schema exists, users: " + users );
				}

				String schema_ver_query = JDBC_GETSCHEMAVER_QUERY;
				if ( db_conn.startsWith( "jdbc:sqlserver" ) || db_conn.startsWith( "jdbc:jtds:sqlserver" ) ){
					schema_ver_query = SQLSERVER_GETSCHEMAVER_QUERY;
					if ( db_conn.startsWith( "jdbc:derby" ) ){
						schema_ver_query = DERBY_GETSCHEMAVER_QUERY;
					}
				}

				query = schema_ver_query;
				String loadedSchemaVersion = variables.getProperty( PARAMETERS.SCHEMA_VERSION.getName() ).replace( "-", "." );
				rs = stmt.executeQuery( query );
				if ( rs.next() ){
					String schema_version = rs.getString( 1 );
					if ( loadedSchemaVersion.equals( schema_version ) ){
						schema_ok = true;
					}
				}
			}
		} catch ( SQLException e ) {
			log.log( Level.WARNING, "Exception, posibly schema hasn't been loaded yet.");
		}
		if ( schema_ok ){
			log.log( Level.INFO, "Schema OK, accounts number: " + users );
			return Result.ok;
		}
		if ( !schema_exists){
			db_conn = getDBUri( variables, true, true );
			log.log( Level.INFO, "DB schema doesn't exists, creating one..., URI: " + db_conn );
			try {
				try ( Connection conn = DriverManager.getConnection( db_conn ) ;
							Statement stmt = conn.createStatement() ) {
					ArrayList<String> queries = loadSchemaQueries( variables );

					for ( String query : queries ) {
						if ( !query.isEmpty() ){
							log.log( Level.FINEST, "Executing query: " + query );
							stmt.execute( query );
						}
					}
				}
				schema_ok = true;
				log.log( Level.INFO, "New schema loaded OK" );
				return Result.ok;
			} catch ( SQLException | IOException ex ) {
				log.log( Level.WARNING, "Can't load schema: " + ex.getMessage() );
				return Result.error;
			}
		} else {
			log.log( Level.INFO, "Old schema, accounts number: " + users );
			return Result.warning;
		}
	}

	/**
	 * Method performs post-installation action using using
	 * {@code *-installer-post.sql} schema file substituting it's variables with
	 * ones provided.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	@Override
	public Result postInstallation( Properties variables ) {
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
		String db_conn = getDBUri( variables, true, true );
		String res_prefix = variables.getProperty( PARAMETERS.DATABASE_TYPE.getName() );
		log.log( Level.INFO, "Post Installation, URI: " + db_conn );
		try {
			log.log( Level.INFO, "Finalizing..." );
			try ( Connection conn = DriverManager.getConnection( db_conn ) ;
						Statement stmt = conn.createStatement() ) {

				ArrayList<String> queries = loadSQLQueries( res_prefix + "-installer-post", res_prefix, variables );
				for ( String query : queries ) {
					if ( !query.isEmpty() ){
						log.log( Level.FINEST, "Executing query: " + query );
						stmt.execute( query );
					}
				}
			}
			schema_ok = true;
			log.log( Level.INFO, " completed OK" );
			return Result.ok;
		} catch ( SQLException | IOException ex ) {
			log.log( Level.WARNING, "Can't finalize: " + ex.getMessage() );
			return Result.error;
		}
	}

	/**
	 * Method performs post-installation action using using
	 * {@code *-installer-post.sql} schema file substituting it's variables with
	 * ones provided.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	@Override
	public Result printInfo( Properties variables ) {
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

		String db_conn = getDBUri( variables, true, false );
		String database = variables.getProperty( PARAMETERS.DATABASE_TYPE.getName() );

		switch ( database ) {
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
	public Result addXmppAdminAccount( Properties variables ) {
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
		Object admins = variables.getProperty( PARAMETERS.ADMIN_JID.getName() );
		Set<BareJID> jids = new LinkedHashSet<>();
		if ( admins != null ){
			String[] adminsStr = admins.toString().split( "," );
			for ( String adminStr : adminsStr ) {
				String jid = adminStr.trim();
				if ( jid != null && !jid.equals( "" ) ){
					jids.add( BareJID.bareJIDInstanceNS( jid ) );
				}
			}
		}
		if ( jids.size() < 1 ){
			log.log( Level.WARNING, "Error: No admin users entered" );
			return Result.warning;
		}

		Object pwdObj = variables.getProperty( PARAMETERS.ADMIN_JID_PASS.getName() );
		if ( pwdObj == null ){
			log.log( Level.WARNING, "Error: No admin password enetered" );
			return Result.warning;
		}
		String pwd = pwdObj.toString();

		String dbUri = getDBUri( variables, true, true );
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
	public Result loadSchemaFile( Properties variables ) {

		// part 1, check db preconditions
		if ( !connection_ok ){
			log.log( Level.INFO, "Connection not validated" );
			return Result.error;
		}
		if ( !db_ok ){
			log.log( Level.INFO, "Database not validated" );
			return Result.error;
		}

		Object fileNameObj = variables.getProperty( PARAMETERS.FILE.getName() );
		if ( fileNameObj == null ){
			log.log( Level.WARNING, "Error: empty query" );
			return Result.error;
		}

		String[] fileName = fileNameObj.toString().split(",");

		String db_conn = getDBUri( variables, true, true );
		log.log(Level.INFO,
		        String.format("Loading schema from file(s): %1$s, URI: %2$s", Arrays.toString(fileName), db_conn));
		try {
			try ( Connection conn = DriverManager.getConnection( db_conn ) ;
						Statement stmt = conn.createStatement() ) {

				ArrayList<String> queries = new ArrayList<>();
				for (String file : fileName) {
					queries.addAll(loadSQLQueries(file, null, variables));
				}
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
			}
			schema_ok = true;
			log.log( Level.INFO, " completed OK" );
			return Result.ok;
		} catch ( SQLException | IOException | NullPointerException ex ) {
			log.log( Level.WARNING, "Can't finalize: " + ex.getMessage() );
			return Result.error;
		}
	}

	/**
	 * Method checks whether the connection to the database is possible and that
	 * database of specified name exists. If yes then a single query is executed.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	protected void executeSingleQuery( Properties variables ) {
		// part 1, check db preconditions
		if ( !connection_ok ){
			log.log( Level.INFO, "Connection not validated" );
			return;
		}
		if ( !db_ok ){
			log.log( Level.INFO, "Database not validated" );
			return;
		}

		Object queryObj = variables.getProperty( PARAMETERS.QUERY.getName() );
		if ( queryObj == null ){
			log.log( Level.WARNING, "Error: empty query" );
			return;
		}
		String simpleQuery = queryObj.toString();

		String db_conn = getDBUri( variables, true, false );
		log.log( Level.INFO, "Executing Simple Query, URI: " + db_conn );
		if ( db_conn == null ){
			log.log( Level.WARNING, "Missing DB connection URL" );
		} else {
			db_conn = getDBUri( variables, false, true );
			try {
				try ( Connection conn = DriverManager.getConnection( db_conn ) ;
							Statement stmt = conn.createStatement() ) {
					ArrayList<String> queries = new ArrayList<>();
					queries.add( simpleQuery );
					for ( String query : queries ) {
						log.log( Level.FINEST, "Executing query: " + query );
						if ( !query.isEmpty() ){
							try {
								stmt.execute( query );
								stmt.close();
							} catch ( SQLException ex ) {
								log.log( Level.WARNING, "Query failed: " + query + ", " + ex.getMessage() );
							}
						}
					}
				}
				log.log( Level.INFO, " OK" );
				db_ok = true;
			} catch ( Exception ex ) {
				log.log( Level.WARNING, ex.getMessage() );
			}
		}
	}

	/**
	 * Helper method used to generate proper database URI depending on properties.
	 *
	 * @param props              set of {@code Properties} with all configuration
	 *                           options
	 * @param includeDbName      configure whether to include database name in the
	 *                           URI
	 * @param useRootCredentials whether to put in the URI credentials of database
	 *                           administrator ({@code true}) or regular user.
	 * @return
	 */
	private static String getDBUri( Properties props, boolean includeDbName, boolean useRootCredentials ) {
		String db_uri = "jdbc:";
		String database = props.getProperty( PARAMETERS.DATABASE_TYPE.getName() );
		String USERNAME;
		String PASSWORD;
		if ( useRootCredentials ){
			USERNAME = PARAMETERS.ROOT_USERNAME.getName();
			PASSWORD = PARAMETERS.ROOT_PASSWORD.getName();
		} else {
			USERNAME = PARAMETERS.TIGASE_USERNAME.getName();
			PASSWORD = PARAMETERS.TIGASE_USERNAME.getName();
		}

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
				db_uri += props.getProperty( PARAMETERS.DATABASE_NAME.getName() ) + ";create=true";
				break;
			case "sqlserver":
				db_uri += "//" + props.getProperty( PARAMETERS.DATABASE_HOSTNAME.getName() );
				if ( includeDbName ){
					db_uri += ";databaseName=" + props.getProperty( PARAMETERS.DATABASE_NAME.getName() );
				}
				db_uri += ";user=" + props.getProperty( USERNAME );
				if ( props.getProperty( PASSWORD ) != null
						 && !props.getProperty( PASSWORD ).isEmpty() ){
					db_uri += ";password=" + props.getProperty( PASSWORD );
				}
				db_uri += ";schema=dbo";
				db_uri += ";lastUpdateCount=false";
				db_uri += ";cacheMetaData=false";
				if ( Boolean.valueOf(props.getProperty(PARAMETERS.USE_SSL.getName())) ) {
					db_uri += ";encrypt=true";
				}
				break;
			default:
				db_uri += "//" + props.getProperty( PARAMETERS.DATABASE_HOSTNAME.getName() ) + "/";
				if ( includeDbName ){
					db_uri += props.getProperty( PARAMETERS.DATABASE_NAME.getName() );
				} else if (database.equals( "postgresql")) {
					db_uri +=  "postgres" ;
				}
				db_uri += "?user=" + props.getProperty( USERNAME );
				if ( props.getProperty( PASSWORD ) != null
						 && !props.getProperty( PASSWORD ).isEmpty() ){
					db_uri += "&password=" + props.getProperty( PASSWORD );
				}
				if ( Boolean.valueOf(props.getProperty(PARAMETERS.USE_SSL.getName())) ) {
					db_uri += "&useSSL=true";
				}
				else if ( props.getProperty(PARAMETERS.USE_SSL.getName()) != null ) {
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
			public void execute( DBSchemaLoader helper, Properties variables ) {
				helper.validateDBConnection( variables );
			}
		},
		VALIDATE_DB_EXISTS( "Checking if the database exists" ) {
			@Override
			public void execute( DBSchemaLoader helper, Properties variables ) {
				helper.validateDBExists( variables );
			}
		},
		VALIDATE_DB_SCHEMA( "Checking the database schema" ) {
			@Override
			public void execute( DBSchemaLoader helper, Properties variables ) {
				helper.validateDBSchema( variables );
			}
		},
		ADD_ADMIN_XMPP_ACCOUNT( "Adding XMPP admin accounts" ) {
			@Override
			public void execute( DBSchemaLoader helper, Properties variables ) {
				helper.addXmppAdminAccount( variables );
			}
		},
		EXECUTE_SIMPLE_QUERY( "Executing simple single query" ) {
			@Override
			public void execute( DBSchemaLoader helper, Properties variables ) {
				helper.executeSingleQuery( variables );
			}
		},
		LOAD_SCHEMA_FILE( "Loading schema file from provided file" ) {
			@Override
			public void execute( DBSchemaLoader helper, Properties variables ) {
				helper.loadSchemaFile( variables );
			}
		},
		POST_INSTALLATION( "Post installation actions" ) {
			@Override
			public void execute( DBSchemaLoader helper, Properties variables ) {
				helper.postInstallation( variables );
			}
		},
		SHUTDOWN_DATABASE( "Shutting Down Database" ) {
			@Override
			public void execute( DBSchemaLoader helper, Properties variables ) {
				helper.shutdownDerby( variables );
			}
		},
		PRINT_INFO_TASK( "Database Configuration Details" ) {
			@Override
			public void execute( DBSchemaLoader helper, Properties variables ) {
				helper.printInfo( variables );
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
			return new TigaseDBTask[] { VALIDATE_CONNECTION, VALIDATE_DB_EXISTS, LOAD_SCHEMA_FILE, SHUTDOWN_DATABASE, PRINT_INFO_TASK };
		}

		public static TigaseDBTask[] getQueryTasks() {
			return new TigaseDBTask[] { VALIDATE_CONNECTION, VALIDATE_DB_EXISTS, EXECUTE_SIMPLE_QUERY, SHUTDOWN_DATABASE, PRINT_INFO_TASK };
		}
	}

	static interface TigaseDBTask {

		String getDescription();

		abstract void execute( DBSchemaLoader helper, Properties variables );
	}
}
