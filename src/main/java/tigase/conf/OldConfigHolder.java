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
package tigase.conf;

import tigase.db.RepositoryFactory;
import tigase.kernel.beans.Bean;
import tigase.server.ext.AbstractCompDBRepository;
import tigase.server.ext.ComponentProtocol;
import tigase.server.xmppsession.SessionManagerConfig;
import tigase.util.reflection.ClassUtilBean;
import tigase.util.repository.DataTypes;
import tigase.xmpp.XMPPImplIfc;
import tigase.xmpp.XMPPProcessor;

import java.io.*;
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
import java.util.stream.Stream;

import static tigase.conf.ConfigHolder.renameIfExists;
import static tigase.conf.Configurable.GEN_SM_PLUGINS;
import static tigase.conf.Configurable.GEN_TEST;
import static tigase.conf.ConfiguratorAbstract.PROPERTY_FILENAME_PROP_DEF;
import static tigase.conf.ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY;

/**
 * Created by andrzej on 10.07.2017.
 */
public class OldConfigHolder {

	private static final Logger log = Logger.getLogger(OldConfigHolder.class.getCanonicalName());

	public enum Format {
		dsl,
		properties
	}
	private List<String> output = new ArrayList<>();
	private Path[] propertyFileNames = null;
	private Map<String, Object> props = new LinkedHashMap<>();

