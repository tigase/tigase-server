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
package tigase.util;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.*;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.AuthRepository;
import tigase.xmpp.BareJID;

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
 * are: {@code 5-1}, {@code 5}, {@code 4};
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
class DBSchemaLoader {

	/** Denotes whether there wasn't any problem establishing connection to the
	 * database */
	private boolean connection_ok = false;
	/** Denotes whether database exists */
	private boolean db_ok = false;
	/** Denotes whether schema has proper version */
	private boolean schema_ok = false;
	/** Denotes whether schema exists */
	private boolean schema_exists = false;
	/** Holds map of all replacement variables */
	private Map<String, String> replacementMap = new HashMap<String, String>();
	private static final Logger log = Logger.getLogger( DBSchemaLoader.class.getCanonicalName() );
	// drivers
	public static final String PGSQL_DRIVER = "org.postgresql.Driver";
	public static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
	public static final String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String SQLSERVER_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
	// queries
	public static final String JDBC_CHECKUSERTABLE_QUERY = "select count(*) from tig_users";
	public static final String JDBC_GETSCHEMAVER_QUERY = "select TigGetDBProperty('schema-version')";
	public static final String DERBY_GETSCHEMAVER_QUERY = "values TigGetDBProperty('schema-version')";
	public static final String SQLSERVER_GETSCHEMAVER_QUERY = "select dbo.TigGetDBProperty('schema-version')";
	private String schema_ver_query = JDBC_GETSCHEMAVER_QUERY;
	// keys
	public static final String DATABASE_TYPE_KEY = "dbType";
	public static final String SCHEMA_VERSION_KEY = "schemaVersion";
	public static final String DATABASE_NAME_KEY = "dbName";
	public static final String DATABASE_HOSTNAME_KEY = "dbHostname";
	public static final String TIGASE_USERNAME_KEY = "dbUser";
	public static final String TIGASE_PASSWORD_KEY = "dbPass";
	public static final String ROOT_USERNAME_KEY = "rootUser";
	public static final String ROOT_PASSWORD_KEY = "rootPass";
	public static final String QUERY_KEY = "query";
	public static final String FILE_KEY = "file";
	public static final String ADMIN_JID_KEY = "adminJID";
	public static final String ADMIN_JID_PASS_KEY = "adminJIDpass";
	public static final String LOG_LEVEL_KEY = "logLevel";
	public static final String DASH = "-";
	// defaults
	public static final String DATABASE_TYPE_DEF = "mysql";
	public static final String SCHEMA_VERSION_DEF = "5-1";
	public static final String DATABASE_NAME_DEF = "tigasedb";
	public static final String DATABASE_HOSTNAME_DEF = "localhost";
	public static final String TIGASE_USERNAME_DEF = "tigase_user";
	public static final String TIGASE_PASSWORD_DEF = "tigase_pass";
	public static final String ROOT_USERNAME_DEF = "root";
	public static final String ROOT_PASSWORD_DEF = "root";
	public static final String LOG_LEVEL_DEF = "CONFIG";

	enum SQL_LOAD_STATE {

		INIT, IN_SQL;
	}

	/**
	 * Constructs {@link DBSchemaLoader} and set default values for missing
	 * properties - for the complete list see {@link DBSchemaLoader}
	 * documentation.
	 *
	 * @param props user defined {@link Properties}
	 */
	public DBSchemaLoader( Properties props ) {

		// set defaults
		if ( props.get( DATABASE_TYPE_KEY ) == null ){
			props.setProperty( DATABASE_TYPE_KEY, DATABASE_TYPE_DEF );
		}
		if ( props.get( SCHEMA_VERSION_KEY ) == null ){
			props.setProperty( SCHEMA_VERSION_KEY, SCHEMA_VERSION_DEF );
		}
		if ( props.get( DATABASE_NAME_KEY ) == null ){
			props.setProperty( DATABASE_NAME_KEY, DATABASE_NAME_DEF );
		}
		if ( props.get( DATABASE_HOSTNAME_KEY ) == null ){
			props.setProperty( DATABASE_HOSTNAME_KEY, DATABASE_HOSTNAME_DEF );
		}
		if ( props.get( TIGASE_USERNAME_KEY ) == null ){
			props.setProperty( TIGASE_USERNAME_KEY, TIGASE_USERNAME_DEF );
		}
		if ( props.get( TIGASE_PASSWORD_KEY ) == null ){
			props.setProperty( TIGASE_PASSWORD_KEY, TIGASE_PASSWORD_DEF );
		}
		if ( props.get( ROOT_USERNAME_KEY ) == null ){
			props.setProperty( ROOT_USERNAME_KEY, ROOT_USERNAME_DEF );
		}
		if ( props.get( ROOT_PASSWORD_KEY ) == null ){
			props.setProperty( ROOT_PASSWORD_KEY, ROOT_PASSWORD_DEF );
		}
		if ( props.get( LOG_LEVEL_KEY ) == null ){
			props.setProperty( LOG_LEVEL_KEY, LOG_LEVEL_DEF );
		}

		// convert all properties to replacement map which allow usage
		// of variables in schema files
		for ( String key : props.stringPropertyNames() ) {
			replacementMap.put( "${" + key + "}", props.get( key ).toString() );
		}

		// configure logger
		Level lvl = Level.parse( String.valueOf( props.get( LOG_LEVEL_KEY ) ) );

		System.out.println( "LogLevel: " + lvl );

		Handler handler = new ConsoleHandler();
		handler.setLevel( lvl );
		handler.setFormatter( new myFormatter() );
		log.setUseParentHandlers( false );
		log.addHandler( handler );
		log.setLevel( lvl );

		log.log( Level.CONFIG, String.format( "Properties: %1$s", Arrays.asList( props ) ) );
		
	}

