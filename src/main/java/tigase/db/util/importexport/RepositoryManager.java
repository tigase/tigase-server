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
package tigase.db.util.importexport;

import tigase.component.exceptions.RepositoryException;
import tigase.conf.ConfigHolder;
import tigase.conf.ConfigReader;
import tigase.db.UserRepository;
import tigase.db.comp.ConfigRepository;
import tigase.db.util.SchemaManager;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.Kernel;
import tigase.util.ClassUtil;
import tigase.util.ui.console.CommandlineParameter;
import tigase.util.ui.console.ParameterParser;
import tigase.util.ui.console.Task;
import tigase.vhosts.VHostItemDefaults;
import tigase.vhosts.VHostItemExtensionManager;
import tigase.vhosts.VHostJDBCRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RepositoryManager {

	public static boolean isSet(CommandlineParameter parameter) throws Exception {
		String value = parameter.getValue().or(parameter::getDefaultValue).orElseThrow();
		return Boolean.parseBoolean(value);
	}
	
	private static final Logger log = Logger.getLogger(RepositoryManager.class.getSimpleName());

	private final CommandlineParameter EXPORT_TO = new CommandlineParameter.Builder(null, "to").description(
					"Path to export data to")
			.required(true)
			.build();
	private final CommandlineParameter IMPORT_FROM = new CommandlineParameter.Builder(null, "from").description(
					"Path to omport data from")
			.required(true)
			.build();
	private final CommandlineParameter TDSL_CONFIG_FILE = new CommandlineParameter.Builder(null,
																						   "config-file").description(
			"Path to Tigase XMPP Server config file")
			.defaultValue(ConfigHolder.TDSL_CONFIG_FILE_DEF)
			.required(false)
			.build();

	private final CommandlineParameter DEBUG = new CommandlineParameter.Builder(null, "debug").description(
			"Enable verbose logging").defaultValue("false").required(false).requireArguments(false).build();

	private static final String LOGGING_CONFIG = """
handlers = java.util.logging.ConsoleHandler
.level = ALL
java.util.logging.ConsoleHandler.formatter = tigase.util.log.LogFormatter
			""";
	public void execute(String args[]) throws Exception {
		configureLogging(Level.SEVERE);
		String scriptName = System.getProperty("scriptName");
		ParameterParser parser = new ParameterParser(true);

		extensions = ClassUtil.getClassesImplementing(RepositoryManagerExtension.class)
				.stream()
				.map(clazz -> {
					try {
						return clazz.getConstructor().newInstance();
					} catch (Throwable ex) {
						log.log(Level.WARNING, ex.getMessage(), ex);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.toList();
		
		parser.setTasks(new Task[]{new Task.Builder().name("export-data")
										   .description("Export data to XML")
										   .additionalParameterSupplier(() -> Stream.concat(Stream.of(TDSL_CONFIG_FILE,
																									  EXPORT_TO, DEBUG),
																							extensions.stream()
																									.flatMap(
																											RepositoryManagerExtension::getExportParameters))
												   .toList())
										   .function(this::exportData).build(), new Task.Builder().name("import-data")
										   .description("Import data from XML")
										   .additionalParameterSupplier(() -> Stream.concat(Stream.of(TDSL_CONFIG_FILE,
																									  IMPORT_FROM, DEBUG),
																							extensions.stream()
																									.flatMap(
																											RepositoryManagerExtension::getImportParameters))
												   .toList())
										   .function(this::importData).build()});
		Properties props = null;
		try {
			props = parser.parseArgs(args);
		} catch (IllegalArgumentException ex) {
			// IllegalArgumentException is thrown if any of required parameters is missing
			// We can ignore it and just display help for this command.
			ex.printStackTrace();
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, ex.getMessage(), ex);
			}
		}
		Optional<Task> task = parser.getTask();
		if (props != null && task.isPresent()) {
			task.get().execute(props);
		} else {
			String executionCommand = null;
			if (scriptName != null) {
				executionCommand = "$ " + scriptName + " [task] [params-file.conf] [options]" + "\n\t\t" +
						"if the option defines default then <value> is optional";
			}

			System.out.println(parser.getHelp(executionCommand));
		}
	}

	public static void configureLogging(Level level) throws IOException {
		String config =  """
handlers = java.util.logging.ConsoleHandler
.level = ALL
tigase.xml.level = SEVERE
java.util.logging.ConsoleHandler.level = """ + level.getName() + """

java.util.logging.ConsoleHandler.formatter = tigase.util.log.LogFormatter""";
		byte[] data = config.getBytes(StandardCharsets.UTF_8);
		try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
			LogManager.getLogManager().reset();
			LogManager.getLogManager()
					.updateConfiguration(in, (k) -> k.endsWith(".handlers")
													? ((o, n) -> (o == null ? n : o))
													: ((o, n) -> n));
		}
	}

	private Path rootPath;
	private Map<String, Object> config;
	private Kernel kernel;
	private DataSourceHelper dataSourceHelper;
	private RepositoryHolder repositoryHolder;
	private List<RepositoryManagerExtension> extensions = new ArrayList<>();
	private VHostJDBCRepository vhostRepository;

	private void initialize(CommandlineParameter fileParam, Properties properties)
			throws ConfigReader.ConfigException, IOException, ClassNotFoundException, RepositoryException,
				   InstantiationException, IllegalAccessException, NoSuchFieldException {
		rootPath = Paths.get(properties.getProperty(fileParam.getFullName().get()));
		ConfigHolder holder = new ConfigHolder();
		holder.loadConfiguration(new String[] { ConfigHolder.TDSL_CONFIG_FILE_KEY,
												TDSL_CONFIG_FILE.getValue().orElseThrow()
		});
		config = holder.getProperties();
		config.remove("cluster-mode");
		kernel = SchemaManager.prepareKernel(config);
		List<BeanConfig> repoBeans = SchemaManager.getRepositoryBeans(kernel, SchemaManager.getRepositoryClasses(), config);

		List<SchemaManager.RepoInfo> repositories = SchemaManager.getRepositories(kernel, repoBeans, config);

		Map<String, SchemaManager.RepoInfo> userRepoMap = repositories.stream()
				.filter(repoInfo -> UserRepository.class.isAssignableFrom(repoInfo.getImplementation()))
				.collect(Collectors.toMap(repoInfo -> repoInfo.getDataSource().getName(), Function.identity()));

		dataSourceHelper = new DataSourceHelper(
				repositories.stream().map(SchemaManager.RepoInfo::getDataSource).distinct().toList());
		repositoryHolder = new RepositoryHolder(dataSourceHelper, repositories);

		vhostRepository = new VHostJDBCRepository();
		vhostRepository.setRepo(repositoryHolder.getDefaultRepository(UserRepository.class));
		vhostRepository.setMainVHostName((String) config.get("default-virtual-host"));
		vhostRepository.setExtensionManager(new VHostItemExtensionManager());
		vhostRepository.setVhostDefaultValues(new VHostItemDefaults());
		vhostRepository.reload();
		// mark vhostRepository as initialized to allow saving items (not enabled autoreload intentionally)
		Field f = ConfigRepository.class.getDeclaredField("initialized");
		f.setAccessible(true);
		f.set(vhostRepository, true);
		
		for (RepositoryManagerExtension extension : extensions) {
			extension.initialize(kernel, dataSourceHelper, repositoryHolder, rootPath);
		}

		Level level = Boolean.parseBoolean(DEBUG.getValue().orElseThrow()) ? Level.FINEST : Level.INFO;
		configureLogging(level);
		log.finest("extensions: " + extensions);
	}

	private void importData(Properties properties) throws Exception {
		initialize(IMPORT_FROM, properties);
		if (!Files.exists(rootPath)) {
			throw new RuntimeException("Source directory does not exist");
		}

		Importer importer = new Importer(repositoryHolder, vhostRepository, extensions, rootPath);
		importer.process(rootPath.resolve("server-data.xml"));
	}

	private void exportData(Properties properties) throws Exception {
		initialize(EXPORT_TO, properties);

		if (!Files.exists(rootPath)) {
			Files.createDirectories(rootPath);
		}

		Exporter exporter = new Exporter(repositoryHolder, vhostRepository, extensions, rootPath);
		exporter.export("server-data.xml");
	}
	
	@FunctionalInterface
	public interface ThrowingConsumer<X> {
		void accept(X x) throws Exception;
	}

	public static void main(String args[]) throws IOException, ConfigReader.ConfigException {
		try {
			RepositoryManager repositoryManager = new RepositoryManager();
			repositoryManager.execute(args);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Error while executing task", ex);
		} finally {
			System.exit(0);
		}
	}
}
