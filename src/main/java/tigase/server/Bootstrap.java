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
package tigase.server;

import tigase.component.DSLBeanConfigurator;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.conf.ConfigHolder;
import tigase.conf.ConfigReader;
import tigase.conf.ConfiguratorAbstract;
import tigase.conf.LoggingBean;
import tigase.db.beans.DataSourceBean;
import tigase.db.beans.MDPoolBean;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.KernelException;
import tigase.kernel.beans.Autostart;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.beans.selector.ServerBeanSelector;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyGrapher;
import tigase.kernel.core.Kernel;
import tigase.net.ConnectionOpenThread;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.monitor.MonitorRuntime;
import tigase.sys.ShutdownHook;
import tigase.util.dns.DNSResolverDefault;
import tigase.util.dns.DNSResolverFactory;
import tigase.util.dns.DNSResolverIfc;
import tigase.util.reflection.ClassUtilBean;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.jid.BareJID;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.conf.Configurable.GEN_DEBUG;
import static tigase.conf.Configurable.GEN_DEBUG_PACKAGES;
import static tigase.conf.ConfiguratorAbstract.LOGGING_KEY;

/**
 * Bootstrap class is responsible for initialization of Kernel to start Tigase XMPP Server.
 * <br>
 * Created by andrzej on 05.03.2016.
 */
public class Bootstrap {

	private static final Logger log = Logger.getLogger(Bootstrap.class.getCanonicalName());

	private final Kernel kernel;
	private final ShutdownHook shutdownHook = new BootstrapShutdownHook();
	private ConfigHolder config = new ConfigHolder();
	// Common logging setup
	private Map<String, String> loggingSetup = new LinkedHashMap<String, String>(10);

	public Bootstrap() {
		kernel = new Kernel("root");
	}

	private void configureLogManager() {
		Map<String, Object> cfg = prepareLogManagerConfiguration(config.getProperties());
		setupLogManager(cfg);
	}

	public <T> T getInstance(String beanName) {
		return kernel.getInstance(beanName);
	}

	public <T> T getInstance(Class<T> clazz) {
		return kernel.getInstance(clazz);
	}

	protected Kernel getKernel() {
		return kernel;
	}

	public void init(String[] args) throws ConfigReader.ConfigException, IOException {
		config.loadConfiguration(args);
		configureLogManager();
	}

	private void initializeAutostartBeans(Kernel kernel) {
		log.config("Starting 'autostart' beans in kernel " + kernel.getName());
		for (BeanConfig bc : kernel.getDependencyManager().getBeanConfigs()) {
			if (Kernel.class.isAssignableFrom(bc.getClazz())) {
				Kernel sk = kernel.getInstance(bc.getBeanName());
				if (sk != kernel) {
					initializeAutostartBeans(sk);
					continue;
				}
			}
			Autostart autostart = bc.getClazz().getAnnotation(Autostart.class);

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Checking for autostart " + bc.getKernel().getName() + "." + bc.getBeanName() + ":: state=" + bc.getState() +
								   "; " + "autostart=" + (autostart != null));
			}

