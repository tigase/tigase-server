/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.conf;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.ComponentRepository;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserAuthRepository;
import tigase.db.UserRepository;
import tigase.db.UserRepositoryPool;

import tigase.server.AbstractComponentRegistrator;
import tigase.server.ServerComponent;

//~--- JDK imports ------------------------------------------------------------

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.script.Bindings;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Dec 7, 2009 4:15:31 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class ConfiguratorAbstract
				extends AbstractComponentRegistrator<Configurable> {

	/** Field description */
	public static final String CONFIG_REPO_CLASS_INIT_KEY = "--tigase-config-repo-class";

	/** Field description */
	public static final String CONFIG_REPO_CLASS_PROP_KEY = "tigase-config-repo-class";
	private static final String LOGGING_KEY = "logging/";

	/** Field description */
	public static final String PROPERTY_FILENAME_PROP_KEY = "--property-file";
	private static final Logger log =
		Logger.getLogger(ConfiguratorAbstract.class.getName());

	/** Field description */
	public static String logManagerConfiguration = null;
	private static MonitoringSetupIfc monitoring = null;

	//~--- fields ---------------------------------------------------------------

	private Map<String, String> auth_repo_params = null;

	// Default user auth repository instance which can be shared among components
	private UserAuthRepository auth_repository = null;
	private ConfigRepositoryIfc configRepo = new ConfigurationCache();
	private UserRepositoryPool repo_pool = null;
	private Map<String, String> user_repo_params = null;

	// Default user repository instance which can be shared among components
	private UserRepository user_repository = null;

	/**
	 * Configuration settings read from the init.properties file or any other source
	 * which provides startup configuration.
	 */
	private List<String> initSettings = new LinkedList<String>();

	/**
	 * Properties from the command line parameters and init.properties file or any
	 * other source which are used to generate default configuration. All the settings
	 * starting with '--'
	 */
	private Map<String, Object> initProperties = new LinkedHashMap<String, Object>();

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param objName
	 *
	 * @return
	 */
	public static Object getMXBean(String objName) {
		if (monitoring != null) {
			return monitoring.getMXBean(objName);
		} else {
			return null;
		}
	}

	//~--- methods --------------------------------------------------------------

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
	 * @param component
	 */
	@Override
	public void componentAdded(Configurable component) {
		if (log.isLoggable(Level.CONFIG)) {
			log.config(" component: " + component.getName());
		}

		setup(component);
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	@Override
	public void componentRemoved(Configurable component) {}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public Map<String, Object> getDefConfigParams() {
		return initProperties;
	}

	/**
	 * Returns defualt configuration settings in case if there is no
	 * config file.
	 * @param params
	 * @return
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defaults = super.getDefaults(params);

		if ((Boolean) params.get(GEN_TEST)) {
			defaults.put(LOGGING_KEY + ".level", "WARNING");
		} else {
			defaults.put(LOGGING_KEY + ".level", "CONFIG");
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

		// System.out.println("Setting logging properties:\n" + defaults.toString());
		String user_repo_class = DERBY_REPO_CLASS_PROP_VAL;
		String user_repo_url = DERBY_REPO_URL_PROP_VAL;
		String auth_repo_class = DERBY_REPO_CLASS_PROP_VAL;
		String auth_repo_url = DERBY_REPO_URL_PROP_VAL;

		if (params.get(GEN_USER_DB) != null) {
			user_repo_class = (String) params.get(GEN_USER_DB);
			auth_repo_class = (String) params.get(GEN_USER_DB);
		}

		if (params.get(GEN_USER_DB_URI) != null) {
			user_repo_url = (String) params.get(GEN_USER_DB_URI);
			auth_repo_url = user_repo_url;
		}

		if (params.get(GEN_AUTH_DB) != null) {
			auth_repo_class = (String) params.get(GEN_AUTH_DB);
		}

		if (params.get(GEN_AUTH_DB_URI) != null) {
			auth_repo_url = (String) params.get(GEN_AUTH_DB_URI);
		}

		if (params.get(USER_REPO_POOL_SIZE) != null) {
			defaults.put(USER_REPO_POOL_SIZE_PROP_KEY, params.get(USER_REPO_POOL_SIZE));
		} else {
			defaults.put(USER_REPO_POOL_SIZE_PROP_KEY, "" + 1);
		}

		defaults.put(USER_REPO_CLASS_PROP_KEY, user_repo_class);
		defaults.put(USER_REPO_URL_PROP_KEY, user_repo_url);
		defaults.put(USER_REPO_PARAMS_NODE + "/param-1", "value-1");
		defaults.put(AUTH_REPO_CLASS_PROP_KEY, auth_repo_class);
		defaults.put(AUTH_REPO_URL_PROP_KEY, auth_repo_url);
		defaults.put(AUTH_REPO_PARAMS_NODE + "/param-1", "value-1");

		// Setup tracer, this is a temporarily code...
//  String ips = (String)params.get(TigaseTracer.TRACER_IPS_PROP_KEY);
//  if (ips != null) {
//    String[] ipsa = ips.split(",");
//    for (String ip : ipsa) {
//      TigaseTracer.addIP(ip);
//    }
//  }
//  String jids = (String)params.get(TigaseTracer.TRACER_JIDS_PROP_KEY);
//  if (jids != null) {
//    String[] jidsa = jids.split(",");
//    for (String jid : jidsa) {
//      TigaseTracer.addJid(jid);
//    }
//  }
		return defaults;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getMessageRouterClassName() {
		return "tigase.server.MessageRouter";
	}

	/**
	 * Method description
	 *
	 *
	 * @param nodeId
	 *
	 * @return
	 *
	 * @throws ConfigurationException
	 */
	public Map<String, Object> getProperties(String nodeId) throws ConfigurationException {
		return configRepo.getProperties(nodeId);
	}

	//~--- methods --------------------------------------------------------------

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

		configRepo.setDefHostname(getDefHostName());
		configRepo.init(initProperties);

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
		// maybe it should be initialized init initializationCompleted but
		// Then some stuff might be missing. Let's try to do it here for now
		// and maybe change it later.
		String property_filename = (String) initProperties.get(PROPERTY_FILENAME_PROP_KEY);

		if (property_filename != null) {
			initMonitoring((String) initProperties.get(MONITORING),
										 new File(property_filename).getParent());
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param binds
	 */
	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(ComponentRepository.COMP_REPO_BIND, configRepo);
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void initializationCompleted() {
		super.initializationCompleted();

		if (monitoring != null) {
			monitoring.initializationCompleted();
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param component
	 *
	 * @return
	 */
	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof Configurable;
	}

	//~--- methods --------------------------------------------------------------

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

				if (key != null) {
					initProperties.put(key, val);

					// System.out.println("Setting defaults: " + key + "=" + val.toString());
					log.config("Setting defaults: " + key + "=" + val.toString());
				}    // end of if (key != null)
			}      // end of for (int i = 0; i < args.length; i++)
		}

		String property_filename = (String) initProperties.get(PROPERTY_FILENAME_PROP_KEY);

		if (property_filename != null) {
			log.config("Loading initial properties from property file: " + property_filename);

			try {
				Properties defProps = new Properties();

				defProps.load(new FileReader(property_filename));

				Set<String> prop_keys = defProps.stringPropertyNames();

				for (String key : prop_keys) {
					String value = defProps.getProperty(key).trim();

					if (key.startsWith("-") || key.equals("config-type")) {
						initProperties.put(key.trim(), value);

						// defProperties.remove(key);
						log.config("Added default config parameter: (" + key + "=" + value + ")");
					} else {
						initSettings.add(key + "=" + value);
					}
				}
			} catch (FileNotFoundException e) {
				log.warning("Given property file was not found: " + property_filename);
			} catch (IOException e) {
				log.log(Level.WARNING, "Can not read property file: " + property_filename, e);
			}
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

	//~--- set methods ----------------------------------------------------------

	/**
	 * Sets all configuration properties for object.
	 * @param props
	 */
	@Override
	public void setProperties(final Map<String, Object> props) {
		super.setProperties(props);
		setupLogManager(props);

		int repo_pool_size = 1;

		try {
			repo_pool_size = Integer.parseInt((String) props.get(USER_REPO_POOL_SIZE_PROP_KEY));
		} catch (Exception e) {
			repo_pool_size = 1;
		}

		repo_pool = new UserRepositoryPool();
		user_repo_params = new LinkedHashMap<String, String>();
		auth_repo_params = new LinkedHashMap<String, String>();

		for (Map.Entry<String, Object> entry : props.entrySet()) {
			if (entry.getKey().startsWith(USER_REPO_PARAMS_NODE)) {

				// Split the key to configuration nodes separated with '/'
				String[] nodes = entry.getKey().split("/");

				// The plugin ID part may contain many IDs separated with comma ','
				if (nodes.length > 1) {
					user_repo_params.put(nodes[1], entry.getValue().toString());
				}
			}

			if (entry.getKey().startsWith(AUTH_REPO_PARAMS_NODE)) {

				// Split the key to configuration nodes separated with '/'
				String[] nodes = entry.getKey().split("/");

				// The plugin ID part may contain many IDs separated with comma ','
				if (nodes.length > 1) {
					auth_repo_params.put(nodes[1], entry.getValue().toString());
				}
			}
		}

		try {
			String cls_name = (String) props.get(USER_REPO_CLASS_PROP_KEY);
			String res_uri = (String) props.get(USER_REPO_URL_PROP_KEY);

			repo_pool.initRepository(res_uri, user_repo_params);

			for (int i = 0; i < repo_pool_size; i++) {
				user_repository = RepositoryFactory.getUserRepository(getName() + "-" + (i + 1),
								cls_name, res_uri, user_repo_params);
				repo_pool.addRepo(user_repository);
			}

			log.config("Initialized " + cls_name + " as user repository: " + res_uri);
			log.config("Initialized user repository pool: " + repo_pool_size);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can't initialize user repository: ", e);
		}    // end of try-catch

		try {
			String cls_name = (String) props.get(AUTH_REPO_CLASS_PROP_KEY);
			String res_uri = (String) props.get(AUTH_REPO_URL_PROP_KEY);

			auth_repository = RepositoryFactory.getAuthRepository(getName(), cls_name, res_uri,
							auth_repo_params);
			log.config("Initialized " + cls_name + " as auth repository: " + res_uri);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can't initialize auth repository: ", e);
		}    // end of try-catch
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	public void setup(Configurable component) {
		String compId = component.getName();
		Map<String, Object> prop = null;

		try {
			prop = configRepo.getProperties(compId);
		} catch (ConfigurationException ex) {
			log.log(Level.WARNING,
							"Propblem retrieving configuration properties for component: " + compId,
							ex);

			return;
		}

//  if (component == this) {
//    System.out.println("Properties: " + prop.toString());
//  }
		Map<String, Object> defs = component.getDefaults(getDefConfigParams());
		Set<Map.Entry<String, Object>> defs_entries = defs.entrySet();
		boolean modified = false;

		for (Map.Entry<String, Object> entry : defs_entries) {
			if (!prop.containsKey(entry.getKey())) {
				prop.put(entry.getKey(), entry.getValue());
				modified = true;
			}    // end of if ()
		}      // end of for ()

		if (modified) {
			try {
				configRepo.putProperties(compId, prop);
			} catch (ConfigurationException ex) {
				log.log(Level.WARNING,
								"Propblem with saving configuration properties for component: " + compId,
								ex);
			}
		}    // end of if (modified)

		prop.put(SHARED_USER_REPO_PROP_KEY, user_repository);
		prop.put(SHARED_USER_REPO_PARAMS_PROP_KEY, user_repo_params);
		prop.put(SHARED_AUTH_REPO_PROP_KEY, auth_repository);
		prop.put(SHARED_AUTH_REPO_PARAMS_PROP_KEY, auth_repo_params);
		prop.put(SHARED_USER_REPO_POOL_PROP_KEY, repo_pool);
		component.setProperties(prop);
	}

	private void initMonitoring(String settings, String configDir) {
		if ((monitoring == null) && (settings != null)) {
			try {
				monitoring =
					(MonitoringSetupIfc) Class.forName("tigase.management.MonitoringSetup").newInstance();
				monitoring.initMonitoring(settings, configDir);
			} catch (Exception e) {
				log.log(Level.WARNING, "Can not initialize monitoring: ", e);
			}
		}
	}

	private void setupLogManager(Map<String, Object> properties) {
		Set<Map.Entry<String, Object>> entries = properties.entrySet();
		StringBuilder buff = new StringBuilder();

		for (Map.Entry<String, Object> entry : entries) {
			if (entry.getKey().startsWith(LOGGING_KEY)) {
				String key = entry.getKey().substring(LOGGING_KEY.length());

				buff.append(key + "=" + entry.getValue() + "\n");

				if (key.equals("java.util.logging.FileHandler.pattern")) {
					File log_path = new File(entry.getValue().toString()).getParentFile();

					if (!log_path.exists()) {
						log_path.mkdirs();
					}
				}    // end of if (key.equals())
			}      // end of if (entry.getKey().startsWith(LOGGING_KEY))
		}

		// System.out.println("Setting logging: \n" + buff.toString());
		loadLogManagerConfig(buff.toString());
		log.config("DONE");
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
