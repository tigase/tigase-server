/*
 * AbstractBeanConfigurator.java
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

package tigase.kernel.beans.config;

import tigase.kernel.BeanUtils;
import tigase.kernel.KernelException;
import tigase.kernel.TypesConverter;
import tigase.kernel.beans.Converter;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyManager;
import tigase.kernel.core.Kernel;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractBeanConfigurator implements BeanConfigurator {

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	private final ConcurrentHashMap<BeanConfig, HashMap<Field, Object>> defaultFieldValues = new ConcurrentHashMap<BeanConfig, HashMap<Field, Object>>();

	@Inject(bean = "kernel", nullAllowed = false)
	protected Kernel kernel;

	@Inject(bean = "defaultTypesConverter")
	protected TypesConverter defaultTypesConverter;

	private boolean accessToAllFields = false;

	public void configure(final BeanConfig beanConfig, final Object bean, final Map<String, Object> values) {
		if (values == null) return;
		if (log.isLoggable(Level.CONFIG))
			log.config("Configuring bean '" + beanConfig.getBeanName() + "'...");

		final HashMap<Field, Object> valuesToSet = new HashMap<>();

		for (Map.Entry<String, Object> entry : values.entrySet()) {
			final String property = entry.getKey();
			final Object value = entry.getValue();

			try {
				if (log.isLoggable(Level.FINEST))
					log.finest("Preparing property '" + property + "' of bean '" + beanConfig.getBeanName() + "'...");

				final Field field = BeanUtils.getField(beanConfig, property);
				if (field == null) {
					log.warning(
							"Field '" + property + "' does not exists in bean '" + beanConfig.getBeanName() + "'. Ignoring!");
					continue;
				}
				ConfigField cf = field.getAnnotation(ConfigField.class);
				if (!accessToAllFields && cf == null) {
					log.warning("Field '" + property + "' of bean '" + beanConfig.getBeanName()
							+ "' Can't be configured (missing @ConfigField). Ignoring!");
					continue;
				}

				TypesConverter converter = defaultTypesConverter;
				Converter cAnn = field.getAnnotation(Converter.class);
				if (cAnn != null) {
					converter = kernel.getInstance(cAnn.converter());
				}
				Object v = converter.convert(value, field.getType());

				valuesToSet.put(field, v);

			} catch (Exception e) {
				log.log(Level.WARNING, "Can't prepare value of property '" + property + "' of bean '" + beanConfig.getBeanName()
						+ "': '" + value + "'", e);
				throw new RuntimeException("Can't prepare value of property '" + property + "' of bean '"
						+ beanConfig.getBeanName() + "': '" + value + "'");
			}
		}

		final HashSet<String> changedFields = new HashSet<>();

		for (Map.Entry<Field, Object> item : valuesToSet.entrySet()) {
			if (log.isLoggable(Level.FINEST))
				log.finest("Setting property '" + item.getKey().getName() + "' of bean '" + beanConfig.getBeanName() + "'...");
			try {
				Object oldValue = BeanUtils.getValue(bean, item.getKey());
				Object newValue = item.getValue();

				if (!equals(oldValue, newValue)) {
					BeanUtils.setValue(bean, item.getKey(), newValue);
					changedFields.add(item.getKey().getName());
					if (log.isLoggable(Level.FINEST))
						log.finest("Property '" + item.getKey().getName() + "' of bean '" + beanConfig.getBeanName()
								+ "' has been set to " + item.getValue());
				} else if (log.isLoggable(Level.FINEST))
					log.finest("Property '" + item.getKey().getName() + "' of bean '" + beanConfig.getBeanName()
							+ "' has NOT been set to " + item.getValue() + " because is identical with previous value.");

			} catch (Exception e) {
				log.log(Level.WARNING, "Can't set property '" + item.getKey().getName() + "' of bean '"
						+ beanConfig.getBeanName() + "' with value '" + item.getValue() + "'", e);
				throw new RuntimeException("Can't set property '" + item.getKey().getName() + "' of bean '"
						+ beanConfig.getBeanName() + "' with value '" + item.getValue() + "'");
			}
		}

		if (bean instanceof ConfigurationChangedAware) {
			((ConfigurationChangedAware) bean).beanConfigurationChanged(Collections.unmodifiableCollection(changedFields));
		}
	}

	@Override
	public void configure(BeanConfig beanConfig, Object bean) throws KernelException {
		try {
			grabDefaultConfig(beanConfig, bean);
			Map<String, Object> ccc = getConfiguration(beanConfig);
			configure(beanConfig, bean, ccc);
		} catch (Exception e) {
			throw new KernelException("Cannot inject configuration to bean " + beanConfig.getBeanName(), e);
		}
	}

	private final boolean equals(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}

	protected abstract Map<String, Object> getConfiguration(BeanConfig beanConfig);

	public TypesConverter getDefaultTypesConverter() {
		return defaultTypesConverter;
	}

	public void setDefaultTypesConverter(TypesConverter defaultTypesConverter) {
		this.defaultTypesConverter = defaultTypesConverter;
	}

	public Kernel getKernel() {
		return kernel;
	}

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	protected void grabDefaultConfig(final BeanConfig beanConfig, final Object bean) {
		try {
			HashMap<Field, Object> defaultConfig = defaultFieldValues.get(beanConfig);
			if (defaultConfig == null) {
				defaultConfig = new HashMap<Field, Object>();
				defaultFieldValues.put(beanConfig, defaultConfig);
			}
			final Field[] fields = DependencyManager.getAllFields(beanConfig.getClazz());
			for (Field field : fields) {

				ConfigField configField = field.getAnnotation(ConfigField.class);

				if (configField == null) {
					continue;
				} else {
					Object currentValue = BeanUtils.getValue(bean, field);
					if (!defaultConfig.containsKey(field)) {
						defaultConfig.put(field, currentValue);
					}
				}
			}
		} catch (Exception e) {
			throw new KernelException("Cannot grab default values of bean " + beanConfig.getBeanName(), e);
		}
	}

	public void restoreDefaults(String beanName) {
		BeanConfig beanConfig = kernel.getDependencyManager().getBeanConfig(beanName);
		Object bean = kernel.getInstance(beanName);

		try {
			HashMap<Field, Object> defaultConfig = defaultFieldValues.get(beanConfig);
			if (defaultConfig == null) {
				return;
			}

			final Field[] fields = DependencyManager.getAllFields(beanConfig.getClazz());
			for (Field field : fields) {

				ConfigField configField = field.getAnnotation(ConfigField.class);

				if (configField == null) {
					continue;
				}

				if (!defaultConfig.containsKey(field))
					continue;

				Object valueToSet = defaultConfig.get(field);
				BeanUtils.setValue(bean, field, valueToSet);

			}
		} catch (Exception e) {
			throw new KernelException("Cannot inject configuration to bean " + beanConfig.getBeanName(), e);
		}

	}

}