			if ((bc.getState() == BeanConfig.State.registered || bc.getState() == BeanConfig.State.initialized) &&
					autostart != null) {
				log.config("Autostarting bean " + bc);
				kernel.getInstance(bc.getBeanName());
			}
		}
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

	private void initializeDnsResolver() {
		Map<String, Object> resolverConfig = (Map<String, Object>) config.getProperties().get("dns-resolver");
		if (resolverConfig != null) {
			String resolverClass = (String) resolverConfig.get(DNSResolverFactory.TIGASE_RESOLVER_CLASS);
			if (resolverClass != null) {
				DNSResolverFactory.setDnsResolverClassName(resolverClass);
			}
			DNSResolverIfc resolver = DNSResolverFactory.getInstance();
			if (resolver instanceof DNSResolverDefault) {
				Object config = resolverConfig.get(DNSResolverIfc.TIGASE_PRIMARY_ADDRESS);
				if (config instanceof ConfigReader.AbstractEnvironmentPropertyVariable) {
					config = ((ConfigReader.AbstractEnvironmentPropertyVariable) config).calculateValue();
				}
				String host = (String) config;
				if (host != null && !host.isEmpty()) {
					((DNSResolverDefault) resolver).setPrimaryHost(host);
				}
				config = resolverConfig.get(DNSResolverIfc.TIGASE_SECONDARY_ADDRESS);
				if (config instanceof ConfigReader.AbstractEnvironmentPropertyVariable) {
					config = ((ConfigReader.AbstractEnvironmentPropertyVariable) config).calculateValue();
				}
				host = (String) config;
				if (host != null && !host.isEmpty()) {
					((DNSResolverDefault) resolver).setSecondaryHost(host);
				}
			}
		}
	}

	private Map<String, Object> prepareLogManagerConfiguration(Map<String, Object> params) {
		Map<String, Object> defaults = new HashMap<>();
		String levelStr = ".level";

//		if ((Boolean) params.get("test")) {
//			defaults.put(LOGGING_KEY + levelStr, "WARNING");
//		} else {
		defaults.put(LOGGING_KEY + levelStr, "CONFIG");
//		}
		defaults.put(LOGGING_KEY + "handlers", "java.util.logging.ConsoleHandler java.util.logging.FileHandler");
		defaults.put(LOGGING_KEY + "java.util.logging.ConsoleHandler.formatter", "tigase.util.log.LogFormatter");
		defaults.put(LOGGING_KEY + "java.util.logging.ConsoleHandler.level", "WARNING");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.append", "true");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.count", "5");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.formatter", "tigase.util.log.LogFormatter");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.limit", "10000000");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.pattern", "logs/tigase.log");
		defaults.put(LOGGING_KEY + "tigase.useParentHandlers", "true");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.level", "ALL");
		defaults.put(LOGGING_KEY + "tigase.kernel.core.Kernel.level", "CONFIG");
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

	public void setProperties(Map<String, Object> props) {
		this.config.setProperties(props);
	}

	private void setupLogManager(Map<String, Object> properties) {
		Set<Map.Entry<String, Object>> entries = properties.entrySet();
		StringBuilder buff = new StringBuilder(200);

		for (Map.Entry<String, Object> entry : entries) {
			if (entry.getKey().startsWith(LOGGING_KEY)) {
				String key = entry.getKey().substring(LOGGING_KEY.length());
				loggingSetup.put(key, entry.getValue().toString());
			}
		}

		for (String key : loggingSetup.keySet()) {
			String entry = loggingSetup.get(key);
			buff.append(key).append("=").append(entry).append("\n");
			if (key.equals("java.util.logging.FileHandler.pattern")) {
				File log_path = new File(entry).getParentFile();
				if (!log_path.exists()) {
					log_path.mkdirs();
				}
			}    // end of if (key.equals())
		}      // end of if (entry.getKey().startsWith(LOGGING_KEY))

		// System.out.println("Setting logging: \n" + buff.toString());
		ConfiguratorAbstract.loadLogManagerConfig(buff.toString());
		log.config("DONE");
	}

	public void start() {
		initializeDnsResolver();
		Object clusterMode = config.getProperties()
				.getOrDefault("cluster-mode", config.getProperties().getOrDefault("--cluster-mode", Boolean.FALSE));
		if (clusterMode instanceof ConfigReader.Variable) {
			clusterMode = ((ConfigReader.Variable) clusterMode).calculateValue();
			if (clusterMode == null) {
				clusterMode = Boolean.FALSE;
			}
		}
		if (clusterMode instanceof String) {
			clusterMode = Boolean.parseBoolean((String) clusterMode);
		}
		if ((Boolean) clusterMode) {
			System.setProperty("tigase.cache", "false");
			log.log(Level.WARNING, "Tigase cache turned off");
		}
		config.getProperties().put("cluster-mode", clusterMode);

		Optional.ofNullable((String) config.getProperties().get("stringprep-processor"))
				.ifPresent(val -> BareJID.useStringprepProcessor(val));

		for (Map.Entry<String, Object> e : config.getProperties().entrySet()) {
			if (e.getKey().startsWith("--")) {
				String key = e.getKey().substring(2);
				Object value = e.getValue();
				if (value instanceof ConfigReader.Variable) {
					value = ((ConfigReader.Variable) value).calculateValue();
				}
				System.setProperty(key, value.toString());
			}
		}

		try {
			ClassUtilBean classUtilBean = null;
			if (XMPPServer.isOSGi()) {
				classUtilBean = (ClassUtilBean) Class.forName("tigase.osgi.util.ClassUtilBean").newInstance();
			} else {
				classUtilBean = (ClassUtilBean) Class.forName("tigase.util.reflection.ClassUtilBean").newInstance();
			}

			classUtilBean.initialize(ClassUtilBean.getPackagesToSkip(null));
			kernel.registerBean("classUtilBean").asInstance(classUtilBean).exportable().exec();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
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

		kernel.registerBean(RosterFactory.Bean.class).setPinned(true).exec();
		kernel.getInstance(RosterFactory.Bean.class);

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
		log.info("Starting MessageRouter");
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
			log.fine("Dump configuration");
			File f = new File("etc/config-dump.properties");
			if (f.exists()) {
				f.delete();
			}
			configurator.dumpConfiguration(f);
		} catch (IOException ex) {
			log.log(Level.FINE, "failed to dump configuration to file etc/config-dump.properties");
		}

		MonitorRuntime.getMonitorRuntime().addShutdownHook(shutdownHook);

		initializeAutostartBeans(kernel);
	}

	public void stop() {
		kernel.shutdown((bc1, bc2) -> {
			if (MDPoolBean.class.isAssignableFrom(bc1.getClazz())) {
				return Integer.MIN_VALUE;
			}
			if (MDPoolBean.class.isAssignableFrom(bc2.getClazz())) {
				return Integer.MIN_VALUE;
			}
			return 0;
		});
	}

	public class BootstrapShutdownHook
			implements ShutdownHook {

		@Override
		public String getName() {
			return "bootstrap-shutdown";
		}

		@Override
		public String shutdown() {
			ConnectionOpenThread.getInstance().stop();

			long start = System.currentTimeMillis();
			try {
				Bootstrap.this.stop();
			} catch (Throwable ex) {
				System.out.println("Warning: failed to shutdown Tigase Kernel with error: " + ex.getMessage());
				ex.printStackTrace();
				return ex.getMessage();

			}

			long end = System.currentTimeMillis();

			return "Tigase Kernel stopped in " + (end - start) + "ms";
		}
	}
}
