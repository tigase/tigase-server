/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017, "Tigase, Inc." <office@tigase.com>
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

import tigase.component.DSLBeanConfigurator;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.component.exceptions.RepositoryException;
import tigase.conf.ConfigBuilder;
import tigase.conf.ConfigHolder;
import tigase.conf.ConfigReader;
import tigase.conf.ConfigWriter;
import tigase.db.*;
import tigase.db.beans.*;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.beans.selector.ServerBeanSelector;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.Kernel;
import tigase.kernel.core.RegistrarKernel;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.XMPPServer;
import tigase.server.monitor.MonitorRuntime;
import tigase.sys.TigaseRuntime;
import tigase.util.ClassUtilBean;
import tigase.util.DNSResolverFactory;
import tigase.util.Version;
import tigase.util.setup.BeanDefinition;
import tigase.util.setup.SetupHelper;
import tigase.util.ui.console.CommandlineParameter;
import tigase.util.ui.console.ParameterParser;
import tigase.util.ui.console.Task;
import tigase.xmpp.BareJID;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andrzej on 02.05.2017.
 */
public class SchemaManager {

	private static final Logger log = Logger.getLogger(SchemaManager.class.getCanonicalName());

	protected static final Class[] SUPPORTED_CLASSES = {MDPoolBean.class, MDRepositoryBean.class,
														SDRepositoryBean.class};

	private static final Comparator<SchemaInfo> SCHEMA_INFO_COMPARATOR = (o1, o2) -> {
		if (o1.getId().equals("<unknown>") || o2.getId().equals(Schema.SERVER_SCHEMA_ID))
			return 1;
		if (o2.getId().equals("<unknown>") || o1.getId().equals(Schema.SERVER_SCHEMA_ID))
			return -1;
		return o1.getId().compareTo(o2.getId());
	};
	private String adminPass = null;
	private List<BareJID> admins = null;
	private Level logLevel = Level.CONFIG;

	private static Stream<String> getNonCoreComponentNames() {
		return SetupHelper.getAvailableComponents().stream()
				.filter(def -> !def.isCoreComponent())
				.map(BeanDefinition::getName);
	}
	
	private static Stream<String> getActiveNonCoreComponentNames() {
		return SetupHelper.getAvailableComponents().stream()
				.filter(BeanDefinition::isActive)
				.filter(def -> !def.isCoreComponent())
				.map(BeanDefinition::getName);
	}

	private CommandlineParameter ROOT_USERNAME = new CommandlineParameter.Builder("R", DBSchemaLoader.PARAMETERS_ENUM.ROOT_USERNAME.getName())
			.description("Database root account username used to create/remove tigase user and database")
			.build();

	private CommandlineParameter ROOT_PASSWORD = new CommandlineParameter.Builder("A", DBSchemaLoader.PARAMETERS_ENUM.ROOT_PASSWORD.getName())
			.description("Database root account password used to create/remove tigase user and database")
			.secret()
			.build();

	private CommandlineParameter PROPERTY_CONFIG_FILE = new CommandlineParameter.Builder(null, ConfigHolder.PROPERTIES_CONFIG_FILE_KEY.replace("--", ""))
			.defaultValue(ConfigHolder.PROPERTIES_CONFIG_FILE_DEF)
			.description("Path to properties configuration file")
			.requireArguments(true)
			.build();

	private CommandlineParameter TDSL_CONFIG_FILE = new CommandlineParameter.Builder(null, ConfigHolder.TDSL_CONFIG_FILE_KEY.replace("--", ""))
			.defaultValue(ConfigHolder.TDSL_CONFIG_FILE_DEF)
			.description("Path to DSL configuration file")
			.requireArguments(true)
			.build();

	private CommandlineParameter COMPONENTS = new CommandlineParameter.Builder("C", "components").description(
			"List of enabled components identifiers (+/-)")
			.defaultValue(getActiveNonCoreComponentNames().sorted().collect(Collectors.joining(",")))
			.options(getNonCoreComponentNames().sorted().toArray(String[]::new))
			.build();

	private final List<Class<?>> repositoryClasses;
	private Map<String, Object> config;
	
	private RootCredentialsCache rootCredentialsCache = new RootCredentialsCache();