	/**
	 * Main method allowing pass arguments to the class and setting all logging to
	 * be printed to console.
	 *
	 * @param args key-value (in the form of {@code "-<variable> value"})
	 *             parameters.
	 */
	public static void main( String[] args ) {

		Properties otherArgs = parseArgs( args );

		DBSchemaLoader dbHelper = new DBSchemaLoader( otherArgs );
		DBSchemaLoader.execute( dbHelper, otherArgs );
	}

	/**
	 * Executes set of {@link TigaseDBTask} tasks selected based on set on passed
	 * properties
	 *
	 * @param helper {@link DBSchemaLoader} for which tasks will be executed.
	 * @param props  set of configuration properties.
	 */
	private static void execute( DBSchemaLoader helper, Properties props ) {

		// Get list of appropriate takst and execute them;
		TigaseDBTask[] tasks;
		if ( props.getProperty( QUERY_KEY ) != null ){
			tasks = Tasks.getQueryTasks();
		} else if ( props.getProperty( FILE_KEY ) != null ){
			tasks = Tasks.getSchemaTasks();
		} else {
			tasks = Tasks.getTasksInOrder();
		}
		for ( TigaseDBTask task : tasks ) {
			task.execute( helper, props );
		}
	}

	/**
	 * Simple parser of command line arguments passed as parameter which converts
	 * them into key-value {@code Properties}. If the argument list is empty then
	 * a help outlining possible parameters is printed.
	 *
	 * @param args set of command line arguments.
	 * @return {@link Properties} object created from provided set of arguments.
	 */
	private static Properties parseArgs( String[] args ) {
		Properties props = new Properties();
		if ( args == null || args.length == 0 ){
			System.out.println( help() );
			System.exit( 0 );
		} else {
			for ( int i = 0 ; i < args.length ; i++ ) {
				switch ( args[i] ) {
					case DASH + DATABASE_TYPE_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( DATABASE_TYPE_KEY, args[i] );
						}
						break;
					case DASH + SCHEMA_VERSION_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( SCHEMA_VERSION_KEY, args[i] );
						}
						break;
					case DASH + DATABASE_NAME_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( DATABASE_NAME_KEY, args[i] );
						}
						break;
					case DASH + DATABASE_HOSTNAME_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( DATABASE_HOSTNAME_KEY, args[i] );
						}
						break;
					case DASH + TIGASE_USERNAME_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( TIGASE_USERNAME_KEY, args[i] );
						}
						break;
					case DASH + TIGASE_PASSWORD_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( TIGASE_PASSWORD_KEY, args[i] );
						}
						break;
					case DASH + ROOT_USERNAME_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( ROOT_USERNAME_KEY, args[i] );
						}
						break;
					case DASH + ROOT_PASSWORD_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( ROOT_PASSWORD_KEY, args[i] );
						}
						break;
					case DASH + FILE_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( FILE_KEY, args[i] );
						}
						break;
					case DASH + QUERY_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( QUERY_KEY, args[i] );
						}
						break;
					case DASH + ADMIN_JID_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( ADMIN_JID_KEY, args[i] );
						}
						break;
					case DASH + ADMIN_JID_PASS_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( ADMIN_JID_PASS_KEY, args[i] );
						}
						break;
					case DASH + LOG_LEVEL_KEY:
						if ( args.length > i + 1 ){
							i++;
							props.setProperty( LOG_LEVEL_KEY, args[i].toUpperCase() );
						}
						break;
				}
			}
		}
		return props;
	}

	/**
	 * Creates and returns list of possible parameters.
	 *
	 * @return {@code String} describing possible parameters.
	 */
	private static String help() {
		String help = "Usage:"
									+ "$ java -cp \"jars/*.jar\" tigase.util.DBSchemaLoader"
									+ "\n\t -" + DATABASE_TYPE_KEY + " database_type {derby, mysql, postgresql, sqlserver} "
									+ "\n\t[-" + SCHEMA_VERSION_KEY + " schema_version {4, 5, 5-1} ]"
									+ "\n\t[-" + DATABASE_NAME_KEY + " database_name]"
									+ "\n\t[-" + DATABASE_HOSTNAME_KEY + " database hostname]"
									+ "\n\t[-" + TIGASE_USERNAME_KEY + " tigase_username]"
									+ "\n\t[-" + TIGASE_PASSWORD_KEY + " tigase_userpass]"
									+ "\n\t[-" + ROOT_USERNAME_KEY + " database_root_username]"
									+ "\n\t[-" + ROOT_PASSWORD_KEY + " database_root_password]"
									+ "\n\t[-" + FILE_KEY + " path_to_sql_schema_file]"
									+ "\n\t[-" + QUERY_KEY + " sql_query_to_execute]"
									+ "\n\t[-" + LOG_LEVEL_KEY + " java logger Level]"
									+ "\n\t[-" + ADMIN_JID_KEY + " comma separated list of admin JIDs]"
									+ "\n\t[-" + ADMIN_JID_PASS_KEY + " password (one for all entered JIDs]"
									+ "\n\targuments take following precedent: query, file, whole schema"
									+ "\n\t(i.e. file superseeds schema execution, query superseeds file execution).";
		return help;
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
		log.log( Level.FINER,
						 String.format( "Loading queries, resource: %1$s, res_prefix: %2$s ", resource, res_prefix ) );
		ArrayList<String> results = new ArrayList<>();
		boolean path = res_prefix == null ? true : false;
		BufferedReader br = new BufferedReader( new InputStreamReader( getResource( resource, path ) ) );
		String line;
		String sql_query = "";
		SQL_LOAD_STATE state = SQL_LOAD_STATE.INIT;
		while ( ( line = br.readLine() ) != null ) {
			switch ( state ) {
				case INIT:
					if ( line.startsWith( "-- QUERY START:" ) ){
						sql_query = "";
						state = SQL_LOAD_STATE.IN_SQL;
					}
					if ( line.startsWith( "-- LOAD SCHEMA:" ) ){
						results.addAll( loadSchemaQueries( variables ) );
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
						String substitute = variableSubstitutor.substitute( sql_query, replacementMap );
						results.add( substitute );
					}
					if ( ( line.trim().startsWith( "source" ) || line.trim().startsWith( "run" )
								 || line.trim().startsWith( "\\i" ) ) && line.trim().contains( "sql" ) ){
						Matcher matcher = Pattern.compile( res_prefix + "-(.*).sql" ).matcher( line );
						if ( matcher.find() ){
							log.log( Level.FINEST,
											 String.format( "\n\n *** trying to load schema: %1$s \t", matcher.group( 1 ) ) );
							results.addAll( loadSQLQueries( res_prefix + DASH + matcher.group( 1 ), res_prefix, variables ) );
						}
						continue;
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
		br.close();
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

		String res_prefix = variables.get( DATABASE_TYPE_KEY ).toString();
		String version = variables.get( SCHEMA_VERSION_KEY ).toString();
		ArrayList<String> queries = new ArrayList<>();
		queries.addAll( loadSQLQueries( res_prefix + "-schema-" + version + "-schema", res_prefix, variables ) );
		queries.addAll( loadSQLQueries( res_prefix + "-schema-" + version + "-sp", res_prefix, variables ) );
		queries.addAll( loadSQLQueries( res_prefix + "-schema-" + version + "-props", res_prefix, variables ) );
		log.log( Level.FINE,
						 String.format( "Loading schema queries: %1$s // %2$s",
														Arrays.asList( queries ),
														queries.toArray() ) );
		return queries;
	}

	/**
	 * Created an {@code InputStream} for the desired resource.
	 *
	 * @param resource name of the resource for which an {@link InputStream}
	 *                 should be created, either excerpt of the name or a
	 *                 full/relative path to the schema {@code .sql} file.
	 * @param path     denotes whether passed resource name is a full path to the
	 *                 file ({@code true}) or a simple resource mapping
	 *                 ({@code false}).
	 * @return an {@code InputStream} for the desired resource.
	 */
	protected InputStream getResource( String resource, boolean path ) {
		File f;
		if ( path ){
			f = new File( resource );
		} else {
			f = new File( "database/" + resource + ".sql" );
		}
		InputStream is = null;
		try {
			is = new FileInputStream( f );
		} catch ( FileNotFoundException ex ) {
			Logger.getLogger( DBSchemaLoader.class.getName() ).log( Level.SEVERE, null, ex );
		}
		log.log( Level.FINEST,
						 String.format( "Getting resource: %1$s @ filename: %2$s",
														resource, f.getAbsolutePath() ) );
		return is;
	}

	/**
	 * Method validates whether the connection can at least be established. If yes
	 * then appropriate flag is set.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	public void validateDBConnection( Properties variables ) {
		connection_ok = false;
		String db_conn = getDBUri( variables, false, true );
		log.log( Level.INFO, "Validating DBConnection, URI: " + db_conn );
		if ( db_conn == null ){
			log.log( Level.WARNING, "Missing DB connection URL" );
		} else {
			try ( Connection conn = DriverManager.getConnection( db_conn ) ) {
				Enumeration<Driver> drivers = DriverManager.getDrivers();
				ArrayList<String> availableDrivers = new ArrayList<>();
				while ( drivers.hasMoreElements() ) {
					availableDrivers.add( drivers.nextElement().toString() );
				}
				log.log( Level.CONFIG, "DriverManager (available drivers): " + Arrays.asList( availableDrivers ) );
				conn.close();
				connection_ok = true;
				log.log( Level.INFO, "Connection OK" );
			} catch ( SQLException e ) {
				//e.printStackTrace();
				log.log( Level.WARNING, e.getMessage() );
			}
		}
	}

	public void shutdownDerby( Properties variables ) {
		String db_conn = getDBUri( variables, false, true );
		String database = variables.getProperty( DATABASE_TYPE_KEY );
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
					log.log( Level.CONFIG, "DriverManager (available drivers): " + Arrays.asList( availableDrivers ) );
					conn.close();
					connection_ok = true;
					log.log( Level.INFO, "Connection OK" );
				} catch ( SQLException e ) {
					//e.printStackTrace();
					log.log( Level.WARNING, e.getMessage() );
				}
			}
		}
	}

	/**
	 * Method, if the connection is validated by {@code validateDBConnection},
	 * checks whether desired database exists. If not it creates such database
	 * using {@code *-installer-create-db.sql} schema file substituting it's
	 * variables with ones provided.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	public void validateDBExists( Properties variables ) {
		if ( !connection_ok ){
			log.log( Level.WARNING, "Connection not validated" );
			return;
		}

		String res_prefix = variables.get( DATABASE_TYPE_KEY ).toString();
		db_ok = false;
		String db_conn = getDBUri( variables, true, false );
		log.log( Level.INFO, "Validating whether DB Exists, URI: " + db_conn );
		if ( db_conn == null ){
			log.log( Level.WARNING, "Missing DB connection URL" );
		} else {
			try
				( Connection conn = DriverManager.getConnection( db_conn ) ) {
				conn.close();
				db_ok = true;
				log.log( Level.INFO, "Exists OK" );
			} catch ( SQLException e ) {
				log.log( Level.INFO, "Doesn't exist, creating..." );

				db_conn = getDBUri( variables, false, true );
				try
					(Connection conn = DriverManager.getConnection( db_conn ) ) {
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
								log.log( Level.WARNING, "Query failed: " + ex.getMessage() );
							}
						}
					}
					conn.close();
					log.log( Level.INFO, " OK" );
					db_ok = true;
				} catch ( SQLException | IOException ex ) {
					log.log( Level.WARNING, ex.getMessage() );
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
	public void validateDBSchema( Properties variables ) {
		if ( !connection_ok ){
			log.log( Level.WARNING, "Connection not validated" );
			return;
		}
		if ( !db_ok ){
			log.log( Level.WARNING, "Database not validated" );
			return;
		}
		schema_exists = false;
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

				schema_ver_query = JDBC_GETSCHEMAVER_QUERY;
				if ( db_conn.startsWith( "jdbc:sqlserver" ) || db_conn.startsWith( "jdbc:jtds:sqlserver" ) ){
					schema_ver_query = SQLSERVER_GETSCHEMAVER_QUERY;
					if ( db_conn.startsWith( "jdbc:derby" ) ){
						schema_ver_query = DERBY_GETSCHEMAVER_QUERY;
					}
				}

				query = schema_ver_query;
				String loadedSchemaVersion = variables.get( SCHEMA_VERSION_KEY ).toString().replace( "-", "." );
				rs = stmt.executeQuery( query );
				if ( rs.next() ){
					String schema_version = rs.getString( 1 );
					if ( loadedSchemaVersion.equals( schema_version ) ){
						schema_ok = true;
					}
				}
			}
		} catch ( SQLException e ) {
			log.log( Level.INFO, "Exception, posibly schema hasn't been loaded yet.");
		}
		if ( schema_ok ){
			log.log( Level.INFO, "Schema OK, accounts number: " + users );
			return;
		}
		if ( !schema_exists ){
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
			} catch ( SQLException | IOException ex ) {
				log.log( Level.INFO, "Can't load schema: " + ex.getMessage() );
			}
		} else {
			log.log( Level.INFO, "Old schema, accounts number: " + users );
		}
	}

	/**
	 * Method performs post-installation action using using
	 * {@code *-installer-post.sql} schema file substituting it's variables with
	 * ones provided.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	public void postInstallation( Properties variables ) {
		// part 1, check db preconditions
		if ( !connection_ok ){
			log.log( Level.INFO, "Connection not validated" );
			return;
		}
		if ( !db_ok ){
			log.log( Level.INFO, "Database not validated" );
			return;
		}

		if ( !schema_ok ){
			log.log( Level.INFO, "Database schema is invalid" );
			return;
		}

		// part 2, acquire reqired fields and validate them
		String db_conn = getDBUri( variables, true, true );
		String res_prefix = variables.get( DATABASE_TYPE_KEY ).toString();
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
		} catch ( SQLException | IOException ex ) {
			log.log( Level.INFO, "Can't finalize: " + ex.getMessage() );
		}
	}

	/**
	 * Method attempts to add XMPP admin user account to the database using
	 * {@link AuthRepository}.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	protected void addXmppAdminAccount( Properties variables ) {
		// part 1, check db preconditions
		if ( !connection_ok ){
			log.log( Level.WARNING, "Connection not validated" );
			return;
		}
		if ( !db_ok ){
			log.log( Level.WARNING, "Database not validated" );
			return;
		}

		if ( !schema_ok ){
			log.log( Level.WARNING, "Database schema is invalid" );
			return;
		}

		// part 2, acquire reqired fields and validate them
		Object admins = variables.get( ADMIN_JID_KEY );
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
			return;
		}

		Object pwdObj = variables.get( ADMIN_JID_PASS_KEY );
		if ( pwdObj == null ){
			log.log( Level.WARNING, "Error: No admin password enetered" );
			return;
		}
		String pwd = pwdObj.toString();

		String dbUri = getDBUri( variables, true, true );
		log.log( Level.INFO, "Adding XMPP Admin Account, URI: " + dbUri );

		try {
			Map<String, String> params = new HashMap<>();
			params.put( RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY, String.valueOf( 1 ) );

			log.log( Level.CONFIG, "RepositoryFactory.getAuthRepository(" + null + ", "
														 + dbUri + "," + Arrays.asList( params ) + ")" );
			AuthRepository repo = RepositoryFactory.getAuthRepository( null, dbUri, params );
			for ( BareJID jid : jids ) {
				repo.addUser( jid, pwd );
			}

			log.log( Level.INFO, "All users added" );
		} catch ( TigaseDBException | ClassNotFoundException | InstantiationException | IllegalAccessException e ) {
			log.log( Level.WARNING, "Error initializing DB" + e );
		}
	}

	/**
	 * Method checks whether the connection to the database is possible and that
	 * database of specified name exists. If yes then a schema file from
	 * properties is loaded.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	protected void loadSchemaFile( Properties variables ) {

		// part 1, check db preconditions
		if ( !connection_ok ){
			log.log( Level.INFO, "Connection not validated" );
			return;
		}
		if ( !db_ok ){
			log.log( Level.INFO, "Database not validated" );
			return;
		}

		Object fileNameObj = variables.get( FILE_KEY );
		if ( fileNameObj == null ){
			log.log( Level.WARNING, "Error: empty query" );
			return;
		}
		String fileName = fileNameObj.toString();

		String db_conn = getDBUri( variables, true, true );
		log.log( Level.INFO,
						 String.format( "Loading schema from file: %1$s, URI: %2$s",
														fileName, db_conn ) );
		try {
			try ( Connection conn = DriverManager.getConnection( db_conn ) ;
						Statement stmt = conn.createStatement() ) {

				ArrayList<String> queries = loadSQLQueries( fileName, null, variables );
				for ( String query : queries ) {
					if ( !query.isEmpty() ){
						log.log( Level.FINEST, "Executing query: " + query );
						stmt.execute( query );
					}
				}
			}
			schema_ok = true;
			log.log( Level.INFO, " completed OK" );
		} catch ( SQLException | IOException ex ) {
			log.log( Level.WARNING, "Can't finalize: " + ex.getMessage() );
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

		Object queryObj = variables.get( QUERY_KEY );
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
		String database = props.getProperty( DATABASE_TYPE_KEY );
		String USERNAME;
		String PASSWORD;
		if ( useRootCredentials ){
			USERNAME = ROOT_USERNAME_KEY;
			PASSWORD = ROOT_PASSWORD_KEY;
		} else {
			USERNAME = TIGASE_USERNAME_KEY;
			PASSWORD = TIGASE_PASSWORD_KEY;
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
				db_uri += props.getProperty( DATABASE_NAME_KEY ) + ";create=true";
				break;
			case "sqlserver":
				db_uri += "//" + props.getProperty( DATABASE_HOSTNAME_KEY );
				if ( includeDbName ){
					db_uri += ";databaseName=" + props.getProperty( DATABASE_NAME_KEY );
				}
				db_uri += ";user=" + props.getProperty( USERNAME );
				if ( props.getProperty( PASSWORD ) != null
						 && !props.getProperty( PASSWORD ).isEmpty() ){
					db_uri += ";password=" + props.getProperty( PASSWORD );
				}
				db_uri += ";schema=dbo";
				db_uri += ";lastUpdateCount=false";
				db_uri += ";cacheMetaData=false";
				break;
			default:
				db_uri += "//" + props.getProperty( DATABASE_HOSTNAME_KEY ) + "/";
				if ( includeDbName ){
					db_uri += props.getProperty( DATABASE_NAME_KEY );
				} else if (database.equals( "postgresql")) {
					db_uri +=  "postgres" ;
				}
				db_uri += "?user=" + props.getProperty( USERNAME );
				if ( props.getProperty( PASSWORD ) != null
						 && !props.getProperty( PASSWORD ).isEmpty() ){
					db_uri += "&password=" + props.getProperty( PASSWORD );
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
			};
		}

		public static TigaseDBTask[] getSchemaTasks() {
			return new TigaseDBTask[] { VALIDATE_CONNECTION, VALIDATE_DB_EXISTS, LOAD_SCHEMA_FILE, SHUTDOWN_DATABASE };
		}

		public static TigaseDBTask[] getQueryTasks() {
			return new TigaseDBTask[] { VALIDATE_CONNECTION, VALIDATE_DB_EXISTS, EXECUTE_SIMPLE_QUERY, SHUTDOWN_DATABASE };
		}
	}

	static interface TigaseDBTask {

		String getDescription();

		abstract void execute( DBSchemaLoader helper, Properties variables );
	}

	/**
	 * Simple helper class allowing to substituting variables.
	 */
	static class variableSubstitutor {

		/**
		 * Method substitutes all occurrences of items in string based on the
		 * substitution map.
		 *
		 * @param str            input string in which replacement should be made.
		 * @param replacementMap replacement map listing all replacement patterns.
		 *
		 * @return string with replaced items.
		 */
		static String substitute( String str, Map<String, String> replacementMap ) {
			String replaced = str;
			for ( Map.Entry<String, String> entry : replacementMap.entrySet() ) {
				replaced = replaced.replace( entry.getKey(), entry.getValue() );
			}
			return replaced;
		}
	}
}

class myFormatter extends java.util.logging.Formatter {

	@Override
	public String format( LogRecord rec ) {
		StringBuilder buf = new StringBuilder( 1000 );

		buf.append( String.format( "%-30s \t %-15s \t %-10s %4$s",
															 rec.getSourceClassName(),
															 rec.getSourceMethodName(),
															 rec.getLevel(),
															 rec.getMessage() ) );
		buf.append( "\n" );

		return buf.toString();
	}
}
