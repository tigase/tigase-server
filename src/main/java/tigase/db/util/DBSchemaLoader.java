/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

import tigase.component.exceptions.RepositoryException;
import tigase.db.AuthRepository;
import tigase.db.DataRepository;
import tigase.db.DataSource;
import tigase.db.Schema;
import tigase.db.jdbc.DataRepositoryImpl;
import tigase.util.Version;
import tigase.util.log.LogFormatter;
import tigase.util.ui.console.CommandlineParameter;
import tigase.util.ui.console.SystemConsole;
import tigase.xmpp.jid.BareJID;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.AbstractMap.SimpleImmutableEntry;
import static tigase.db.DataRepository.dbTypes;
import static tigase.db.jdbc.DataRepositoryImpl.JDBC_SCHEMA_VERSION_QUERY;

/**
 * Simple utility class allowing various Database operations, including executing simple queries, loading specific
 * schema files or performing complete load of all Tigase schema required to run the server.
 * <br>
 * Following set of {@link Properties} is accepted: <ul> <li>{@code dbType} - type of the database, possible values are:
 * {@code mysql}, {@code postgresql}, {@code derby}, {@code sqlserver}; </li><li>{@code schemaVersion} - schema version to be
 * loaded, , possible values are: {@code 7-2}, {@code 7-1}, {@code 5-1}, {@code 5}, {@code 4}; </li><li>{@code dbName} - name
 * of the database to be created/used; </li><li>{@code dbHostname} - hostname of the database; </li><li>{@code dbUser} - username
 * of the regular user; </li><li>{@code dbPass} - password of the regular user; </li><li>{@code rootUser} - username of the
 * database administrator user; </li><li>{@code rootPass} - password of the database administrator user; </li><li>{@code query} -
 * simple, single query to be executed; </li><li>{@code file} - path to the single schema file to be loaded to the database;
 * </li><li>{@code adminJID} - JID address of the XMPP administrator account; </li><li>{@code adminJIDpass} - password of the XMPP
 * administrator account.</li></ul>
 *
 * @author wojtek
 */
