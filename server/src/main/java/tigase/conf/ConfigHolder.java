/*
 * ConfigHolder.java
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
package tigase.conf;

import tigase.cluster.ClusterConnectionManager;
import tigase.db.RepositoryFactory;
import tigase.io.CertificateContainer;
import tigase.io.SSLContextContainerIfc;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.server.ConnectionManager;
import tigase.server.amp.ActionAbstract;
import tigase.server.bosh.BoshConnectionManager;
import tigase.server.monitor.MonitorRuntime;
import tigase.server.xmppsession.SessionManagerConfig;
import tigase.sys.TigaseRuntime;
import tigase.util.DNSResolverFactory;
import tigase.util.DNSResolverIfc;
import tigase.util.NonpriorityQueue;
import tigase.util.PriorityQueueAbstract;
import tigase.util.ui.console.CommandlineParameter;
import tigase.util.ui.console.ParameterParser;
import tigase.util.ui.console.Task;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.impl.roster.RosterFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static tigase.conf.Configurable.CLUSTER_NODES_PROP_KEY;
import static tigase.io.SSLContextContainerIfc.*;

/**
 * Created by andrzej on 18.09.2016.
 */
public class ConfigHolder {

	private static final Logger log = Logger.getLogger(ConfigHolder.class.getCanonicalName());

	public static final String PROPERTIES_CONFIG_FILE_DEF = ConfiguratorAbstract.PROPERTY_FILENAME_PROP_DEF;
	public static final String PROPERTIES_CONFIG_FILE_KEY = ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY;

	public static final String TDSL_CONFIG_FILE_DEF = ConfiguratorAbstract.PROPERTY_FILENAME_PROP_DEF.replace("/init.properties", "/config.tdsl");
	public static final String TDSL_CONFIG_FILE_KEY = "--config-file";
	
	private Path configFile = Paths.get(TDSL_CONFIG_FILE_DEF);
	private Map<String, Object> props = new LinkedHashMap<>();

