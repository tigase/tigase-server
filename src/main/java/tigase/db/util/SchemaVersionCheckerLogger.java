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
package tigase.db.util;

import tigase.db.DataSourceAware;
import tigase.db.Repository;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.HandleEvent;
import tigase.eventbus.events.StartupFinishedEvent;
import tigase.util.Version;
import tigase.util.dns.DNSResolverFactory;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SchemaVersionCheckerLogger {

	private final static Logger log = Logger.getLogger(SchemaVersionCheckerLogger.class.getName());
	private static final SchemaVersionCheckerLogger INSTANCE = new SchemaVersionCheckerLogger();
	private final EventBus eventBus;
	private Set<VersionCheckerSchemaInfo> components = new ConcurrentSkipListSet<>();

	public static SchemaVersionCheckerLogger getInstance() {
		return INSTANCE;
	}

	private SchemaVersionCheckerLogger() {
		eventBus = EventBusFactory.getInstance();
		eventBus.registerAll(this);
	}

	public void logVersion(VersionCheckerSchemaInfo component) {
		components.add(component);
	}

	@HandleEvent
	public void printErrorMessage(StartupFinishedEvent event) {

		// if not this node is being shutdown then do nothing
		if (event.getNode() == null || !DNSResolverFactory.getInstance().getDefaultHost().equals(event.getNode())) {
			return;
		}

		if (!components.isEmpty()) {

			StringBuilder sb = new StringBuilder();
			sb.append("\n\n\tIt's possible that following data sources are out of date:");
			components.forEach(item -> sb.append("\n\t\t* ").append(item));
			sb.append("\n\tPlease upgrade the installation by running:");
			sb.append("\n\t\t$ ./scripts/tigase.sh upgrade-schema etc/tigase.conf").append("\n");
			sb.append("\n\t(this warning is printed each time SNAPSHOT version is started, you can ignore this");
			sb.append("message if you've just run above command)").append("\n");

			log.log(Level.WARNING, sb.toString());
		}
	}

	public static class VersionCheckerSchemaInfo
			implements Comparable<VersionCheckerSchemaInfo> {

		private static final Comparator<VersionCheckerSchemaInfo> VERSION_COMPARATOR = Comparator.comparing(
				(VersionCheckerSchemaInfo schemaInfo) -> schemaInfo.repositoryId)
				.thenComparing(ver -> ver.databaseVersion.orElse(null),
				               Comparator.nullsLast(Version.VERSION_COMPARATOR))
				.thenComparing(schemaInfo -> schemaInfo.requiredVersion);
		Optional<Version> databaseVersion;
		String datasourceClassName;
		String repositoryId;
		Version requiredVersion;

		public VersionCheckerSchemaInfo(final Class<? extends DataSourceAware> datasourceClass,
		                                Optional<Version> databaseVersion, Version requiredVersion) {
			this.datasourceClassName = datasourceClass.getSimpleName();
			this.repositoryId = datasourceClass.getAnnotation(Repository.SchemaId.class).id();
			this.databaseVersion = databaseVersion;
			this.requiredVersion = requiredVersion;
		}

		@Override
		public int compareTo(VersionCheckerSchemaInfo that) {
			return VERSION_COMPARATOR.compare(this, that);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			VersionCheckerSchemaInfo that = (VersionCheckerSchemaInfo) o;

			if (repositoryId != null ? !repositoryId.equals(that.repositoryId) : that.repositoryId != null) {
				return false;
			}
			if (databaseVersion.isPresent()
			    ? !databaseVersion.equals(that.databaseVersion)
			    : that.databaseVersion.isPresent()) {
				return false;
			}
			return requiredVersion != null
			       ? requiredVersion.equals(that.requiredVersion)
			       : that.requiredVersion == null;
		}

		@Override
		public int hashCode() {
			int result = repositoryId != null ? repositoryId.hashCode() : 0;
			result = 31 * result + (databaseVersion != null ? databaseVersion.hashCode() : 0);
			result = 31 * result + (requiredVersion != null ? requiredVersion.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append(repositoryId).append(" (").append(datasourceClassName).append(")");
			String ver = databaseVersion.isPresent() ? databaseVersion.get().toString() : "n/a";
			sb.append(" ~ version in database: ")
					.append(ver)
					.append(", required version: ")
					.append(requiredVersion)
					.append(")");
			return sb.toString();
		}
	}
}
