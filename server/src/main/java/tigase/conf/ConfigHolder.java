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

import tigase.db.RepositoryFactory;
import tigase.kernel.beans.Bean;
import tigase.server.ext.ComponentProtocol;
import tigase.server.monitor.MonitorRuntime;
import tigase.server.xmppsession.SessionManagerConfig;
import tigase.sys.TigaseRuntime;
import tigase.util.ClassUtilBean;
import tigase.util.DataTypes;
import tigase.util.ui.console.CommandlineParameter;
import tigase.util.ui.console.ParameterParser;
import tigase.util.ui.console.Task;
import tigase.xmpp.XMPPImplIfc;
import tigase.xmpp.XMPPProcessor;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static tigase.conf.ConfigHolder.Format.dsl;
import static tigase.conf.Configurable.GEN_SM_PLUGINS;
import static tigase.conf.ConfiguratorAbstract.PROPERTY_FILENAME_PROP_DEF;
import static tigase.conf.ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY;

/**
 * Created by andrzej on 18.09.2016.
 */
public class ConfigHolder {

	private static final Logger log = Logger.getLogger(ConfigHolder.class.getCanonicalName());

	public enum Format {
		dsl,
		properties
	}

	private Format format = null;
	private Map<String, Object> props = new LinkedHashMap<>();
	private Path initPropertiesPath;