public class DBSchemaLoader
		extends SchemaLoader<DBSchemaLoader.Parameters> {

	private static final Logger log = Logger.getLogger(DBSchemaLoader.class.getCanonicalName());
	public enum PARAMETERS_ENUM {
		DATABASE_TYPE("dbType", "mysql"),
		SCHEMA_VERSION("schemaVersion", "8-0"),
		DATABASE_NAME("dbName", "tigasedb"),
		DATABASE_HOSTNAME("dbHostname", "localhost"),
		TIGASE_USERNAME("dbUser", "tigase_user"),
		TIGASE_PASSWORD("dbPass", "tigase_pass"),
		ROOT_USERNAME("rootUser", "root"),
		ROOT_PASSWORD("rootPass", "root"),
		LOG_LEVEL("logLevel", "CONFIG"),
		USE_SSL("useSSL", "false"),
		GET_URI("getURI", "false"),
		QUERY("query", null),
		FILE("file", null),
		ADMIN_JID("adminJID", null),
		ADMIN_JID_PASS("adminJIDpass", null),
		IGNORE_MISSING_FILES("ignoreMissingFiles", "false"),
		FORCE_RELOAD_ALL_SCHEMA_FILES("forceReloadAllSchemaFiles", "false"),
		DATABASE_OPTIONS("dbOptions", null),
		USE_LEGACY_DATETIME_CODE("useLegacyDatetimeCode", "false"),
		SERVER_TIMEZONE("serverTimezone", null);

		private String defaultValue = null;
		private String name = null;

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
	/**
	 * Denotes whether there wasn't any problem establishing connection to the database
	 */
	private boolean connection_ok = false;
	/** Denotes whether database exists */
	private boolean db_ok = false;
	private Parameters params;
	/** Holds map of all replacement variables */
	private Map<String, String> replacementMap = new HashMap<String, String>();
	/** Denotes whether schema has proper version */
	private boolean schema_ok = false;

	/**
	 * Main method allowing pass arguments to the class and setting all logging to be printed to console.
	 *
	 * @param args key-value (in the form of {@code "-<variable> value"}) parameters.
	 */
	public static void main(String[] args) {
		SchemaLoader.main(args);
	}

	private static final List<TypeInfo> suppertedTypes = Stream.of(
			new TypeInfo("derby", "Derby (built in database)", "org.apache.derby.jdbc.EmbeddedDriver"),
			new TypeInfo("mysql", "MySQL", "com.mysql.jdbc.Driver"),
			new TypeInfo("postgresql", "PostgreSQL", "org.postgresql.Driver"),
			new TypeInfo("sqlserver", "SQLServer", "net.sourceforge.jtds.jdbc.Driver",
						 "You have selected MS SQL Server as your database. While we provide you, for easy install, with open-source jTDS JDBC driver allowing to connect to MS SQL Server, we recommend using JDBC driver from Microsoft."))
			.collect(Collectors.toList());

	public DBSchemaLoader() {

	}

	@Override
	public void init(Parameters params, Optional<SchemaManager.RootCredentialsCache> rootCredentialsCache) {
		params.init(rootCredentialsCache);
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

		log.setUseParentHandlers(false);
		log.setLevel(lvl);

		Arrays.stream(log.getHandlers())
				.filter((handler) -> handler instanceof ConsoleHandler)
				.findAny()
				.orElseGet(() -> {
					Handler handler = new ConsoleHandler();
					handler.setLevel(lvl);
					handler.setFormatter(new LogFormatter());
					log.addHandler(handler);
					return handler;
				});

		log.log(Level.CONFIG, "Parameters: {0}", new Object[]{params});
		this.params = params;
	}

	public List<TypeInfo> getSupportedTypes() {
		return suppertedTypes;
	}

	public Map<Version,Path> getSchemaFileNames(String schemaId) {
		Path databaseDirectory = Paths.get(params.getSchemaDirectory());
		String databaseType = params.getDbType();

		final BiPredicate<Path, BasicFileAttributes> predicate = (path, attributes) -> {
			final String regex = databaseType + "-" + schemaId + "(-\\d+\\.\\d+\\.\\d+)(-b\\d+)?\\.sql";
			return path.getFileName().toString().matches(regex);
		};

		try (final Stream<Path> pathStream = Files.find(databaseDirectory, 1, predicate, FileVisitOption.FOLLOW_LINKS)) {
			return pathStream.map(Path::getFileName)
					.map(filename -> new AbstractMap.SimpleImmutableEntry<>(
							getVersionFromSchemaFilename(filename, databaseType, schemaId), filename))
					.sorted(Comparator.comparing(SimpleImmutableEntry::getKey, Version.VERSION_COMPARATOR))
					.collect(Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));
		} catch (IOException e) {
			log.log(Level.WARNING, "Error while getting schema file list: {0}", e.getMessage());
		}

		return Collections.emptyMap();
	}

	static Map<Version, Path> getSchemaFileNamesInRange(Map<Version, Path> paths, Optional<Version> currentVersion,
	                                                    Version requiredVersion) {

		boolean isRequiredFinal = Version.TYPE.FINAL.equals(requiredVersion.getVersionType());
		boolean isCurrentFinal =
				currentVersion.isPresent() && Version.TYPE.FINAL.equals(currentVersion.get().getVersionType());

		boolean startInclusive = !isCurrentFinal;// || !isRequiredFinal;
		boolean endInclusive = true; //isCurrentFinal;// || !isRequiredFinal;

		return paths.entrySet()
				.stream()
				.sorted(Comparator.comparing(Map.Entry::getKey, Version.VERSION_COMPARATOR))
				.filter(entry -> !currentVersion.isPresent()
						|| (startInclusive
						    ? entry.getKey().compareTo(currentVersion.get()) >= 0
						    : entry.getKey().compareTo(currentVersion.get()) > 0))
				.filter(entry -> endInclusive
				                 ? entry.getKey().compareTo(requiredVersion.getBaseVersion()) <= 0
				                 : entry.getKey().compareTo(requiredVersion) < 0)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> null, TreeMap::new));
	}

	private static Version getVersionFromSchemaFilename(Path v, String dbType, String component) {
		String filename = v.getFileName().toString();
		int start = (dbType + "-" + component + "-").length();
		int end = filename.length()-4;
		return Version.of(filename.substring(start,end));
	}

	@Override
	public Parameters createParameters() {
		return new Parameters();
	}

	/**
	 * Executes set of {@link TigaseDBTask} tasks selected based on set on passed properties
	 *
	 * @param params set of configuration parameters.
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

	@Override
	public Result validateDBConnection() {
		connection_ok = false;
		String db_conn = getDBUri(false, true);
		log.log(Level.INFO, "Validating DBConnection, URI: " + db_conn);
		if (db_conn == null) {
			log.log(Level.WARNING, "Missing DB connection URL");
			return Result.ok;
		} else {
			try (Connection conn = DriverManager.getConnection(db_conn)) {
				logAvailableDrivers();
				conn.close();
				connection_ok = true;
				log.log(Level.INFO, "Connection OK");
				return Result.ok;
			} catch (SQLException e) {
				log.log(Level.WARNING, e.getMessage());
				return Result.error;
			}
		}
	}

	@Override
	public Result shutdown() {
		return shutdownDerby();
	}

	public Result shutdownDerby() {
		String db_conn = getDBUri(false, true);
		if ("derby".equals(params.getDbType())) {
			log.log(Level.INFO, "Validating DBConnection, URI: " + db_conn);
			if (db_conn == null) {
				log.log(Level.WARNING, "Missing DB connection URL");
			} else {
				db_conn += ";shutdown=true";
				return withConnection(db_conn, conn -> {
					connection_ok = true;
					log.log(Level.INFO, "Connection OK");
					return Result.ok;
				});
			}
		}
		return Result.ok;
	}

	@Override
	public Result validateDBExists() {
		if (!connection_ok) {
			log.log(Level.WARNING, "Connection not validated");
			return Result.error;
		}

		db_ok = false;
		String db_conn1 = getDBUri(true, false);
		log.log(Level.INFO, "Validating whether DB Exists, URI: " + db_conn1);
		if (db_conn1 == null) {
			log.log(Level.WARNING, "Missing DB connection URL");
			return Result.error;
		} else {
			return withConnection(db_conn1, conn -> {
				db_ok = true;
				log.log(Level.INFO, "Exists OK");
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
					log.log(Level.INFO, " OK");
					db_ok = true;
				} catch (IOException ex) {
					log.log(Level.WARNING, ex.getMessage());
					result = Result.error;
				}
				return result;
			}));
		}
	}

	/**
	 * Method performs post-installation action using using {@code *-installer-post.sql} schema file substituting it's
	 * variables with ones provided.
	 */
	@Override
	public Result postInstallation() {
		// part 1, check db preconditions
		if (!connection_ok) {
			log.log(Level.WARNING, "Connection not validated");
			return Result.error;
		}
		if (!db_ok) {
			log.log(Level.WARNING, "Database not validated");
			return Result.error;
		}

		if (!schema_ok) {
			log.log(Level.WARNING, "Database schema is invalid");
			return Result.error;
		}

		// part 2, acquire reqired fields and validate them
		String db_conn = getDBUri(true, true);
		log.log(Level.INFO, "Post Installation, URI: " + db_conn);
		return withStatement(db_conn, stmt -> {
			log.log(Level.INFO, "Finalizing...");
			ArrayList<String> queries = loadSQLQueries("database/" + params.getDbType() + "-installer-post.sql");
			for (String query : queries) {
				if (!query.isEmpty()) {
					log.log(Level.FINEST, "Executing query: " + query);
					try {
						stmt.execute(query);
					} catch (SQLException ex) {
						log.log(Level.SEVERE, "Failed to execute query: " + query);
						throw ex;
					}
				}
			}
			log.log(Level.INFO, " completed OK");
			return Result.ok;
		});
	}

	/**
	 * Method performs post-installation action using using {@code *-installer-post.sql} schema file substituting it's
	 * variables with ones provided.
	 */
	@Override
	public Result printInfo() {
		// part 1, check db preconditions
		if (!connection_ok) {
			log.log(Level.WARNING, "Connection not validated");
			return Result.error;
		}
		if (!db_ok) {
			log.log(Level.WARNING, "Database not validated");
			return Result.error;
		}

		if (!schema_ok) {
			log.log(Level.WARNING, "Database schema is invalid");
			return Result.error;
		}

		return super.printInfo();
	}

	@Override
	public Result addXmppAdminAccount(SchemaManager.SchemaInfo schemaInfo) {
		// part 1, check db preconditions
		if (!connection_ok) {
			log.log(Level.WARNING, "Connection not validated");
			return Result.error;
		}
		if (!db_ok) {
			log.log(Level.WARNING, "Database not validated");
			return Result.error;
		}

		if (!schema_ok) {
			log.log(Level.WARNING, "Database schema is invalid");
			return Result.error;
		}

		// part 2, acquire required fields and validate them
		List<BareJID> jids = params.getAdmins();
		if (jids.size() < 1) {
			log.log(Level.WARNING, "Error: No admin users entered");
			return Result.warning;
		}

		String pwd = params.getAdminPassword();
		if (pwd == null) {
			log.log(Level.WARNING, "Error: No admin password entered");
			return Result.warning;
		}

		String dbUri = getDBUri();
		log.log(Level.INFO, "Adding XMPP Admin Account, URI: " + dbUri);

		try {
			DataRepository dataSource = new DataRepositoryImpl();
			dataSource.initialize(dbUri);

			Result result = addUsersToRepository(schemaInfo, dataSource, DataRepository.class, jids, pwd, log);
			if (result == Result.ok) {
				log.log(Level.INFO, "All users added");
			}
			return result;
		} catch (RepositoryException e) {
			log.log(Level.WARNING, "Error initializing DB" + e);
			return Result.error;
		}
	}

	@Override
	public Result setComponentVersion(String component, String version) {
		// part 1, check db preconditions
		if (!connection_ok) {
			log.log(Level.INFO, "Connection not validated");
			return Result.error;
		}

		if (component == null) {
			log.log(Level.WARNING, "Invalid component");
			return Result.error;
		}

		if (version == null) {
			log.log(Level.WARNING, "Invalid version");
			return Result.error;
		}

		String db_conn = getDBUri();
		log.log(Level.INFO, "Setting version of the component: {0} to: {1} for connection: {2}",
				new Object[]{component, version, db_conn});

		if (db_conn == null) {
			log.log(Level.WARNING, "Missing DB connection URL");
			return Result.error;
		} else {
			return withConnection(db_conn, cmd -> {
				String procedure = "{ call TigSetComponentVersion(?,?) }";
				try (PreparedStatement ps = cmd.prepareCall(procedure)) {
					ps.setString(1, component);
					ps.setString(2, version);
					ps.executeUpdate();
					ps.close();
					return Result.ok;
				} catch (SQLException ex) {
					log.log(Level.WARNING, "Setting version failed: " + procedure + ", " + ex.getMessage());
					return Result.warning;
				}
			});
		}
	}

	@Override
	public Optional<Version> getComponentVersionFromDb(String component) {
		if (component == null || component.trim().isEmpty()) {
			log.log(Level.WARNING, "Missing DB connection URL");
			throw new IllegalArgumentException("Wrong component name");
		} else {
			String db_conn = getDBUri(true, true);
			final SQLCommand<Connection, Version> versionCommand = cmd -> {

				try (PreparedStatement ps = cmd.prepareCall(JDBC_SCHEMA_VERSION_QUERY)) {
					ps.setString(1, component);
					final ResultSet rs = ps.executeQuery();
					if (rs.next()) {
						final String versionString = rs.getString(1);
						if (versionString != null) {
							return Version.of(versionString);
						}
					}
					ps.close();
				} catch (SQLException ex) {
					log.log(Level.WARNING,
							"Getting version failed: " + JDBC_SCHEMA_VERSION_QUERY + ", " + ex.getMessage());
				}
				return null;
			};

			return withConnectionGeneric(db_conn, versionCommand, null);
		}
	}

	@Override
	public Optional<Version> getMinimalRequiredComponentVersionForUpgrade(SchemaManager.SchemaInfo schema) {
		if (params.isForceReloadSchema()) {
			return Optional.of(Version.ZERO);
		}
		Map<Version, Path> versions = getSchemaFileNames(schema.getId());
		if (versions.size() > 1) {
			return versions.keySet().stream().sorted().findFirst();
		} else {
			return Optional.ofNullable(Version.ZERO);
		}
	}

	@Override
	public Result loadSchema(SchemaManager.SchemaInfo schema, String version) {

		log.log(Level.CONFIG, "SchemaInfo:: id: {0}, repositories: {1}; version: {2}",
		        new Object[]{schema.getId(), schema.getRepositories().size(), version});

		if (!connection_ok) {
			log.log(Level.CONFIG, "Connection not validated");
			return Result.error;
		}

		Result result;
		Optional<Version> currentVersion = getComponentVersionFromDb(schema.getId());
		Version requiredVersion = Version.of(version);

		if (schema.isExternal()) {
			result = loadSchemaFromSQLFiles(schema, currentVersion, requiredVersion);
		} else {
			result = loadSchemaFromClass(schema, currentVersion, requiredVersion);
		}

		// particular repository is version aware therefore we store version:
		// * if no version was yet stored and the update was skipped (because no logic
		//   was yet defined to perform any update
		// * version was set and the update was correctly performed
		if (!currentVersion.isPresent() && Result.skipped.equals(result) || Result.ok.equals(result)) {
			Version build = new Version.Builder(requiredVersion).setCommit(null).build();
			setComponentVersion(schema.getId(), build.toString() );
		}
		if (Result.skipped.equals(result)) {
			log.log(Level.INFO, "Required schema is already loaded in correct version");
		}

		// logic to check if the schema version was correctly loaded

		Optional<String> passwordEncoding = schema.getRepositories()
				.stream()
				.filter(info -> AuthRepository.class.isAssignableFrom(info.getImplementation()))
				.map(this::getDataSourcePasswordEncoding)
				.filter(Objects::nonNull)
				.findFirst();

		passwordEncoding.ifPresent(encoding -> {
			log.log(Level.WARNING, "You have 'password-encoding' property set to " + encoding + "."
			        + "\n" + "This setting will no longer work out of the box with this version of Tigase XMPP Server."
					+ "\n" + "Please check Tigase XMPP Server Administration Guide, section \"Changes to Schema in v8.0.0\"" +
					" at http://docs.tigase.org/ for more details.");
		});

		return passwordEncoding.isPresent()
			   ? (result == Result.ok ? Result.warning : result)
			   : result;
	}
	
	private Result loadSchemaFromClass(SchemaManager.SchemaInfo schema, Optional<Version> currentVersion,
	                                   Version requiredVersion) {
		Result result;
		try {
			String dbUri = getDBUri();
			log.log(Level.CONFIG, "Loading schema {0}, version: {1} into repo: {2}",
			        new Object[]{schema.getId(), requiredVersion, dbUri});
			DataRepository dataSource = new DataRepositoryImpl();
			dataSource.initialize(dbUri);

			final Set<Result> collect = getInitializedDataSourceAwareForSchemaInfo(schema, DataSource.class,
			                                                                       dataSource, log).filter(
					clazz -> RepositoryVersionAware.class.isAssignableFrom(clazz.getClass()))
					.map(RepositoryVersionAware.class::cast)
					.map(updateSchemaFunction(currentVersion, requiredVersion))
					.collect(Collectors.toSet());
			result = parseResultsSet(collect);

		} catch (RepositoryException e) {
			log.log(Level.WARNING, e.getMessage());
			result = Result.warning;
		}
		return result;
	}

	private Result loadSchemaFromSQLFiles(SchemaManager.SchemaInfo schema, Optional<Version> currentVersion,
	                                      Version requiredVersion) {
		Result result;
		log.log(Level.CONFIG, "Loading schema {0}, version: {1} from files, current: {2}",
		        new Object[]{schema.getId(), requiredVersion, currentVersion.orElse(Version.ZERO)});

		Map<Version, Path> schemaFileNames = getSchemaFileNames(schema.getId());

		if (!params.isForceReloadSchema()) {
			schemaFileNames = getSchemaFileNamesInRange(schemaFileNames, currentVersion, requiredVersion);
		}

		Collection<Path> schemaFiles = schemaFileNames.values();

		if (schemaFiles.isEmpty()) {
			log.log(Level.FINEST, "Empty schema list");
			result = Result.skipped;
		} else {
			log.log(Level.INFO, "Schema files to load: {0}", new Object[]{schemaFiles});
			final Set<Result> collect = schemaFiles.stream()
					.map(file -> loadSchemaFile(file.toString()))
					.collect(Collectors.toSet());

			result = parseResultsSet(collect);
		}
		return result;
	}

	/**
	 * Method parses provided collection and based on the collection items returns
	 * single result. If there is single item then it will be return, otherwise
	 * collection will be checked and if either {@code Result.error} or {@code Result.warning}
	 * is present then it will be return. Otherwise {@code Result.ok} will be returned
	 *
	 * @param collect Collection of the {@link tigase.db.util.SchemaLoader.Result} to be processed
	 *
	 * @return single {@link tigase.db.util.SchemaLoader.Result} appropriate for the whole collection
	 */
	private Result parseResultsSet(Set<Result> collect) {
		Result result;
		if (collect.size() == 1) {
			result = collect.iterator().next();
		} else {
			result = collect.contains(Result.error)
			         ? Result.error
			         : (collect.contains(Result.warning) ? Result.warning : Result.ok);
		}
		return result;
	}

	private Function<RepositoryVersionAware, Result> updateSchemaFunction(Optional<Version> currentVersion,
	                                                                      Version requiredVersion) {
		return versionAware -> {
			try {
				return versionAware.updateSchema(currentVersion, requiredVersion);
			} catch (Exception e) {
				log.log(Level.WARNING, e.getMessage());
				return Result.warning;
			}
		};
	}

	private String getDataSourcePasswordEncoding(SchemaManager.RepoInfo repoInfo) {
		AtomicReference<String> passwordEncoding = new AtomicReference<>();
		withStatement(repoInfo.getDataSource().getResourceUri(), stmt -> {

			String query = null;

			final dbTypes dbTypes = DataRepositoryImpl.parseDatabaseType(
					repoInfo.getDataSource().getResourceUri());

			switch (dbTypes) {
				case derby:
					query = "values TigGetDBProperty('password-encoding')";
					break;
				case jtds:
				case sqlserver:
					query = "select dbo.TigGetDBProperty('password-encoding')";
					break;
				default:
					query = "select TigGetDBProperty('password-encoding')";
					break;
			}

			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				passwordEncoding.set(rs.getString(1));
			}
			rs.close();
			return Result.ok;
		});

		return passwordEncoding.get();
	}

	private Result loadSchema(String schemaId, String version) {

		return Result.error;
	}

	@Override
	public Result loadSchemaFile(String fileName) {
		// part 1, check db preconditions
		if (!connection_ok) {
			log.log(Level.INFO, "Connection not validated");
			return Result.error;
		}
		if (!db_ok) {
			log.log(Level.INFO, "Database not validated");
			return Result.error;
		}

		if (fileName == null) {
			log.log(Level.WARNING, "Error: empty query");
			return Result.error;
		}

		String db_conn = getDBUri();
		log.log(Level.INFO, String.format("Loading schema from file(s): %1$s, URI: %2$s", fileName, db_conn));

		return withStatement(db_conn, stmt -> {
			ArrayList<String> queries = new ArrayList<>(loadSQLQueries("database/" + fileName));
			for (String query : queries) {
				if (!query.isEmpty()) {
					log.log(Level.FINEST, "Executing query: " + query);
					try {
						stmt.execute(query);
					} catch (SQLException ex) {
						if ("derby".equals(params.getDbType())) {
							String lowerQuery = query.toLowerCase().trim();
							if (("X0Y32".equals(ex.getSQLState()) || "X0Y68".equals(ex.getSQLState())) &&
									(lowerQuery.startsWith("create ") || lowerQuery.startsWith("alter "))) {
								// if object already exists then we should not consider creation failure as an error for DerbyDB
								log.log(Level.FINEST, "Object already exists!");
								continue;
							}
							if ("42Y55".equals(ex.getSQLState()) && lowerQuery.startsWith("drop ")) {
								// if object does not exists then wen should not consider drop failure as an error for DerbyDB
								log.log(Level.FINEST, "Object already dropped!");
								continue;
							}
						}
						log.log(Level.SEVERE, "Failed to execute query: " + query);
						throw ex;
					}
				}
			}
			schema_ok = true;
			log.log(Level.INFO, " completed OK");
			return Result.ok;
		});
	}

	public Result destroyDataSource() {
		if (!connection_ok) {
			log.log(Level.INFO, "Connection not validated");
			return Result.error;
		}

		if ("derby".equals(params.getDbType())) {
			shutdown();
			Path path = Paths.get(params.getDbName());
			try {
				Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
				return Result.ok;
			} catch (IOException ex) {
				log.log(Level.SEVERE, "Failed to remove path " + path.toString(), ex);
				return Result.error;
			}
		} else {
			String db_conn = getDBUri(false, true);
			log.log(Level.INFO, "Dropping database, URI: " + db_conn);
			return withStatement(db_conn, stmt -> {
				String query = "drop database " + params.getDbName();
				log.log(Level.FINEST, "Executing query: " + query);
				try {
					stmt.execute(query);
					stmt.close();
					return Result.ok;
				} catch (SQLException ex) {
					log.log(Level.WARNING, "Query failed: " + query + ", " + ex.getMessage());
					return Result.warning;
				}
			});
		}
	}

	public String getDBUri() {
		return getDBUri(true, false);
	}

	public List<CommandlineParameter> getCommandlineParameters() {
		List<CommandlineParameter> options = new ArrayList<>();

		options.add(new CommandlineParameter.Builder("T", PARAMETERS_ENUM.DATABASE_TYPE.getName()).description(
				"Database server type")
							.defaultValue(PARAMETERS_ENUM.DATABASE_TYPE.getDefaultValue())
							.required(true)
							.build());
		options.addAll(getSetupOptions());

		options.add(new CommandlineParameter.Builder("F", PARAMETERS_ENUM.FILE.getName()).description(
				"Comma separated list of SQL files that will be processed").build());
		options.add(new CommandlineParameter.Builder("Q", PARAMETERS_ENUM.QUERY.getName()).description(
				"Custom query to be executed").build());
		options.add(new CommandlineParameter.Builder("J", PARAMETERS_ENUM.ADMIN_JID.getName()).description(
				"Comma separated list of administrator JID(s)").build());
		options.add(new CommandlineParameter.Builder("N", PARAMETERS_ENUM.ADMIN_JID_PASS.getName()).description(
				"Password that will be used for the entered JID(s) - one for all configured administrators")
							.secret()
							.build());
		options.add(new CommandlineParameter.Builder(null, PARAMETERS_ENUM.GET_URI.getName()).description(
				"Generate database URI")
							.requireArguments(false)
							.defaultValue(PARAMETERS_ENUM.GET_URI.getDefaultValue())
							.build());
		options.add(new CommandlineParameter.Builder(null, PARAMETERS_ENUM.IGNORE_MISSING_FILES.getName()).description(
				"Force ignoring missing files errors")
							.defaultValue(PARAMETERS_ENUM.IGNORE_MISSING_FILES.getDefaultValue())
							.build());
		return options;
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
		switch (this.getType()) {
			case "mysql":
				options.add(new CommandlineParameter.Builder(null,
															 PARAMETERS_ENUM.USE_LEGACY_DATETIME_CODE.getName()).description(
						"Use legacy datetime code for JDBC connection")
									.requireArguments(true)
									.defaultValue(PARAMETERS_ENUM.USE_LEGACY_DATETIME_CODE.getDefaultValue())
									.type(Boolean.class)
									.build());
				options.add(
						new CommandlineParameter.Builder(null, PARAMETERS_ENUM.SERVER_TIMEZONE.getName()).description(
								"Time zone of database server")
								.defaultValue(PARAMETERS_ENUM.SERVER_TIMEZONE.getDefaultValue())
								.requireArguments(true)
								.build());
				break;
			default:
				break;
		}
		return options;
	}

	/**
	 * Method checks whether the connection to the database is possible and that database of specified name exists. If
	 * yes then a single query is executed.
	 *
	 * @param query to execute
	 */
	protected Result executeSingleQuery(String query) {
		// part 1, check db preconditions
		if (!connection_ok) {
			log.log(Level.INFO, "Connection not validated");
			return Result.error;
		}

		if (query == null) {
			log.log(Level.WARNING, "Error: empty query");
			return Result.error;
		}

		String db_conn = getDBUri();
		log.log(Level.INFO, "Executing Simple Query, URI: " + db_conn);
		if (db_conn == null) {
			log.log(Level.WARNING, "Missing DB connection URL");
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

	/**
	 * Performs actual read and parsing of queries from resource file. It skips comments and require proper syntax of
	 * the file, i.e. each query enclosed in {@code "-- QUERY START:"} and {@code "-- QUERY END:"}. If the file
	 * references/sources/import another schema file then it will also be read and parsed.
	 *
	 * @param resource name of the resource for which an {@link InputStream} should be created.
	 */
	private ArrayList<String> loadSQLQueries(String resource) throws IOException {
		log.log(Level.FINER, "Loading queries, resource: {0}", new Object[]{resource});
		ArrayList<String> results = new ArrayList<>();
		Path p = Paths.get(resource);

		if (!Files.exists(p)) {
			Path srcPath = Paths.get("src/main/" + resource);
			if (!Files.exists(srcPath)) {
				if (params.isIgnoreMissingFiles()) {
					log.log(Level.WARNING, "Provided path: {0} doesn''t exist, skipping!", new Object[]{p.toString()});
					return results;
				} else {
					throw new IOException("Required file at " + p.toString() + " doesn't exist!");
				}
			} else {
				p = srcPath;
			}
		}

		String sql_query = "";
		SQL_LOAD_STATE state = SQL_LOAD_STATE.INIT;
		for (String line : Files.readAllLines(p)) {
			switch (state) {
				case INIT:
					if (line.startsWith("-- QUERY START:")) {
						sql_query = "";
						state = SQL_LOAD_STATE.IN_SQL;
					}
					if (line.startsWith("-- LOAD FILE:") && line.trim().contains("sql")) {
						Matcher matcher = Pattern.compile("-- LOAD FILE:\\s*(.*\\.sql)").matcher(line);
						if (matcher.find()) {
							results.addAll(loadSQLQueries(matcher.group(1)));
						}
					}
					break;
				case IN_SQL:
					if (line.startsWith("-- QUERY END:")) {
						state = SQL_LOAD_STATE.INIT;
						sql_query = sql_query.trim();
						if (sql_query.endsWith(";")) {
							sql_query = sql_query.substring(0, sql_query.length() - 1);
						}
						if (sql_query.endsWith("//")) {
							sql_query = sql_query.substring(0, sql_query.length() - 2);
						}
						for (Map.Entry<String, String> entry : replacementMap.entrySet()) {
							sql_query = sql_query.replace(entry.getKey(), entry.getValue());
						}

						results.add(sql_query);
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
		return results;
	}

	private Result withConnection(String db_conn, SQLCommand<Connection, Result> cmd) {
		return withConnection(db_conn, cmd, null);
	}

	private <R> Optional<R> withConnectionGeneric(String db_conn, SQLCommand<Connection, R> cmd,
												  ExceptionHandler<Exception, R> exceptionHandler) {
		R result = null;
		try (Connection conn = DriverManager.getConnection(db_conn)) {
			logAvailableDrivers();
			result = cmd.execute(conn);
			conn.close();
		} catch (SQLException | IOException e) {

			// Handle Derby shutdown - it throws exception even if the shutdown was correct…
			if (e instanceof SQLException) {
				SQLException se = (SQLException) e;
				if (((se.getErrorCode() == 50000) && ("XJ015".equals(se.getSQLState()))) ||
						((se.getErrorCode() == 45000) && ("08006".equals(se.getSQLState())))) {
					System.out.println("Derby shut down normally");
					log.log(Level.INFO, "Derby shut down normally");
					return Optional.empty();
				}
			}

			if (exceptionHandler != null) {
				return Optional.of(exceptionHandler.handleException(e));
			} else {
				log.log(Level.SEVERE, "\n\n\n=====\nFailure: " + e.getMessage() + "\n=====\n\n");
				if (log.isLoggable(Level.FINEST)) {

					log.log(Level.SEVERE, "Failure: " + e.getMessage(), e);
				}
				return Optional.empty();
			}
		}
		return Optional.ofNullable(result);
	}

	private void logAvailableDrivers() {
		String availableDrivers = Collections.list(DriverManager.getDrivers())
				.stream()
				.map(driver -> driver.getClass().getName() + "[" + driver.getMajorVersion() + "." +
						driver.getMinorVersion() + "]")
				.collect(Collectors.joining(" ,"));
		log.log(Level.FINE, "DriverManager (available drivers): {0}", availableDrivers);
	}

	private Result withConnection(String db_conn, SQLCommand<Connection, Result> cmd,
								  ExceptionHandler<Exception, Result> exceptionHandler) {
		final Optional<Result> result = withConnectionGeneric(db_conn, cmd, exceptionHandler);
		return result.orElse(Result.error);
	}

	private Result withStatement(String dbConn, SQLCommand<Statement, Result> cmd) {
		return withConnection(dbConn, conn -> {
			try (Statement stmt = conn.createStatement()) {
				return cmd.execute(stmt);
			}
		});
	}

	/**
	 * Helper method used to generate proper database URI depending on properties.
	 *
	 * @param includeDbName configure whether to include database name in the URI
	 * @param useRootCredentials whether to put in the URI credentials of database administrator ({@code true}) or
	 * regular user.
	 *
	 */
	private String getDBUri(boolean includeDbName, boolean useRootCredentials) {
		String db_uri = "jdbc:";
		String database = params.getDbType();
		String USERNAME = useRootCredentials ? params.getDbRootUser() : params.getDbUser();
		String PASSWORD = useRootCredentials ? params.getDbRootPass() : params.getDbPass();

		switch (database) {
			case "sqlserver":
				db_uri += "jtds:sqlserver:";
				break;
			default:
				db_uri += database + ":";
				break;
		}
		switch (database) {
			case "derby":
				db_uri += params.getDbName() + ";create=true";
				break;
			case "sqlserver":
				db_uri += "//" + params.getDbHostname();
				if (includeDbName) {
					db_uri += ";databaseName=" + params.getDbName();
				}
				db_uri += ";user=" + USERNAME;
				if (PASSWORD != null && !PASSWORD.isEmpty()) {
					db_uri += ";password=" + PASSWORD;
				}
				db_uri += ";schema=dbo";
				db_uri += ";lastUpdateCount=false";
				db_uri += ";cacheMetaData=false";
				if (Boolean.TRUE.equals(params.isUseSSL())) {
					db_uri += ";encrypt=true";
				}
				break;
			case "postgresql":
				db_uri += "//" + params.getDbHostname() + "/";
				if (includeDbName) {
					db_uri += params.getDbName();
				} else if (useRootCredentials) {
					db_uri += "postgres";
				}
				db_uri += "?user=" + USERNAME;
				if (PASSWORD != null && !PASSWORD.isEmpty()) {
					db_uri += "&password=" + PASSWORD;
				}
				if (Boolean.TRUE.equals(params.isUseSSL())) {
					db_uri += "&useSSL=true";
				} else if (Boolean.FALSE.equals(params.isUseSSL())) {
					// explicitly disable SSL to avoid a warning by the driver
					db_uri += "&useSSL=false";
				}
				break;

			case "mysql":
				db_uri += "//" + params.getDbHostname() + "/";
				if (includeDbName) {
					db_uri += params.getDbName();
				}
				db_uri += "?user=" + USERNAME;
				if (PASSWORD != null && !PASSWORD.isEmpty()) {
					db_uri += "&password=" + PASSWORD;
				}
				if (Boolean.TRUE.equals(params.isUseSSL())) {
					db_uri += "&useSSL=true";
				} else if (Boolean.FALSE.equals(params.isUseSSL())) {
					// explicitly disable SSL to avoid a warning by the driver
					db_uri += "&useSSL=false";
				}
				if (!params.isUseLegacyDatetimeCode()) {
					db_uri += "&useLegacyDatetimeCode=" + params.isUseLegacyDatetimeCode();
				}
				if (params.getServerTimezone() != null) {
					db_uri += "&serverTimezone=" + params.getServerTimezone();
				}
				db_uri += "&allowPublicKeyRetrieval=true";
				break;
			default:
				throw new IllegalArgumentException("Unknown database type: " + database);
		}
		return db_uri;
	}

	enum SQL_LOAD_STATE {

		INIT,
		IN_SQL;
	}

	enum Tasks
			implements TigaseDBTask {

		VALIDATE_CONNECTION("Checking connection to the database") {
			@Override
			public void execute(DBSchemaLoader helper, Parameters params) {
				helper.validateDBConnection();
			}
		},
		VALIDATE_DB_EXISTS("Checking if the database exists") {
			@Override
			public void execute(DBSchemaLoader helper, Parameters params) {
				helper.validateDBExists();
			}
		},
		VALIDATE_DB_SCHEMA("Checking the database schema") {
			@Override
			public void execute(DBSchemaLoader helper, Parameters params) {
				SchemaManager.getDefaultDataSourceAndSchemas(helper.getDBUri()).values().stream().findAny().ifPresent( schemas -> {
					schemas.forEach(schema -> helper.loadSchema(schema, schema.getVersion().get().toString()));
				});
			}
		},
		ADD_ADMIN_XMPP_ACCOUNT("Adding XMPP admin accounts") {
			@Override
			public void execute(DBSchemaLoader helper, Parameters params) {
				SchemaManager.getDefaultDataSourceAndSchemas(helper.getDBUri()).values().stream().findAny().ifPresent( schemas -> {
					schemas.stream().filter(schema -> Schema.SERVER_SCHEMA_ID.equals(schema.getId())).findAny().ifPresent(helper::addXmppAdminAccount);
				});
			}
		},
		EXECUTE_SIMPLE_QUERY("Executing simple single query") {
			@Override
			public void execute(DBSchemaLoader helper, Parameters params) {
				helper.executeSingleQuery(params.getQuery());
			}
		},
		LOAD_SCHEMA_FILE("Loading schema file from provided file") {
			@Override
			public void execute(DBSchemaLoader helper, Parameters params) {
				helper.loadSchemaFile(params.getFile());
			}
		},
		POST_INSTALLATION("Post installation actions") {
			@Override
			public void execute(DBSchemaLoader helper, Parameters params) {
				helper.postInstallation();
			}
		},
		SHUTDOWN_DATABASE("Shutting Down Database") {
			@Override
			public void execute(DBSchemaLoader helper, Parameters params) {
				helper.shutdownDerby();
			}
		},
		PRINT_INFO_TASK("Database Configuration Details") {
			@Override
			public void execute(DBSchemaLoader helper, Parameters params) {
				helper.printInfo();
			}
		};
		private final String description;

		public static TigaseDBTask[] getQueryTasks() {
			return new TigaseDBTask[]{VALIDATE_CONNECTION, EXECUTE_SIMPLE_QUERY, SHUTDOWN_DATABASE};
		}

		public static TigaseDBTask[] getSchemaTasks() {
			return new TigaseDBTask[]{VALIDATE_CONNECTION, VALIDATE_DB_EXISTS, LOAD_SCHEMA_FILE, POST_INSTALLATION,
									  SHUTDOWN_DATABASE, PRINT_INFO_TASK};
		}

		public static TigaseDBTask[] getTasksInOrder() {
			return new TigaseDBTask[]{VALIDATE_CONNECTION, VALIDATE_DB_EXISTS, VALIDATE_DB_SCHEMA,
									  ADD_ADMIN_XMPP_ACCOUNT, POST_INSTALLATION, SHUTDOWN_DATABASE, PRINT_INFO_TASK};
		}

		private Tasks(String description) {
			this.description = description;
		}

		@Override
		public String getDescription() {
			return description;
		}
	}

	public interface ExceptionHandler<T extends Exception, R> {

		R handleException(T ex);
	}

	public interface SQLCommand<C, R> {

		R execute(C conn) throws SQLException, IOException;

	}

	static interface TigaseDBTask {

		String getDescription();

		abstract void execute(DBSchemaLoader helper, Parameters variables);
	}

	public static class Parameters
			implements SchemaLoader.Parameters {

		private String adminPassword;
		private List<BareJID> admins;
		private String dbHostname = null;
		private String dbName = null;
		private String dbPass = null;
		private String dbRootPass;
		private String dbRootUser;
		private String dbType;
		private String dbUser = null;
		private String file;
		private Boolean ingoreMissingFiles = false;
		private Level logLevel = Level.CONFIG;
		private String query;
		private String serverTimezone = null;
		private Boolean useLegacyDatetimeCode = false;
		private Boolean useSSL = null;
		private boolean forceReloadSchema = false;
		private String schemaDirectory = "database/";

		private static String getProperty(Properties props, PARAMETERS_ENUM param) {
			return props.getProperty(param.getName(), null); //param.getDefaultValue());
		}

		private static String getPropertyWithDefault(Properties props, DBSchemaLoader.PARAMETERS_ENUM param) {
			return props.getProperty(param.getName(), param.getDefaultValue());
		}

		private static <T> T getProperty(Properties props, PARAMETERS_ENUM param, Function<String, T> converter) {
			String tmp = getProperty(props, param);
			if (tmp == null) {
				return null;
			}
			return converter.apply(tmp);
		}

		private static <T> T getPropertyWithDefault(Properties props, DBSchemaLoader.PARAMETERS_ENUM param, Function<String, T> converter) {
			String tmp = getPropertyWithDefault(props, param);
			if (tmp == null) {
				return null;
			}
			return converter.apply(tmp);
		}

		@Override
		public String getAdminPassword() {
			return adminPassword;
		}

		@Override
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

		public boolean isForceReloadSchema() {
			return forceReloadSchema;
		}

		@Override
		public void setForceReloadSchema(boolean forceReloadSchema) {
			this.forceReloadSchema = forceReloadSchema;
		}

		public boolean isIgnoreMissingFiles() {
			return ingoreMissingFiles;
		}

		public Boolean isUseSSL() {
			return useSSL;
		}

		public Boolean isUseLegacyDatetimeCode() {
			return useLegacyDatetimeCode;
		}

		public String getServerTimezone() {
			return serverTimezone;
		}

		public String getSchemaDirectory() {
			return schemaDirectory;
		}

		@Override
		public void setSchemaDirectory(String schemaDirectory) {
			this.schemaDirectory = schemaDirectory;
		}

		@Override
		public void parseUri(String uri) {
			int idx = uri.indexOf(":", 5);
			dbType = uri.substring(5, idx);
			if ("jtds".equals(dbType)) {
				dbType = "sqlserver";
			}

			String rest = null;
			switch (dbType) {
				case "derby":
					dbName = uri.substring(idx + 1, uri.indexOf(";"));
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
					rest = rest.substring(idx + 1);
					idx = rest.indexOf("?");
					dbName = rest.substring(0, idx);
					rest = rest.substring(idx + 1);
					useLegacyDatetimeCode = true;
					for (String x : rest.split("&")) {
						String p[] = x.split("=");
						if (p.length < 2) {
							continue;
						}
						switch (p[0]) {
							case "user":
								dbUser = p[1];
								break;
							case "password":
								dbPass = p[1];
								break;
							case "useSSL":
								useSSL = Boolean.valueOf(p[1]);
								break;
							case "useLegacyDatetimeCode":
								useLegacyDatetimeCode = Boolean.valueOf(p[1]);
								break;
							case "serverTimezone":
								serverTimezone = p[1];
								break;
							default:
								break;
						}
					}
					break;
			}
		}

		@Override
		public void setProperties(Properties props) {
			logLevel = getPropertyWithDefault(props, PARAMETERS_ENUM.LOG_LEVEL, Level::parse);
			ingoreMissingFiles = getProperty(props, PARAMETERS_ENUM.IGNORE_MISSING_FILES, Boolean::valueOf);
			admins = getProperty(props, PARAMETERS_ENUM.ADMIN_JID, tmp -> Arrays.stream(tmp.split(","))
					.map(BareJID::bareJIDInstanceNS)
					.collect(Collectors.toList()));
			adminPassword = getProperty(props, PARAMETERS_ENUM.ADMIN_JID_PASS);

			dbType = getProperty(props, PARAMETERS_ENUM.DATABASE_TYPE);
			dbName = getProperty(props, PARAMETERS_ENUM.DATABASE_NAME);
			dbHostname = getProperty(props, PARAMETERS_ENUM.DATABASE_HOSTNAME);
			dbUser = getProperty(props, PARAMETERS_ENUM.TIGASE_USERNAME);
			dbPass = getProperty(props, PARAMETERS_ENUM.TIGASE_PASSWORD);
			useSSL = getProperty(props, PARAMETERS_ENUM.USE_SSL, Boolean::parseBoolean);
			useLegacyDatetimeCode = getProperty(props, PARAMETERS_ENUM.USE_LEGACY_DATETIME_CODE,
												tmp -> Boolean.parseBoolean(tmp));
			serverTimezone = getProperty(props, PARAMETERS_ENUM.SERVER_TIMEZONE);

			dbRootUser = getProperty(props, PARAMETERS_ENUM.ROOT_USERNAME);
			dbRootPass = getProperty(props, PARAMETERS_ENUM.ROOT_PASSWORD);

			file = getProperty(props, PARAMETERS_ENUM.FILE);
			query = getProperty(props, PARAMETERS_ENUM.QUERY);
			forceReloadSchema = getPropertyWithDefault(props, PARAMETERS_ENUM.FORCE_RELOAD_ALL_SCHEMA_FILES, Boolean::valueOf);
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

		@Override
		public Level getLogLevel() {
			return this.logLevel;
		}

		@Override
		public void setLogLevel(Level level) {
			this.logLevel = level;
		}

		public Boolean getIngoreMissingFiles() {
			return ingoreMissingFiles;
		}

		public void setIngoreMissingFiles(Boolean ingoreMissingFiles) {
			this.ingoreMissingFiles = ingoreMissingFiles;
		}

		@Override
		public String toString() {
			return "[" + Arrays.stream(this.getClass().getDeclaredFields()).map(field -> {
				String result = field.getName() + ": ";
				Object value;
				try {
					field.setAccessible(true);
					value = field.get(this);
				} catch (Exception ex) {
					value = "Error!";
				}
				return result + value;
			}).collect(Collectors.joining(", ")) + "]";
		}

		protected void init(Optional<SchemaManager.RootCredentialsCache> rootCredentialsCache) {
			if (dbRootUser == null || dbRootPass == null) {
				SchemaManager.RootCredentials credentials = rootCredentialsCache.isPresent()
															? rootCredentialsCache.get().get(dbHostname)
															: null;
				if (credentials != null) {
					dbRootUser = credentials.user;
					dbRootPass = credentials.password;
				} else {
					if (!"derby".equals(dbType)) {
						SystemConsole console = new SystemConsole();
						console.writeLine("");
						if (dbRootUser == null) {
							dbRootUser = console.readLine(
									"Database root account username used to create tigase user and database at " +
											dbHostname + " : ");
						}
						if (dbRootPass == null) {
							dbRootPass = new String(console.readPassword(
									"Database root account password used to create tigase user and database at " +
											dbHostname + " : "));
						}
						rootCredentialsCache.ifPresent(cache -> cache.set(dbHostname,
																		  new SchemaManager.RootCredentials(dbRootUser,
																											dbRootPass)));
					}
				}
			}
		}

		private String getFile() {
			return file;
		}

		private String getQuery() {
			return query;
		}
	}
}
