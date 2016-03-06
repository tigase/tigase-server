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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

@Bean(name = BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)
public class PropertiesBeanConfigurator extends AbstractBeanConfigurator {


	private Map<String, Object> props;

	private HashSet<String> getBeanProps(String beanName) {
		HashSet<String> result = new HashSet<>();

		if (props != null) {
			for (String pn : props.keySet()) {
				if (pn.startsWith(beanName + "/")) {
					result.add(pn);
				}
			}
		}

		return result;
	}

	@Override
	protected Map<String, Object> getConfiguration(BeanConfig beanConfig) {
		final HashMap<String, Object> valuesToSet = new HashMap<>();

		// Preparing set of properties based on @ConfigField annotation and
		// aliases
		Field[] fields = DependencyManager.getAllFields(beanConfig.getClazz());
		for (Field field : fields) {
			final ConfigField cf = field.getAnnotation(ConfigField.class);
			if (cf != null && !cf.alias().isEmpty() && props.containsKey(cf.alias())) {
				final Object value = props.get(cf.alias());

				if (valuesToSet.containsKey(field)) {
					if (log.isLoggable(Level.CONFIG))
						log.config("Alias '" + cf.alias() + "' for property " + beanConfig.getBeanName() + "." + field.getName() + " will not be used, because there is configuration for this property already.");
					continue;
				}
				if (log.isLoggable(Level.CONFIG))
					log.config("Using alias '" + cf.alias() + "' for property " + beanConfig.getBeanName() + "." + field.getName());

				valuesToSet.put(field.getName(), value);
			}
		}
		// Preparing set of properties based on given properties set
		HashSet<String> beanProps = getBeanProps(beanConfig.getBeanName());
		for (String key : beanProps) {
			// TODO: split is a no-go, there can be more than just 1 '/' char - support for multilevel!
			String[] tmp = key.split("/");
			final String property = tmp[1];
			final Object value = props.get(key);

			valuesToSet.put(property, value);
		}


		return valuesToSet;
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
