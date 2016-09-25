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
import tigase.server.xmppsession.SessionManagerConfig;
import tigase.util.ClassUtilBean;
import tigase.util.DataTypes;
import tigase.xmpp.XMPPImplIfc;
import tigase.xmpp.XMPPProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

	public void loadConfiguration(String[] args) {
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


				break;
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

	protected void detectPathAndFormat() {
		String property_filenames = (String) props.remove(PROPERTY_FILENAME_PROP_KEY);
		if (property_filenames == null) {
			property_filenames = PROPERTY_FILENAME_PROP_DEF;
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

			initPropertiesPath = Paths.get(prop_files[0]);
			for (String property_filename : prop_files) {
				try (BufferedReader reader = new BufferedReader(new FileReader(property_filename))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (line.contains("{") && !line.contains("{clusterNode}")) {
							format = Format.dsl;
							return;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		format = Format.properties;
	}

	private void loadFromDSLFiles() {
		try {
			Map<String, Object> loaded = new ConfigReader().read(initPropertiesPath.toFile());
			props.putAll(loaded);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load configuration from file " + initPropertiesPath, e);
		}
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

		private void loadFromPropertyStrings(List<String> settings) {
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
		}

		private void convertFromOldFormat() {
			// converting old component configuration to new one
			Map<String, Object> toAdd = new HashMap<>();
			List<String> toRemove = new ArrayList<>();

			List<String> dataSourceNames = new ArrayList<>();
			Map<String, Map<String,String>> dataSources = new HashMap<>();

			props.forEach((k,v) -> {
				if (k.startsWith("--comp-name")) {
					String suffix = k.replace("--comp-name-", "");
					String name = ((String) v).trim();
					String cls = ((String) props.get("--comp-class-" + suffix)).trim();
					String active = "true";
					toAdd.put(name + "/class", cls);
					toAdd.put(name + "/active", active);
				}
				if (k.endsWith("/processors")) {
					toAdd.put(k.replace("/processors", "/beans"), v);
					toRemove.add(k);
				}
				if (k.startsWith("--user-db") || k.startsWith("--auth-db")) {
					String domain = "default";
					if (k.endsWith("]")) {
						domain = k.substring(k.indexOf('[') + 1, k.length() - 1);
					}
					toRemove.add(k);

					Map<String,String> ds = dataSources.computeIfAbsent(domain, key -> new HashMap<>());
					if (k.startsWith("--user-db-uri"))  {
						ds.put("user-uri", (String) v);
					} else if (k.startsWith("--user-db")) {
						ds.put("user-type", (String) v);
					}
					if (k.startsWith("--auth-db-uri"))  {
						ds.put("auth-uri", (String) v);
					} else if (k.startsWith("--auth-db")) {
						ds.put("auth-type", (String) v);
					}
				}
				if (k.contains("pubsub-repo-url")) {
					props.put("dataSource/pubsub/uri", v);
					dataSourceNames.add("pubsub");
					toRemove.add(k);
				}
				if (k.startsWith("http/port/")) {
					toRemove.add(k);
					toAdd.put(k.replace("http/port/", "httpServer/connections/"), v);
				}
				if (k.startsWith("sess-man/plugins-conf/")) {
					toRemove.add(k);
					toAdd.put(k.replace("/plugins-conf/", "/"), v);
				}
			});

			List<String> userDbDomains = new ArrayList<>();
			List<String> authDbDomains = new ArrayList<>();
			dataSources.forEach((domain, cfg) -> {
				String userType = cfg.get("user-type");
				String userUri = cfg.get("user-uri");
				String authType = cfg.get("auth-type");
				String authUri = cfg.get("auth-uri");

				if (userUri != null) {
					if (!domain.equals("default")) {
						userDbDomains.add(domain);
						authDbDomains.add(domain);
						dataSourceNames.add(domain);
					}
					props.put("dataSource/" + domain + "/uri", userUri);
				}

				if ((authUri != null) && (userUri == null || !userUri.contains(authUri))) {
					if (!authDbDomains.contains(domain))
						authDbDomains.add(domain);
					dataSourceNames.add(domain + "-auth");
					props.put("dataSource/" + domain + "-auth/uri", authUri);
					props.put("authRepository/" + domain + "/uri", "dataSource:" + domain + "-auth");
				}

				if (userType != null && !userType.equals("mysql") && !userType.equals("pgsql") && !userType.equals("derby")
						&& !userType.equals("sqlserver")) {
					String cls = RepositoryFactory.getRepoClass(userType);
					props.put("userRepository/" + domain + "/cls", cls);
				}
				if (authType != null && !authType.equals("tigase-custom-auth")) {
					String cls = RepositoryFactory.getRepoClass(userType);
					props.put("authRepository/" + domain + "/cls", cls);
				}
			});

			if (!dataSourceNames.isEmpty()) {
				props.put("dataSource/domains", dataSourceNames);
			}
			if (!userDbDomains.isEmpty()) {
				props.put("userRepository/domains", userDbDomains);
			}
			if (!authDbDomains.isEmpty()) {
				props.put("authRepository/domains", authDbDomains);
			}

			String external = (String) props.remove("--external");
			if (external != null) {
				props.forEach((k,v) -> {
					if (k.endsWith("/class") && v.equals(ComponentProtocol.class.getCanonicalName())) {
						toAdd.put(k.replace("/class", "/repository/items"), v);
					}
				});
			}

			String admins = (String) props.remove("--admins");
			if (admins != null) {
				props.put("admins", admins.split(","));
			}

			Iterator<Map.Entry<String, Object>> it = props.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Object> e = it.next();
				if (e.getKey().startsWith("--comp-"))
					it.remove();
			}

			for (String k : toRemove) {
				props.remove(k);
			}

			props.putAll(toAdd);

			// converting list of sess-man processors from --sm-plugins to sess-man/beans
			// and converting concurrency settings as well
			String plugins = (String) props.remove(GEN_SM_PLUGINS);
			if (plugins != null) {
				Set<XMPPProcessor> knownProcessors = ClassUtilBean.getInstance().getAllClasses().stream()
						.filter(cls -> XMPPProcessor.class.isAssignableFrom(cls) && !(Modifier.isAbstract(cls.getModifiers()) || Modifier.isInterface(cls.getModifiers())))
						.map(cls -> {
							try {
								return (XMPPProcessor) cls.newInstance();
							} catch (InstantiationException|IllegalAccessException e) {
								log.log(Level.WARNING, "Failed to instanticate");
								return null;
							}
						})
						.collect(Collectors.toSet());

				StringBuilder smBeans = new StringBuilder();
				Map<String,String> plugins_concurrency = new HashMap<>();
				String[] parts = plugins.split(",");
				for (String part : parts) {
					String[] tmp = part.split("=");
					final String name = (tmp[0].charAt(0) == '+' || tmp[0].charAt(0) == '-') ? tmp[0].substring(1) : tmp[0];

					if (tmp.length > 1) {
						plugins_concurrency.put(name, tmp[1]);
					}
					if (smBeans.length() != 0)
						smBeans.append(",");
					smBeans.append(tmp[0]);

					try {

						XMPPImplIfc proc = knownProcessors.stream().filter(p -> p != null && name.equals(p.id())).findFirst().orElse(null);

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
	}
}