	public static void main(String[] args) throws Exception {
		String scriptName = System.getProperty("scriptName");
		ParameterParser parser = new ParameterParser(true);

		parser.setTasks(new Task[]{new Task.Builder().name("upgrade-config")
										   .description("Checks configuration file and upgrades it if needed")
										   .function(ConfigHolder::upgradeConfig).build()});
		parser.addOption(new CommandlineParameter.Builder(null,
														  ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY.replace("--",
																												  "")).defaultValue(
				ConfiguratorAbstract.PROPERTY_FILENAME_PROP_DEF)
								 .description("Path to configuration file")
								 .requireArguments(true)
								 .required(true)
								 .build());

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

	private static void upgradeConfig(Properties args) throws Exception {
		String configFile = (String) args.get(ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY.replace("--", ""));
		ConfigHolder holder = new ConfigHolder();
		holder.props.put(ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY, configFile);

		holder.fixShutdownThreadIssue();

		if (!new File(configFile).exists()) {
			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{"Configuration file " + configFile + " does not exist."});
			return;
		}

		try {
			List<String> output = new ArrayList<>();
			holder.detectPathAndFormat();
			switch (holder.format) {
				case dsl:
					holder.loadFromDSLFiles();
					TigaseRuntime.getTigaseRuntime()
							.shutdownTigase(new String[]{
									"Configuration file " + configFile + " is in DSL format and is valid."});
					break;
				case properties:
					holder.loadFromPropertiesFiles();
					Path initPropsFile = holder.initPropertiesPath;
					Path initPropsFileOld = initPropsFile.resolveSibling(
							holder.initPropertiesPath.getFileName() + ".old");
					holder.props = ConfigWriter.buildTree(holder.props);
					try {
						Files.deleteIfExists(initPropsFileOld);
						Files.move(initPropsFile, initPropsFileOld);
						holder.saveToDSLFile(initPropsFile.toFile());
					} catch (IOException ex) {
						TigaseRuntime.getTigaseRuntime()
								.shutdownTigase(new String[]{"Error! Failed to save upgraded configuration file",
															 ex.getMessage()});
					}
					TigaseRuntime.getTigaseRuntime()
							.shutdownTigase(
									new String[]{"Configuration file " + configFile + " was converted to DSL format.",
												 "Previous version of a configuration file was saved at " +
														 initPropsFileOld});
					break;
			}
		} catch (ConfigReader.UnsupportedOperationException e) {
			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{"ERROR! Error in configuration file: " + configFile,
												 e.getMessage() + " at line " + e.getLine() + " position " +
														 e.getPosition(), "Line: " + e.getLineContent()});
		} catch (ConfigReader.ConfigException e) {
			TigaseRuntime.getTigaseRuntime()
					.shutdownTigase(new String[]{"ERROR! Error in configuration file: " + configFile,
												 "Issue with configuration file: " + e});
		}
	}

	public void loadConfiguration(String[] args) throws IOException, ConfigReader.ConfigException {
		List<String> settings = new LinkedList<>();
		ConfiguratorAbstract.parseArgs(props, settings, args);

		detectPathAndFormat();

		switch (format) {
			case dsl:
				loadFromDSLFiles();
				break;
			case properties:
				loadFromPropertiesFiles();
				Path initPropsFile = initPropertiesPath;
				Path initPropsFileOld = initPropsFile.resolveSibling(initPropertiesPath.getFileName() + ".old");
				props = ConfigWriter.buildTree(props);
				try {
					Files.deleteIfExists(initPropsFileOld);
					Files.move(initPropsFile, initPropsFileOld);
					saveToDSLFile(initPropsFile.toFile());
				} catch (IOException ex) {
					log.log(Level.SEVERE, "could not replace configuration file with file in DSL format", ex);
					throw new RuntimeException(ex);
				}
				log.log(Level.WARNING,
						"Configuration file {0} was a properties file and was converted to new DSL configuration format." +
								" Previous version of configuration file was saved at {1}",
						new Object[]{initPropsFile, initPropsFileOld});

				break;
		}
		try (Writer w = new StringWriter()) {
			new ConfigWriter().resolveVariables().write(w, props);
			log.log(Level.CONFIG, "Loaded configuration:\n" + w.toString());
		}
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

	protected Format detectPathAndFormat() {
		String property_filenames = (String) props.remove(PROPERTY_FILENAME_PROP_KEY);
		if (property_filenames == null) {
			property_filenames = PROPERTY_FILENAME_PROP_DEF;
			log.log(Level.WARNING, "No property file not specified! Using default one {0}", property_filenames);
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

			initPropertiesPath = Paths.get(prop_files[0]);
			for (String property_filename : prop_files) {
				try (BufferedReader reader = new BufferedReader(new FileReader(property_filename))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (line.startsWith("--user-db")) {
							break;
						}
						if (line.contains("{")) {
							if (line.contains("{clusterNode}"))
								continue;

							if (line.contains("{ call") && !(line.contains("\'{ call") || line.contains("\"{ call")))
								continue;
							
							format = dsl;
							return format;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		format = Format.properties;
		return format;
	}

	public Path getConfigFilePath() {
		return initPropertiesPath;
	}

	private void loadFromDSLFiles() throws IOException, ConfigReader.ConfigException {
		Map<String, Object> loaded = new ConfigReader().read(initPropertiesPath.toFile());
		props.putAll(loaded);
	}

	private void loadFromPropertiesFiles() {
		props.putAll(new PropertiesConfigReader().read(initPropertiesPath));
	}

	public static class PropertiesConfigReader {

		private final LinkedHashMap<String, Object> props;

		public PropertiesConfigReader() {
			props = new LinkedHashMap<>();
		}

		public Map read(Path path) {
			LinkedList<String> settings = new LinkedList<>();
			ConfiguratorAbstract.loadFromPropertiesFiles(path.toString(), props, settings);
			loadFromPropertyStrings(settings);
			convertFromOldFormat();
			return props;
		}

		protected Map<String, Object> loadFromPropertyStrings(List<String> settings) {
			for (String propString : settings) {
				int idx_eq = propString.indexOf('=');

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
			return props;
		}

		protected void convertFromOldFormat() {
			// converting old component configuration to new one
			Map<String, Object> toAdd = new HashMap<>();
			List<String> toRemove = new ArrayList<>();

			//List<String> dataSourceNames = new ArrayList<>();
			Map<String, Map<String, Object>> dataSources = new HashMap<>();

			Pattern commandsPattern = Pattern.compile("^([^\\/]+)\\/command\\/(.+)$");
			Pattern processorsPattern = Pattern.compile("^([^\\/]+)\\/processors\\/(.+)$");

			props.forEach((k, v) -> {
				if (k.equals("config-type")) {
					switch ((String) v) {
						case "--gen-config-all":
						case "--gen-config-default":
							toAdd.put(k, "default");
							break;
						case "--gen-config-sm":
							toAdd.put(k, "session-manager");
							break;
						case "--gen-config-cs":
							toAdd.put(k, "connection-managers");
							break;
						case "--gen-config-comp":
							toAdd.put(k, "component");
							break;
						default:
							break;
					}
				}
				if (k.startsWith("--comp-name")) {
					String suffix = k.replace("--comp-name-", "");
					String name = ((String) v).trim();
					String cls = ((String) props.get("--comp-class-" + suffix)).trim();
					String active = "true";
					toAdd.put(name + "/class", cls);
					toAdd.put(name + "/active", active);

					if ("tigase.http.HttpMessageReceiver".equals(cls)) {
						props.entrySet().stream().filter(e -> e.getKey().startsWith(name + "/http/")).forEach(e -> {
							String key = e.getKey().replace(name + "/http/", "");
							Map<String, Object> httpServerCfg = (Map<String, Object>) toAdd.computeIfAbsent(
									"httpServer", (k1) -> new HashMap<>());
							if (key.equals("server-class")) {
								httpServerCfg.put("class", e.getValue());
								toRemove.add(e.getKey());
							} else if (key.equals("ports")) {
								Map<String, Object> connections = (Map<String, Object>) httpServerCfg.computeIfAbsent(
										"connections", (k1) -> new HashMap<>());
								for (int port : ((int[]) e.getValue())) {
									connections.compute(String.valueOf(port), (k1, v1) -> {
										if (v1 == null) {
											v1 = new HashMap<>();
										}
										((Map) v1).put("active", true);
										return v1;
									});
								}
								if (!Arrays.stream((int[]) e.getValue()).filter(v1 -> v1 == 8080).findAny().isPresent()) {
									connections.compute(String.valueOf(8080), (k1, v1) -> {
										if (v1 == null) {
											v1 = new HashMap<>();
										}
										((Map) v1).put("active", true);
										return v1;
									});
								}
								toRemove.add(e.getKey());
							} else if (key.endsWith("/socket") || key.endsWith("/domain")) {
								Map<String, Object> connections = (Map<String, Object>) httpServerCfg.computeIfAbsent(
										"connections", (k1) -> new HashMap<>());
								String[] parts = key.split("/");
								String port = parts[0];
								Map<String, Object> portCfg = (Map<String, Object>) connections.computeIfAbsent(port, (k1) -> new HashMap<>());
								portCfg.put(parts[1], e.getValue());

								toRemove.add(e.getKey());
							} else {
								httpServerCfg.put(key, e.getValue());
								toRemove.add(e.getKey());
							}
						});
					}

				} if (k.startsWith("--user-db") || k.startsWith("--auth-db") || k.startsWith("--amp-repo")) {
					String domain = "default";
					if (k.endsWith("]")) {
						domain = k.substring(k.indexOf('[') + 1, k.length() - 1);
					}
					toRemove.add(k);

					Map<String, Object> ds = dataSources.computeIfAbsent(domain, key -> new HashMap<>());
					if (k.startsWith("--user-db-uri")) {
						ds.put("user-uri", (String) v);
					} else if (k.startsWith("--user-db")) {
						ds.put("user-type", (String) v);
					}
					if (k.startsWith("--auth-db-uri")) {
						ds.put("auth-uri", (String) v);
					} else if (k.startsWith("--auth-db")) {
						ds.put("auth-type", (String) v);
					}
					if (k.startsWith("--amp-repo-uri")) {
						ds.put("amp-uri", (String) v);
					} else if (k.startsWith("--amp-repo-class")) {
						ds.put("amp-type", (String) v);
					}
				}
				if (k.equals("--sm-cluster-strategy-class")) {
					toAdd.put("sess-man/strategy/class", v.toString());
					toAdd.put("sess-man/strategy/active", "true");
					toRemove.add(k);
				}
				if (k.contains("pubsub-repo-url")) {
					props.put("dataSource/pubsub/uri", v);
					props.put("dataSource/pubsub/active", "true");
					props.put("pubsub/dao/default/data-source", "pubsub");
					toRemove.add(k);
				}
				if (k.startsWith("sess-man/plugins-conf/")) {
					if (k.equals("sess-man/plugins-conf/dynamic-roster-classes")) {
						toAdd.put("sess-man/dynamic-rosters/active", "true");
						for (String clazzName : v.toString().split(",")) {
							String[] parts = clazzName.split("\\.");
							toAdd.put("sess-man/dynamic-rosters/" + parts[parts.length - 1] + "/active", "true");
							toAdd.put("sess-man/dynamic-rosters/" + parts[parts.length - 1] + "/class", clazzName);
						}
						toRemove.add(k);
					} else {
						toRemove.add(k);
						toAdd.put(k.replace("/plugins-conf/", "/"), v);
					}
				}
				if (k.startsWith("stats/stats-archiv/")) {
					toRemove.add(k);
					toAdd.put(k.replace("stats/stats-archiv/", "stats/"), v);
				}
				if (k.contains("/command/")) {
					Matcher m = commandsPattern.matcher(k);
					if (m.matches()) {
						String cmp = m.group(1);
						String cmdId = m.group(2).replace("\\:", ":");
						Map<String, Object> acls = (Map<String, Object>) toAdd.computeIfAbsent(cmp + "/commands",
																							   (k1) -> new HashMap<>());
						List<String> values = Arrays.stream(v.toString().split(",")).map(x -> {
							int idx = x.indexOf(':');
							if (idx > 0) {
								return x.substring(idx + 1);
							}
							return x;
						}).collect(Collectors.toList());
						acls.put(cmdId, values.size() > 1 ? values : values.get(0));
						toRemove.add(k);
					}
				}
				if (k.equals("--" + SessionManagerConfig.SM_THREADS_POOL_PROP_KEY)) {
					props.put("sess-man/" + SessionManagerConfig.SM_THREADS_POOL_PROP_KEY, v);
				}
				if (k.endsWith("/processors")) {
					String cmp = k.replace("/processors", "");
					Arrays.stream((String[]) v).forEach(procId -> {
						toAdd.compute(cmp, (k1, v1) -> {
							if (v1 == null) {
								v1 = new HashMap<>();
							}
							((Map<String, Object>) v1).compute(procId, (k2, v2) -> {
								if (v2 == null) {
									v2 = new HashMap<>();
								}
								((Map) v2).put("active", true);
								return v2;
							});
							return v1;
						});
						toAdd.put(cmp + "/" + procId + "/active", true);
					});
					toRemove.add(k);
				}
				if (k.contains("/processors/")) {
					Matcher m = processorsPattern.matcher(k);
					if (m.matches()) {
						String cmp = m.group(1);
						String key = m.group(2).replace("\\:", ":");
						String[] parts = key.split("/");
						Map<String, Object> cmpCfg = (Map<String, Object>) toAdd.computeIfAbsent(cmp,
																								 (k1) -> new HashMap<>());
						Map<String, Object> procCfg = (Map<String, Object>) cmpCfg.computeIfAbsent(parts[0],
																								   (k1) -> new HashMap<>());
						procCfg.put(parts[1], v);
					}
					toRemove.add(k);
				}
				if (k.startsWith("basic-conf/logging/")) {
					String t = k.replace("basic-conf/logging/","");
					if (t.contains(".")) {
						int idx = t.lastIndexOf('.');
						if (idx > 0) {
							String group = t.substring(0, idx);
							String key = t.substring(idx+1);
							Map<String, Object> logging = (Map<String, Object>) toAdd.computeIfAbsent("logging", (k1) -> new HashMap<>());
							Map<String, Object> handler = (Map<String, Object>) logging.computeIfAbsent(group, (k1) -> new HashMap<>());
							handler.put(key, v);
							toRemove.add(k);
						}
					}
				}
				if (k.startsWith("basic-conf/auth-repo-params/")) {
					String[] tmp = k.replace("basic-conf/auth-repo-params/", "").split("/");
					String domain = "default";
					String prop = null;
					if (tmp.length == 1) {
						prop = tmp[0];
					} else {
						domain = tmp[0];
						prop = tmp[1];
					}
					Map<String, Object> ds = dataSources.computeIfAbsent(domain, key -> new HashMap<>());
					Map<String, Object> authParams = (Map<String, Object>) ds.computeIfAbsent("auth-params", (x) -> new HashMap<String, Object>());
					authParams.put(prop, v.toString());
					toRemove.add(k);
				}
			});

//			List<String> userDbDomains = new ArrayList<>();
//			List<String> authDbDomains = new ArrayList<>();
			dataSources.forEach((domain, cfg) -> {
				String userType = (String) cfg.get("user-type");
				String userUri = (String) cfg.get("user-uri");
				String authType = (String) cfg.get("auth-type");
				String authUri = (String) cfg.get("auth-uri");
				String ampUri = (String) cfg.get("amp-uri");
				String ampType = (String) cfg.get("amp-type");
				Map<String, Object> authParams = (Map<String, Object>) cfg.get("auth-params");

				if (userUri != null) {
					props.put("dataSource/" + domain + "/uri", userUri);
					props.put("dataSource/" + domain + "/active", "true");
					props.put("userRepository/" + domain + "/active", "true");
					props.put("authRepository/" + domain + "/active", "true");
				}

				if ((authUri != null) && (userUri == null || !userUri.contains(authUri))) {
					props.put("dataSource/" + domain + "-auth/uri", authUri);
					props.put("dataSource/" + domain + "-auth/active", "true");
					props.put("authRepository/" + domain + "/data-source", domain + "-auth");
					props.put("authRepository/" + domain + "/active", "true");
				}

				if (ampUri != null && (userUri == null || !userUri.equals(ampUri))) {
					props.put("dataSource/" + domain + "-amp/uri", authUri);
					props.put("dataSource/" + domain + "-amp/active", "true");
					props.put("msgRepository/" + domain + "/data-source", domain + "-amp");
					props.put("msgRepository/" + domain + "/active", "true");
				}

				if (userType != null && !userType.equals("mysql") && !userType.equals("pgsql") &&
						!userType.equals("derby") && !userType.equals("sqlserver")) {
					String cls = RepositoryFactory.getRepoClass(userType);
					props.put("userRepository/" + domain + "/cls", cls);
				}
				if (authType != null && !authType.equals("tigase-custom-auth")) {
					String cls = RepositoryFactory.getRepoClass(authType);
					props.put("authRepository/" + domain + "/cls", cls);
				}
				if (ampType != null) {
					props.put("msgRepository/" + domain + "/cls", ampType);
				}
				if (authParams != null) {
					authParams.forEach((k,v) -> {
						props.put("authRepository/" + domain + "/" + k, v);
					});
				}
			});

			String external = (String) props.remove("--external");
			if (external != null) {
				props.forEach((k, v) -> {
					if (k.endsWith("/class") && v.equals(ComponentProtocol.class.getCanonicalName())) {
						toAdd.put(k.replace("/class", "/repository/items"), v);
					}
				});
			}

			String admins = (String) props.remove("--admins");
			if (admins != null) {
				props.put("admins", admins.split(","));
			}

			String trusted = (String) props.remove("--trusted");
			if (trusted != null) {
				props.put("trusted", trusted.split(","));
			}

			String maxQueueSize = (String)props.remove("--max-queue-size");
			if (maxQueueSize != null) {
				props.put("max-queue-size", Integer.valueOf(maxQueueSize));
			}

			String monitoring = (String) props.remove("--monitoring");
			if (monitoring != null) {
				props.put("monitoring/active", "true");
				String[] entries = monitoring.split(",");
				for (String e : entries) {
					String[] tmp = e.split(":");
					if (tmp.length == 2) {
						props.put("monitoring/" + tmp[0] + "/active", "true");
						props.put("monitoring/" + tmp[0] + "/port", tmp[1]);
					}
				}
			}

			String statsArchiv = (String) props.remove("--stats-archiv");
			if (statsArchiv != null) {
				Arrays.stream(statsArchiv.split(",")).forEach(archiver -> {
					String[] parts = archiver.split(":");
					String k = "stats/" + parts[1];
					props.put(k + "/class", parts[0]);
					props.put(k + "/active", "true");
					if (parts.length > 2) {
						props.put(k + "/frequency", parts[2]);
					}
					if ("tigase.mongo.stats.CounterDataLoggerMongo".equals(parts[1]) ||
							"tigase.stats.CounterDataLogger".equals(parts[1])) {
						props.putIfAbsent(k + "/db-url", props.get("dataSource/default/uri"));
					}
				});
			}
			String statsHistory = (String) props.remove("--stats-history");
			if (statsHistory != null) {
				String[] parts = statsHistory.split(",");
				props.putIfAbsent("stats/stats-history-size", parts[0]);
				if (parts.length > 1) {
					props.putIfAbsent("stats/stats-update-interval", parts[1]);
				}
			}

			Iterator<Map.Entry<String, Object>> it = props.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Object> e = it.next();
				if (e.getKey().startsWith("--comp-")) {
					it.remove();
				}
			}

			for (String k : toRemove) {
				props.remove(k);
			}

			props.putAll(toAdd);

			// converting list of sess-man processors from --sm-plugins to sess-man/beans
			// and converting concurrency settings as well
			String plugins = (String) props.remove(GEN_SM_PLUGINS);
			if (plugins != null) {
				Set<XMPPProcessor> knownProcessors = ClassUtilBean.getInstance()
						.getAllClasses()
						.stream()
						.filter(cls -> XMPPProcessor.class.isAssignableFrom(cls) &&
								!(Modifier.isAbstract(cls.getModifiers()) || Modifier.isInterface(cls.getModifiers())))
						.map(cls -> {
							try {
								return (XMPPProcessor) cls.newInstance();
							} catch (InstantiationException | IllegalAccessException e) {
								log.log(Level.WARNING, "Failed to instanticate");
								return null;
							}
						})
						.collect(Collectors.toSet());

				Map<String, String> plugins_concurrency = new HashMap<>();
				String[] parts = plugins.split(",");
				for (String part : parts) {
					String[] tmp = part.split("=");
					final String name = (tmp[0].charAt(0) == '+' || tmp[0].charAt(0) == '-')
										? tmp[0].substring(1)
										: tmp[0];
					boolean active = !tmp[0].startsWith("-");

					props.put("sess-man/" + name + "/active", String.valueOf(active));

					if (tmp.length > 1) {
						plugins_concurrency.put(name, tmp[1]);
					}

					try {

						XMPPImplIfc proc = knownProcessors.stream()
								.filter(p -> p != null && name.equals(p.id()))
								.findFirst()
								.orElse(null);

						if (proc != null) {
							Bean ann = proc.getClass().getAnnotation(Bean.class);
							if (ann == null) {
								props.put("sess-man/" + name + "/class", proc.getClass().getCanonicalName());
							}
						} else {
							log.log(Level.WARNING, "could not find class for processor " + name);
						}
					} catch (Exception ex) {
						log.log(Level.WARNING, "not able to get instance of processor " + name, ex);
					}
				}

				String concurrency = (String) props.get(SessionManagerConfig.PLUGINS_CONCURRENCY_PROP_KEY);
				if (concurrency != null) {
					for (String part : concurrency.split(",")) {
						String[] tmp = part.split("=");
						plugins_concurrency.put(tmp[0], tmp[1]);
					}
				}

				for (Map.Entry<String, String> e : plugins_concurrency.entrySet()) {
					String prefix = "sess-man/" + e.getKey() + "/";
					String[] tmp = e.getValue().split(":");
					try {
						props.put(prefix + "threadsNo", Integer.parseInt(tmp[0]));
					} catch (Exception ex) {
						log.log(Level.WARNING, "Plugin " + e.getKey() + " concurrency parsing error for: " + tmp[0],
								ex);
					}
					if (tmp.length > 1) {
						try {
							props.put(prefix + "queueSize", Integer.parseInt(tmp[1]));
						} catch (Exception ex) {
							log.log(Level.WARNING, "Plugin " + e.getKey() + " queueSize parsing error for: " + tmp[1],
									ex);
						}
					}
				}
			}

		}
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