	public Optional<String[]> getOutput() {
		if (output.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(output.toArray(new String[output.size()]));
	}

	public void convert(String[] args, Path tdslPath) throws IOException, ConfigReader.ConfigException {
		List<String> settings = new LinkedList<>();
		ConfiguratorAbstract.parseArgs(props, settings, args);
		props.remove(GEN_TEST);

		if (detectPathAndFormat() == Format.dsl) {
			Optional<Path> backup = Optional.empty();
			if (Files.exists(tdslPath)) {
				backup = Optional.ofNullable(ConfigHolder.backupOldConfigFile(tdslPath));
			}
			Files.move(propertyFileNames[0], tdslPath);
			logOutput("Configuration file {0} was updated to match current configuration format and renamed to {1}.",
					  propertyFileNames[0], tdslPath);
			backup.ifPresent(
					backupPath -> logOutput("Previous version of a configuration file was saved as {0}", backupPath));
			return;
		}

		if (propertyFileNames.length == 0) {
			return;
		}

		loadFromPropertyFiles();
		convertFromOldFormat();

		props = ConfigWriter.buildTree(props);
		props.remove("--property-file");
		props.remove("--config-file");

		if (Files.exists(tdslPath)) {
			Path backup = ConfigHolder.backupOldConfigFile(tdslPath);
			logOutput("Existing DSL config file {0} was renamed to {1} as conversion from property files was started.",
					  tdslPath, backup);
		}

		new ConfigWriter().write(tdslPath.toFile(), props);
		logOutput("Configuration files {0} were updated to DSL configuration format and saved as {1}.",
				  Arrays.stream(propertyFileNames).map(path -> path.toString()).collect(Collectors.joining()),
				  tdslPath);

		for (Path path : propertyFileNames) {
			Path backup = ConfigHolder.backupOldConfigFile(path);
			logOutput("Old configuration file {0} was renamed to {1}.", path, backup);
		}
	}

	protected Map<String, Object> getProperties() {
		return props;
	}

	protected Format detectPathAndFormat() {
		String property_filenames = (String) props.remove(PROPERTY_FILENAME_PROP_KEY);
		if (property_filenames == null) {
			property_filenames = PROPERTY_FILENAME_PROP_DEF;
			log.log(Level.FINEST, "No property file not specified! Using default one {0}", property_filenames);
		}

		if (property_filenames != null) {
			String[] prop_filenames = property_filenames.split(",");

			if (prop_filenames.length == 1) {
				File f = new File(prop_filenames[0]);
				if (!f.exists()) {
					log.log(Level.FINEST, "Provided property file {0} does NOT EXISTS! Trying to use default one {1}",
							new String[]{f.getAbsolutePath(), PROPERTY_FILENAME_PROP_DEF});
					prop_filenames[0] = PROPERTY_FILENAME_PROP_DEF;
				}
			}

			propertyFileNames = Arrays.stream(prop_filenames)
					.map(filename -> Paths.get(filename))
					.filter(path -> Files.exists(path))
					.toArray(x -> new Path[x]);

			for (Path path : propertyFileNames) {
				File file = path.toFile();
				try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (line.startsWith("--user-db")) {
							break;
						}
						if (line.contains("{")) {
							if (line.contains("{clusterNode}")) {
								continue;
							}

							if (line.contains("{ call") && !(line.contains("\'{ call") || line.contains("\"{ call"))) {
								continue;
							}

							return Format.dsl;
						}
					}
				} catch (IOException e) {
					log.log(Level.FINEST, "Error detecting paths script", e);
				}
			}
		}
		return Format.properties;
	}

	protected Map<String, Object> loadFromPropertyStrings(List<String> settings) {
		for (String propString : settings) {
			int idx_eq = propString.indexOf('=');

			// String key = prop[0].trim();
			// Object val = prop[1];
			String key = propString.substring(0, idx_eq).trim();
			String valStr = propString.substring(idx_eq + 1);
			if (valStr != null) {
				valStr = valStr.trim();
			}

			Object val = valStr;
			if (key.matches(".*\\[[LISBlisb]\\]$")) {
				char c = key.charAt(key.length() - 2);

				key = key.substring(0, key.length() - 3);
				// decoding value for basckward compatibility
				if (val != null) {
					val = DataTypes.decodeValueType(c, valStr);
				}
			}

			props.put(key, val);
		}
		return props;
	}

	protected void convertFromOldFormat() throws ConfigReader.ConfigException {
		// converting old component configuration to new one
		Map<String, Object> toAdd = new HashMap<>();
		List<String> toRemove = new ArrayList<>();

		//List<String> dataSourceNames = new ArrayList<>();
		Map<String, Map<String, Object>> dataSources = new HashMap<>();

		Pattern commandsPattern = Pattern.compile("^([^\\/]+)\\/command\\/(.+)$");
		Pattern processorsPattern = Pattern.compile("^([^\\/]+)\\/processors\\/(.+)$");

		if (props.containsKey(RepositoryFactory.AUTH_DOMAIN_POOL_CLASS)) {
			throw new ConfigReader.ConfigException(
					"Cannot convert property " + RepositoryFactory.AUTH_DOMAIN_POOL_CLASS +
							"!\nPlease check if provided class is compatible with Tigase XMPP Server data sources. If so, then please remove this property, convert the configuration file and then manually modify configuration.");
		}

		if (props.containsKey(RepositoryFactory.USER_DOMAIN_POOL_CLASS)) {
			throw new ConfigReader.ConfigException(
					"Cannot convert property " + RepositoryFactory.USER_DOMAIN_POOL_CLASS +
							"!\nPlease check if provided class is compatible with Tigase XMPP Server data sources. If so, then please remove this property, convert the configuration file and then manually modify configuration.");
		}

		Object defDataPoolSize = props.remove(RepositoryFactory.DATA_REPO_POOL_SIZE);
		Object defAuthRepoPool = props.remove(RepositoryFactory.AUTH_REPO_POOL_SIZE);

		props.forEach((k, v) -> {
			if (k.equals("config-type")) {
				switch ((String) v) {
					case "--gen-config-all":
					case "--gen-config-def":
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
						Map<String, Object> httpServerCfg = (Map<String, Object>) toAdd.computeIfAbsent("httpServer",
																										(k1) -> new HashMap<>());
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
							Map<String, Object> portCfg = (Map<String, Object>) connections.computeIfAbsent(port,
																											(k1) -> new HashMap<>());
							portCfg.put(parts[1], e.getValue());

							toRemove.add(e.getKey());
						} else {
							httpServerCfg.put(key, e.getValue());
							toRemove.add(e.getKey());
						}
					});
				}

			}
			if (k.startsWith("--user-db") || k.startsWith("--auth-db") || k.startsWith("--amp-repo")) {
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
					stringToStreamOfStrings(v.toString()).forEach(clazzName -> {
						String[] parts = clazzName.split("\\.");
						toAdd.put("sess-man/dynamic-rosters/" + parts[parts.length - 1] + "/active", "true");
						toAdd.put("sess-man/dynamic-rosters/" + parts[parts.length - 1] + "/class", clazzName);
					});
					toRemove.add(k);
				} else {
					toRemove.add(k);
					k = k.replace("/plugins-conf/", "/");
					if (k.endsWith("/amp/msg-offline")) {
						toAdd.put(k.replace("/msg-offline", "/msgoffline/active"), v);
					} else if (k.endsWith("/presence-state/disable-roster-lazy-loading")) {
						k = k.replace("/disable-roster-lazy-loading", "/enable-roster-lazy-loading");
						if ("true".equals(v)) {
							toAdd.put(k, false);
						} else {
							toAdd.put(k, true);
						}
					} else if (k.equals("/presence-state/extended-presence-processors")) {
						String k1 = k.replace("presence-state/extended-presence-processors", "");
						final String prefix = k;
						stringToStreamOfStrings(v.toString()).forEach(ext -> {
							String[] parts = ext.split(".");
							toAdd.put(prefix + "/" + parts[parts.length - 1] + "/class", ext);
							toAdd.put(prefix + "/" + parts[parts.length - 1] + "/active", true);
						});
					} else {
						toAdd.put(k, v);
					}
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
					List<String> values = stringToStreamOfStrings(v.toString()).map(x -> {
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
				String t = k.replace("basic-conf/logging/", "");
				if (t.contains(".")) {
					int idx = t.lastIndexOf('.');
					if (idx > 0) {
						String group = t.substring(0, idx);
						String key = t.substring(idx + 1);
						Map<String, Object> logging = (Map<String, Object>) toAdd.computeIfAbsent("logging",
																								  (k1) -> new HashMap<>());
						Map<String, Object> handler = (Map<String, Object>) logging.computeIfAbsent(group,
																									(k1) -> new HashMap<>());
						handler.put(key, v);
						toRemove.add(k);
					}
				}
			}
			if (k.startsWith("basic-conf/user-repo-params/")) {
				String[] tmp = k.replace("basic-conf/user-repo-params/", "").split("/");
				String domain = "default";
				String prop = null;
				if (tmp.length == 1) {
					prop = tmp[0];
				} else {
					domain = tmp[0];
					prop = tmp[1];
				}
				Map<String, Object> ds = dataSources.computeIfAbsent(domain, key -> new HashMap<>());
				Map<String, Object> authParams = (Map<String, Object>) ds.computeIfAbsent("user-params",
																						  (x) -> new HashMap<String, Object>());
				authParams.put(prop, v.toString());
				toRemove.add(k);
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
				Map<String, Object> authParams = (Map<String, Object>) ds.computeIfAbsent("auth-params",
																						  (x) -> new HashMap<String, Object>());
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
			Map<String, Object> userParams = (Map<String, Object>) cfg.get("user-params");

			String sourcePrefix = "dataSource/" + domain + "/";
			String authPrefix = "authRepository/" + domain + "/";
			String userPrefix = "userRepository/" + domain + "/";

			if (userUri != null) {
				props.put("dataSource/" + domain + "/uri", userUri);
				props.put("dataSource/" + domain + "/active", "true");
				if (defDataPoolSize != null) {
					props.put(sourcePrefix + "pool-size", defDataPoolSize);
				}
				props.put("userRepository/" + domain + "/active", "true");
				props.put("authRepository/" + domain + "/active", "true");
			}

			if ((authUri != null) && (userUri == null || !userUri.contains(authUri))) {
				props.put("dataSource/" + domain + "-auth/uri", authUri);
				props.put("dataSource/" + domain + "-auth/active", "true");
				if (defDataPoolSize != null) {
					props.put("dataSource/" + domain + "-auth/pool-size", defDataPoolSize);
				}
				props.put("authRepository/" + domain + "/data-source", domain + "-auth");
				props.put("authRepository/" + domain + "/active", "true");
			}

			if (ampUri != null && (userUri == null || !userUri.equals(ampUri))) {
				props.put("dataSource/" + domain + "-amp/uri", ampUri);
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
				authParams.forEach((k, v) -> {
					if (k.equals(RepositoryFactory.AUTH_REPO_CLASS_PROP_KEY)) {
						props.put(authPrefix + "/cls", v);
					} else if (k.equals(RepositoryFactory.AUTH_REPO_POOL_CLASS_PROP_KEY)) {
						props.put(authPrefix + "/pool-class", v);
					} else if (k.equals(RepositoryFactory.AUTH_REPO_POOL_SIZE_PROP_KEY)) {
						props.put(authPrefix + "/pool-size", v);
					} else {
						props.put(authPrefix + k, v);
					}
				});
			}
			if (userParams != null) {
				userParams.forEach((k, v) -> {
					if (k.equals(RepositoryFactory.USER_REPO_CLASS_PROP_KEY)) {
						props.put(userPrefix + "/cls", v);
					} else if (k.equals(RepositoryFactory.USER_REPO_POOL_CLASS_PROP_KEY)) {
						props.put(userPrefix + "/pool-class", v);
					} else if (k.equals(RepositoryFactory.USER_REPO_POOL_SIZE_PROP_KEY)) {
						props.put(userPrefix + "/pool-size", v);
					} else {
						props.put(userPrefix + k, v);
					}
				});
			}
			if (defAuthRepoPool != null) {
				props.putIfAbsent(authPrefix + "/pool-size", defAuthRepoPool);
			}
		});

		String external = (String) props.remove("--external");
		if (external != null) {
			Optional<String> extCmpName = toAdd.entrySet()
					.stream()
					.filter(e -> e.getKey().endsWith("/class") &&
							e.getValue().equals(ComponentProtocol.class.getCanonicalName()))
					.map(e -> e.getKey().replace("/class", ""))
					.findFirst();

			extCmpName.ifPresent(cmpName -> {
				String[] components = external.split(",");
				saveOldExternalComponentConfigItems(components);
			});
			renameIfExists(props, ComponentProtocol.EXTCOMP_BIND_HOSTNAMES,
						   ComponentProtocol.EXTCOMP_BIND_HOSTNAMES_PROP_KEY,
						   value -> stringToListOfStrings(value.toString()));
		} else {
			props.remove(ComponentProtocol.EXTCOMP_BIND_HOSTNAMES);
		}

		String admins = (String) props.remove("--admins");
		if (admins != null) {
			props.put("admins", stringToListOfStrings(admins));
		}

		String trusted = (String) props.remove("--trusted");
		if (trusted != null) {
			props.put("trusted", stringToListOfStrings(trusted));
		}

		String maxQueueSize = (String) props.remove("--max-queue-size");
		if (maxQueueSize != null) {
			props.put("max-queue-size", Integer.valueOf(maxQueueSize.trim()));
		}

		String monitoring = (String) props.remove("--monitoring");
		if (monitoring != null) {
			props.put("monitoring/active", "true");
			stringToStreamOfStrings(monitoring).forEach(e -> {
				String[] tmp = e.split(":");
				if (tmp.length == 2) {
					props.put("monitoring/" + tmp[0] + "/active", "true");
					props.put("monitoring/" + tmp[0] + "/port", tmp[1]);
				}
			});
		}

		String statsArchiv = (String) props.remove("--stats-archiv");
		if (statsArchiv != null) {
			stringToStreamOfStrings(statsArchiv).forEach(archiver -> {
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
		if (!"connection-managers".equals(props.get("config-type")) && !"component".equals(props.get("config-type"))) {
			props.put("sess-man/active", "true");
		}
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
			stringToStreamOfStrings(plugins).forEach(part -> {
				String[] tmp = part.split("=");
				final String name = (tmp[0].charAt(0) == '+' || tmp[0].charAt(0) == '-') ? tmp[0].substring(1) : tmp[0];
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
			});

			String concurrency = (String) props.get(SessionManagerConfig.PLUGINS_CONCURRENCY_PROP_KEY);
			if (concurrency != null) {
				stringToStreamOfStrings(concurrency).forEach(part -> {
					String[] tmp = part.split("=");
					plugins_concurrency.put(tmp[0], tmp[1]);
				});
			}

			for (Map.Entry<String, String> e : plugins_concurrency.entrySet()) {
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

	public static void saveOldExternalComponentConfigItems(String[] components) {
		File f = new File(AbstractCompDBRepository.ITEMS_IMPORT_FILE);
		if (f.exists()) {
			f.delete();
		}
		try (FileWriter writer = new FileWriter(f)) {
			for (int i=0; i<components.length; i++) {
				if (i > 0) {
					writer.write('\n');
				}
				writer.write(components[i]);
			}
		} catch (IOException ex) {
			// ignoring...
		}
	}

	private void logOutput(String msg, Object... args) {
		if (log.isLoggable(Level.CONFIG)) {
			log.log(Level.CONFIG, msg, args);
		}
		if (args.length > 0) {
			output.add(java.text.MessageFormat.format(msg, args));
		} else {
			output.add(msg);
		}
	}

	private void loadFromPropertyFiles() throws ConfigReader.ConfigException {
		LinkedList<String> settings = new LinkedList<>();
		for (Path path : propertyFileNames) {
			ConfiguratorAbstract.loadFromPropertiesFiles(path.toString(), props, settings);
			loadFromPropertyStrings(settings);
		}
	}

	private Stream<String> stringToStreamOfStrings(String val) {
		return Arrays.stream(val.split(",")).map(String::trim);
	}

	private List<String> stringToListOfStrings(String val) {
		return stringToStreamOfStrings(val).collect(Collectors.toList());
	}
}
