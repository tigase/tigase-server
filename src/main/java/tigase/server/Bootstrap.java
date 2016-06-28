/*
 * Bootstrap.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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

package tigase.server;

import tigase.component.PropertiesBeanConfigurator;
import tigase.component.PropertiesBeanConfiguratorWithBackwordCompatibility;
import tigase.conf.ConfigReader;
import tigase.conf.ConfigWriter;
import tigase.conf.ConfiguratorAbstract;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.DependencyGrapher;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.xmppsession.SessionManagerConfig;
import tigase.util.DataTypes;
import tigase.xmpp.ProcessorFactory;
import tigase.xmpp.XMPPImplIfc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.conf.Configurable.GEN_SM_PLUGINS;
import static tigase.conf.ConfiguratorAbstract.PROPERTY_FILENAME_PROP_DEF;
import static tigase.conf.ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY;

/**
 * Bootstrap class is responsible for initialization of Kernel to start Tigase XMPP Server.
 *
 * Created by andrzej on 05.03.2016.
 */
public class Bootstrap implements Lifecycle {

	private static final Logger log = Logger.getLogger(Bootstrap.class.getCanonicalName());

	private final Kernel kernel;
	private Map<String, Object> props;

	public Bootstrap() {
		kernel = new Kernel("root");
	}

	public void init(String[] args) {
		props = new LinkedHashMap<>();
		List<String> settings = new LinkedList<>();
		ConfiguratorAbstract.parseArgs(props, settings, args);
		if (isDslConfig()) {
			loadFromDSLFiles();
		} else {
			loadFromPropertiesFiles(settings);
			dumpToDSLFile();
		}
	}

	private boolean isDslConfig() {
		String property_filenames = (String) props.get(PROPERTY_FILENAME_PROP_KEY);
		if (property_filenames == null) {
			property_filenames = PROPERTY_FILENAME_PROP_DEF;
			props.put(PROPERTY_FILENAME_PROP_KEY, property_filenames);
			log.log(Level.WARNING, "No property file not specified! Using default one {0}",
					property_filenames);
		}

		if (property_filenames != null) {
			String[] prop_files = property_filenames.split(",");

			if (prop_files.length == 1) {
				File f = new File(prop_files[0]);
				if (!f.exists()) {
					log.log(Level.WARNING, "Provided property file {0} does NOT EXISTS! Using default one {1}",
							new String[]{f.getAbsolutePath(), PROPERTY_FILENAME_PROP_DEF});
					prop_files[0] = PROPERTY_FILENAME_PROP_DEF;
				}
			}

			for (String property_filename : prop_files) {
				try (BufferedReader reader = new BufferedReader(new FileReader(property_filename))) {
					if (reader.readLine().contains("{")) {
						return true;
					}
				} catch (IOException e) {
				   e.printStackTrace();
				}
			}
		}
		return false;
	}