	public static void main(String args[]) throws IOException, ConfigReader.ConfigException {
		try {
			SchemaManager schemaManager = new SchemaManager();
			schemaManager.execute(args);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	public void execute(String args[]) throws Exception {
		String scriptName = System.getProperty("scriptName");
		ParameterParser parser = new ParameterParser(true);

		parser.setTasks(new Task[] {
				new Task.Builder().name("upgrade-schema")
						.description("Upgrade schema of databases specified in your config file - it's not possible to specify parameters")
						.additionalParameterSupplier(this::upgradeSchemaParametersSupplier)
						.function(this::upgradeSchema).build(),
				new Task.Builder().name("install-schema")
						.description("Install schema to database - it requires specifying database parameters where schema will be installed (config file will be ignored)")
						.additionalParameterSupplier(this::installSchemaParametersSupplier)
						.function(this::installSchema).build(),
				new Task.Builder().name("destroy-schema")
						.description("Destroy database and schemas (DANGEROUS)")
						.additionalParameterSupplier(this::destroySchemaParametersSupplier)
						.function(this::destroySchema).build()

		});

		Properties props = parser.parseArgs(args);
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
	                                                                                                                             
	private List<CommandlineParameter> destroySchemaParametersSupplier() {
		List<CommandlineParameter> options = new ArrayList<>();
		options.addAll(Arrays.asList(ROOT_USERNAME, ROOT_PASSWORD, TDSL_CONFIG_FILE, PROPERTY_CONFIG_FILE));
		options.addAll(SchemaLoader.getMainCommandlineParameters(true));
		return options;
	}

	public void destroySchema(Properties props) throws IOException, ConfigReader.ConfigException {
		fixShutdownThreadIssue();
		String type = props.getProperty(DBSchemaLoader.PARAMETERS_ENUM.DATABASE_TYPE.getName());
		Map<String, Object> config = null;
		Optional<String[]> conversionMessages = Optional.empty();
		if (type != null) {
			SchemaLoader loader = SchemaLoader.newInstance(type);
			SchemaLoader.Parameters params = loader.createParameters();
			params.setProperties(props);
			loader.init(params, Optional.ofNullable(rootCredentialsCache));
			String dbUri = loader.getDBUri();

			String[] vhosts = new String[]{DNSResolverFactory.getInstance().getDefaultHost()};
			ConfigBuilder configBuilder = SetupHelper.generateConfig(ConfigTypeEnum.DefaultMode, dbUri, false, false,
																	 Optional.empty(), Optional.empty(), Optional.empty(), vhosts,
																	 Optional.empty(), Optional.empty());

			config = configBuilder.build();
		} else {
			ConfigHolder holder = new ConfigHolder();
			conversionMessages = holder.loadConfiguration(new String[] { ConfigHolder.PROPERTIES_CONFIG_FILE_KEY, PROPERTY_CONFIG_FILE.getValue().get(),
													ConfigHolder.TDSL_CONFIG_FILE_KEY, TDSL_CONFIG_FILE.getValue().get()});

			config = holder.getProperties();
		}

		Optional<String> rootUser = getProperty(props, ROOT_USERNAME);
		Optional<String> rootPass = getProperty(props, ROOT_PASSWORD);
		
		setConfig(config);
		if (rootUser.isPresent() && rootPass.isPresent()) {
			setDbRootCredentials(rootUser.get(), rootPass.get());
		}

		Map<String, DataSourceInfo> result = getDataSources();
		log.info("found " + result.size() + " data sources to destroy...");
		Map<DataSourceInfo, List<SchemaManager.ResultEntry>> results = destroySchemas(result.values());
		log.info("data sources  destruction finished!");
		List<String> output = prepareOutput("Data source destruction finished", results, conversionMessages);

		final int exitCode = isErrorPresent(results) ? 1 : 0;
		final String[] message = output.toArray(new String[output.size()]);
		TigaseRuntime.getTigaseRuntime().shutdownTigase(message, exitCode);
	}

	private List<CommandlineParameter> installSchemaParametersSupplier() {

		List<CommandlineParameter> options = new ArrayList<>();
		options.add(COMPONENTS);
		options.addAll(SchemaLoader.getMainCommandlineParameters(false));
		return options;
	}

	public void installSchema(Properties props) throws IOException, ConfigReader.ConfigException {
		String type = props.getProperty(DBSchemaLoader.PARAMETERS_ENUM.DATABASE_TYPE.getName());
		SchemaLoader loader = SchemaLoader.newInstance(type);
		SchemaLoader.Parameters params = loader.createParameters();
		params.setProperties(props);
		loader.init(params, Optional.ofNullable(rootCredentialsCache));
		String dbUri = loader.getDBUri();

		// split list of components and group them according to enable/disable sign (and none translates to "+"
		final Function<String, List<String>> stringToListFunction = (listStr) -> Arrays.asList(listStr.split(","));

		final Function<String, String> signRemovingFunction = (v) -> (v.startsWith("-") || v.startsWith("+"))
		                                                             ? v.substring(1)
		                                                             : v;
		Map<String, Set<String>> changes = getProperty(props, COMPONENTS, stringToListFunction).orElse(Collections.emptyList())
				.stream()
				.collect(Collectors.groupingBy((v) -> v.startsWith("-") ? "-" : "+",
				                               Collectors.mapping(signRemovingFunction, Collectors.toSet())));




		Set<String> components = getActiveNonCoreComponentNames().collect(Collectors.toSet());

		changes.forEach((k,v) -> {
			switch (k) {
				case "+":
					components.addAll(v);
					break;
				case "-":
					components.removeAll(v);
					break;
			}
		});

		
		String[] vhosts = new String[]{DNSResolverFactory.getInstance().getDefaultHost()};
		ConfigBuilder configBuilder = SetupHelper.generateConfig(ConfigTypeEnum.DefaultMode, dbUri, false, false,
																 Optional.ofNullable(components), Optional.ofNullable(changes.get("+")), Optional.empty(), vhosts,
																 Optional.empty(), Optional.empty());

		admins = params.getAdmins();
		adminPass = params.getAdminPassword();
		logLevel = params.getLogLevel();

		Map<String, Object> config = configBuilder.build();
		Map<SchemaManager.DataSourceInfo, List<SchemaManager.ResultEntry>> results = loadSchemas(config, props);

		List<String> output = prepareOutput("Schema installation finished", results, Optional.empty());
		output.add("");
		output.add("Example " + ConfigHolder.TDSL_CONFIG_FILE_DEF + " configuration file:");
		output.add("");
		try (StringWriter writer = new StringWriter()) {
			new ConfigWriter().write(writer, config);
			output.addAll(Arrays.stream(writer.toString().split("\n")).collect(Collectors.toList()));
		}
		final int exitCode = isErrorPresent(results) ? 1 : 0;
		final String[] message = output.toArray(new String[output.size()]);

		TigaseRuntime.getTigaseRuntime().shutdownTigase(message, exitCode);
	}

	private List<CommandlineParameter> upgradeSchemaParametersSupplier() {
		return Arrays.asList(ROOT_USERNAME, ROOT_PASSWORD, TDSL_CONFIG_FILE, PROPERTY_CONFIG_FILE);
	}

	public void upgradeSchema(Properties props) throws IOException, ConfigReader.ConfigException {
		ConfigHolder holder = new ConfigHolder();
		Optional<String[]> conversionMessages = holder.loadConfiguration(new String[] { ConfigHolder.PROPERTIES_CONFIG_FILE_KEY, PROPERTY_CONFIG_FILE.getValue().get(),
																	 ConfigHolder.TDSL_CONFIG_FILE_KEY, TDSL_CONFIG_FILE.getValue().get()});

		Map<String, Object> config = holder.getProperties();
		Map<SchemaManager.DataSourceInfo, List<SchemaManager.ResultEntry>> results = loadSchemas(config, props);
		List<String> output = prepareOutput("Schema upgrade finished", results, conversionMessages);

		final int exitCode = isErrorPresent(results) ? 1 : 0;
		final String[] message = output.toArray(new String[output.size()]);

		TigaseRuntime.getTigaseRuntime().shutdownTigase(message, exitCode);

	}

	private boolean isErrorPresent(Map<DataSourceInfo, List<ResultEntry>> results) {
		return results.values()
					.stream()
					.flatMap(Collection::stream)
					.map(entry -> entry.result)
					.anyMatch(r -> r == SchemaLoader.Result.error);
	}

	private Map<SchemaManager.DataSourceInfo, List<SchemaManager.ResultEntry>> loadSchemas(Map<String, Object> config, Properties props) throws IOException, ConfigReader.ConfigException {
		Optional<String> rootUser = getProperty(props, ROOT_USERNAME);
		Optional<String> rootPass = getProperty(props, ROOT_PASSWORD);

		setConfig(config);
		if (rootUser.isPresent() && rootPass.isPresent()) {
			setDbRootCredentials(rootUser.get(), rootPass.get());
		}

		log.info("beginning upgrade...");
		Map<SchemaManager.DataSourceInfo, List<SchemaManager.ResultEntry>> results = loadSchemas();
		log.info("schema upgrade finished!");
		return results;
	}

	private List<String> prepareOutput(String title, Map<DataSourceInfo, List<SchemaManager.ResultEntry>> results, Optional<String[]> conversionMessages) {
		List<String> output = new ArrayList<>(Arrays.asList("\t" + title));
		conversionMessages.ifPresent(msgs -> {
			output.add("");
			output.addAll(Arrays.asList(msgs));
		});
		results.forEach((k,v) -> {
			output.add("");
			output.add("Data source: " + k.getName() + " with uri " + k.getResourceUri());
			v.forEach(r -> {
				output.add("\t" + r.name + "\t" + r.result);
				if (r.result != SchemaLoader.Result.ok && r.message != null) {
					String[] lines = r.message.split("\n");
					for (int i=0; i<lines.length; i++) {
						if (i == 0) {
							output.add("\t\tMessage: " + lines[0]);
						} else {
							output.add("\t\t         " + lines[i]);
						}
					}
				}
			});
		});
		return output;
	}

	public static Optional<String> getProperty(Properties props, CommandlineParameter parameter) {
		Optional<String> value = Optional.ofNullable(props.getProperty(parameter.getFullName(false).get()));
		if (!value.isPresent()) {
			return parameter.getDefaultValue();
		}
		return value;
	}

	public static <T> Optional<T> getProperty(Properties props, CommandlineParameter parameter, Function<String, T> converter) {
		Optional<String> value = getProperty(props, parameter);
		if (!value.isPresent()) {
			return Optional.empty();
		}
		T result = converter.apply(value.get());
		return Optional.ofNullable(result);
	}

	public SchemaManager() {
		repositoryClasses = ClassUtilBean.getInstance()
				.getAllClasses()
				.stream()
				.filter(clazz -> Arrays.stream(SUPPORTED_CLASSES)
						.anyMatch(supClazz -> supClazz.isAssignableFrom(clazz)))
				.filter(clazz -> {
					Bean bean = clazz.getAnnotation(Bean.class);
					return bean != null && (bean.parent() != Object.class || bean.parents().length > 0);
				})
				.filter(clazz -> !DataSourceBean.class.isAssignableFrom(clazz))
				.collect(Collectors.toList());

		log.log(Level.FINE, "found following data source related classes: {0}", repositoryClasses);
	}

	public void readConfig(File file) throws IOException, ConfigReader.ConfigException {
		config = new ConfigReader().read(file);
	}

	public void readConfig(String configString) throws IOException, ConfigReader.ConfigException {
		try (StringReader reader = new StringReader(configString)) {
			readConfig(reader);
		}
	}

	public void readConfig(Reader reader) throws IOException, ConfigReader.ConfigException {
		config = new ConfigReader().read(reader);
	}

	public void setAdmins(List<BareJID> admins, String adminPass) {
		this.admins = admins;
		this.adminPass = adminPass;
	}

	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

	public void setDbRootCredentials(String user, String pass) {
		rootCredentialsCache.set(null, new RootCredentials(user, pass));
	}

	public Map<DataSourceInfo, List<SchemaInfo>> getDataSourcesAndSchemas() {
		Kernel kernel = prepareKernel();
		List<BeanConfig> repoBeans = getRepositoryBeans(kernel);
		List<RepoInfo> repositories = getRepositories(kernel, repoBeans);
		Map<DataSourceInfo, List<RepoInfo>> repositoriesByDataSource = repositories
				.stream()
				.collect(Collectors.groupingBy(RepoInfo::getDataSource, Collectors.toList()));

		return collectSchemasByDataSource(repositoriesByDataSource);
	}

	public Map<DataSourceInfo, List<ResultEntry>> destroySchemas(Collection<DataSourceInfo> dataSources) {
		return dataSources.stream().map(e -> new Pair<>(e, destroySchemas(e))).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	public List<ResultEntry> destroySchemas(DataSource ds) {
		return executeWithSchemaLoader(ds, (schemaLoader, handler) -> {
			List<ResultEntry> results = new ArrayList<>();
			log.log(Level.FINEST, "removing database for data source " + ds);
			results.add(new ResultEntry("Destroying data source", schemaLoader.destroyDataSource(), handler));
			return results;
		});
	}

	public Map<DataSourceInfo, List<ResultEntry>> loadSchemas() {
		Map<DataSourceInfo, List<SchemaInfo>> dataSourceSchemas = getDataSourcesAndSchemas();
		return dataSourceSchemas.entrySet()
				.stream()
				.map(e -> new Pair<DataSourceInfo, List<ResultEntry>>(e.getKey(), loadSchemas(e.getKey(), e.getValue())))
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	public List<ResultEntry> loadSchemas(DataSource ds, List<SchemaInfo> schemas) {
		final List<SchemaInfo> validSchemas = schemas.stream().filter(SchemaInfo::isValid).collect(Collectors.toList());

		if (validSchemas.isEmpty()) {
			log.log(Level.FINER, "no known schemas for data source " + ds + ", skipping schema loading...");
			return Collections.emptyList();
		}

		return executeWithSchemaLoader(ds, (schemaLoader, handler) -> {
			List<ResultEntry> results = new ArrayList<>();
			results.add(new ResultEntry("Checking if database exists", schemaLoader.validateDBExists(), handler));
			log.log(Level.FINER, "loading schemas for data source " + ds);
			schemas.sort(SCHEMA_INFO_COMPARATOR);

			results.add(new ResultEntry("Loading Common Schema Files", schemaLoader.loadSchema("common", null), handler));

			for (SchemaInfo schema : validSchemas) {
				Version version = schema.getVersion().get();
				Version versionInDb = schemaLoader.getComponentVersionFromDb(schema.getId());

				ResultEntry schemaLoadResultEntry;
				if (!Version.TYPE.FINAL.equals(version.getVersionType())
						|| (!versionInDb.getBaseVersion().equals(version.getBaseVersion()))) {
					log.log(Level.FINER, "loading schema with id ='" + schema + "'");
					schemaLoadResultEntry = new ResultEntry(
							"Loading schema: " + schema.getName() + ", version: " + version + " (had database schema: " + versionInDb + ")",
							schemaLoader.loadSchema(schema.getId(), version.getBaseVersion().toString()), handler);
				} else {
					log.log(Level.FINER, "Skipped loading schema with id ='" + schema + "'");
					schemaLoadResultEntry = new ResultEntry(
							"Skipping schema: " + schema.getName() + ", version: " + version + " (had database schema: " + versionInDb + ")",
							SchemaLoader.Result.skipped, handler);
				}
				results.add(schemaLoadResultEntry);
			}

			if (schemas.stream().anyMatch(schema -> Schema.SERVER_SCHEMA_ID.equals(schema.getId()))) {
				results.add(new ResultEntry("Adding XMPP admin accounts", schemaLoader.addXmppAdminAccount(), handler));
			}

			results.add(new ResultEntry("Post installation action", schemaLoader.postInstallation(), handler));
			return results;
		});
	}

	private List<ResultEntry> executeWithSchemaLoader(DataSource ds, SchemaLoaderExecutor function) {
		SchemaLoader schemaLoader = SchemaLoader.newInstanceForURI(ds.getResourceUri());
		List<ResultEntry> results = new ArrayList<>();

		Logger logger = java.util.logging.Logger.getLogger(schemaLoader.getClass().getCanonicalName());
		SchemaManagerLogHandler handler = Arrays.stream(logger.getHandlers())
				.filter(h -> h instanceof SchemaManagerLogHandler)
				.map(h -> (SchemaManagerLogHandler) h)
				.findAny()
				.orElseGet(() -> {
					SchemaManagerLogHandler handler1 = new SchemaManagerLogHandler();
					logger.addHandler(handler1);
					return handler1;
				});
		handler.setLevel(java.util.logging.Level.FINEST);
		logger.setLevel(java.util.logging.Level.FINEST);

		SchemaLoader.Parameters params = schemaLoader.createParameters();
		params.parseUri(ds.getResourceUri());
		params.setAdmins(admins, adminPass);
		params.setLogLevel(logLevel);
		schemaLoader.init(params, Optional.ofNullable(rootCredentialsCache));

		results.add(new ResultEntry("Checking connection to database", schemaLoader.validateDBConnection(), handler));

		results.addAll(function.execute(schemaLoader, handler));

		schemaLoader.shutdown();
		return results;
	}

	private Map<String, DataSourceInfo> getDataSources() {
		Map<String, DataSourceInfo> dataSources = ((Map<String, Object>) config.get("dataSource")).values()
				.stream()
				.filter(v -> v instanceof AbstractBeanConfigurator.BeanDefinition)
				.map(v -> (AbstractBeanConfigurator.BeanDefinition) v)
				.filter(AbstractBeanConfigurator.BeanDefinition::isActive)
				.map(this::createDataSourceInfo)
				.collect(Collectors.toMap(DataSourceInfo::getName, Function.identity()));
		return dataSources;
	}

	private DataSourceInfo createDataSourceInfo(AbstractBeanConfigurator.BeanDefinition def) {
		Object v = def.getOrDefault("uri", def.get("repo-uri"));
		return new DataSourceInfo(def.getBeanName(), v instanceof ConfigReader.Variable
		                                             ? ((ConfigReader.Variable) v).calculateValue()
				                                             .toString()
		                                             : v.toString());
	}

	private List<RepoInfo> getRepositories(Kernel kernel, List<BeanConfig> repoBeans) {
		DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
		Map<String, DataSourceInfo> dataSources = getDataSources();
		return repoBeans.stream().flatMap(bc -> {
			try {
				if (SDRepositoryBean.class.isAssignableFrom(bc.getClazz())) {
					String dataSourceName = getDataSourceNameOr(configurator, bc, "default");
					DataSourceInfo dataSource = dataSources.get(dataSourceName);
					Class<?> implementation = getRepositoryImplementation(configurator, dataSource, bc, null);
					return Stream.of(new RepoInfo(bc, dataSource, implementation));
				} else {
					return bc.getKernel()
							.getDependencyManager()
							.getBeanConfigs()
							.stream()
							.filter(bc1 -> !Kernel.class.isAssignableFrom(bc1.getClazz()))
							.filter(bc1 -> !Kernel.DelegatedBeanConfig.class.isAssignableFrom(bc1.getClass()))
							.map(bc1 -> {
								try {
									String dataSourceName = getDataSourceNameOr(configurator, bc1, bc1.getBeanName());
									DataSourceInfo dataSource = dataSources.get(dataSourceName);
									Class<?> implementation = getRepositoryImplementation(configurator, dataSource, bc1,
																						  bc);
									return new RepoInfo(bc1, dataSource, implementation);
								} catch (Exception ex) {
									ex.printStackTrace();
									return null;
								}
							});
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				return Stream.empty();
			}
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private Map<DataSourceInfo, List<SchemaInfo>> collectSchemasByDataSource(Map<DataSourceInfo, List<RepoInfo>> repositoriesByDataSource) {
		Map<DataSourceInfo, List<SchemaInfo>> dataSourceSchemas = new HashMap<>();
		for (Map.Entry<DataSourceInfo,List<RepoInfo>> entry : repositoriesByDataSource.entrySet()) {

			List<SchemaInfo> schemas = entry.getValue()
					.stream()
					.collect(Collectors.groupingBy(this::getSchemaId, Collectors.toList()))
					.entrySet()
					.stream()
					.map(e -> {
						final Repository.SchemaId annotation = e.getValue()
								     .iterator()
								     .next()
								     .getImplementation()
								     .getAnnotation(Repository.SchemaId.class);
						     return new SchemaInfo(annotation, e.getValue());
					     }
					)
					.collect(Collectors.toList());

			dataSourceSchemas.put(entry.getKey(), schemas);
		}

		return dataSourceSchemas;
	}

	private String getSchemaId(RepoInfo repoInfo) {
		Repository.SchemaId schemaId = repoInfo.getImplementation().getAnnotation(Repository.SchemaId.class);
		return schemaId == null ? "<unknown>" : schemaId.id();
	}

	private String getDataSourceNameOr(DSLBeanConfigurator configurator, BeanConfig bc, String defValue) {
		Map<String, Object> cfg = configurator.getConfiguration(bc);
		return (String) cfg.getOrDefault("dataSourceName", cfg.getOrDefault("data-source", defValue));
	}

	private Class<?> getRepositoryImplementation(DSLBeanConfigurator configurator, DataSourceInfo dataSource,
												 BeanConfig beanConfig, BeanConfig mdRepoBeanConfig)
			throws ClassNotFoundException, DBInitException, IllegalAccessException, InstantiationException,
				   NoSuchMethodException, InvocationTargetException {
		Map<String, Object> cfg = configurator.getConfiguration(beanConfig);
		String cls = (String) cfg.getOrDefault("cls", cfg.get("repo-cls"));
		if (cls != null) {
			return ModulesManagerImpl.getInstance().forName(cls);
		}

		Object bean = beanConfig.getClazz().newInstance();
		if (bean instanceof MDPoolConfigBean) {
			Method m = MDPoolConfigBean.class.getDeclaredMethod("getRepositoryIfc");
			m.setAccessible(true);
			return DataSourceHelper.getDefaultClass((Class<?>) m.invoke(bean), dataSource.getResourceUri());
		}
		if (bean instanceof SDRepositoryBean) {
			Method m = SDRepositoryBean.class.getDeclaredMethod("findClassForDataSource", DataSource.class);
			m.setAccessible(true);
			return (Class<?>) m.invoke(bean, dataSource);
		}
		if (mdRepoBeanConfig != null) {
			Object mdRepoBean = mdRepoBeanConfig.getClazz().newInstance();
			if (mdRepoBean instanceof MDRepositoryBean) {
				Method m = MDRepositoryBean.class.getDeclaredMethod("findClassForDataSource", DataSource.class);
				m.setAccessible(true);
				return (Class<?>) m.invoke(mdRepoBean, dataSource);
			}
		}
		throw new RuntimeException("Unknown repository!");
	}

	private Kernel prepareKernel() {
		Kernel kernel = new Kernel("root");
		try {
			if (XMPPServer.isOSGi()) {
				kernel.registerBean("classUtilBean")
						.asInstance(Class.forName("tigase.osgi.util.ClassUtilBean").newInstance())
						.exportable()
						.exec();
			} else {
				kernel.registerBean("classUtilBean")
						.asInstance(Class.forName("tigase.util.ClassUtilBean").newInstance())
						.exportable()
						.exec();
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		// register default types converter and properties bean configurator
		kernel.registerBean(DefaultTypesConverter.class).exportable().exec();
		kernel.registerBean(DSLBeanConfiguratorWithBackwardCompatibility.class).exportable().exec();
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();

		DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
		configurator.setProperties(config);
		ModulesManagerImpl.getInstance().setBeanConfigurator(configurator);

		kernel.registerBean("beanSelector").asInstance(new ServerBeanSelector()).exportable().exec();

		return kernel;
	}

	private List<BeanConfig> getRepositoryBeans(Kernel kernel) {
		DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
		configurator.registerBeans(null, null, config);

		List<BeanConfig> repoBeans = crawlKernel(repositoryClasses, kernel, configurator, config);
		fixShutdownThreadIssue();
		return repoBeans;
	}

	private void fixShutdownThreadIssue() {
		MonitorRuntime.getMonitorRuntime();
		try {
			Field f = MonitorRuntime.class.getDeclaredField("mainShutdownThread");
			f.setAccessible(true);
			MonitorRuntime monitorRuntime = MonitorRuntime.getMonitorRuntime();
			Runtime.getRuntime().removeShutdownHook((Thread) f.get(monitorRuntime));
		} catch (NoSuchFieldException | IllegalAccessException ex) {
			log.log(Level.FINEST, "There was an error with unregistration of shutdown hook", ex);
		}
	}

	private List<BeanConfig> crawlKernel(List<Class<?>> repositoryClasses, Kernel kernel,
										 DSLBeanConfigurator configurator, Map<String, Object> config) {
		List<BeanConfig> results = new ArrayList<>();
		kernel.getDependencyManager()
				.getBeanConfigs()
				.stream()
				.filter(bc -> bc.getState() == BeanConfig.State.registered)
				.filter(bc -> !Kernel.DelegatedBeanConfig.class.isAssignableFrom(bc.getClass()))
				.forEach(bc -> {
					try {
						Class<?> clazz = bc.getClazz();
						if ("tigase.muc.cluster.MUCComponentClustered".equals(clazz.getName())) {
							clazz = ModulesManagerImpl.getInstance().forName("tigase.muc.MUCComponent");
						}
						if ("tigase.pubsub.cluster.PubSubComponentClustered".equals(clazz.getName())) {
							clazz = ModulesManagerImpl.getInstance().forName("tigase.pubsub.PubSubComponent");
						}
						Object bean = clazz.newInstance();
						if (RegistrarBean.class.isAssignableFrom(clazz)) {
							RegistrarKernel k = new RegistrarKernel();
							k.setName(bc.getBeanName());
							bc.getKernel().registerBean(bc.getBeanName() + "#KERNEL").asInstance(k).exec();
							Method m = bc.getClass().getDeclaredMethod("setKernel", Kernel.class);
							m.setAccessible(true);
							m.invoke(bc, k);

							Kernel parent = bc.getKernel().getParent();
							// without this line setBeanActive() fails
							//parent.ln(beanConfig.getBeanName(), beanConfig.getKernel(), beanConfig.getBeanName());
							parent.ln(bc.getBeanName(), bc.getKernel(), "service");

							((RegistrarBean) bean).register(bc.getKernel());
							Map<String, Object> cfg = (Map<String, Object>) config.getOrDefault(bc.getBeanName(), new HashMap<>());
							configurator.registerBeans(bc, bean, cfg);
							results.addAll(crawlKernel(repositoryClasses, bc.getKernel(), configurator, cfg));
						}

						if (repositoryClasses.stream().anyMatch(repoClazz -> repoClazz.isAssignableFrom(bc.getClazz()))) {
							results.add(bc);
						}

					} catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException ex) {
						log.log(Level.SEVERE, "Exception while crawling kernel", ex);
					} catch (StackOverflowError ex) {
						ex.printStackTrace();
						Kernel k = bc.getKernel();
						List<String> list = new ArrayList<>();
						do {
							list.add(k.getName());
						} while ((k = k.getParent()) != null);
						log.log(Level.SEVERE, "exception in path " + list);
					}
				});
		return results;
	}

	public static class DataSourceInfo
			implements DataSource {

		private final String name;
		private final String uri;

		private DataSourceInfo(String name, String uri) {
			this.name = name;
			this.uri = uri;
		}

		public String getName() {
			return name;
		}

		@Override
		public String getResourceUri() {
			return uri;
		}

		@Override
		public void initialize(String connStr) throws RepositoryException {
			// nothing to do
		}

		@Override
		@Deprecated
		public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
			// nothing to do
		}

		@Override
		public String toString() {
			return name + "[uri=" + uri + "]";
		}
	}

	public static class RepoInfo {

		private final BeanConfig beanConfig;
		private final DataSourceInfo dataSource;
		private final Class<?> implementation;

		public RepoInfo(BeanConfig beanConfig, DataSourceInfo dataSource, Class<?> implementation) {
			this.beanConfig = beanConfig;
			this.dataSource = dataSource;
			this.implementation = implementation;
		}

		public DataSourceInfo getDataSource() {
			return dataSource;
		}

		public Class<?> getImplementation() {
			return implementation;
		}

		@Override
		public String toString() {
			return beanConfig.getBeanName() + "[dataSource=" + dataSource.getName() + ", class=" + implementation + "]";
		}
	}

	public static class SchemaInfo {

		private final Repository.SchemaId schema;
		private final List<RepoInfo> repositories;

		public SchemaInfo(Repository.SchemaId schema, List<RepoInfo> repositories) {
			this.schema = schema;
			this.repositories = repositories;
		}

		public String getId() {
			return (schema == null ? "<unknown>" : schema.id());
		}

		public String getName() {
			return (schema != null ? schema.name() : "");
		}

		public List<RepoInfo> getRepositories() {
			return repositories;
		}

		public Optional<Version> getVersion() {
			return repositories.stream()
					.map(RepoInfo::getImplementation)
					.map(Class::getPackage)
					.map(Package::getImplementationVersion)
					.filter(Objects::nonNull)
					.map(Version::of)
					.findAny();
		}
		
		public boolean isValid() {
			return schema != null && getVersion().isPresent();
		}                          

		@Override
		public String toString() {
			return "SchemaInfo[id=" + (schema == null ? "<unknown>" : schema.id()) + ", repositories=" + repositories + "]";
		}
	}

	public static class ResultEntry {

		public final String name;
		public final SchemaLoader.Result result;
		public final String message;

		private ResultEntry(String name, SchemaLoader.Result result, SchemaManagerLogHandler logHandler) {
			this.name = name;
			this.result = result;
			LogRecord rec;
			StringBuilder sb = null;
			while ((rec = logHandler.poll()) != null) {
				if (rec.getLevel().intValue() <= Level.FINE.intValue()) {
					continue;
				}
				if (rec.getMessage() == null) {
					continue;
				}
				if (sb == null) {
					sb = new StringBuilder();
				} else {
					sb.append("\n");
				}
				sb.append(String.format(rec.getMessage(), rec.getParameters()));
			}
			this.message = sb == null ? null : sb.toString();
		}

	}

	public static class Pair<K,V> {

		private final K key;
		private final V value;

		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}
	}

	public interface SchemaLoaderExecutor {
		List<ResultEntry> execute(SchemaLoader schemaLoader, SchemaManagerLogHandler handler);
	}

	public static class RootCredentialsCache {

		private final Map<String, RootCredentials> cache = new ConcurrentHashMap<>();

		public RootCredentials get(String server) {
			return cache.getOrDefault(createKey(server), cache.get("default"));
		}

		public void set(String server, RootCredentials credentials) {
			cache.put(createKey(server), credentials);
		}

		private String createKey(String server) {
			if (server == null) {
				return "default";
			}
			return server;
		}

	}

	public static class RootCredentials {

		public final String user;
		public final String password;

		public RootCredentials(String user, String password) {
			this.user = user;
			this.password = password;
		}
	}
}
