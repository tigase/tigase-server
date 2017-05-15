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

import tigase.component.DSLBeanConfigurator;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.conf.ConfigHolder;
import tigase.conf.ConfigReader;
import tigase.conf.ConfiguratorAbstract;
import tigase.conf.LoggingBean;
import tigase.db.beans.DataSourceBean;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.KernelException;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.beans.selector.ServerBeanSelector;
import tigase.kernel.core.DependencyGrapher;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.conf.Configurable.*;
import static tigase.conf.ConfiguratorAbstract.LOGGING_KEY;

/**
 * Bootstrap class is responsible for initialization of Kernel to start Tigase XMPP Server.
 *
 * Created by andrzej on 05.03.2016.
 */
public class Bootstrap implements Lifecycle {

	private static final Logger log = Logger.getLogger(Bootstrap.class.getCanonicalName());

	private final Kernel kernel;
	private ConfigHolder config = new ConfigHolder();

	public Bootstrap() {
		kernel = new Kernel("root");
	}

	public void init(String[] args) throws ConfigReader.ConfigException, IOException {
		config.loadConfiguration(args);
		configureLogManager();
	}

	public void setProperties(Map<String,Object> props) {
		this.config.setProperties(props);
	}


	@Override
	public void start() {
		for (Map.Entry<String, Object> e : config.getProperties().entrySet()) {
			if (e.getKey().startsWith("--")) {
				String key = e.getKey().substring(2);
				System.setProperty(key, e.getValue().toString());
				if (CLUSTER_MODE.equals(e.getKey())) {
					System.setProperty("tigase.cache", "false");
				}					
			}
		}

		try {
			if (XMPPServer.isOSGi()) {
				kernel.registerBean("classUtilBean").asInstance(Class.forName("tigase.osgi.util.ClassUtilBean").newInstance()).exportable().exec();
			} else {
				kernel.registerBean("classUtilBean").asInstance(Class.forName("tigase.util.ClassUtilBean").newInstance()).exportable().exec();
			}
		} catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		// register default types converter and properties bean configurator
		kernel.registerBean(DefaultTypesConverter.class).exportable().exec();
		kernel.registerBean(DSLBeanConfiguratorWithBackwardCompatibility.class).exportable().exec();
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();

		DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
		configurator.setConfigHolder(config);
		ModulesManagerImpl.getInstance().setBeanConfigurator(configurator);

		kernel.registerBean("logging").asClass(LoggingBean.class).setActive(true).setPinned(true).exec();
		kernel.getInstance("logging");

		kernel.registerBean("beanSelector").asInstance(new ServerBeanSelector()).exportable().exec();

		// if null then we register global subbeans
		configurator.registerBeans(null, null, config.getProperties());

		DependencyGrapher dg = new DependencyGrapher();
		dg.setKernel(kernel);

		log.log(Level.CONFIG, dg.getDependencyGraph());

		// this is called to make sure that data sources are properly initialized
		if (ServerBeanSelector.getConfigType(kernel) != ConfigTypeEnum.SetupMode) {
			DataSourceBean dataSource = kernel.getInstance(DataSourceBean.class);
			if (dataSource == null || dataSource.getDataSourceNames().isEmpty()) {
				throw new KernelException("Failed to initialize data sources!");
			}
		}
		MessageRouter mr = kernel.getInstance("message-router");
		mr.start();

//		StringBuilder sb = new StringBuilder("\n======");
//		sb.append("\n");
//		final Collection<BeanConfig> beanConfigs = kernel.getDependencyManager().getBeanConfigs();
//		for (BeanConfig beanConfig : beanConfigs) {
//			sb.append("bean config: ").append(beanConfig).append("\n");
//			final Set<BeanConfig> registeredBeans = beanConfig.getRegisteredBeans();
//			for (BeanConfig registeredBean : registeredBeans) {
//				sb.append("  -> registered bean: ").append(registeredBean).append("\n");
//				final Set<BeanConfig> registeredBeans1 = registeredBean.getRegisteredBeans();
//				for (BeanConfig beanConfig1 : registeredBeans1) {
//					sb.append("    -> registered bean1: ").append(beanConfig1).append("\n");
//				}
//			}
//		}
//		sb.append("======\n");
//		System.out.println(sb);

		try {
			File f = new File("etc/config-dump.properties");
			if (f.exists()) {
				f.delete();
			}
			configurator.dumpConfiguration(f);
		} catch (IOException ex) {
			log.log(Level.FINE, "failed to dump configuration to file etc/config-dump.properties");
		}
	}

	@Override
	public void stop() {
		MessageRouter mr = kernel.getInstance("message-router");
		mr.stop();
	}

	public <T> T getInstance(String beanName) {
		return kernel.getInstance(beanName);
	}

	public <T> T getInstance(Class<T> clazz) {
		return kernel.getInstance(clazz);
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

	// Common logging setup
	private Map<String, String> loggingSetup = new LinkedHashMap<String, String>(10);

	private void configureLogManager() {
		Map<String, Object> cfg = prepareLogManagerConfiguration(config.getProperties());
		setupLogManager(cfg);
	}

	private Map<String, Object> prepareLogManagerConfiguration(Map<String, Object> params) {
		Map<String, Object> defaults = new HashMap<>();
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

		return defaults;
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
		ConfiguratorAbstract.loadLogManagerConfig(buff.toString());
		log.config("DONE");
	}
}
