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

import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import tigase.db.util.SchemaLoader;
import tigase.db.util.SchemaManager;
import tigase.kernel.AbstractKernelTestCase;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract class providing support for classes requiring DataSource instances.
 * 
 * @param <DS>
 */
public class AbstractDataSourceTestCase<DS extends DataSource> extends AbstractKernelTestCase {

	protected static String uri = System.getProperty("testDbUri");
	@ClassRule
	public static TestRule rule = new TestRule() {
		@Override
		public Statement apply(Statement stmnt, Description d) {
			if (uri == null) {
				return new Statement() {
					@Override
					public void evaluate() throws Throwable {
						Assume.assumeTrue("Ignored due to not passed DB URI!", false);
					}
				};
			}
			return stmnt;
		}
	};

	private DS dataSource;

	@AfterClass
	public static void cleanDerby() {
		if (uri.contains("jdbc:derby:")) {
			File f = new File("derby_test");
			if (f.exists()) {
				if (f.listFiles() != null) {
					Arrays.asList(f.listFiles()).forEach(f2 -> {
						if (f2.listFiles() != null) {
							Arrays.asList(f2.listFiles()).forEach(f3 -> f3.delete());
						}
						f2.delete();
					});
				}
				f.delete();
			}
		}
	}

	protected static void loadSchema(String schemaId, String schemaVersion, Set<String> components) throws DBInitException {
		SchemaLoader loader = SchemaLoader.newInstanceForURI(uri);
		SchemaLoader.Parameters params = loader.createParameters();
		params.parseUri(uri);
		params.setDbRootCredentials(null, null);
		params.setSchemaDirectory("src/main/database/");
		loader.init(params, Optional.empty());
		loader.validateDBConnection();
		loader.validateDBExists();
		Assert.assertNotEquals(SchemaLoader.Result.error, loader.loadCommonSchema());
		Optional<SchemaManager.SchemaInfo> schemaInfo = SchemaManager.getDefaultSchemaFor(uri, schemaId, components);
		Assert.assertEquals(SchemaLoader.Result.ok, loader.loadSchema(schemaInfo.get(), schemaVersion));
		loader.postInstallation();
		loader.shutdown();
	}

	protected DS getDataSource() {
		return dataSource;
	}

	@Before
	public void setupDataSource() throws Exception {
		dataSource = prepareDataSource();
		getKernel().registerBean("dataSource").asInstance(dataSource).setPinned(false).exportable().exec();
	}
	
	protected DS prepareDataSource() throws DBInitException, IllegalAccessException, InstantiationException {
		DataSource dataSource = DataSourceHelper.getDefaultClass(DataSource.class, uri)
				.newInstance();//RepositoryFactory.getRepoClass(DataSource.class, uri).newInstance();
		dataSource.initRepository(uri, new HashMap<>());
		return (DS) dataSource;
	}

}
