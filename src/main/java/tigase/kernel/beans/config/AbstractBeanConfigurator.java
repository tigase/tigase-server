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
import tigase.kernel.beans.*;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.BeanConfigBuilder;
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

	private static final Logger log = Logger.getLogger(AbstractBeanConfigurator.class.getCanonicalName());

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

		registerBeans(beanConfig, bean, values);

		final HashMap<Field, Object> valuesToSet = new HashMap<>();

		for (Map.Entry<String, Object> entry : values.entrySet()) {
			final String property = entry.getKey();
			final Object value = entry.getValue();

			try {
				if (log.isLoggable(Level.FINEST))
					log.finest("Preparing property '" + property + "' of bean '" + beanConfig.getBeanName() + "'...");

				final Field field = BeanUtils.getField(beanConfig, property);
				if (field == null) {
					switch (property) {
						case "name":
						case "class":
						case "beans":
							// ignoring as this properties are handled by configurator and kernel
							break;
						default:
							// ignoring if property contains "/" as this mean it is configuration property for subbean
							if (!property.contains("/")
									&& !(value instanceof BeanDefinition)
									&& kernel.getDependencyManager().getBeanConfig(property) == null
									&& (!(bean instanceof RegistrarBean) || ((Kernel) kernel.getInstance(beanConfig.getBeanName() + "#KERNEL")).getDependencyManager().getBeanConfig(property) == null)) {
								log.warning(
										"Field '" + property + "' does not exists in bean '" + beanConfig.getBeanName() + "'. Ignoring!");
							}
							break;
					}
					continue;
				}
				ConfigField cf = field.getAnnotation(ConfigField.class);
				if (!accessToAllFields && cf == null) {
					log.fine("Field '" + property + "' of bean '" + beanConfig.getBeanName()
							+ "' Can't be configured (missing @ConfigField). Ignoring!");
					continue;
				}

				TypesConverter converter = defaultTypesConverter;
				Converter cAnn = field.getAnnotation(Converter.class);
				if (cAnn != null) {
					converter = kernel.getInstance(cAnn.converter());
				}

				Class expType = BeanUtils.getGetterSetterMethodsParameterType(field);

				if (expType != null) {
					Object v = converter.convert(value, expType);
					valuesToSet.put(field, v);
				} else {
					Object v = converter.convert(value, field.getType(), field.getGenericType());
					valuesToSet.put(field, v);
				}

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

	protected Map<String, BeanDefinition> getBeanDefinitions(Map<String, Object> values) {
		return new HashMap<>();
	}

	@Override
	public void registerBeans(BeanConfig beanConfig, Object bean, Map<String, Object> values) {
		Kernel kernel = beanConfig == null ? this.getKernel() : beanConfig.getKernel();

		List<BeanConfig> registeredBeans = registerBeansForBeanOfClass(kernel, beanConfig == null ? Kernel.class : beanConfig.getClazz());

		if (values != null) {
			Map<String, BeanDefinition> beanPropConfigMap = getBeanDefinitions(values);

			List<String> beansProp = null;
			Object beansValue = values.get("beans");
			if (beansValue instanceof String) {
				beansProp = Arrays.asList(((String) beansValue).split(","));
			} else if (beansValue instanceof List) {
				beansProp = (List<String>) beansValue;
			}
			if (beansProp != null) {
				for (String beanStr : beansProp) {
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
					BeanDefinition cfg = new BeanDefinition();
					cfg.setBeanName(beanName);
					cfg.setActive(active);
					beanPropConfigMap.put(beanName, cfg);
				}
			}

			for (BeanDefinition cfg : beanPropConfigMap.values()) {
				// TODO configuration is not as it should be - unknown class for bean!
				try {
					Class<?> clazz = cfg.getClazzName() == null ? null : ModulesManagerImpl.getInstance().forName(cfg.getClazzName());
					if (clazz == null && !kernel.isBeanClassRegistered(cfg.getBeanName(), false)) {
						if (bean != null && bean instanceof RegistrarBeanWithDefaultBeanClass) {
							clazz = ((RegistrarBeanWithDefaultBeanClass) bean).getDefaultBeanClass();
						}
						if (clazz == null) {
							continue;
						}
					}

					boolean register = !kernel.isBeanClassRegistered(cfg.getBeanName(), false);
					if (!register) {
						if (kernel.getClass() != null && clazz != null && !kernel.getClass().equals(clazz)) {
							register = true;
						} else {
							kernel.setBeanActive(cfg.getBeanName(), cfg.isActive());
						}
					}
					if (register) {
						BeanConfig oldCfg = kernel.getDependencyManager().getBeanConfig(cfg.getBeanName());
						BeanConfigBuilder cfgBuilder = kernel.registerBean(cfg.getBeanName()).asClass(clazz).setActive(cfg.isActive());
						if (oldCfg != null && oldCfg.isExportable()) {
							cfgBuilder.exportable();
						}
						Bean ba = clazz.getAnnotation(Bean.class);
						if (ba != null) {
							if (ba.exportable()) {
								cfgBuilder.exportable();
							}
						}
						if (cfg.isExportable()) {
							cfgBuilder.exportable();
						}
						BeanConfig registeredBeanConfig = cfgBuilder.execWithoutInject();
						if (registeredBeanConfig != null) {
							registeredBeans.add(registeredBeanConfig);
						}
					}
				} catch (ClassNotFoundException ex) {
					log.log(Level.FINER, "could not register bean '" + cfg.getBeanName() + "' as class '" +
							cfg.getClazzName() + "' is not available", ex);
				}
			}
		}
	}

	public static List<BeanConfig> registerBeansForBeanOfClass(Kernel kernel, Class<?> cls) {
		// TODO - needs to be adjusted to support OSGi
		try {
			Set<Class<?>> classes = ClassUtil.getClassesFromClassPath();
			classes.addAll(ModulesManagerImpl.getInstance().getClasses());
			return registerBeansForBeanOfClass(kernel, cls, classes);
		} catch (IOException |ClassNotFoundException ex) {
			log.log(Level.WARNING, "could not load clases for bean registration", ex);
			return new ArrayList<>();
		}
	}

	protected static List<BeanConfig> registerBeansForBeanOfClass(Kernel kernel, Class<?> requiredClass, Set<Class<?>> classes) {
		List<BeanConfig> registered = new ArrayList<>();
		List<Class<?>> toRegister = registerBeansForBeanOfClassGetBeansToRegister(kernel, requiredClass, classes);
		for (Class<?> cls : toRegister) {
			Bean annotation = cls.getAnnotation(Bean.class);
			if (annotation != null) {
				BeanConfig existingBeanConfig = null;
				Kernel tmpKernel = kernel;

				do {
					existingBeanConfig = tmpKernel.getDependencyManager().getBeanConfig(annotation.name());
					tmpKernel = tmpKernel.getParent();
				}
				while (existingBeanConfig == null && tmpKernel != null);

				boolean register = true;
				if (existingBeanConfig == null) {
					register = true;
				} else if (cls.equals(existingBeanConfig.getClazz())) {
					register = false;
				} else {
					for (Class<?> ifc : cls.getInterfaces()) {
						if (ifc.isAssignableFrom(existingBeanConfig.getClazz()) && !ifc.equals(RegistrarBean.class)) {
							register = false;
							break;
						}
					}
					if (register)
						registered.remove(existingBeanConfig);
				}

				if (register) {
					BeanConfig beanConfig = kernel.registerBean(cls).execWithoutInject();
					if (beanConfig != null) {
						registered.add(beanConfig);
					}
				}
			}
		}

//		for (BeanConfig beanConfig : registered) {
//			kernel.injectIfRequired(beanConfig);
//		}
		return registered;
	}

	protected static List<Class<?>> registerBeansForBeanOfClassGetBeansToRegister(Kernel kernel, Class<?> requiredClass, Set<Class<?>> classes) {
		Map<Class<?>,Bean> matching = new HashMap<>();
		for (Class<?> cls : classes) {
			Bean bean = registerBeansForBeanOfClassShouldRegister(kernel, requiredClass, cls);
			if (bean  != null) {
				matching.put(cls, bean);
			}
		}

		List<Class<?>> toRegister = new ArrayList<>();
		Class<?> req = requiredClass;
		do {
			Iterator<Map.Entry<Class<?>,Bean>> it = matching.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Class<?>,Bean> e = it.next();
				Class expParent = e.getValue().parent();
				if (expParent.equals(req)) {
					toRegister.add(0, e.getKey());
					it.remove();
					continue;
				}
				Class[] exParents = e.getValue().parents();
				for (Class exp : exParents) {
					if (exp.equals(req)) {
						toRegister.add(0, e.getKey());
						it.remove();
						break;
					}
				}
			}
		} while ((req = req.getSuperclass()) != null && !req.equals(Object.class) && !matching.isEmpty());
		return toRegister;
	}

	protected static Bean registerBeansForBeanOfClassShouldRegister(Kernel kernel, Class<?> requiredClass, Class<?> cls) {
		Bean annotation = cls.getDeclaredAnnotation(Bean.class);
		if (annotation == null)
			return null;

		Class parent = annotation.parent();
		if (parent == Object.class) {
			Class[] parents = annotation.parents();
			boolean matches = false;

			for (Class p : parents) {
				matches |= !p.isAssignableFrom(requiredClass);
			}

			if (!matches)
				return null;

		} else if (!parent.isAssignableFrom(requiredClass)) {
			return null;
		}

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

	public static class BeanDefinition extends HashMap {

		private String beanName;

		private String clazzName;

		private boolean active = true;

		private boolean exportable = false;

		public BeanDefinition() {
		}

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

		public boolean isExportable() {
			return exportable;
		}

		public void setExportable(boolean exportable) {
			this.exportable = exportable;
		}
	}

}
