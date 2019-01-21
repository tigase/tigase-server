/**
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
package tigase.db;

import tigase.component.exceptions.RepositoryException;
import tigase.db.util.RepositoryVersionAware;
import tigase.db.util.SchemaVersionCheckerLogger;
import tigase.sys.TigaseRuntime;
import tigase.util.Version;

import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interface implemented by every class providing access to data storage, ie. databases, files, key-value stores.
 * <br>
 * Created by andrzej on 09.03.2016.
 */
public interface DataSource
		extends Repository {

	Logger log = Logger.getLogger(DataSource.class.getName());

	/**
	 * This method is called by data source bean watchdog mechanism to ensure that there is proper connectivity to
	 * underlying data storage.
	 *
	 * @param watchdogTime time which should pass between checks
	 */
	default void checkConnectivity(Duration watchdogTime) {}

	/**
	 * Method checks version of the particular DataSource stored in the defined source.
	 *
	 * @param datasource implementation of {@link DataSourceAware} interface
	 * @param shutdownServer specifies whether server should be shutdown automatically if the version in the database
	 * doesn't match required version.
	 *
	 * @return a {@code false} when the version doesn't match or there is no version information in the repository. if
	 * {@code shutdownServer} is set to {@code true} and the component version is final it would force shutting down of
	 * the server, otherwise (for non-final version) only a warning would be printed.
	 *
	 */
	default public boolean checkSchemaVersion(DataSourceAware<? extends DataSource> datasource, boolean shutdownServer) {
		boolean result = false;

		final Class<? extends DataSourceAware> datasourceClass = datasource.getClass();
		if (datasourceClass.isAnnotationPresent(SchemaId.class)
				&& RepositoryVersionAware.class.isAssignableFrom(datasourceClass)) {
			final String dataSourceID = datasourceClass.getAnnotation(SchemaId.class).id();

			if (!automaticSchemaManagement()) {
				if (log.isLoggable(Level.WARNING)) {
					log.log(Level.WARNING, "Automatic schema management is disabled for " + this.getResourceUri() +
							", skipping version check for " + dataSourceID + "(" + datasourceClass.getSimpleName() +
							")");
					return true;
				}
			}

			Optional<Version> dbVer = getSchemaVersion(dataSourceID);

			Version implementationVersion;
			try {
				final RepositoryVersionAware repositoryVersionAware = (RepositoryVersionAware) datasourceClass.newInstance();
				implementationVersion = repositoryVersionAware.getVersion();
			} catch (InstantiationException | IllegalAccessException e) {
				log.log(Level.WARNING, "Error creating instance", e);
				implementationVersion = Version.of(datasourceClass.getPackage().getImplementationVersion());
			}

			implementationVersion = new Version.Builder(implementationVersion).setCommit(null).build();

			if (!dbVer.isPresent() || !implementationVersion.getBaseVersion().equals(dbVer.get().getBaseVersion()) ||
					(!Version.TYPE.FINAL.equals(dbVer.get().getVersionType()) &&
							Version.TYPE.FINAL.equals(implementationVersion.getVersionType()))) {
				result = false;

				String[] errorMsg = new String[]{
							"ERROR! Component " + dataSourceID + " (" + datasourceClass.getSimpleName() + ") " +  "schema version is not loaded in the database or it is old!",
							(dbVer.isPresent() ? ("Version in database: " + dbVer.get() + ". ") : "")
									+ "Required version: " + implementationVersion,
							"Please upgrade the installation by running:",
							"\t$ ./scripts/tigase.sh upgrade-schema etc/tigase.conf"};

					if (shutdownServer) {
						TigaseRuntime.getTigaseRuntime().shutdownTigase(errorMsg);
					}
				} else if (implementationVersion.getBaseVersion().equals(dbVer.get().getBaseVersion()) &&
						!Version.TYPE.FINAL.equals(implementationVersion.getVersionType())) {
					result = false;

				final SchemaVersionCheckerLogger.VersionCheckerSchemaInfo versionInfo = new SchemaVersionCheckerLogger.VersionCheckerSchemaInfo(
						datasourceClass, dbVer, implementationVersion);
				SchemaVersionCheckerLogger.getInstance().logVersion(versionInfo);

			} else {
					// schema version present in DB and matches component version
					result = true;
				}
		} else {
			// there is no annotation so we assume schema version is correct;
			result = true;
		}
		return result;
	}

	default boolean automaticSchemaManagement() {
		return true;
	}

	/**
	 * Method obtains version of the schema for particular component stored in the database.
	 *
	 * @param component name of the component for which we want to get the schema version
	 *
	 * @return an optional value of the version.
	 */
	Optional<Version> getSchemaVersion(String component);


	/**
	 * Returns a DB connection string or DB connection URI.
	 *
	 * @return a <code>String</code> value representing database connection string.
	 */
	String getResourceUri();

	/**
	 * The method is called to initialize the data repository.
	 *
	 * @param resource_uri value in most cases representing the database connection string.
	 *
	 * @throws RepositoryException if there was an error during initialization of data source. Some implementations,
	 * though, perform so called lazy initialization so even though there is a problem with the underlying data source
	 * it may not be signaled through this method call.
	 */
	void initialize(String resource_uri) throws RepositoryException;

}
