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
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.BeanSelector;
import tigase.kernel.beans.Converter;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyManager;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;
import tigase.util.ClassUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
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

		registerBeans(beanConfig, values);

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
				Object v = converter.convert(value, field.getType(), field.getGenericType());

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

	public boolean isAccessToAllFields() {
		return accessToAllFields;
	}

	public void setAccessToAllFields(boolean accessToAllFields) {
		this.accessToAllFields = accessToAllFields;
	}

	@Override
	public void registerBeans(BeanConfig beanConfig, Map<String, Object> values) {
		Kernel kernel = beanConfig == null ? this.getKernel() : beanConfig.getKernel();

		registerBeansForBeanOfClass(kernel, beanConfig == null ? Kernel.class : beanConfig.getClazz());

		if (values != null) {
			Map<String, BeanPropConfig> beanPropConfigMap = new HashMap<>();

			String beansProp = (String) values.get("beans");
			if (beansProp != null) {
				String[] beansPropArr = beansProp.split(",");
				for (String beanStr : beansPropArr) {
					String beanName = beanStr;
					boolean active = true;
					if (beanStr.startsWith("-")) {
						beanName = beanStr.substring(1);
						active = false;
					} else if (beanStr.startsWith("+")) {
						beanName = beanStr.substring(1);
					}

					if (beanPropConfigMap.get(beanName) != null) {
						throw new RuntimeException("Invalid 'beans' property value - duplicated entry for bean " +
								beanName + "! in " + beansProp);
					}
					BeanPropConfig cfg = new BeanPropConfig();
					cfg.setBeanName(beanName);
					cfg.setActive(active);
					beanPropConfigMap.put(beanName, cfg);
				}
			}

			List<String> keys = new ArrayList<>(values.keySet());
			Collections.sort(keys);
			for (String key : keys) {
				String[] keyParts = key.split("/");
				if (keyParts.length != 2)
					continue;

				String beanName = keyParts[0];
				String action = keyParts[1];
				Object value = values.get(key);

				BeanPropConfig cfg = beanPropConfigMap.get(beanName);
				switch (action) {
					case "active":
					case "class":
						if (cfg == null) {
							cfg = new BeanPropConfig();
							cfg.setBeanName(beanName);
							beanPropConfigMap.put(beanName, cfg);
						}
						break;
					default:
						if (kernel.isBeanClassRegistered(beanName) && cfg == null) {
							cfg = new BeanPropConfig();
							cfg.setBeanName(beanName);
							beanPropConfigMap.put(beanName, cfg);
						}
						break;
				}
				switch (action) {
					case "active":
						cfg.setActive(Boolean.parseBoolean(value.toString()));
						break;
					case "class":
						cfg.setClazzName(value.toString());
						break;
					default:
						break;
				}
			}

			for (BeanPropConfig cfg : beanPropConfigMap.values()) {
				// TODO configuration is not as it should be - unknown class for bean!
				if (cfg.getClazzName() == null && !kernel.isBeanClassRegistered(cfg.getBeanName()))
					continue;

				if (kernel.isBeanClassRegistered(cfg.getBeanName())) {
					kernel.setBeanActive(cfg.getBeanName(), cfg.isActive());
				} else {
					try {
						Class<?> cls = ModulesManagerImpl.getInstance().forName(cfg.getClazzName());
						kernel.registerBean(cfg.getBeanName()).asClass(cls).setActive(cfg.isActive()).exec();
					} catch (ClassNotFoundException ex) {
						log.log(Level.FINER, "could not register bean '" + cfg.getBeanName() + "' as class '" +
								cfg.getClazzName() + "' is not available", ex);
					}
				}
			}
		}
	}

	protected void registerBeansForBeanOfClass(Kernel kernel, Class<?> cls) {
		// TODO - needs to be adjusted to support OSGi
		try {
			Set<Class<?>> classes = ClassUtil.getClassesFromClassPath();
			classes.addAll(ModulesManagerImpl.getInstance().getClasses());
			registerBeansForBeanOfClass(kernel, cls, classes);
		} catch (IOException |ClassNotFoundException ex) {
			log.log(Level.WARNING, "could not load clases for bean registration", ex);
		}
	}

	protected void registerBeansForBeanOfClass(Kernel kernel, Class<?> requiredClass, Set<Class<?>> classes) {
		for (Class<?> cls : classes) {
			Bean annotation = registerBeansForBeanOfClassShouldRegister(kernel, requiredClass, cls);
			if (annotation != null) {
				kernel.registerBean(cls).exec();
			}
		}
	}

	protected Bean registerBeansForBeanOfClassShouldRegister(Kernel kernel, Class<?> requiredClass, Class<?> cls) {
		Bean annotation = cls.getDeclaredAnnotation(Bean.class);
		if (annotation == null)
			return null;

		Class parent = annotation.parent();
		if (parent == Object.class)
			return null;

		if (!parent.isAssignableFrom(requiredClass))
			return null;

		Class<? extends BeanSelector>[] selectors = annotation.selectors();
		if (selectors.length == 0)
			return annotation;

		for (Class<? extends BeanSelector> selectorCls : selectors) {
			try {
				BeanSelector selector = selectorCls.newInstance();
				if (!selector.shouldRegister(kernel))
					return null;
			} catch (InstantiationException | IllegalAccessException e) {
				log.log(Level.SEVERE, "could not instantiate BeanSelector " + selectorCls.getCanonicalName() +
						" for " + cls.getCanonicalName(), e);
			}
		}
		return annotation;
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

	private class BeanPropConfig {

		private String beanName;

		private String clazzName;

		private boolean active = true;

		public String getBeanName() {
			return beanName;
		}

		public void setBeanName(String beanName) {
			this.beanName = beanName;
		}

		public String getClazzName() {
			return clazzName;
		}

		public void setClazzName(String clazzName) {
			this.clazzName = clazzName;
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}
	}

}