	public static void main(String[] args) throws Exception {
		String scriptName = System.getProperty("scriptName");
		ParameterParser parser = new ParameterParser(true);

		parser.setTasks(new Task[]{new Task.Builder().name("upgrade-config")
										   .description("Checks configuration file and upgrades it if needed")
										   .function(ConfigHolder::upgradeConfig).build()});
		parser.addOption(
				new CommandlineParameter.Builder(null, PROPERTIES_CONFIG_FILE_KEY.replace("--", "")).defaultValue(
						PROPERTIES_CONFIG_FILE_DEF)
						.description("Path to properties configuration file")
						.requireArguments(true)
						.build());

		parser.addOption(new CommandlineParameter.Builder(null, TDSL_CONFIG_FILE_KEY.replace("--", "")).defaultValue(
				TDSL_CONFIG_FILE_DEF).description("Path to DSL configuration file").requireArguments(true).build());

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

	private static void upgradeConfig(Properties props) throws Exception {
		String[] args = props.entrySet()
				.stream()
				.flatMap(e -> Stream.of("--" + e.getKey(), e.getValue()))
				.toArray(x -> new String[x]);

		ConfigHolder holder = new ConfigHolder();

		try {
			holder.fixShutdownThreadIssue();

			Optional<String[]> output = holder.loadConfiguration(args);

			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(output.orElse(new String[]{
							"Configuration file " + holder.configFile + " is in DSL format and is valid."}));

		} catch (ConfigReader.UnsupportedOperationException e) {
			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{"ERROR! Error in configuration file: " + holder.configFile,
												 e.getMessage() + " at line " + e.getLine() + " position " +
														 e.getPosition(), "Line: " + e.getLineContent()});
		} catch (ConfigReader.ConfigException e) {
			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{"ERROR! Error in configuration file: " + holder.configFile,
												 "Issue with configuration file: " + e});
		} catch (RuntimeException e) {
			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{"Error! Failed to save upgraded configuration file",
												 e.getMessage()});
		}
	}

	public static Path backupOldConfigFile(Path initPropsFile) throws IOException {
		Path initPropsFileOld = initPropsFile.resolveSibling(initPropsFile.getFileName() + ".old");
		int i=0;
		while (Files.exists(initPropsFileOld)) {
			i++;
			initPropsFileOld = initPropsFile.resolveSibling(initPropsFile.getFileName() + ".old." + i);
		}

		Files.deleteIfExists(initPropsFileOld);
		Files.move(initPropsFile, initPropsFileOld);
		return initPropsFileOld;
	}

	public Optional<String[]> loadConfiguration(String[] args) throws IOException, ConfigReader.ConfigException {
		boolean hasCustomConfigFile = false;
		for (int i=0; i<args.length; i++) {
			if (TDSL_CONFIG_FILE_KEY.equals(args[i]) && (i+1) < args.length) {
				configFile = Paths.get(args[++i]);
				hasCustomConfigFile = true;
			}
			if (PROPERTIES_CONFIG_FILE_KEY.equals(args[i]) && (i+1) < args.length) {
				if (!hasCustomConfigFile) {
					Path propsFilePath = Paths.get(args[++i]);
					configFile = propsFilePath.resolveSibling(configFile.getFileName());
				}
			}
		}

		OldConfigHolder oldConfigHolder = new OldConfigHolder();
		oldConfigHolder.convert(args, configFile);
		Optional<String[]> output = oldConfigHolder.getOutput();
		
		loadFromDSLFiles();
		if (upgradeDSL(props)) {
			try {
				Optional<Path> configFileOld = Optional.ofNullable(backupOldConfigFile(configFile));
				saveToDSLFile(configFile.toFile());
				log.log(Level.CONFIG, "Configuration file {0} was updated to match current format." +
						configFileOld.map(path -> " Previous version of configuration file was saved as " + path).orElse(""),
						new Object[]{configFile});

				if (!oldConfigHolder.getOutput().isPresent()) {
					output = Optional.of(Stream.of(java.text.MessageFormat.format(
							"Configuration file {0} was updated to match current format.", configFile),
												   configFileOld.map(
														   path -> "Previous version of configuration file was saved as " +
																   path).orElse(""))
												 .filter(line -> !line.isEmpty())
												 .toArray(x -> new String[x]));
				}
			} catch (IOException ex) {
				log.log(Level.SEVERE, "could not replace configuration file with file in DSL format", ex);
				throw new RuntimeException(ex.getMessage(), ex);
			}
		}

		try (Writer w = new StringWriter()) {
			new ConfigWriter().resolveVariables().write(w, props);
			log.log(Level.CONFIG, "Loaded configuration:\n" + w.toString());
		}

		return output;
	}

	public Map<String, Object> getProperties() {
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		this.props = props;
	}

	public void saveToDSLFile(File f) throws IOException {
		new ConfigWriter().write(f, props);
	}

	public Path getConfigFilePath() {
		return configFile;
	}

	private void loadFromDSLFiles() throws IOException, ConfigReader.ConfigException {
		log.log(Level.CONFIG, "Loading configuration from file: {0}", configFile);
		Map<String, Object> loaded = new ConfigReader().read(configFile.toFile());
		props.putAll(loaded);
	}
	
	private static void putIfAbsent(Map<String, Object> props, String newKey, Object value) {
		Map<String, Object> parentProps = null;
		String[] parts = newKey.split("/");
		for (int i=0; i < parts.length-1; i++) {
			parentProps = props;
			props = (Map<String, Object>) props.computeIfAbsent(parts[i], (k) -> new HashMap<>());
		}
		String key = parts[parts.length-1];
		if (key.equals("class")) {
			if (!(props instanceof AbstractBeanConfigurator.BeanDefinition)) {
				AbstractBeanConfigurator.BeanDefinition tmp  = new AbstractBeanConfigurator.BeanDefinition();
				tmp.setBeanName(parts[parts.length-2]);
				tmp.putAll(props);
				props = tmp;
				parentProps.put(tmp.getBeanName(), tmp);
			}
			((AbstractBeanConfigurator.BeanDefinition) props).setClazzName(value.toString());
		} else {
			props.put(key, value);
		}
	}

	public static Optional renameIfExists(Map<String, Object> props, String oldKey, String newKey, Function<Object, Object> converter) {
		Optional value = Optional.ofNullable(props.remove(oldKey));
		value = value.map(converter);
		if (value.isPresent()) {
			putIfAbsent(props, newKey, value.get());
		}
		return value;
	}

	public static void removeIfExistsAnd(Map<String, Object> props, String oldKey, BiConsumer<BiConsumer<String, Object>, Object> consumer) {
		Object value = props.remove(oldKey);
		if (value != null) {
			consumer.accept((k,v) -> {
				putIfAbsent(props, k, v);
			}, value);
		}
	}

	private static boolean upgradeDSL(Map<String, Object> props) {
		String before = props.toString();
		props.remove("--config-file");
		renameIfExists(props, "--cluster-mode", "cluster-mode", Function.identity());
		renameIfExists(props, Configurable.GEN_VIRT_HOSTS, "virtual-hosts", value -> {
			if (value instanceof String) {
				return Arrays.asList(((String) value).split(","));
			} else {
				return value;
			}
		});
		renameIfExists(props, Configurable.GEN_DEBUG, "debug", value -> {
			if (value instanceof String) {
				return Arrays.asList(((String) value).split(","));
			} else {
				return value;
			}
		});
		renameIfExists(props, Configurable.GEN_DEBUG_PACKAGES, "debug-packages", value -> {
			if (value instanceof String) {
				return Arrays.asList(((String) value).split(","));
			} else {
				return value;
			}
		});
		renameIfExists(props, "--packet.debug.full", "logging/packet-debug-full", Function.identity());
		renameIfExists(props, "--" + PriorityQueueAbstract.QUEUE_IMPLEMENTATION, "priority-" + PriorityQueueAbstract.QUEUE_IMPLEMENTATION, Function.identity());
		if (Boolean.parseBoolean("" + props.remove("--nonpriority-queue"))) {
			props.putIfAbsent("priority-" + PriorityQueueAbstract.QUEUE_IMPLEMENTATION, NonpriorityQueue.class.getCanonicalName());
		}
		renameIfExists(props, Configurable.CLUSTER_NODES, CLUSTER_NODES_PROP_KEY, value -> {
			if (value instanceof String) {
				return Arrays.asList(((String) value).split(","));
			} else {
				return value;
			}
		});
		renameIfExists(props, "--client-port-delay-listening", "client-port-delay-listening", Function.identity());
		renameIfExists(props, "--shutdown-thread-dump", "shutdown-thread-dump", Function.identity());
		renameIfExists(props, ActionAbstract.AMP_SECURITY_LEVEL, "amp-security-level", Function.identity());
		removeIfExistsAnd(props, "--cm-see-other-host", (setter, value) -> {
			setter.accept("c2s/seeOtherHost/class", value);
			setter.accept("bosh/seeOtherHost/class", value);
			setter.accept("ws2s/seeOtherHost/class", value);
		});
		renameIfExists(props,"--watchdog_timeout","watchdog-timeout", Function.identity());
		renameIfExists(props,"--watchdog_delay", "watchdog-timeout", Function.identity());
		renameIfExists(props,"--watchdog_ping_type", "watchdog-ping-type", Function.identity());
		renameIfExists(props, "--installation-id", "installation-id", Function.identity());
		renameIfExists(props, ConnectionManager.NET_BUFFER_HT_PROP_KEY, ConnectionManager.NET_BUFFER_HT_PROP_KEY.substring(2), Function.identity());
		renameIfExists(props, ConnectionManager.NET_BUFFER_ST_PROP_KEY, ConnectionManager.NET_BUFFER_ST_PROP_KEY.substring(2), Function.identity());
		renameIfExists(props, ConnectionManager.HT_TRAFFIC_THROTTLING_PROP_KEY, ConnectionManager.HT_TRAFFIC_THROTTLING_PROP_KEY.substring(2), Function.identity());
		renameIfExists(props, ConnectionManager.ST_TRAFFIC_THROTTLING_PROP_KEY, ConnectionManager.ST_TRAFFIC_THROTTLING_PROP_KEY.substring(2), Function.identity());
		renameIfExists(props, "--ws-allow-unmasked-frames", "ws-allow-unmasked-frames", Function.identity());
		renameIfExists(props, "--vhost-tls-required", "vhost-tls-required", Function.identity());
		renameIfExists(props, "--vhost-register-enabled", "vhost-register-enabled", Function.identity());
		renameIfExists(props, "--vhost-message-forward-jid", "vhost-message-forward-jid", Function.identity());
		renameIfExists(props, "--vhost-presence-forward-jid", "vhost-presence-forward-jid", Function.identity());
		renameIfExists(props, "--vhost-max-users", "vhost-max-users", Function.identity());
		renameIfExists(props, "--vhost-anonymous-enabled", "vhost-anonymous-enabled", Function.identity());
		renameIfExists(props, "--vhost-disable-dns-check", "vhost-disable-dns-check", Function.identity());
		renameIfExists(props, "--s2s-secret", "vhost-man/defaults/s2s-secret", Function.identity());
		renameIfExists(props, "--test", "logging/rootLevel", value -> Boolean.TRUE.equals(value) ? Level.WARNING : Level.CONFIG);

		renameIfExists(props, "--" + SSLContextContainerIfc.DEFAULT_DOMAIN_CERT_KEY, "certificate-container/" + SSLContextContainerIfc.DEFAULT_DOMAIN_CERT_KEY, Function.identity());
		renameIfExists(props, "--" + CertificateContainer.SNI_DISABLE_KEY, "certificate-container/" + CertificateContainer.SNI_DISABLE_KEY, Function.identity());
		renameIfExists(props, "--" + SSLContextContainerIfc.SERVER_CERTS_LOCATION_KEY, "certificate-container/" + SSLContextContainerIfc.SERVER_CERTS_LOCATION_KEY, Function.identity());
		renameIfExists(props, "--" + SSLContextContainerIfc.TRUSTED_CERTS_DIR_KEY, "certificate-container/" + SSLContextContainerIfc.TRUSTED_CERTS_DIR_KEY, Function.identity());
		renameIfExists(props, "--pem-privatekey-password", "certificate-container/pem-privatekey-password", Function.identity());
		renameIfExists(props, "--tls-jdk-nss-bug-workaround-active", "tls-jdk-nss-bug-workaround-active", Function.identity());
		renameIfExists(props, "--tls-enabled-protocols", "tls-enabled-protocols", value -> {
			if (value instanceof String) {
				return Arrays.asList(((String) value).split(","));
			} else {
				return value;
			}
		});
		renameIfExists(props, "--tls-enabled-ciphers", "tls-enabled-ciphers", value -> {
			if (value instanceof String) {
				return Arrays.asList(((String) value).split(","));
			} else {
				return value;
			}
		});
		renameIfExists(props, "--hardened-mode", "hardened-mode", Function.identity());

		boolean allowInvalidCerts = Boolean.parseBoolean((String) props.remove(ALLOW_INVALID_CERTS_KEY));
		boolean allowSelfSignedCerts = Boolean.parseBoolean((String) props.remove(ALLOW_SELF_SIGNED_CERTS_KEY));
		if (allowInvalidCerts || allowSelfSignedCerts) {
			props.put("certificate-container/ssl-trust-model", allowInvalidCerts ? "all" : (allowSelfSignedCerts ? "selfsigned" : "trusted"));
		}

		renameIfExists(props, "--" + BoshConnectionManager.BOSH_EXTRA_HEADERS_FILE_PROP_KEY, BoshConnectionManager.BOSH_EXTRA_HEADERS_FILE_PROP_KEY, Function.identity());
		renameIfExists(props, "--" + BoshConnectionManager.BOSH_CLOSE_CONNECTION_PROP_KEY, BoshConnectionManager.BOSH_CLOSE_CONNECTION_PROP_KEY, Function.identity());
		renameIfExists(props, "--" + BoshConnectionManager.CLIENT_ACCESS_POLICY_FILE_PROP_KEY, BoshConnectionManager.CLIENT_ACCESS_POLICY_FILE_PROP_KEY, Function.identity());
		renameIfExists(props, "--stringprep-processor", "stringprep-processor", Function.identity());
		renameIfExists(props, ClusterConnectionManager.CONNECT_ALL_PAR, "cl-comp/" + ClusterConnectionManager.CONNECT_ALL_PROP_KEY, Function.identity());
		renameIfExists(props, "--cluster-connections-per-node", "cl-comp/connections-per-node", Function.identity());

		renameIfExists(props,"--" + DNSResolverFactory.TIGASE_RESOLVER_CLASS, "dns-resolver/" + DNSResolverFactory.TIGASE_RESOLVER_CLASS, Function.identity());
		renameIfExists(props, "--" + DNSResolverIfc.TIGASE_PRIMARY_ADDRESS, "dns-resolver/" + DNSResolverIfc.TIGASE_PRIMARY_ADDRESS, Function.identity());
		renameIfExists(props, "--" + DNSResolverIfc.TIGASE_SECONDARY_ADDRESS, "dns-resolver/" + DNSResolverIfc.TIGASE_SECONDARY_ADDRESS, Function.identity());

		renameIfExists(props, "--s2s-skip-tls-hostnames", "s2s/skip-tls-hostnames", value -> {
			if (value instanceof String) {
				return Arrays.asList(((String) value).split(","));
			} else {
				return value;
			}
		});
		renameIfExists(props, "--" + Configurator.SCRIPTS_DIR_PROP_KEY, Configurator.SCRIPTS_DIR_PROP_KEY, Function.identity());

		renameIfExists(props, "--" + XMPPIOService.CROSS_DOMAIN_POLICY_FILE_PROP_KEY, XMPPIOService.CROSS_DOMAIN_POLICY_FILE_PROP_KEY, Function.identity());
		renameIfExists(props, "--domain-filter-policy", "domain-filter-policy", Function.identity());
		props.remove("--script-dir");
		removeIfExistsAnd(props, "--new-connections-throttling", (setter, value) -> {
			String[] all_ports_thr = ((String)value).split(",");

			for (String port_thr : all_ports_thr) {
				String[] port_thr_ar = port_thr.split(":");

				if (port_thr_ar.length == 2) {
					try {
						int port_no = Integer.parseInt(port_thr_ar[0]);
						long throttling = Long.parseLong(port_thr_ar[1]);
						switch (port_no) {
							case 5222:
							case 5223:
								setter.accept("c2s/connections/" + port_no + "/new-connections-throttling", throttling);
								break;
							case 5280:
								setter.accept("bosh/connections/" + port_no + "/new-connections-throttling", throttling);
								break;
							case 5277:
								setter.accept("c2s/connections/" + port_no + "/new-connections-throttling", throttling);
								break;
							case 5290:
								setter.accept("ws2s/connections/" + port_no + "/new-connections-throttling", throttling);
								break;
							case 5269:
								setter.accept("s2s/connections/" + port_no + "/new-connections-throttling", throttling);
								break;
							default:
								Stream.of("c2s", "bosh", "ws2s", "s2s", "ext").forEach(cmp -> {
									Map<String, Object> cmpCfg = (Map<String, Object>) props.get(cmp);
									if (cmpCfg == null) {
										return;
									}
									Map<String, Object> cmpConns = (Map<String, Object>) cmpCfg.get("connections");
									if (cmpConns == null) {
										return;
									}

									Map<String, Object> portSettings = (Map<String, Object>) cmpConns.get("" + port_no);
									if (portSettings == null) {
										return;
									}

									portSettings.putIfAbsent("new-connections-throttling", throttling);
								});

						}
					} catch (Exception e) {

						// bad configuration
						log.log(Level.WARNING,
								"Connections throttling configuration error, bad format, " +
										"check the documentation for a correct syntax, " +
										"port throttling config: {0}", port_thr);
					}
				} else {

					// bad configuration
					log.log(Level.WARNING,
							"Connections throttling configuration error, bad format, " +
									"check the documentation for a correct syntax, " +
									"port throttling config: {0}", port_thr);
				}
			}

		});
		renameIfExists(props, "--" + RosterFactory.ROSTER_IMPL_PROP_KEY, RosterFactory.ROSTER_IMPL_PROP_KEY, Function.identity());
		renameIfExists(props, "--s2s-ejabberd-bug-workaround-active", "s2s/dialback/ejabberd-bug-workaround", (v)-> Boolean.parseBoolean("" + v));
		renameIfExists(props,"--" + SessionManagerConfig.SM_THREADS_POOL_PROP_KEY, "sess-man/" + SessionManagerConfig.SM_THREADS_POOL_PROP_KEY, Function.identity());

		removeIfExistsAnd(props, "--" + SSL_CONTAINER_CLASS_KEY, (setter, value) -> {
			Map<String, Object> cfg = (Map<String, Object>) props.getOrDefault("rootSslContextContainer", new HashMap<>());
			if (cfg instanceof AbstractBeanConfigurator.BeanDefinition) {
				((AbstractBeanConfigurator.BeanDefinition) cfg).setClazzName(value.toString());
			} else {
				AbstractBeanConfigurator.BeanDefinition.Builder builder = new AbstractBeanConfigurator.BeanDefinition.Builder()
						.name("rootSslContextContainer")
						.active(true)
						.clazz(value.toString());
				cfg.forEach(builder::property);
				props.put("rootSslContextContainer", builder.build());
			}
		});

		Stream.of("c2s", "bosh", "ws2s").forEach(cmp -> {
			Map<String, Object> cmpCfg = (Map<String, Object>) props.get(cmp);
			if (cmpCfg != null) {
				Map<String, Object> oldCfg = (Map<String, Object>) cmpCfg.remove("cm-see-other-host");
				Map<String, Object> newCfg = (Map<String, Object>) cmpCfg.computeIfAbsent("seeOtherHost", (x) -> new HashMap<String, Object>());
				if (oldCfg != null) {
					newCfg.putAll(oldCfg);
				}
			}
		});

		removeIfExistsAnd(props, RepositoryFactory.USER_REPO_POOL_CLASS, (setter, value) -> {
			Map<String, Object> repos = ((Map<String, Object>) props.get("userRepository"));
			(repos != null ? repos.keySet() : Collections.singleton("default")).forEach( name -> {
				setter.accept("userRepository/" + name + "/pool-class", value);
			});
		});
		removeIfExistsAnd(props, RepositoryFactory.USER_REPO_POOL_SIZE, (setter, value) -> {
			Map<String, Object> repos = ((Map<String, Object>) props.get("userRepository"));
			(repos != null ? repos.keySet() : Collections.singleton("default")).forEach( name -> {
				setter.accept("userRepository/" + name + "/pool-size", value);
			});
		});

		Map<String, Object> sessManCfg = (Map<String, Object>) props.get("sess-man");
		if (sessManCfg != null) {
			Object saslMechanisms = (String) sessManCfg.remove("enabled-mechanisms");
			if (saslMechanisms != null) {
				if (saslMechanisms instanceof String) {
					putIfAbsent(props, "sess-man/sasl-provider/allowed-mechanisms", Arrays.asList(((String) saslMechanisms).split(",")));
				} else {
					putIfAbsent(props, "sess-man/sasl-provider/allowed-mechanisms", saslMechanisms);
				}
			}
		}

		removeIfExistsAnd(props, "--api-keys", (setter, value) -> {
			putIfAbsent(props,"http/api-keys", Arrays.asList(value.toString().split(",")));
		});

		String after = props.toString();
		return !before.equals(after);
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

}