	private void loadFromDSLFiles() {
		String property_filenames = (String) props.get(PROPERTY_FILENAME_PROP_KEY);
		for (String prop_file : property_filenames.split(",")) {
			try {
				Map<String, Object> loaded = new ConfigReader().read(new File(prop_file));
				props.putAll(ConfigReader.flatTree(loaded));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void dumpToDSLFile() {
		File f = new File(PROPERTY_FILENAME_PROP_DEF+".new");
		Map<String, Object> tree = ConfigWriter.buildTree(props);
		try {
			new ConfigWriter().write(f, tree);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadFromPropertiesFiles(List<String> settings) {
		ConfiguratorAbstract.loadFromPropertiesFiles(props, settings);
		loadFromPropertyStrings(settings);
	}

	private void loadFromPropertyStrings(List<String> settings) {
		for (String propString : settings) {
			int idx_eq    = propString.indexOf('=');

			// String key = prop[0].trim();
			// Object val = prop[1];
			String key = propString.substring(0, idx_eq);
			Object val = propString.substring(idx_eq + 1);

			if (key.matches(".*\\[[LISBlisb]\\]$")) {
				char c = key.charAt(key.length() - 2);

				key = key.substring(0, key.length() - 3);
				// decoding value for basckward compatibility
				if (val != null) {
					val = DataTypes.decodeValueType(c, val.toString());
				}
			}

			props.put(key, val);
		}

		// converting old component configuration to new one
		Map<String, Object> toAdd = new HashMap<>();

		props.forEach((k,v) -> {
			if (k.startsWith("--comp-name")) {
				String suffix = k.replace("--comp-name-", "");
				String name = ((String) v).trim();
				String cls = ((String) props.get("--comp-class-" + suffix)).trim();
				String active = "true";
				toAdd.put(name + "/class", cls);
				toAdd.put(name + "/active", active);
			}
		});

		Iterator<Map.Entry<String, Object>> it = props.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> e = it.next();
			if (e.getKey().startsWith("--comp-"))
				it.remove();
		}

		props.putAll(toAdd);

		// converting list of sess-man processors from --sm-plugins to sess-man/beans
		// and converting concurrency settings as well
		String plugins = (String) props.get(GEN_SM_PLUGINS);
		if (plugins != null) {
			StringBuilder smBeans = new StringBuilder();
			Map<String,String> plugins_concurrency = new HashMap<>();
			String[] parts = plugins.split(",");
			for (String part : parts) {
				String[] tmp = part.split("=");
				String name = tmp[0];
				if (name.charAt(0) == '+' || name.charAt(0) == '-') {
					name = name.substring(1);
				}
				if (tmp.length > 1) {
					plugins_concurrency.put(name, tmp[1]);
				}
				if (smBeans.length() != 0)
					smBeans.append(",");
				smBeans.append(tmp[0]);

				try {
					XMPPImplIfc proc = ModulesManagerImpl.getInstance().getPlugin(name);
					if (proc == null) {
						proc = ProcessorFactory.getImplementation(name);
					}
					if (proc != null) {
						props.put("sess-man/" + name + "/class", proc.getClass().getCanonicalName());
					} else {
						log.log(Level.WARNING, "could not find class for processor " + name);
					}
				} catch (Exception ex) {
					log.log(Level.WARNING, "not able to get instance of processor " + name, ex);
				}
			}
			props.put("sess-man/beans", smBeans.toString());

			String concurrency = (String) props.get(SessionManagerConfig.PLUGINS_CONCURRENCY_PROP_KEY);
			if (concurrency != null) {
				for (String part : concurrency.split(",")) {
					String[] tmp = part.split("=");
					plugins_concurrency.put(tmp[0], tmp[1]);
				}
			}

			for (Map.Entry<String,String> e : plugins_concurrency.entrySet()) {
				String prefix = "sess-man/" + e.getKey() + "/";
				String[] tmp = e.getValue().split(":");
				try {
					props.put(prefix + "threadsNo", Integer.parseInt(tmp[0]));
				} catch (Exception ex) {
					log.log(Level.WARNING, "Plugin " + e.getKey() + " concurrency parsing error for: " + tmp[0], ex);
				}
				if (tmp.length > 1) {
					try {
						props.put(prefix + "queueSize", Integer.parseInt(tmp[1]));
					} catch (Exception ex) {
						log.log(Level.WARNING, "Plugin " + e.getKey() + " queueSize parsing error for: " + tmp[1], ex);
					}
				}
			}
		}

	}

	public void setProperties(Map<String,Object> props) {
		this.props = props;
	}


	@Override
	public void start() {
		for (Map.Entry<String, Object> e : props.entrySet()) {
			if (e.getKey().startsWith("--")) {
				String key = e.getKey().substring(2);
				System.setProperty(key, e.getValue().toString());
			}
		}
		// register default types converter and properties bean configurator
		kernel.registerBean(DefaultTypesConverter.class).exec();
		kernel.registerBean(PropertiesBeanConfiguratorWithBackwordCompatibility.class).exec();
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();

		// moved to AbstractBeanConfigurator
		//registerBeans();

		BeanConfigurator configurator = kernel.getInstance(PropertiesBeanConfiguratorWithBackwordCompatibility.class);
		if (configurator instanceof PropertiesBeanConfigurator) {
			PropertiesBeanConfigurator propertiesBeanConfigurator = (PropertiesBeanConfigurator) configurator;
			propertiesBeanConfigurator.setProperties(props);
		}
		// if null then we register global subbeans
		configurator.registerBeans(null, props);

		DependencyGrapher dg = new DependencyGrapher();
		dg.setKernel(kernel);
		System.out.println(dg.getDependencyGraph());

		MessageRouter mr = kernel.getInstance("message-router");
		mr.start();
	}

	@Override
	public void stop() {
		MessageRouter mr = kernel.getInstance("message-router");
		mr.stop();
	}

	// moved to AbstractBeanConfigurator
//	public void registerBeans() {
//	}
//
//	protected void registerBeans(Set<Class<?>> classes) {
//		for (Class<?> cls : classes) {
//			Bean annotation = shouldRegister(cls, this.getClass());
//			if (annotation != null) {
//				kernel.registerBean(cls);
//			}
//		}
//	}
//
//	protected Bean shouldRegister(Class<?> cls, Class<?> requiredClass) {
//		Bean annotation = cls.getAnnotation(Bean.class);
//		if (annotation == null)
//			return null;
//
//		Class parent = annotation.parent();
//		if (parent == Object.class)
//			return null;
//
//		return parent.isAssignableFrom(requiredClass) ? annotation : null;
//	}

	protected Kernel getKernel() {
		return kernel;
	}
}
