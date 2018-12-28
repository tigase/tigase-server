/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

import tigase.util.Version;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * Interface indicates that the implementation is aware of the version, can specify required version of the schema that should be present in the database and perform update
 * from current version in the database to the required version
 */
public interface RepositoryVersionAware {

	/**
	 * Returns current required version of the repository implementing this interface
	 *
	 * (If we are version aware then we have to specify the version)
	 *
	 * @return current required version of the schema for the
	 */
	default public Version getVersion() {

		if (this.getClass().isAnnotationPresent(SchemaVersion.class)) {
			SchemaVersion sv = this.getClass().getAnnotation(SchemaVersion.class);
			return Version.of(sv.version());
		} else {
			String impl = this.getClass().getPackage().getImplementationVersion();
			if (impl != null && !impl.isEmpty()) {
				return Version.of(impl);
			} else {
				return Version.ZERO;
			}
		}
	}

	/**
	 * Method used to update schema in the database from the (optional) {@code oldVersion} to {@code newVersion}. If the
	 * process was correct (i.e. return {@link tigase.db.util.SchemaLoader.Result#ok}) then new version will be stored
	 * in the database.
	 *
	 * @param oldVersion optional version of the schema currently loaded in the database
	 * @param newVersion version to which component schema should be updated
	 *
	 * @return result of the update process - if the process was correct then  {@link tigase.db.util.SchemaLoader.Result#ok}
	 * should be returned
	 *
	 * @throws Exception when something unexpected happened
	 */
	public default SchemaLoader.Result updateSchema(Optional<Version> oldVersion, Version newVersion) throws Exception {
		// by default we don't need to upgrade anything so we skip it
		return SchemaLoader.Result.skipped;
	};

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	public static @interface SchemaVersion {

		String version() default "0.0.0-b0";
	}
}
