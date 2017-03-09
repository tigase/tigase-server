/*
 * PropertiesBeanConfiguratorWithBackwardCompatibility.java
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
package tigase.component;

import tigase.conf.Configurable;
import tigase.conf.ConfigurationException;
import tigase.db.comp.ComponentRepository;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.Kernel;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.component.BackwardCompatibilityHelper.getDefConfigParams;
import static tigase.conf.Configurable.GEN_CONFIG_DEF;

/**
 * Created by andrzej on 30.05.2016.
 */
public class PropertiesBeanConfiguratorWithBackwardCompatibility extends PropertiesBeanConfigurator {

	private static final Logger log = Logger.getLogger(PropertiesBeanConfiguratorWithBackwardCompatibility.class.getCanonicalName());

	@Override
	public void configure(BeanConfig beanConfig, Object bean, Map<String, Object> values) {
		// execute bean based configuration
		super.configure(beanConfig, bean, values);

		try {
			if (bean instanceof Configurable) {
				Configurable conf = (Configurable) bean;
				Method getDefaultsMethod = bean.getClass().getMethod("getDefaults", Map.class);
				Method setPropertiesMethod = bean.getClass().getMethod("setProperties", Map.class);

				if (getDefaultsMethod != null && getDefaultsMethod.getAnnotation(Deprecated.class) == null) {
					log.log(Level.WARNING, "Class {0} is using deprecated configuration using methods getDefaults() and setProperties()", bean.getClass().getCanonicalName());
				}

				if (setPropertiesMethod != null && setPropertiesMethod.getAnnotation(Deprecated.class) == null) {
					String configType = (String) getProperties().getOrDefault("config-type", GEN_CONFIG_DEF);
					String dbUri = (String) this.getProperties().get("dataSource/repo-uri");
					Map<String, Object> params = getDefConfigParams(getKernel(), configType, dbUri, getProperties());
					Map<String, Object> props = new HashMap<>(params);

					props.putAll( conf.getDefaults(params) );
					fillProps(beanConfig, props);
					((Configurable) bean).setProperties(props);
				}
			}
			if (bean instanceof ComponentRepository) {
				ComponentRepository conf = (ComponentRepository) bean;
				Method getDefaultsMethod = bean.getClass().getMethod("getDefaults", Map.class, Map.class);
				Method setPropertiesMethod = bean.getClass().getMethod("setProperties", Map.class);

				if (getDefaultsMethod != null && getDefaultsMethod.getAnnotation(Deprecated.class) == null) {
					log.log(Level.WARNING, "Class {0} is using deprecated configuration using methods getDefaults() and setProperties()", bean.getClass().getCanonicalName());
				}
				if (setPropertiesMethod != null && setPropertiesMethod.getAnnotation(Deprecated.class) == null) {
					String configType = (String) getProperties().getOrDefault("config-type", GEN_CONFIG_DEF);
					String dbUri = (String) this.getProperties().get("dataSource/repo-uri");
					Map<String, Object> params = getDefConfigParams(getKernel(), configType, dbUri, getProperties());
					Map<String, Object> props = new HashMap<>();

					conf.getDefaults(props, params);
					fillProps(beanConfig, props);
					((ComponentRepository) bean).setProperties(props);
				}
			}
		} catch (NoSuchMethodException ex) {
			// method getDefaults() not found - this should not happen
		} catch (ConfigurationException ex) {
			throw new RuntimeException("Could not configure bean " + beanConfig.getBeanName(), ex);
		}
	}

	public Map<String, Object> getBeanConfigurationProperties(BeanConfig beanConfig) {
		Map<String, Object> result = new HashMap<>();
		Map<String, Object> props = getProperties();

		if (props != null) {
			StringBuilder sb = new StringBuilder();
			ArrayDeque<Kernel> kernels = new ArrayDeque<>();
			Kernel kernel = beanConfig.getKernel();
			while (kernel.getParent() != null && kernel != this.kernel) {
				kernels.push(kernel);
				kernel = kernel.getParent();
			}
			while ((kernel = kernels.poll()) != null) {
				if (sb.length() > 0)
					sb.append("/");
				sb.append(kernel.getName());
			}

			if (!beanConfig.getBeanName().equals(beanConfig.getKernel().getName())) {
				if (sb.length() > 0)
					sb.append("/");
				sb.append(beanConfig.getBeanName());
			}

			String prefix = sb.toString();
			for (Map.Entry<String, Object> e : props.entrySet()) {
				if (e.getKey().startsWith(prefix + "/")) {
					String key = e.getKey().substring(prefix.length() + 1);
					result.put(key, e.getValue());
				}
			}
		}

		result.put("name", beanConfig.getBeanName());

		return result;
	}

	private void fillProps(BeanConfig beanConfig, Map<String, Object> result) {
		Map<String, Object> props = getBeanConfigurationProperties(beanConfig);
		result.putAll(BackwardCompatibilityHelper.fillProps(props));
	}

}
