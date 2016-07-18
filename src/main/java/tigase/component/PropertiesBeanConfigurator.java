/*
 * PropertiesBeanConfigurator.java
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

import tigase.kernel.BeanUtils;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyManager;
import tigase.kernel.core.Kernel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;

@Bean(name = BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)
public class PropertiesBeanConfigurator extends AbstractBeanConfigurator {


	private Map<String, Object> props;

	private HashMap<String, Object> getBeanProps(BeanConfig beanConfig) {
		HashMap<String, Object> result = new HashMap<>();

		// this map needs to be filled with name of fields or field aliases
		// which implements Map
		Set<String> mapFields = new HashSet<>();
		Set<String> aliases = new HashSet<>();
		Field[] fields = DependencyManager.getAllFields(beanConfig.getClazz());
		for (Field field : fields) {
			ConfigField cf = field.getAnnotation(ConfigField.class);
			if (cf != null) {
				if (Map.class.isAssignableFrom(field.getType())) {
					mapFields.add(field.getName());
				}
				if (!cf.alias().isEmpty()) {
					aliases.add(cf.alias());
					if (Map.class.isAssignableFrom(field.getType())) {
						mapFields.add(cf.alias());
					}
				}
			}
		}

		if (props != null) {
			List<String> path = new ArrayList<>();
			ArrayDeque<Kernel> kernels = new ArrayDeque<>();
			Kernel kernel = beanConfig.getKernel();
			while (kernel.getParent() != null && kernel != this.kernel) {
				kernels.push(kernel);
				kernel = kernel.getParent();
			}
			while((kernel = kernels.poll()) != null) {
				path.add(kernel.getName());
			}

			if (!beanConfig.getBeanName().equals(beanConfig.getKernel().getName())) {
				path.add(beanConfig.getBeanName());
			}

			for (int i=0; i<=path.size(); i++) {
				StringBuilder sb = new StringBuilder();
				for (int j=0; j<i; j++) {
					if (sb.length() != 0)
						sb.append("/");
					sb.append(path.get(j));
				}
				String prefix = sb.toString();
				for (Map.Entry<String, Object> e : props.entrySet()) {
					if (prefix.isEmpty() || e.getKey().startsWith(prefix + "/")) {
						String key = prefix.isEmpty() ? e.getKey() : e.getKey().substring(prefix.length() + 1);
						int idx = key.indexOf("/");
						// if there is next / char and there is field or alias implementing Map for prefix of this key
						// then we need to gather all key with this prefix and create map of values
						if (idx > 0) {
							String fname = key.substring(0, idx);
							if (mapFields.contains(fname)) {
								Map<String, Object> vals = (Map<String, Object>) result.get(fname);
								if (vals == null) {
									vals = new HashMap<>();
									result.put(fname, vals);
								}
								vals.put(key.substring(idx + 1), e.getValue());
								continue;
							}
						}
						if (i == path.size() || aliases.contains(key)) {
							result.put(key, e.getValue());
						}
					}
				}
			}
		}

		result.put("name", beanConfig.getBeanName());

		return result;
	}

	@Override
	protected Map<String, Object> getConfiguration(BeanConfig beanConfig) {
		final HashMap<String, Object> valuesToSet = new HashMap<>();

		resolveAliases(beanConfig, props, valuesToSet);

		// Preparing set of properties based on given properties set
		HashMap<String, Object> beanProps = getBeanProps(beanConfig);
		resolveAliases(beanConfig, beanProps, valuesToSet);
		for (Map.Entry<String, Object> e : beanProps.entrySet()) {
			final String property = e.getKey();
			final Object value = e.getValue();

			valuesToSet.put(property, value);
		}

		return valuesToSet;
	}

	protected void resolveAliases(BeanConfig beanConfig, Map<String, Object> props, Map<String, Object> valuesToSet) {
		// Preparing set of properties based on @ConfigField annotation and
		// aliases
		Field[] fields = DependencyManager.getAllFields(beanConfig.getClazz());
		for (Field field : fields) {
			final ConfigField cf = field.getAnnotation(ConfigField.class);
			if (cf != null && !cf.alias().isEmpty() && props.containsKey(cf.alias()) && (this.props != props || cf.allowAliasFromParent())) {
				final Object value = props.get(cf.alias());

				if (props.containsKey(field)) {
					if (log.isLoggable(Level.CONFIG))
						log.config("Alias '" + cf.alias() + "' for property " + beanConfig.getBeanName() + "." + field.getName() + " will not be used, because there is configuration for this property already.");
					continue;
				}
				if (log.isLoggable(Level.CONFIG))
					log.config("Using alias '" + cf.alias() + "' for property " + beanConfig.getBeanName() + "." + field.getName());

				valuesToSet.put(field.getName(), value);
			}
		}
	}

	public Map<String, Object> getCurrentConfigurations() {
		HashMap<String, Object> result = new HashMap<>();

		for (BeanConfig bc : kernel.getDependencyManager().getBeanConfigs()) {
			final Object bean = kernel.getInstance(bc.getBeanName());
			final Class<?> cl = bc.getClazz();
			java.lang.reflect.Field[] fields = DependencyManager.getAllFields(cl);
			for (java.lang.reflect.Field field : fields) {
				final ConfigField cf = field.getAnnotation(ConfigField.class);
				if (cf != null) {
					String key = bc.getBeanName() + "/" + field.getName();
					try {
						Object currentValue = BeanUtils.getValue(bean, field);

						result.put(key, currentValue);
					} catch (IllegalAccessException | InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return result;
	}

	public Map<String, Object> getProperties() {
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		this.props = props;
	}

}
