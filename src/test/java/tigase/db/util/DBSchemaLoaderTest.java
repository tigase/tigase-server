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

import org.junit.Assert;
import org.junit.Test;
import tigase.util.Version;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static tigase.db.util.DBSchemaLoader.getSchemaFileNamesInRange;

public class DBSchemaLoaderTest {

	@Test
	public void getSchemaFileNames() {
	}

	@Test
	public void getSchemaFilesCurrentEmptyRequiredSnapshot() {

		Map<Version, Path> fileVersions = new ConcurrentHashMap<>();
		fileVersions.put(Version.of("3.2.0"), Paths.get("mysql-3.2.0.sql"));
		fileVersions.put(Version.of("3.1.0"), Paths.get("mysql-3.1.0.sql"));
		fileVersions.put(Version.of("4.0.0"), Paths.get("mysql-4.0.0.sql"));
		fileVersions.put(Version.of("3.0.0"), Paths.get("mysql-3.0.0.sql"));

		final Optional<Version> currentVersion = Optional.empty();
		final Version requiredVersion = Version.of("4.0.0-SNAPSHOT");

		final Map<Version, Path> result = getSchemaFileNamesInRange(fileVersions, currentVersion, requiredVersion);

		Assert.assertEquals("Should contain all available files",
		                    result.keySet(), fileVersions.keySet());
	}

	@Test
	public void getSchemaFilesSameVersionCurrentSnapshotRequiredSnapshot() {

		Map<Version, Path> fileVersions = new ConcurrentHashMap<>();
		fileVersions.put(Version.of("3.2.0"), Paths.get("mysql-3.2.0.sql"));
		fileVersions.put(Version.of("3.1.0"), Paths.get("mysql-3.1.0.sql"));
		fileVersions.put(Version.of("3.0.0"), Paths.get("mysql-3.0.0.sql"));
		fileVersions.put(Version.of("4.0.0"), Paths.get("mysql-4.0.0.sql"));

		final Optional<Version> currentVersion = Optional.of(Version.of("4.0.0-SNAPSHOT"));
		final Version requiredVersion = Version.of("4.0.0-SNAPSHOT");

		final Map<Version, Path> result = getSchemaFileNamesInRange(fileVersions, currentVersion, requiredVersion);

		Assert.assertEquals("Should contain only latest version (same as required)",
		                    result.keySet(), new TreeSet<>(Arrays.asList(Version.of("4.0.0"))));
	}


	@Test
	public void getSchemaFilesSameVersionCurrentFinalRequiredFinal() {

		Map<Version, Path> fileVersions = new ConcurrentHashMap<>();
		fileVersions.put(Version.of("3.2.0"), Paths.get("mysql-3.2.0.sql"));
		fileVersions.put(Version.of("3.0.0"), Paths.get("mysql-3.0.0.sql"));
		fileVersions.put(Version.of("4.0.0"), Paths.get("mysql-4.0.0.sql"));
		fileVersions.put(Version.of("3.1.0"), Paths.get("mysql-3.1.0.sql"));

		final Optional<Version> currentVersion = Optional.of(Version.of("4.0.0"));
		final Version requiredVersion = Version.of("4.0.0");

		final Map<Version, Path> result = getSchemaFileNamesInRange(fileVersions, currentVersion, requiredVersion);

		Assert.assertTrue("Should be empty",
		                  result.isEmpty());
	}

	@Test
	public void getSchemaFilesSameVersionCurrentFinalRequiredSnapshot() {

		Map<Version, Path> fileVersions = new ConcurrentHashMap<>();
		fileVersions.put(Version.of("3.2.0"), Paths.get("mysql-3.2.0.sql"));
		fileVersions.put(Version.of("3.0.0"), Paths.get("mysql-3.0.0.sql"));
		fileVersions.put(Version.of("4.0.0"), Paths.get("mysql-4.0.0.sql"));
		fileVersions.put(Version.of("3.1.0"), Paths.get("mysql-3.1.0.sql"));

		final Optional<Version> currentVersion = Optional.of(Version.of("4.0.0"));
		final Version requiredVersion = Version.of("4.0.0-SNAPSHOT");

		final Map<Version, Path> result = getSchemaFileNamesInRange(fileVersions, currentVersion, requiredVersion);

		Assert.assertEquals("Should contain only latest version (same as required)",
		                    result.keySet(), new TreeSet<>(Arrays.asList()));
	}

	@Test
	public void getSchemaFilesSameVersionCurrentSnapshotRequiredFinal() {

		Map<Version, Path> fileVersions = new ConcurrentHashMap<>();
		fileVersions.put(Version.of("3.1.0"), Paths.get("mysql-3.1.0.sql"));
		fileVersions.put(Version.of("4.0.0"), Paths.get("mysql-4.0.0.sql"));
		fileVersions.put(Version.of("3.2.0"), Paths.get("mysql-3.2.0.sql"));
		fileVersions.put(Version.of("3.0.0"), Paths.get("mysql-3.0.0.sql"));

		final Optional<Version> currentVersion = Optional.of(Version.of("4.0.0-SNAPSHOT"));
		final Version requiredVersion = Version.of("4.0.0");

		final Map<Version, Path> result = getSchemaFileNamesInRange(fileVersions, currentVersion, requiredVersion);

		Assert.assertEquals("Should contain only latest version (same as required)",
		                    result.keySet(), new TreeSet<>(Arrays.asList(Version.of("4.0.0"))));
	}


	@Test
	public void getSchemaFileNamesInRangeOrderTest() {

		Map<Version, Path> fileVersions = new ConcurrentHashMap<>();
		fileVersions.put(Version.of("3.1.0"), Paths.get("mysql-3.1.0.sql"));
		fileVersions.put(Version.of("4.0.0"), Paths.get("mysql-4.0.0.sql"));
		fileVersions.put(Version.of("3.2.0"), Paths.get("mysql-3.2.0.sql"));
		fileVersions.put(Version.of("3.0.0"), Paths.get("mysql-3.0.0.sql"));

		final Optional<Version> currentVersion = Optional.of(Version.of("3.0.0-SNAPSHOT"));
		final Version requiredVersion = Version.of("4.0.0");

		final Map<Version, Path> result = getSchemaFileNamesInRange(fileVersions, currentVersion, requiredVersion);
		Assert.assertEquals(result.keySet().toArray(),
		                    new Object[]{Version.of("3.0.0"), Version.of("3.1.0"), Version.of("3.2.0"),
		                                 Version.of("4.0.0")});
	}

}