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
package tigase.kernel.beans.config;

import tigase.conf.AbstractConfigBuilder;
import tigase.kernel.BeanUtils;
import tigase.kernel.KernelException;
import tigase.kernel.TypesConverter;
import tigase.kernel.beans.*;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.BeanConfigBuilder;
import tigase.kernel.core.DependencyManager;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;
import tigase.osgi.util.ClassUtilBean;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractBeanConfigurator
		implements BeanConfigurator {

	private static final Logger log = Logger.getLogger(AbstractBeanConfigurator.class.getCanonicalName());

	private final ConcurrentHashMap<BeanConfig, HashMap<Field, Object>> defaultFieldValues = new ConcurrentHashMap<BeanConfig, HashMap<Field, Object>>();
	@Inject(bean = "defaultTypesConverter")
	protected TypesConverter defaultTypesConverter;
	@Inject(bean = "kernel", nullAllowed = false)
	protected Kernel kernel;
	private boolean accessToAllFields = false;

	/**
	 * Method looks for bean classes (classes annotated with <code>@Bean</code> which has <code>parent</code> property
	 * set to passed class.
	 *
	 * @param kernel instance of the Kernel
	 * @param requiredClass class to look for as <code>parent</code> property value of <code>@Bean</code> annotation
	 * @return map of bean classes in for of "bean name" - "class"
	 */
	public static Map<String, Class<?>> getBeanClassesFromAnnotations(Kernel kernel, Class<?> requiredClass) {
		Set<Class<?>> classes = ClassUtilBean.getInstance().getAllClasses();
		List<Class<?>> toRegister = registerBeansForBeanOfClassGetBeansToRegister(kernel, requiredClass, classes);

		Map<String, Class<?>> result = new HashMap<>();
		for (Class<?> cls : toRegister) {
			Bean annotation = cls.getAnnotation(Bean.class);
			result.put(annotation.name(), cls);
		}

		return result;
	}

	/**
	 * Method checkes if bean is already registered in parent kernel.
	 * @param kernel kernel instance to check
	 * @param name name of the bean
	 * @param clazz expected class of the bean
	 * @return
	 */
	protected static boolean isBeanClassRegisteredInParentKernel(Kernel kernel, String name, Class<?> clazz) {
		if (kernel == null) {
			return false;
		}

		BeanConfig bc = kernel.getDependencyManager().getBeanConfig(name);
		if (bc == null) {
			return isBeanClassRegisteredInParentKernel(kernel.getParent(), name, clazz);
		}

		if (bc.getClazz().equals(clazz)) {
			return true;
		}

		Bean annotation = clazz.getAnnotation(Bean.class);
		for (Class<?> ifc : clazz.getInterfaces()) {
			if (ifc.isAssignableFrom(bc.getClazz()) && !ifc.equals(RegistrarBean.class)) {
				Bean existingBeanAnnotation = bc.getClazz().getAnnotation(Bean.class);
				if (existingBeanAnnotation == null ||
						annotation.parent().isAssignableFrom(existingBeanAnnotation.parent())) {
					return true;
				}
			}
		}

		return false;
	}
	
	protected static Map<String, BeanDefinition> mergeWithBeansPropertyValue(
			Map<String, BeanDefinition> beanPropConfigMap, Map<String, Object> values) {
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
					throw new RuntimeException(
							"Invalid 'beans' property value - duplicated entry for bean " + beanName + "! in " +
									beansProp);
				}
				BeanDefinition cfg = new BeanDefinition();
				cfg.setBeanName(beanName);
				cfg.setActive(active);
				beanPropConfigMap.put(beanName, cfg);
			}
		}
		return beanPropConfigMap;
	}

	/**
	 * Method registers beans which classes are annotated with <code>@Bean</code> and have <code>parent</code> set to
	 * the class passed as parameter.
	 * @param kernel
	 * @param cls
	 */
	public static void registerBeansForBeanOfClass(Kernel kernel, Class<?> cls) {
		Set<Class<?>> classes = ClassUtilBean.getInstance().getAllClasses();
		registerBeansForBeanOfClass(kernel, cls, classes);
	}

	protected static void registerBeansForBeanOfClass(Kernel kernel, Class<?> requiredClass, Set<Class<?>> classes) {
		List<Class<?>> toRegister = registerBeansForBeanOfClassGetBeansToRegister(kernel, requiredClass, classes);
		for (Class<?> cls : toRegister) {
			Bean annotation = cls.getAnnotation(Bean.class);
			if (isBeanClassRegisteredInParentKernel(kernel.getParent(), annotation.name(), cls)) {
				continue;
			}

			kernel.registerBean(cls).execWithoutInject();
		}
	}

	protected static List<Class<?>> registerBeansForBeanOfClassGetBeansToRegister(Kernel kernel, Class<?> requiredClass,
																				  Set<Class<?>> classes) {
		Map<Class<?>, Bean> matching = new HashMap<>();
		for (Class<?> cls : classes) {
			Bean bean = registerBeansForBeanOfClassShouldRegister(kernel, requiredClass, cls);
			if (bean != null) {
				matching.put(cls, bean);
			}
		}

		List<Class<?>> toRegister = new ArrayList<>();
		Class<?> req = requiredClass;
		do {
			Iterator<Map.Entry<Class<?>, Bean>> it = matching.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Class<?>, Bean> e = it.next();
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

	protected static Bean registerBeansForBeanOfClassShouldRegister(Kernel kernel, Class<?> requiredClass,
																	Class<?> cls) {
		Bean annotation = cls.getDeclaredAnnotation(Bean.class);
		if (annotation == null) {
			return null;
		}

		Class parent = annotation.parent();
		if (parent == Object.class) {
			Class[] parents = annotation.parents();
			boolean matches = false;

			for (Class p : parents) {
				matches |= p.isAssignableFrom(requiredClass);
			}

			if (!matches) {
				return null;
			}

		} else if (!parent.isAssignableFrom(requiredClass)) {
			return null;
		}

		Class<? extends BeanSelector>[] selectors = annotation.selectors();
		if (selectors.length > 0) {
			for (Class<? extends BeanSelector> selectorCls : selectors) {
				try {
					BeanSelector selector = selectorCls.newInstance();
					if (!selector.shouldRegister(cls, kernel)) {
						return null;
					}
				} catch (InstantiationException | IllegalAccessException e) {
					log.log(Level.SEVERE,
							"could not instantiate BeanSelector " + selectorCls.getCanonicalName() + " for " +
									cls.getCanonicalName(), e);
				}
			}
		}

		try {
			BeanSelector selector = kernel.getInstance(BeanSelector.class);
			if (selector != null) {
				if (!selector.shouldRegister(cls, kernel)) {
					return null;
				}
			}
		} catch (KernelException ex) {
			log.log(Level.FINEST, "Could not find implementation of bean selector, skipping bean selection...");
		}
		return annotation;
	}

	/**
	 * Method returns current configuration map.
	 * @return
	 */
	public abstract Map<String, Object> getProperties();

	/**
	 * Method configures passed bean with provided values.
	 * @param beanConfig
	 * @param bean
	 * @param values
	 */
	public void configure(final BeanConfig beanConfig, final Object bean, final Map<String, Object> values) {
		if (values == null) {
			return;
		}
		if (log.isLoggable(Level.CONFIG)) {
			log.config("Configuring bean '" + beanConfig.getBeanName() + "'...");
		}

		registerBeans(beanConfig, bean, values);

		final HashMap<Field, Object> valuesToSet = new HashMap<>();

		// should we invert it? for every field get value, not for every value find field?
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			final String property = entry.getKey();
			final Object value = entry.getValue();

			try {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Preparing property '" + property + "' of bean '" + beanConfig.getBeanName() + "'...");
				}

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
							if (!property.contains("/") && !(value instanceof BeanDefinition) &&
									kernel.getDependencyManager().getBeanConfig(property) == null &&
									(!(bean instanceof RegistrarBean) || (kernel.getDependencyManager()
											.getBeanConfig(beanConfig.getBeanName() + "#KERNEL") != null &&
											((Kernel) kernel.getInstance(
													beanConfig.getBeanName() + "#KERNEL")).getDependencyManager()
													.getBeanConfig(property) == null))) {
								log.config("Field '" + property + "' does not exists in bean '" +
												   beanConfig.getBeanName() + "'. Ignoring!");
							}
							break;
					}
					continue;
				}
				ConfigField cf = field.getAnnotation(ConfigField.class);
				if (!accessToAllFields && cf == null) {
					log.fine("Field '" + property + "' of bean '" + beanConfig.getBeanName() +
									 "' Can't be configured (missing @ConfigField). Ignoring!");
					continue;
				}

				TypesConverter converter = defaultTypesConverter;
				Converter cAnn = field.getAnnotation(Converter.class);
				if (cAnn != null) {
					converter = kernel.getInstance(cAnn.converter());
				}

				Type expType = BeanUtils.getGetterSetterMethodsParameterType(field);
				Class clazz = null;
				if (expType != null) {
					if (expType instanceof Class) {
						clazz = (Class) expType;
					} else if (expType instanceof ParameterizedType) {
						Type type = ((ParameterizedType) expType).getRawType();
						if (type instanceof Class) {
							clazz = (Class) type;
						} else {
							expType = null;
						}
					} else{
						expType = null;
					}
				}

				if (expType != null) {
					Object v = converter.convert(value, clazz, expType);
					valuesToSet.put(field, v);
				} else {
					Object v = converter.convert(value, field.getType(), field.getGenericType());
					valuesToSet.put(field, v);
				}

			} catch (Exception e) {
				log.log(Level.WARNING,
						"Can't prepare value of property '" + property + "' of bean '" + beanConfig.getBeanName() +
								"': '" + value + "'", e);
				throw new RuntimeException(
						"Can't prepare value of property '" + property + "' of bean '" + beanConfig.getBeanName() +
								"': '" + value + "'");
			}
		}

		final HashSet<String> changedFields = new HashSet<>();

		for (Map.Entry<Field, Object> item : valuesToSet.entrySet()) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Setting property '" + item.getKey().getName() + "' of bean '" + beanConfig.getBeanName() +
								   "'...");
			}
			try {
				Object oldValue = BeanUtils.getValue(bean, item.getKey());
				Object newValue = item.getValue();

				if (!equals(oldValue, newValue)) {
					BeanUtils.setValue(bean, item.getKey(), newValue);
					changedFields.add(item.getKey().getName());
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Property '" + item.getKey().getName() + "' of bean '" + beanConfig.getBeanName() +
										   "' has been set to " + item.getValue());
					}
				} else if (log.isLoggable(Level.FINEST)) {
					log.finest("Property '" + item.getKey().getName() + "' of bean '" + beanConfig.getBeanName() +
									   "' has NOT been set to " + item.getValue() +
									   " because is identical with previous value.");
				}

			} catch (Exception e) {
				log.log(Level.WARNING,
						"Can't set property '" + item.getKey().getName() + "' of bean '" + beanConfig.getBeanName() +
								"' with value '" + item.getValue() + "'", e);
				throw new RuntimeException(
						"Can't set property '" + item.getKey().getName() + "' of bean '" + beanConfig.getBeanName() +
								"' with value '" + item.getValue() + "'");
			}
		}

		if (bean instanceof ConfigurationChangedAware) {
			((ConfigurationChangedAware) bean).beanConfigurationChanged(
					Collections.unmodifiableCollection(changedFields));
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

	/**
	 * Method returns default types converter used by bean configurator.
	 * @return
	 */
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

	public boolean isAccessToAllFields() {
		return accessToAllFields;
	}

	public void setAccessToAllFields(boolean accessToAllFields) {
		this.accessToAllFields = accessToAllFields;
	}

	/**
	 * Method registers all beans which are annotated by <code>@Bean</code> and which <code>parent</code> property is
	 * set to class which instance is passed. Additionally this method registers beans which definitions are passed
	 * in the configuration.
	 * 
	 * @param beanConfig bean config of initializing bean
	 * @param bean instance of initializing bean
	 * @param values configuration of a bean
	 */
	@Override
	public void registerBeans(BeanConfig beanConfig, Object bean, Map<String, Object> values) {
		if (beanConfig != null && Kernel.class.isAssignableFrom(beanConfig.getClazz())) {
			return;
		}

		Kernel kernel = beanConfig == null ? this.getKernel() : beanConfig.getKernel();

		Set<String> toUnregister = new ArrayList<>(kernel.getDependencyManager().getBeanConfigs()).stream()
				.filter(bc -> bc.getSource() == BeanConfig.Source.configuration)
				.filter(bc -> beanConfig == null || bc.getRegisteredBy().contains(beanConfig))
				.map(bc -> bc.getBeanName())
				.collect(Collectors.toSet());

		final Map<String, Class<?>> beansFromAnnotations = getBeanClassesFromAnnotations(kernel, beanConfig == null
																								 ? Kernel.class
																								 : beanConfig.getClazz());
		final Map<String, BeanDefinition> beanDefinitionsFromConfig =
				values == null ? new HashMap<>() : mergeWithBeansPropertyValue(getBeanDefinitions(values), values);

		beansFromAnnotations.forEach((name, cls) -> {
			if (beanDefinitionsFromConfig != null) {
				BeanDefinition definition = beanDefinitionsFromConfig.get(name);
				if (definition != null) {
					return;
				}
			}

			if (isBeanClassRegisteredInParentKernel(kernel.getParent(), name, cls)) {
				return;
			}

			BeanConfig bc = kernel.getDependencyManager().getBeanConfig(name);
			if (bc != null && bc.getSource() == BeanConfig.Source.annotation && bc.getClazz().equals(cls)) {
				return;
			}

			if (beanConfig != null && beanConfig.getState() == BeanConfig.State.initialized) {
				kernel.registerBean(cls).setSource(BeanConfig.Source.annotation).registeredBy(beanConfig).exec();
			} else {
				kernel.registerBean(cls)
						.setSource(BeanConfig.Source.annotation)
						.registeredBy(beanConfig)
						.execWithoutInject();
			}

			bc = kernel.getDependencyManager().getBeanConfig(name);
			if (bc != null && bc.getState() == BeanConfig.State.inactive && hasDirectConfiguration(bc)) {
				log.log(Level.CONFIG, "bean " + bc.getBeanName() + " is disabled but configuration is specified");
			}
		});

		for (BeanDefinition cfg : beanDefinitionsFromConfig.values()) {
			try {
				Class<?> clazz = cfg.getClazzName() == null
								 ? beansFromAnnotations.get(cfg.getBeanName())
								 : ModulesManagerImpl.getInstance().forName(cfg.getClazzName());
				BeanConfig oldBc = kernel.getDependencyManager().getBeanConfig(cfg.getBeanName());
				if (clazz == null) {
					if (bean != null && bean instanceof RegistrarBeanWithDefaultBeanClass) {
						clazz = ((RegistrarBeanWithDefaultBeanClass) bean).getDefaultBeanClass();
					} else if (oldBc != null) {
						clazz = oldBc.getClazz();
					}

					if (clazz == null) {
						log.log(Level.WARNING, "unknown class {0} for bean {1}, skipping registration of a bean",
								new Object[]{cfg.getClazzName(), cfg.getBeanName()});
						continue;
					}
				}

				if (!tigase.util.reflection.ClassUtilBean.getInstance().getAllClasses().contains(clazz)) {
					continue;
				}

				toUnregister.remove(cfg.getBeanName());

				if (oldBc != null && oldBc.getClazz().equals(clazz) &&
						(oldBc.isExportable() || cfg.isExportable() == oldBc.isExportable())) {
					kernel.setBeanActive(cfg.getBeanName(), cfg.isActive());
				} else {
					Bean ba = clazz.getAnnotation(Bean.class);
					BeanConfigBuilder cfgBuilder = kernel.registerBean(cfg.getBeanName()).asClass(clazz);
					cfgBuilder.setActive(cfg.isActive()).setSource(BeanConfig.Source.configuration);
					if (cfg.isExportable()) {
						cfgBuilder.exportable();
					}
					if (ba != null) {
						if (ba.exportable()) {
							cfgBuilder.exportable();
						}
					}

					cfgBuilder.registeredBy(beanConfig);

					if (beanConfig != null && beanConfig.getState() == BeanConfig.State.initialized) {
						cfgBuilder.exec();
					} else {
						cfgBuilder.execWithoutInject();
					}
				}
			} catch (ClassNotFoundException ex) {
				log.log(Level.FINER,
						"could not register bean '" + cfg.getBeanName() + "' as class '" + cfg.getClazzName() +
								"' is not available", ex);
			}
		}

		toUnregister.forEach(beanName -> kernel.unregister(beanName));
	}

	/**
	 * Method applies configuration changes to bean. Should be called after configuration is updated.
	 */
	public void configurationChanged() {
		refreshConfiguration(kernel);
	}

	/**
	 * Method restors default configuration of a bean
	 * @param beanName
	 */
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

				if (!defaultConfig.containsKey(field)) {
					continue;
				}

				Object valueToSet = defaultConfig.get(field);
				BeanUtils.setValue(bean, field, valueToSet);

			}
		} catch (Exception e) {
			throw new KernelException("Cannot inject configuration to bean " + beanConfig.getBeanName(), e);
		}

	}

	/**
	 * Method returns configuration of a bean.
	 * @param beanConfig
	 * @return
	 */
	protected abstract Map<String, Object> getConfiguration(BeanConfig beanConfig);

	protected Map<Field, Object> grabDefaultConfig(final BeanConfig beanConfig, final Object bean) {
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
			return defaultConfig;
		} catch (Exception e) {
			throw new KernelException("Cannot grab default values of bean " + beanConfig.getBeanName(), e);
		}
	}

	protected Map<Field, Object> grabCurrentConfig(final Object bean, String beanName) {
		Map<Field, Object> config = new HashMap<>();
		try {
			final Field[] fields = DependencyManager.getAllFields(bean.getClass());

			for (Field field : fields) {

				ConfigField configField = field.getAnnotation(ConfigField.class);

				if (configField == null) {
					continue;
				} else {
					Object currentValue = BeanUtils.getValue(bean, field);
					if (!config.containsKey(field)) {
						config.put(field, currentValue);
					}
				}
			}
		} catch (Exception ex) {
			log.log(Level.FINEST, "retrieval of configuration for bean " + beanName + " failed", ex);
		}
		return config;
	}

	/**
	 * Method returns map of bean definitions found in the configuration
	 * @param values configuration map
	 * @return
	 */
	protected Map<String, BeanDefinition> getBeanDefinitions(Map<String, Object> values) {
		return new HashMap<>();
	}

	/**
	 * Method returns queue of kernel and bean names to find bean config (path to the bean config from root of
	 * the config).
	 * @param beanConfig
	 * @return
	 */
	protected ArrayDeque<String> getBeanConfigPath(BeanConfig beanConfig) {
		Kernel kernel = beanConfig.getKernel();
		ArrayDeque<String> path = new ArrayDeque<>();

		if (!beanConfig.getBeanName().equals(beanConfig.getKernel().getName())) {
			path.push(beanConfig.getBeanName());
		}

		while (kernel.getParent() != null && kernel != this.kernel) {
			path.push(kernel.getName());
			kernel = kernel.getParent();
		}

		return path;
	}

	protected abstract boolean hasDirectConfiguration(BeanConfig bc);

	protected void refreshConfiguration(final Kernel kernel) {
		// TODO
		//kernel.beginDelayedInjection();
		refreshConfiguration_removeUndefinedBeans(kernel);
		registerBeans(null, null, getProperties());
		refreshConfiguration_updateConfiguration(kernel);
		// TODO
		//kernel.finishDelayedInjection();
	}

	protected void refreshConfiguration_removeUndefinedBeans(Kernel kernel) {
		Set<Class<?>> classes = tigase.util.reflection.ClassUtilBean.getInstance().getAllClasses();
		Set<BeanConfig> toRemove = kernel.getDependencyManager()
				.getBeanConfigs()
				.stream()
				.filter(bc -> bc.getSource() != BeanConfig.Source.hardcoded)
				.filter(bc -> !classes.contains(bc.getClazz()))
				.filter(bc -> {
					String name = bc.getClazz().getCanonicalName();
					return (!name.startsWith("java.")) && (!name.startsWith("javax.")) &&
							(!name.startsWith("com.sun."));
				})
				.collect(Collectors.toSet());
		toRemove.forEach(bc -> kernel.unregister(bc.getBeanName()));

		for (String name : kernel.getNamesOf(Kernel.class)) {
			Kernel subkernel = kernel.getInstance(name);
			if (subkernel == null || subkernel == kernel) {
				continue;
			}

			refreshConfiguration_removeUndefinedBeans(subkernel);
		}
	}

	protected void refreshConfiguration_updateConfiguration(Kernel kernel) {
		Set<BeanConfig> toReconfigure = kernel.getDependencyManager()
				.getBeanConfigs()
				.stream()
				.filter(bc -> bc.getState() == BeanConfig.State.initialized)
				.filter(bc -> !(bc instanceof Kernel.DelegatedBeanConfig))
				.filter(bc -> !Kernel.class.isAssignableFrom(bc.getClazz()))
				.collect(Collectors.toSet());
		toReconfigure.forEach(bc -> {
			if (kernel.isBeanClassRegistered(bc.getBeanName())) {
				AbstractBeanConfigurator.this.configure(bc, kernel.getInstance(bc.getBeanName()));
			}
		});

		for (String name : kernel.getNamesOf(Kernel.class)) {
			Kernel subkernel = kernel.getInstance(name);
			if (subkernel == null || subkernel == kernel) {
				continue;
			}

			refreshConfiguration_updateConfiguration(subkernel);
		}
	}

	private final boolean equals(Object o1, Object o2) {
		return (o1 == null ? o2 == null : o1.equals(o2));
	}

	public static class BeanDefinition
			extends HashMap {

		private boolean active = true;
		private String beanName;
		private String clazzName;
		private boolean exportable = false;

		public BeanDefinition() {
		}

		public BeanDefinition(BeanDefinition def) {
			this.beanName = def.getBeanName();
			this.clazzName = def.getClazzName();
			this.active = def.isActive();
			this.exportable = def.isExportable();
			this.putAll(def);
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

		@Override
		public boolean equals(Object o) {
			if (o instanceof AbstractBeanConfigurator.BeanDefinition) {
				AbstractBeanConfigurator.BeanDefinition o1 = (AbstractBeanConfigurator.BeanDefinition) o;
				if (!beanName.equals(o1.beanName)) {
					return false;
				}
				if (clazzName == null) {
					if (o1.clazzName != null) {
						return false;
					}
				} else {
					if (!clazzName.equals(o1.clazzName)) {
						return false;
					}
				}
				if (exportable != o1.exportable) {
					return false;
				}
				if (active != o1.active) {
					return false;
				}
				return super.equals(o);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return super.hashCode() * 21 + Objects.hash(beanName, clazzName, exportable, active);
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("BeanConfig{");
			sb.append("beanName='").append(beanName).append('\'');
			sb.append(", clazz=").append(clazzName);
			sb.append(", exportable=").append(exportable);
			sb.append(", active=").append(active);
			sb.append(", props=[");
			Iterator<Map.Entry> it = entrySet().iterator();
			while (it.hasNext()) {
				Entry e = it.next();
				sb.append("" + e.getKey()).append("=").append("" + e.getValue());
				if (it.hasNext()) {
					sb.append(", ");
				}
			}
			sb.append('}');
			return sb.toString();
		}

		public static class Builder
				extends AbstractConfigBuilder<BeanDefinition, Builder> {

			private final Map<String, Object> parent;

			public Builder(Map<String, Object> parent) {
				super(new AbstractBeanConfigurator.BeanDefinition());
				this.parent = parent;
			}

			public Builder() {
				this(null);
			}

			public Builder active(boolean active) {
				map.setActive(active);
				return this;
			}

			public Builder name(String name) {
				map.setBeanName(name);
				return this;
			}

			public Builder clazz(Class<?> clazz) {
				map.setClazzName(clazz == null ? null : clazz.getCanonicalName());
				return this;
			}

			public Builder clazz(String clazz) {
				map.setClazzName(clazz);
				return this;
			}

			public BeanDefinition build() {
				if (parent != null) {
					parent.put(map.getBeanName(), map);
				}
				return map;
			}
		}
	}

}
