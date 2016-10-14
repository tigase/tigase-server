/*
 * ConfiguratorAbstract.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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
 *
 */



package tigase.conf;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.*;
import tigase.db.comp.ComponentRepository;
import tigase.db.comp.RepositoryChangeListenerIfc;
import tigase.io.TLSUtil;
import tigase.server.AbstractComponentRegistrator;
import tigase.server.ServerComponent;
import tigase.stats.StatisticsContainer;
import tigase.stats.StatisticsList;
import tigase.util.DNSResolverFactory;
import tigase.util.DataTypes;
import tigase.xmpp.BareJID;

import javax.script.Bindings;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static tigase.io.SSLContextContainerIfc.*;

//~--- JDK imports ------------------------------------------------------------

/**
 * Created: Dec 7, 2009 4:15:31 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class ConfiguratorAbstract
				extends AbstractComponentRegistrator<Configurable>
				implements RepositoryChangeListenerIfc<ConfigItem>, StatisticsContainer {
	/**
	 * Field description
	 * @deprecated moved to RepositoryFactory
	 */
	@Deprecated
	public static final String AUTH_DOMAIN_POOL_CLASS_PROP_KEY = RepositoryFactory
			.AUTH_DOMAIN_POOL_CLASS_PROP_KEY;

	/**
	 * Field description
	 * @deprecated moved to RepositoryFactory
	 */
	@Deprecated
	public static final String AUTH_DOMAIN_POOL_CLASS_PROP_VAL = RepositoryFactory
			.AUTH_DOMAIN_POOL_CLASS_PROP_VAL;

	/** Field description */
	public static final String CONFIG_REPO_CLASS_INIT_KEY = "--tigase-config-repo-class";

	/** Field description */
	public static final String CONFIG_REPO_CLASS_PROP_KEY = "tigase-config-repo-class";

	/** Field description */
	public static final String INIT_PROPERTIES_MAP_BIND = "initProperties";

	/** Field description */
	public static String logManagerConfiguration = null;

	/** Field description */
	public static final String PROPERTY_FILENAME_PROP_KEY = "--property-file";
	public static final String PROPERTY_FILENAME_PROP_DEF = "etc/init.properties";

	/**
	 * Field description
	 * @deprecated moved to RepositoryFactory
	 */
	@Deprecated
	public static final String USER_DOMAIN_POOL_CLASS_PROP_KEY = RepositoryFactory
			.USER_DOMAIN_POOL_CLASS_PROP_KEY;

	/**
	 * Field description
	 * @deprecated moved to RepositoryFactory
	 */
	@Deprecated
	public static final String USER_DOMAIN_POOL_CLASS_PROP_VAL = RepositoryFactory
			.USER_DOMAIN_POOL_CLASS_PROP_VAL;
	private static final String LOGGING_KEY = "logging/";
	private static final Logger log = Logger.getLogger(ConfiguratorAbstract.class
			.getName());
	private static MonitoringSetupIfc monitoring = null;

	//~--- fields ---------------------------------------------------------------

	private AuthRepositoryMDImpl auth_repo_impl   = null;
	private Map<String, String>  auth_repo_params = null;
	private AuthRepository       auth_repository  = null;
	private UserRepositoryMDImpl user_repo_impl   = null;
	private Map<String, String>  user_repo_params = null;

	// Default user repository instance which can be shared among components
	private UserRepository user_repository   = null;
	private boolean        setup_in_progress = false;

	/**
	 * Configuration settings read from the initRepository.properties file or any other
	 * source which provides startup configuration.
	 */
	private List<String> initSettings = new LinkedList<String>();

	/**
	 * Properties from the command line parameters and initRepository.properties file or any
	 * other source which are used to generate default configuration. All the
	 * settings starting with '--'
	 */
	private Map<String, Object> initProperties = new LinkedHashMap<String, Object>(100);

	// Default user auth repository instance which can be shared among components
	private ConfigRepositoryIfc configRepo = new ConfigurationCache();

	// Common logging setup
	private Map<String, String> loggingSetup = new LinkedHashMap<String, String>(10);

	//~--- methods --------------------------------------------------------------

	@Override
	public void componentAdded(Configurable component) throws ConfigurationException {
		if (log.isLoggable(Level.CONFIG)) {
			log.log(Level.CONFIG, " component: {0}", component.getName());
		}
		setup(component);
	}

	@Override
	public void componentRemoved(Configurable component) {}

	/**
	 * Method description
	 *
	 *
	 * @param args
	 *
	 * @throws ConfigurationException
	 * @throws TigaseDBException
	 */
	public void init(String[] args) throws ConfigurationException, TigaseDBException {
		parseArgs(args);

		String stringprep = (String) initProperties.get(STRINGPREP_PROCESSOR);

		if (stringprep != null) {
			BareJID.useStringprepProcessor(stringprep);
		}

		String cnf_class_name = System.getProperty(CONFIG_REPO_CLASS_PROP_KEY);

		if (cnf_class_name != null) {
			initProperties.put(CONFIG_REPO_CLASS_INIT_KEY, cnf_class_name);
		}
		cnf_class_name = (String) initProperties.get(CONFIG_REPO_CLASS_INIT_KEY);
		if (cnf_class_name != null) {
			try {
				configRepo = (ConfigRepositoryIfc) Class.forName(cnf_class_name).newInstance();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Problem initializing configuration system: ", e);
				log.log(Level.SEVERE, "Please check settings, and rerun the server.");
				log.log(Level.SEVERE, "Server is stopping now.");
				System.err.println("Problem initializing configuration system: " + e);
				System.err.println("Please check settings, and rerun the server.");
				System.err.println("Server is stopping now.");
				System.exit(1);
			}
		}
		configRepo.addRepoChangeListener(this);
		String host = getDefHostName() != null ? getDefHostName().getDomain() : DNSResolverFactory.getInstance().getDefaultHost();
		configRepo.setDefHostname(host);
		try {
			// loss of generic types is intentional to make parameter match API
			// and internally all requests are done like:
			// String x = (String) initProperties.get("param");
			// so it should be safe to loss generic types of Map
			configRepo.initRepository(null, (Map) initProperties);
		} catch (DBInitException ex) {
			throw new ConfigurationException(ex.getMessage(), ex);
		}
		for (String prop : initSettings) {
			ConfigItem item = configRepo.getItemInstance();

			item.initFromPropertyString(prop);
			configRepo.addItem(item);
		}

		Map<String, Object> repoInitProps = configRepo.getInitProperties();

		if (repoInitProps != null) {
			initProperties.putAll(repoInitProps);
		}

		// Not sure if this is the correct pleace to initialize monitoring
		// maybe it should be initialized initRepository initializationCompleted but
		// Then some stuff might be missing. Let's try to do it here for now
		// and maybe change it later.
		String property_filenames = (String) initProperties.get(PROPERTY_FILENAME_PROP_KEY);

		if (property_filenames != null) {
			String[] prop_files = property_filenames.split(",");

			initMonitoring((String) initProperties.get(MONITORING), new File(prop_files[0])
					.getParent());
		}
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(ComponentRepository.COMP_REPO_BIND, configRepo);
		binds.put(INIT_PROPERTIES_MAP_BIND, initProperties);
	}

	@Override
	public void initializationCompleted() {
		if (isInitializationComplete()) {

			// Do we really need to do this again?
			return;
		}
		super.initializationCompleted();
		if (monitoring != null) {
			monitoring.initializationCompleted();
		}
		try {

			// Dump the configuration....
			configRepo.store();
		} catch (TigaseDBException ex) {
			log.log(Level.WARNING, "Cannot store configuration.", ex);
		}

		System.out.println("== " + new Date() + " Server finished starting up and (if there wasn't any error) is ready to use\n");

	}

	@Override
	public void itemAdded(ConfigItem item) {

		// Ignored, adding configuration settings does not make sense, for now...
		// right now, just print a log message
		// log.log(Level.INFO, "Adding configuration item not supported yet: {0}", item);
	}

	@Override
	public void itemRemoved(ConfigItem item) {

		// Ignored, removing configuration settings does not make sense, for now...
		// right now, just print a log message
		log.log(Level.INFO, "Removing configuration item not supported yet: {0}", item);
	}

	@Override
	public void itemUpdated(ConfigItem item) {
		log.log(Level.INFO, "Updating configuration item: {0}", item);

		Configurable component = getComponent(item.getCompName());

		if (component != null) {
			Map<String, Object> prop = new HashMap<>();
			prop.put(item.getConfigKey(), item.getConfigVal());

			try {
				component.setProperties(prop);
			}
			catch (ConfigurationException ex) {
				log.log(Level.SEVERE, "Component reconfiguration failed: " + ex.getMessage(), ex);
			}
		} else {
			log.log(Level.WARNING, "Cannot find component for configuration item: {0}", item);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param config
	 */
	public static void loadLogManagerConfig(String config) {
		logManagerConfiguration = config;
		try {
			final ByteArrayInputStream bis = new ByteArrayInputStream(config.getBytes());

			LogManager.getLogManager().readConfiguration(bis);
			bis.close();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Can not configure logManager", e);
		}    // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @param args
	 */
	public void parseArgs(String[] args) {
		initProperties.put(GEN_TEST, Boolean.FALSE);
		initProperties.put("config-type", GEN_CONFIG_DEF);
		if ((args != null) && (args.length > 0)) {
			for (int i = 0; i < args.length; i++) {
				String key = null;
				Object val = null;

				if (args[i].startsWith(GEN_CONFIG)) {
					key = "config-type";
					val = args[i];
				}
				if (args[i].startsWith(GEN_TEST)) {
					key = args[i];
					val = Boolean.TRUE;
				}
				if ((key == null) && args[i].startsWith("-") &&!args[i].startsWith(GEN_CONFIG)) {
					key = args[i];
					val = args[++i];
				}
				if ((key != null) && (val != null)) {
					initProperties.put(key, val);

					// System.out.println("Setting defaults: " + key + "=" +
					// val.toString());
					log.log(Level.CONFIG, "Setting defaults: {0} = {1}", new Object[] { key,
							val.toString() });
				}    // end of if (key != null)
			}      // end of for (int i = 0; i < args.length; i++)
		}

		String property_filenames = (String) initProperties.get(PROPERTY_FILENAME_PROP_KEY);

		// if no property file was specified then use default one.
		if (property_filenames == null) {
			property_filenames = PROPERTY_FILENAME_PROP_DEF;
				log.log(Level.WARNING, "No property file not specified! Using default one {0}",
						property_filenames);
		}

		if (property_filenames != null) {
			String[] prop_files = property_filenames.split(",");

			if ( prop_files.length == 1 ){
				File f = new File( prop_files[0] );
				if ( !f.exists() ){
					log.log( Level.WARNING, "Provided property file {0} does NOT EXISTS! Using default one {1}",
									 new String[] { f.getAbsolutePath(), PROPERTY_FILENAME_PROP_DEF } );
					prop_files[0] = PROPERTY_FILENAME_PROP_DEF;
				}
			}

			for (String property_filename : prop_files) {
				log.log(Level.CONFIG, "Loading initial properties from property file: {0}",
						property_filename);
				try {
					Properties defProps = new Properties();

					defProps.load(new FileReader(property_filename));

					Set<String> prop_keys = defProps.stringPropertyNames();

					for (String key : prop_keys) {
						String value = defProps.getProperty(key).trim();

						if (key.startsWith("-") || key.equals("config-type")) {
							if (GEN_TEST.equalsIgnoreCase(key)) {
								initProperties.put(key.trim().substring(2), DataTypes.parseBool(value));
								initProperties.put(key.trim(), DataTypes.parseBool(value));
							} else {
								initProperties.put(key.trim(), value);
							}

							// defProperties.remove(key);
							log.log(Level.CONFIG, "Added default config parameter: ({0}={1})",
									new Object[] { key,
									value });
						} else {
							initSettings.add(key + "=" + value);
						}
					}
				} catch (FileNotFoundException e) {
					log.log(Level.WARNING, "Given property file was not found: {0}",
							property_filename);
				} catch (IOException e) {
					log.log(Level.WARNING, "Can not read property file: " + property_filename, e);
				}
			}
		}

		// Set all parameters starting with '--' as a system properties with removed
		// the starting '-' characters.
		for (Map.Entry<String, Object> entry : initProperties.entrySet()) {
			if (entry.getKey().startsWith("--")) {
				System.setProperty(entry.getKey().substring(2), ((entry.getValue() == null)
						? null
						: entry.getValue().toString()));

				// In cluster mode we switch DB cache off as this does not play well.
				if (CLUSTER_MODE.equals(entry.getKey())) {
					if ("true".equalsIgnoreCase(entry.getValue().toString())) {
						System.setProperty("tigase.cache", "false");
					}
				}
			}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param objName
	 * @param bean
	 */
	public static void putMXBean(String objName, Object bean) {
		if (monitoring != null) {
			monitoring.putMXBean(objName, bean);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param compId
	 * @param props
	 *
	 * @throws ConfigurationException
	 */
	public void putProperties(String compId, Map<String, Object> props)
					throws ConfigurationException {
		configRepo.putProperties(compId, props);
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	public void setup(Configurable component) throws ConfigurationException {

		// Try to avoid recursive execution of the method
		if (component == this) {
			if (setup_in_progress) {
				return;
			} else {
				setup_in_progress = true;
			}
		}

		String compId = component.getName();

		log.log(Level.CONFIG, "Setting up component: {0}", compId);

		Map<String, Object> prop = null;

		try {
			prop = configRepo.getProperties(compId);
		} catch (ConfigurationException ex) {
			log.log(Level.WARNING,
					"Propblem retrieving configuration properties for component: " + compId, ex);

			return;
		}

		Map<String, Object> defs = component.getDefaults(getDefConfigParams());

		log.log(Level.CONFIG, "Component {0} defaults: {1}", new Object[] { compId, defs });

		Set<Map.Entry<String, Object>> defs_entries = defs.entrySet();
		boolean                        modified     = false;

		for (Map.Entry<String, Object> entry : defs_entries) {
			if (!prop.containsKey(entry.getKey())) {
				prop.put(entry.getKey(), entry.getValue());
				modified = true;
			}    // end of if ()
		}      // end of for ()
		if (modified) {
			try {
				log.log(Level.CONFIG, "Component {0} configuration: {1}", new Object[] { compId,
						prop });
				configRepo.putProperties(compId, prop);
			} catch (ConfigurationException ex) {
				log.log(Level.WARNING,
						"Propblem with saving configuration properties for component: " + compId, ex);
			}
		}    // end of if (modified)
		prop.put(RepositoryFactory.SHARED_USER_REPO_PROP_KEY, user_repo_impl);
		prop.put(RepositoryFactory.SHARED_USER_REPO_PARAMS_PROP_KEY, user_repo_params);
		prop.put(RepositoryFactory.SHARED_AUTH_REPO_PROP_KEY, auth_repo_impl);
		prop.put(RepositoryFactory.SHARED_AUTH_REPO_PARAMS_PROP_KEY, auth_repo_params);
		component.setProperties(prop);
		if (component == this) {
			setup_in_progress = false;
		}
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defaults = super.getDefaults(params);
		String              levelStr = ".level";

		if ((Boolean) params.get(GEN_TEST)) {
			defaults.put(LOGGING_KEY + levelStr, "WARNING");
		} else {
			defaults.put(LOGGING_KEY + levelStr, "CONFIG");
		}
		defaults.put(LOGGING_KEY + "handlers",
				"java.util.logging.ConsoleHandler java.util.logging.FileHandler");
		defaults.put(LOGGING_KEY + "java.util.logging.ConsoleHandler.formatter",
				"tigase.util.LogFormatter");
		defaults.put(LOGGING_KEY + "java.util.logging.ConsoleHandler.level", "WARNING");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.append", "true");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.count", "5");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.formatter",
				"tigase.util.LogFormatter");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.limit", "10000000");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.pattern",
				"logs/tigase.log");
		defaults.put(LOGGING_KEY + "tigase.useParentHandlers", "true");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.level", "ALL");
		if (params.get(GEN_DEBUG) != null) {
			String[] packs = ((String) params.get(GEN_DEBUG)).split(",");

			for (String pack : packs) {
				defaults.put(LOGGING_KEY + "tigase." + pack + ".level", "ALL");
			}    // end of for (String pack: packs)
		}
		if (params.get(GEN_DEBUG_PACKAGES) != null) {
			String[] packs = ((String) params.get(GEN_DEBUG_PACKAGES)).split(",");

			for (String pack : packs) {
				defaults.put(LOGGING_KEY + pack + ".level", "ALL");
			}    // end of for (String pack: packs)
		}

		String repo_pool = null;

		repo_pool = (String) params.get(RepositoryFactory.USER_DOMAIN_POOL_CLASS);
		if (repo_pool == null) {
			repo_pool = RepositoryFactory.USER_DOMAIN_POOL_CLASS_PROP_VAL;
		}
		defaults.put(RepositoryFactory.USER_DOMAIN_POOL_CLASS_PROP_KEY, repo_pool);
		repo_pool = (String) params.get(RepositoryFactory.AUTH_DOMAIN_POOL_CLASS);
		if (repo_pool == null) {
			repo_pool = RepositoryFactory.AUTH_DOMAIN_POOL_CLASS_PROP_VAL;
		}
		defaults.put(RepositoryFactory.AUTH_DOMAIN_POOL_CLASS_PROP_KEY, repo_pool);

		String user_repo_class = null;//RepositoryFactory.DUMMY_REPO_CLASS_PROP_VAL;
		String user_repo_url   = RepositoryFactory.DERBY_REPO_URL_PROP_VAL;
		String auth_repo_class = null;//RepositoryFactory.DUMMY_REPO_CLASS_PROP_VAL;
		String auth_repo_url   = RepositoryFactory.DERBY_REPO_URL_PROP_VAL;

		if (params.get(RepositoryFactory.GEN_USER_DB) != null) {
			user_repo_class = (String) params.get(RepositoryFactory.GEN_USER_DB);
			auth_repo_class = RepositoryFactory.TIGASE_CUSTOM_AUTH_REPO_CLASS_PROP_VAL;
		}
		if (params.get(RepositoryFactory.GEN_USER_DB_URI) != null) {
			user_repo_url = (String) params.get(RepositoryFactory.GEN_USER_DB_URI);
			auth_repo_url = user_repo_url;
		}
		if (params.get(RepositoryFactory.GEN_AUTH_DB) != null) {
			auth_repo_class = (String) params.get(RepositoryFactory.GEN_AUTH_DB);
		}
		if (params.get(RepositoryFactory.GEN_AUTH_DB_URI) != null) {
			auth_repo_url = (String) params.get(RepositoryFactory.GEN_AUTH_DB_URI);
		}
		if (params.get(RepositoryFactory.USER_REPO_POOL_SIZE) != null) {
			defaults.put(RepositoryFactory.USER_REPO_POOL_SIZE_PROP_KEY, params.get(
					RepositoryFactory.USER_REPO_POOL_SIZE));
		} else {
			defaults.put(RepositoryFactory.USER_REPO_POOL_SIZE_PROP_KEY, RepositoryFactory
					.USER_REPO_POOL_SIZE_PROP_VAL);
		}
		if (params.get(RepositoryFactory.DATA_REPO_POOL_SIZE) != null) {
			defaults.put(RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY, params.get(
					RepositoryFactory.DATA_REPO_POOL_SIZE));
		} else if (params.get(RepositoryFactory.USER_REPO_POOL_SIZE) != null) {
			defaults.put(RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY, params.get(
					RepositoryFactory.USER_REPO_POOL_SIZE));
		} else {
			defaults.put(RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY, RepositoryFactory
					.DATA_REPO_POOL_SIZE_PROP_VAL);
		}
		if (params.get(RepositoryFactory.AUTH_REPO_POOL_SIZE) != null) {
			defaults.put(RepositoryFactory.AUTH_REPO_POOL_SIZE_PROP_KEY, params.get(
					RepositoryFactory.AUTH_REPO_POOL_SIZE));
		} else if (params.get(RepositoryFactory.DATA_REPO_POOL_SIZE) != null) {
			defaults.put(RepositoryFactory.AUTH_REPO_POOL_SIZE_PROP_KEY, params.get(
					RepositoryFactory.DATA_REPO_POOL_SIZE));
		} else if (params.get(RepositoryFactory.USER_REPO_POOL_SIZE) != null) {
			defaults.put(RepositoryFactory.AUTH_REPO_POOL_SIZE_PROP_KEY, params.get(
					RepositoryFactory.USER_REPO_POOL_SIZE));
		} else {
			defaults.put(RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY, RepositoryFactory
					.AUTH_REPO_POOL_SIZE_PROP_VAL);
		}
		if (user_repo_class != null)
			defaults.put(RepositoryFactory.USER_REPO_CLASS_PROP_KEY, user_repo_class);
		defaults.put(RepositoryFactory.USER_REPO_URL_PROP_KEY, user_repo_url);
		if (auth_repo_class != null)
			defaults.put(RepositoryFactory.AUTH_REPO_CLASS_PROP_KEY, auth_repo_class);
		defaults.put(RepositoryFactory.AUTH_REPO_URL_PROP_KEY, auth_repo_url);

		List<String> user_repo_domains = new ArrayList<String>(10);
		List<String> auth_repo_domains = new ArrayList<String>(10);

		for (Map.Entry<String, Object> entry : params.entrySet()) {
			if (entry.getKey().startsWith(RepositoryFactory.GEN_USER_DB_URI)) {
				String[] domains = parseUserRepoParams(entry, params, user_repo_class, defaults);

				if (domains != null) {
					user_repo_domains.addAll(Arrays.asList(domains));
				}
			}
			if (entry.getKey().startsWith(RepositoryFactory.GEN_AUTH_DB_URI)) {
				String[] domains = parseAuthRepoParams(entry, params, auth_repo_class, defaults);

				if (domains != null) {
					auth_repo_domains.addAll(Arrays.asList(domains));
				}
			}
		}
		if (user_repo_domains.size() > 0) {
			defaults.put(RepositoryFactory.USER_REPO_DOMAINS_PROP_KEY, user_repo_domains
					.toArray(new String[user_repo_domains.size()]));
		}
		if (auth_repo_domains.size() > 0) {
			defaults.put(RepositoryFactory.AUTH_REPO_DOMAINS_PROP_KEY, auth_repo_domains
					.toArray(new String[auth_repo_domains.size()]));
		}

		// TLS/SSL configuration
		if (params.get("--" + SSL_CONTAINER_CLASS_KEY) != null) {
			defaults.put(SSL_CONTAINER_CLASS_KEY, (String) params.get("--" +
					SSL_CONTAINER_CLASS_KEY));
		} else {
			defaults.put(SSL_CONTAINER_CLASS_KEY, SSL_CONTAINER_CLASS_VAL);
		}
		if (params.get("--" + SERVER_CERTS_LOCATION_KEY) != null) {
			defaults.put(SERVER_CERTS_LOCATION_KEY, (String) params.get("--" +
					SERVER_CERTS_LOCATION_KEY));
		} else {
			defaults.put(SERVER_CERTS_LOCATION_KEY, SERVER_CERTS_LOCATION_VAL);
		}
		if (params.get("--" + DEFAULT_DOMAIN_CERT_KEY) != null) {
			defaults.put(DEFAULT_DOMAIN_CERT_KEY, (String) params.get("--" +
					DEFAULT_DOMAIN_CERT_KEY));
		} else {
			defaults.put(DEFAULT_DOMAIN_CERT_KEY, DEFAULT_DOMAIN_CERT_VAL);
		}
		configRepo.getDefaults(defaults, params);

		return defaults;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public Map<String, Object> getDefConfigParams() {
		return initProperties;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getMessageRouterClassName() {
		return "tigase.server.MessageRouter";
	}

	/**
	 * Method description
	 *
	 *
	 * @param objName
	 *
	 * 
	 */
	public static Object getMXBean(String objName) {
		if (monitoring != null) {
			return monitoring.getMXBean(objName);
		} else {
			return null;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param nodeId
	 *
	 * 
	 *
	 * @throws ConfigurationException
	 */
	public Map<String, Object> getProperties(String nodeId) throws ConfigurationException {
		return configRepo.getProperties(nodeId);
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		RepositoryFactory.statistics.getStatistics(list);
	}

	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof Configurable;
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		if (props.size() == 0) {
			log.log(Level.WARNING,
					"Properties size is 0, incorrect system state, probably OSGI mode and configuration is not yet loaded.");

			return;
		} else {
			log.log(Level.INFO, "Propeties size is {0}, and here are all propeties: {1}",
					new Object[] { props.size(),
					props });
		}
		setupLogManager(props);
		super.setProperties(props);
		if (props.size() == 1) {
			log.log(Level.INFO, "Propeties size is {0}, and here are all propeties: {1}",
					new Object[] { props.size(),
					props });

			return;
		}
		configRepo.setProperties(props);
		TLSUtil.configureSSLContext(props);


		String[] user_repo_domains = (String[]) props.get(RepositoryFactory
				.USER_REPO_DOMAINS_PROP_KEY);
		String[] auth_repo_domains = (String[]) props.get(RepositoryFactory
				.AUTH_REPO_DOMAINS_PROP_KEY);
		String authRepoMDImpl = (String) props.get(RepositoryFactory
				.AUTH_DOMAIN_POOL_CLASS_PROP_KEY);
		String userRepoMDImpl = (String) props.get(RepositoryFactory
				.USER_DOMAIN_POOL_CLASS_PROP_KEY);

		try {

			// Authentication multi-domain repository pool initialization
			Map<String, String> params = getRepoParams(props, RepositoryFactory
					.AUTH_REPO_PARAMS_NODE, null);
			String conn_url = (String) props.get(RepositoryFactory.AUTH_REPO_URL_PROP_KEY);

			auth_repo_impl = (AuthRepositoryMDImpl) Class.forName(authRepoMDImpl).newInstance();
			auth_repo_impl.initRepository(conn_url, params);

			// User multi-domain repository pool initialization
			params         = getRepoParams(props, RepositoryFactory.USER_REPO_PARAMS_NODE,
					null);
			conn_url       = (String) props.get(RepositoryFactory.USER_REPO_URL_PROP_KEY);
			user_repo_impl = (UserRepositoryMDImpl) Class.forName(userRepoMDImpl).newInstance();
			user_repo_impl.initRepository(conn_url, params);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "An error initializing domain repository pool: ", ex);
		}
		user_repository = null;
		auth_repository = null;
		if (user_repo_domains != null) {
			for (String domain : user_repo_domains) {
				try {
					addUserRepo(props, domain);
				} catch (Exception e) {
					log.log(Level.SEVERE, "Can't initialize user repository for domain: " + domain,
							e);
				}
			}
		}
		if (user_repository == null) {
			try {
				addUserRepo(props, null);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Can't initialize user default repository: ", e);
			}
		}
		if (auth_repo_domains != null) {
			for (String domain : auth_repo_domains) {
				try {
					addAuthRepo(props, domain);
				} catch (Exception e) {
					log.log(Level.SEVERE, "Can't initialize user repository for domain: " + domain,
							e);
				}
			}
		}
		if (auth_repository == null) {
			try {
				addAuthRepo(props, null);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Can't initialize auth default repository: ", e);
			}
		}
	}

	//~--- methods --------------------------------------------------------------

	private void addAuthRepo(Map<String, Object> props, String domain)
					throws DBInitException, ClassNotFoundException, InstantiationException,
							IllegalAccessException {
		Map<String, String> params = getRepoParams(props, RepositoryFactory
				.AUTH_REPO_PARAMS_NODE, domain);
		String cls_name = (String) props.get(RepositoryFactory.AUTH_REPO_CLASS_PROP_KEY +
				((domain == null)
				 ? ""
				 : "/" + domain));
		String conn_url = (String) props.get(RepositoryFactory.AUTH_REPO_URL_PROP_KEY +
				((domain == null)
				 ? ""
				 : "/" + domain));

		AuthRepository repo = RepositoryFactory.getAuthRepository(cls_name, conn_url, params);

		if ((domain == null) || domain.trim().isEmpty()) {
			auth_repo_impl.addRepo("", repo);
			auth_repo_impl.setDefault(repo);
			auth_repository = repo;
		} else {
			auth_repo_impl.addRepo(domain, repo);
		}
		log.log(Level.INFO,
				"[{0}] Initialized {1} as user auth repository pool, url: {3}",
				new Object[] { ((domain != null)
				? domain
				: "DEFAULT"), cls_name, conn_url });
	}

	private void addUserRepo(Map<String, Object> props, String domain)
					throws DBInitException, ClassNotFoundException, InstantiationException,
							IllegalAccessException {
		Map<String, String> params = getRepoParams(props, RepositoryFactory
				.USER_REPO_PARAMS_NODE, domain);
		String cls_name = (String) props.get(RepositoryFactory.USER_REPO_CLASS_PROP_KEY +
				((domain == null)
				 ? ""
				 : "/" + domain));
		String conn_url = (String) props.get(RepositoryFactory.USER_REPO_URL_PROP_KEY +
				((domain == null)
				 ? ""
				 : "/" + domain));


		UserRepository repo = RepositoryFactory.getUserRepository(cls_name, conn_url, params);

		if ((domain == null) || domain.trim().isEmpty()) {
			user_repo_impl.addRepo("", repo);
			user_repo_impl.setDefault(repo);
			user_repository = repo;
		} else {
			user_repo_impl.addRepo(domain, repo);
		}
		log.log(Level.INFO, "[{0}] Initialized {1} as user repository pool, url: {2}",
				new Object[] { ((domain != null)
				? domain
				: "DEFAULT"), cls_name, conn_url });
	}

	private void initMonitoring(String settings, String configDir) {
		if ((monitoring == null) && (settings != null)) {
			try {
				String mon_cls = "tigase.management.MonitoringSetup";

				monitoring = (MonitoringSetupIfc) Class.forName(mon_cls).newInstance();
				monitoring.initMonitoring(settings, configDir);
			} catch (Exception e) {
				log.log(Level.WARNING, "Can not initialize monitoring: ", e);
			}
		}
	}

	private String[] parseAuthRepoParams(Entry<String, Object> entry, Map<String,
			Object> params, String auth_repo_class, Map<String, Object> defaults) {
		String key      = entry.getKey();
		int    br_open  = key.indexOf('[');
		int    br_close = key.indexOf(']');

		if ((br_open < 0) || (br_close < 0)) {

			// default database is configured elsewhere
			return null;
		}

		String   repo_class = auth_repo_class;
		String   options    = key.substring(br_open + 1, br_close);
		String[] domains    = options.split(",");

		log.log(Level.INFO, "Found DB domain: {0}", Arrays.toString(domains));

		String get_user_db = RepositoryFactory.GEN_AUTH_DB + "[" + options + "]";

		if (params.get(get_user_db) != null) {
			repo_class = (String) params.get(get_user_db);
		}
		for (String domain : domains) {
			defaults.put(RepositoryFactory.AUTH_REPO_CLASS_PROP_KEY + "/" + domain, repo_class);
			log.log(Level.CONFIG, "Setting defaults: {0}/{1}={2}", new Object[] {
					RepositoryFactory.AUTH_REPO_CLASS_PROP_KEY,
					domain, repo_class });
			defaults.put(RepositoryFactory.AUTH_REPO_URL_PROP_KEY + "/" + domain, entry
					.getValue());
			log.log(Level.CONFIG, "Setting defaults: {0}{1}={2}", new Object[] {
					RepositoryFactory.AUTH_REPO_URL_PROP_KEY,
					domain, entry.getValue() });
		}

		return domains;
	}

	private String[] parseUserRepoParams(Entry<String, Object> entry, Map<String,
			Object> params, String user_repo_class, Map<String, Object> defaults) {
		String key      = entry.getKey();
		int    br_open  = key.indexOf('[');
		int    br_close = key.indexOf(']');

		if ((br_open < 0) || (br_close < 0)) {

			// default database is configured elsewhere
			return null;
		}

		String   repo_class = user_repo_class;
		String   options    = key.substring(br_open + 1, br_close);
		String[] domains    = options.split(",");

		log.log(Level.INFO, "Found DB domain: {0}", Arrays.toString(domains));

		String get_user_db = RepositoryFactory.GEN_USER_DB + "[" + options + "]";

		if (params.get(get_user_db) != null) {
			repo_class = (String) params.get(get_user_db);
		}
		for (String domain : domains) {
			defaults.put(RepositoryFactory.USER_REPO_CLASS_PROP_KEY + "/" + domain, repo_class);
			log.log(Level.CONFIG, "Setting defaults: {0}{1}={2}", new Object[] {
					RepositoryFactory.USER_REPO_CLASS_PROP_KEY,
					domain, repo_class });
			defaults.put(RepositoryFactory.USER_REPO_URL_PROP_KEY + "/" + domain, entry
					.getValue());
			log.log(Level.CONFIG, "Setting defaults: {0}{1}={2}", new Object[] {
					RepositoryFactory.USER_REPO_URL_PROP_KEY,
					domain, entry.getValue() });
		}

		return domains;
	}

	private void setupLogManager( Map<String, Object> properties ) {
		Set<Map.Entry<String, Object>> entries = properties.entrySet();
		StringBuilder buff = new StringBuilder( 200 );

		for ( Map.Entry<String, Object> entry : entries ) {
			if ( entry.getKey().startsWith( LOGGING_KEY ) ){
				String key = entry.getKey().substring( LOGGING_KEY.length() );
				loggingSetup.put( key, entry.getValue().toString() );
			}
		}

		for ( String key : loggingSetup.keySet() ) {
			String entry = loggingSetup.get( key );
			buff.append( key ).append( "=" ).append( entry ).append( "\n" );
			if ( key.equals( "java.util.logging.FileHandler.pattern" ) ){
				File log_path = new File( entry ).getParentFile();
				if ( !log_path.exists() ){
					log_path.mkdirs();
				}
			}    // end of if (key.equals())
		}      // end of if (entry.getKey().startsWith(LOGGING_KEY))

		// System.out.println("Setting logging: \n" + buff.toString());
		loadLogManagerConfig(buff.toString());
		log.config("DONE");
	}

	//~--- get methods ----------------------------------------------------------

	private Map<String, String> getRepoParams(Map<String, Object> props, String repo_type,
			String domain) {
		Map<String, String> result     = new LinkedHashMap<String, String>(10);
		String              prop_start = repo_type + ((domain == null)
				? ""
				: "/" + domain);

		for (Map.Entry<String, Object> entry : props.entrySet()) {
			if (entry.getKey().startsWith(prop_start)) {

				// Split the key to configuration nodes separated with '/'
				String[] nodes = entry.getKey().split("/");

				// The plugin ID part may contain many IDs separated with comma ','
				// We have to make sure that the default repository does not pick up
				// properties set for a specific domain
				if (((domain == null) && (nodes.length == 2)) || ((domain != null) && (nodes
						.length == 3))) {

					// if (nodes.length > 1) {
					result.put(nodes[nodes.length - 1], entry.getValue().toString());
				}
			}
		}

		return result;
	}
}


//~ Formatted in Tigase Code Convention on 13/06/08
