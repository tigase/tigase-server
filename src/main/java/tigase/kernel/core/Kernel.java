/*
 * Kernel.java
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

package tigase.kernel.core;

import tigase.kernel.BeanUtils;
import tigase.kernel.KernelException;
import tigase.kernel.beans.*;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.BeanConfig.State;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class of Kernel.
 */
public class Kernel {

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	private final Map<BeanConfig, Object> beanInstances = new HashMap<BeanConfig, Object>();

	private final DependencyManager dependencyManager = new DependencyManager();

	BeanConfigBuilder currentlyUsedConfigBuilder;

	private boolean forceAllowNull;

	private String name;

	private Kernel parent;

	/**
	 * Creates instance of Kernel.
	 */
	public Kernel() {
		this("<unknown>");
	}

	/**
	 * Creates instance of kernel.
	 *
	 * @param name kernel name.
	 */
	public Kernel(String name) {
		this.name = name;

		BeanConfig bc = dependencyManager.createBeanConfig(this, "kernel", Kernel.class);
		bc.setPinned(true);
		dependencyManager.register(bc);
		putBeanInstance(bc, this);
	}

	protected static void initBean(BeanConfig tmpBC, Set<BeanConfig> createdBeansConfig, int deep)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		final BeanConfig beanConfig = tmpBC instanceof DelegatedBeanConfig ? ((DelegatedBeanConfig) tmpBC).original : tmpBC;

		if (beanConfig.getState() == State.initialized)
			return;

		Object bean;
		if (beanConfig.getState() == State.registered) {
			beanConfig.setState(State.instanceCreated);
			if (beanConfig.getFactory() != null && beanConfig.getFactory().getState() != State.initialized) {
				initBean(beanConfig.getFactory(), new HashSet<BeanConfig>(), 0);
			}
			if (RegistrarBean.class.isAssignableFrom(beanConfig.getClazz())) {
				RegistrarKernel k = new RegistrarKernel();
				k.setName(beanConfig.getBeanName());
				beanConfig.getKernel().registerBean(beanConfig.getBeanName() + "#KERNEL").asInstance(k).exec();
				beanConfig.setKernel(k);
			}
			bean = beanConfig.getKernel().createNewInstance(beanConfig);
			beanConfig.getKernel().putBeanInstance(beanConfig, bean);
			createdBeansConfig.add(beanConfig);
			if (RegistrarBean.class.isAssignableFrom(beanConfig.getClazz())) {
				Kernel parent = beanConfig.getKernel().getParent();
				// without this line setBeanActive() fails
				//parent.ln(beanConfig.getBeanName(), beanConfig.getKernel(), beanConfig.getBeanName());
				parent.ln(beanConfig.getBeanName(), beanConfig.getKernel(), "service");
			}
		} else {
			bean = beanConfig.getKernel().getInstance(beanConfig);
		}

		BeanConfigurator beanConfigurator;
		try {
			if (beanConfig.getKernel().isBeanClassRegistered(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)
					&& !beanConfig.getBeanName().equals(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME))
				beanConfigurator = beanConfig.getKernel().getInstance(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME);
			else
				beanConfigurator = null;
		} catch (KernelException e) {
			beanConfigurator = null;
		}

		if (beanConfigurator != null) {
			beanConfigurator.configure(beanConfig, bean);
		}

		if (bean instanceof RegistrarBean) {
			((RegistrarBean) bean).register(beanConfig.getKernel());
		}

		for (final Dependency dep : beanConfig.getFieldDependencies().values()) {
			beanConfig.getKernel().injectDependencies(bean, dep, createdBeansConfig, deep);
		}

		if (deep == 0) {
			for (BeanConfig bc : createdBeansConfig) {
				Object bi = bc.getKernel().getInstance(bc);
				bc.setState(State.initialized);
				if (bi instanceof Initializable) {
					((Initializable) bi).initialize();
				}
			}

//			if (bean instanceof RegistrarBean) {
//				Kernel parent = beanConfig.getKernel().getParent();
//				parent.ln(beanConfig.getBeanName(), beanConfig.getKernel(), beanConfig.getBeanName());
//			}

		}
	}

	private Object createNewInstance(BeanConfig beanConfig) {
		try {
			if (beanConfig.getFactory() != null) {
				BeanFactory<?> factory = beanConfig.getKernel().getInstance(beanConfig.getFactory());
				return factory.createInstance();
			} else {
				if (log.isLoggable(Level.FINER))
					log.finer("[" + getName() + "] Creating instance of bean " + beanConfig.getBeanName());
				Class<?> clz = beanConfig.getClazz();

				return clz.newInstance();
			}
		} catch (Exception e) {
			throw new KernelException("Can't create instance of bean '" + beanConfig.getBeanName() + "'", e);
		}
	}

	private void fireUnregisterAware(Object i) {
		if (i != null && i instanceof UnregisterAware) {
			try {
				((UnregisterAware) i).beforeUnregister();
			} catch (Exception e) {
				e.printStackTrace();
				log.log(Level.WARNING, "Problem during unregistering bean", e);
			}
		}
	}

	public void gc() {
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Start GC for unused beans.");

		int count;
		do {
			Collection<BeanConfig> injectedBeans = gc_getInjectedBeans();
			Collection<BeanConfig> bcs = dependencyManager.getBeanConfigs();
			count = 0;
			for (BeanConfig bc : bcs) {
				if (bc.getState() == State.initialized && !bc.isPinned() && !injectedBeans.contains(bc)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Removing instance of unused bean " + bc.getBeanName());
					}
					bc.setState(State.registered);
					Object i = bc.getKernel().beanInstances.remove(bc);
					fireUnregisterAware(i);
					if (i instanceof RegistrarBean) {
						((RegistrarBean) i).unregister(bc.getKernel());
					}
					++count;
				}
			}
		} while (count > 0);
	}

	private Collection<BeanConfig> gc_getInjectedBeans() {
		HashSet<BeanConfig> injectedBeans = new HashSet<>();
		Collection<BeanConfig> bcs = dependencyManager.getBeanConfigs();
		for (BeanConfig bc : bcs) {
			if (bc.getState() != State.initialized) continue;
			for (Dependency dp : bc.getFieldDependencies().values()) {
				BeanConfig[] xxx = dependencyManager.getBeanConfig(dp);
				for (BeanConfig beanConfig : xxx) {
					if (beanConfig != null && beanConfig.getState() == State.initialized) {
						injectedBeans.add(beanConfig);
					}
				}
			}
		}
		return injectedBeans;
	}

	/**
	 * Returns {@link DependencyManager} used in Kernel.
	 *
	 * @return {@link DependencyManager depenency manager}.
	 */
	public DependencyManager getDependencyManager() {
		return dependencyManager;
	}

	/**
	 * Returns instance of bean.
	 *
	 * @param beanConfig definition of bean to be returned.
	 * @param <T>        type of bean.
	 * @return bean or <code>null</code> if instance of bean is not created.
	 */
	<T> T getInstance(BeanConfig beanConfig) {
		if (beanConfig instanceof DelegatedBeanConfig) {
			BeanConfig b = ((DelegatedBeanConfig) beanConfig).original;
			return (T) beanConfig.getKernel().beanInstances.get(b);
		} else {
			return (T) beanConfig.getKernel().beanInstances.get(beanConfig);
		}
	}

	/**
	 * Returns instance of bean.
	 *
	 * @param beanClass type of requested bean. Note that if more than one instance of
	 *                  bean will match, then Kernel throws exception.
	 * @param <T>       type of bean to be returned.
	 * @return instance of bean if bean exists and there is only single instance
	 * of it.
	 * @throws KernelException when more than one instance of matching beans will be found
	 *                         or none of matching beans is registered.
	 */
	public <T> T getInstance(Class<T> beanClass) throws KernelException {
		return getInstance(beanClass, true);
	}

	@SuppressWarnings("unchecked")
	protected <T> T getInstance(Class<T> beanClass, boolean allowNonExportable) {
		final List<BeanConfig> bcs = dependencyManager.getBeanConfigs(beanClass, allowNonExportable);

		if (bcs.size() > 1)
			throw new KernelException("Too many beans implemented class " + beanClass);
		else if (bcs.isEmpty() && this.parent != null && this.parent != this) {
			return this.parent.getInstance(beanClass, false);
		}

		if (bcs.isEmpty())
			throw new KernelException("Can't find bean implementing " + beanClass);

		BeanConfig bc = bcs.get(0);

		if (bc.getState() != State.initialized) {
			try {
				initBean(bc, new HashSet<BeanConfig>(), 0);
			} catch (Exception e) {
				e.printStackTrace();
				throw new KernelException(e);
			}
		}

		Object result = bc.getKernel().getInstance(bc);

		return (T) result;
	}

	/**
	 * Returns instance of bean. It creates bean if it is required.
	 *
	 * @param beanName name of bean to be returned.
	 * @param <T>      type of bean to be returned.
	 * @return instance of bean if bean exists and there is only single instance
	 * of it.
	 * @throws KernelException when bean with given name doesn't exists.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getInstance(String beanName) {
		BeanConfig bc = dependencyManager.getBeanConfig(beanName);

		if (bc == null && parent != null && parent.getDependencyManager().isBeanClassRegistered(beanName)) {
			return parent.getInstance(beanName);
		}

		if (bc == null)
			throw new KernelException("Unknown bean '" + beanName + "'.");

		if (bc.getState() != State.initialized) {
			try {
				bc.getKernel().initBean(bc, new HashSet<BeanConfig>(), 0);
			} catch (Exception e) {
				e.printStackTrace();
				throw new KernelException(e);
			}
		}

		Object result = bc.getKernel().getInstance(bc);

		return (T) result;
	}

	/**
	 * Returns name of Kernel.
	 *
	 * @return name of Kernel.
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns name of beans matching to given type.
	 *
	 * @param beanType type of searched beans.
	 * @return collection of matching bean names.
	 */
	public Collection<String> getNamesOf(Class<?> beanType) {
		ArrayList<String> result = new ArrayList<String>();
		List<BeanConfig> bcs = dependencyManager.getBeanConfigs(beanType);
		for (BeanConfig beanConfig : bcs) {
			result.add(beanConfig.getBeanName());
		}
		return Collections.unmodifiableCollection(result);
	}

	/**
	 * Returns parent Kernel.
	 *
	 * @return parent Kernel or <code>null</code> if there is no parent Kernel.
	 */
	public Kernel getParent() {
		return parent;
	}

	void setParent(Kernel parent) {
		this.dependencyManager.setParent(parent.getDependencyManager());
		this.parent = parent;
	}

	/**
	 * Forces initiate all registered beans.
	 */
	public void initAll() {
		try {
			for (BeanConfig bc : dependencyManager.getBeanConfigs()) {
				if (bc.getState() != State.initialized) {
					initBean(bc, new HashSet<BeanConfig>(), 0);
				}
			}
		} catch (Exception e) {
			throw new KernelException("Can't initialize all beans", e);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void inject(Object[] data, Dependency dependency, Object toBean)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {

		if (!this.forceAllowNull && !dependency.isNullAllowed() && (data == null || data.length == 0))
			throw new KernelException("Can't inject <null> to field " + dependency.getField().getDeclaringClass().getName()
					+ "." + dependency.getField().getName());

		Object valueToSet;
		if (data == null) {
			valueToSet = null;
		} else if (Collection.class.isAssignableFrom(dependency.getField().getType())) {
			Collection o;

			if (!dependency.getField().getType().isInterface()) {
				o = (Collection) dependency.getField().getType().newInstance();
			} else if (dependency.getField().getType().isAssignableFrom(Set.class)) {
				o = new HashSet();
			} else {
				o = new ArrayList();
			}

			o.addAll(Arrays.asList(data));

			valueToSet = o;
		} else {
			Object o;
			if (data != null && dependency.getField().getType().equals(data.getClass())) {
				o = data;
			} else {
				int l = Array.getLength(data);
				if (l > 1)
					throw new KernelException("Can't put many objects to single field " + dependency.getField());
				if (l == 0)
					o = null;
				else
					o = Array.get(data, 0);
			}

			valueToSet = o;
		}

		BeanUtils.setValue(toBean, dependency.getField(), valueToSet);
	}

	private void injectDependencies(Object bean, Dependency dep, Set<BeanConfig> createdBeansConfig, int deep)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		BeanConfig[] dependentBeansConfigs = dependencyManager.getBeanConfig(dep);
		ArrayList<Object> dataToInject = new ArrayList<Object>();

		for (BeanConfig b : dependentBeansConfigs) {
			if (b == null) {
				dataToInject.add(null);
			} else {
				if (!b.getKernel().beanInstances.containsKey(b)) {
					initBean(b, createdBeansConfig, deep + 1);
				}
				Object beanToInject = b.getKernel().getInstance(b);
				// if (beanToInject != null)
				dataToInject.add(beanToInject);
			}
		}
		Object[] d;
		if (dataToInject.isEmpty()) {
			d = new Object[]{};
		} else if (dep.getType() != null) {
			Object[] z = (Object[]) Array.newInstance(dep.getType(), 1);
			d = dataToInject.toArray(z);
		} else {
			d = dataToInject.toArray();
		}
		if (log.isLoggable(Level.FINER))
			log.finer("[" + getName() + "] Injecting " + Arrays.toString(d) + " to " + dep.getBeanConfig() + "#" + dep);

		inject(d, dep, bean);

	}

	void injectIfRequired(final BeanConfig beanConfig) {
		try {
			Collection<Dependency> dps = dependencyManager.getDependenciesTo(beanConfig);
			for (Dependency dep : dps) {
				BeanConfig depbc = dep.getBeanConfig();

				if (depbc.getState() == State.initialized) {
					if (beanConfig.getState() != State.initialized)
						initBean(beanConfig, new HashSet<BeanConfig>(), 0);
					Object bean = depbc.getKernel().getInstance(depbc);

					injectDependencies(bean, dep, new HashSet<BeanConfig>(), 0);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new KernelException("Can't inject bean " + beanConfig + " to dependend beans.", e);
		}
	}

	/**
	 * Checks if bean with given name is registered in Kernel.
	 *
	 * @param beanName name of bean to check.
	 * @return <code>true</code> if bean is registered (it may be not
	 * initialized!).
	 */
	public boolean isBeanClassRegistered(final String beanName) {
		boolean x = dependencyManager.isBeanClassRegistered(beanName);
		if (x == false && parent != null) {
			x = parent.isBeanClassRegistered(beanName);
		}
		return x;
	}

	/**
	 * Makes symlink to bean in another Kernel.
	 *
	 * @param exportingBeanName name bean to be linked.
	 * @param destinationKernel destination Kernel.
	 * @param destinationName   name of bean in destination Kernel.
	 */
	public void ln(String exportingBeanName, Kernel destinationKernel, String destinationName) {
		final BeanConfig sbc = dependencyManager.getBeanConfig(exportingBeanName);
		// Object bean = getInstance(sbc.getBeanName());

		BeanConfig dbc = new DelegatedBeanConfig(destinationName, sbc);

		destinationKernel.dependencyManager.register(dbc);
	}

	void putBeanInstance(BeanConfig beanConfig, Object beanInstance) {
		this.beanInstances.put(beanConfig, beanInstance);
		if (beanInstance instanceof Kernel && beanInstance != this) {
			((Kernel) beanInstance).setParent(this);
		}
		beanConfig.setState(State.initialized);
	}

	/**
	 * Registers bean as class in Kernel. Class must be annotated with
	 * {@link Bean} annotation.
	 * <p>
	 * For example:
	 * <p>
	 * <pre>
	 * {@code
	 *
	 *  // If Bean1.class is annotated by @Bean annotation.
	 *  registerBean(Bean1.class).exec();
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param beanClass class of bean to register.
	 * @return {@link BeanConfigBuilder config builder} what allows to finish
	 * bean registering.
	 */
	public BeanConfigBuilder registerBean(Class<?> beanClass) {
		if (currentlyUsedConfigBuilder != null)
			throw new KernelException(
					"Registration of bean '" + currentlyUsedConfigBuilder.getBeanName() + "' is not finished yet!");

		Bean annotation = beanClass.getAnnotation(Bean.class);
		if (annotation == null || annotation.name() == null || annotation.name().isEmpty())
			throw new KernelException("Name of bean class " + beanClass.getName() + " is not defined.");

		BeanConfigBuilder builder = new BeanConfigBuilder(this, dependencyManager, annotation.name());
		this.currentlyUsedConfigBuilder = builder;
		builder.asClass(beanClass);
		builder.setActive(annotation.active());
		return builder;
	}

	/**
	 * Registers bean with given name. Class or instance of bean must be defined
	 * in returned {@link BeanConfigBuilder config builder}.
	 * <p>
	 * For example:
	 * <p>
	 * <pre>
	 * {@code
	 *
	 *  // To register already created variable bean4 as bean "bean4".
	 *  krnl.registerBean("bean4").asInstance(bean4).exec();
	 *
	 *  // If Bean5 have to been created by Bean5Factory.
	 *  krnl.registerBean("bean5").asClass(Bean5.class).withFactory(Bean5Factory.class).exec();
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param beanName name of bean.
	 * @return {@link BeanConfigBuilder config builder} what allows to finish
	 * bean registering.
	 */
	public BeanConfigBuilder registerBean(String beanName) {
		if (currentlyUsedConfigBuilder != null)
			throw new KernelException(
					"Registration of bean '" + currentlyUsedConfigBuilder.getBeanName() + "' is not finished yet!");
		BeanConfigBuilder builder = new BeanConfigBuilder(this, dependencyManager, beanName);
		this.currentlyUsedConfigBuilder = builder;
		return builder;
	}


	public void setBeanActive(String beanName, boolean value) {
		BeanConfig beanConfig = dependencyManager.getBeanConfig(beanName);

		if (beanConfig == null)
			throw new KernelException("Unknown bean '" + beanName + "'.");

		if (beanConfig.getKernel() != this) {
			if (RegistrarBean.class.isAssignableFrom(beanConfig.getClazz()))
				beanName = "service";
			beanConfig.getKernel().setBeanActive(beanName, value);
			return;
		}


		if (value && beanConfig.getState() == State.inactive) {
			// activing bean
			if (log.isLoggable(Level.FINER))
				log.finer("[" + getName() + "] Making bean " + beanName + " active");
			beanConfig.setState(State.registered);
			injectIfRequired(beanConfig);
		}
		if (!value && beanConfig.getState() != State.inactive) {
			// deactiving bean
			if (log.isLoggable(Level.FINER))
				log.finer("[" + getName() + "] Making bean " + beanName + " inactive");
			try {
				if (beanConfig instanceof DelegatedBeanConfig) {
					beanConfig = ((DelegatedBeanConfig) beanConfig).getOriginal();
				}
				Object i = beanConfig.getKernel().beanInstances.remove(beanConfig);
				fireUnregisterAware(i);
				if (i instanceof RegistrarBean) {
					((RegistrarBean) i).unregister(beanConfig.getKernel());
					Kernel parent = beanConfig.getKernel().getParent();
					parent.unregister(beanConfig.getBeanName() + "#KERNEL");
					beanConfig.setKernel(parent);
				}
				beanConfig.setState(State.inactive);
				unloadInjectedBean(beanConfig);
			} catch (Exception e) {
				throw new KernelException("Can't unload bean " + beanName + " from depenent beans", e);
			}
		}

	}

	public void setForceAllowNull(boolean forceAllowNull) {
		this.forceAllowNull = forceAllowNull;
	}


	/**
	 * Unload given bean from all previously injected objects.
	 *
	 * @param beanConfig
	 */
	private void unloadInjectedBean(BeanConfig beanConfig) throws IllegalAccessException, InstantiationException, InvocationTargetException {
		for (BeanConfig bc : dependencyManager.getBeanConfigs()) {
			if (bc.getState() != State.initialized)
				continue;
			Object ob = bc.getKernel().getInstance(bc);
			for (Dependency d : bc.getFieldDependencies().values()) {
				if (DependencyManager.match(d, beanConfig)) {
					BeanConfig[] cbcs = dependencyManager.getBeanConfig(d);
					if (cbcs.length == 0) {
						inject(null, d, ob);
					} else if (cbcs.length == 1) {// Clearing single-instance
						// dependency. Like single field.
						// BeanConfig cbc = cbcs[0];
						// if (cbc != null && cbc.equals(removingBC)) {
						inject(null, d, ob);
						// }
					} else if (cbcs.length > 1) { // Clearing multi-instance
						// dependiency. Like
						// collections and arrays.

						injectDependencies(ob, d, new HashSet<BeanConfig>(), 0);
					}
				}
			}
		}
	}

	/**
	 * Removes bean from Kernel.
	 *
	 * @param beanName name of bean to be removed.
	 */
	public void unregister(final String beanName) {
		if (log.isLoggable(Level.FINER))
			log.finer("[" + getName() + "] Unregistering bean " + beanName);
		BeanConfig unregisteredBeanConfig = dependencyManager.getBeanConfig(beanName);
		if (unregisteredBeanConfig.getKernel() != this) {
			unregisteredBeanConfig.getKernel().unregister(beanName);
			return;
		}

		unregisterInt(beanName);
		try {
			unloadInjectedBean(unregisteredBeanConfig);
		} catch (Exception e) {
			e.printStackTrace();
			throw new KernelException("Can't unload bean " + beanName + " from depenent beans", e);
		} finally {
			dependencyManager.unregister(beanName);
		}
	}

	void unregisterInt(String beanName) {
		if (dependencyManager.isBeanClassRegistered(beanName)) {
			// unregistering
			if (log.isLoggable(Level.FINER))
				log.finer("[" + getName() + "] Found registred bean " + beanName + ". Unregistering...");

			BeanConfig oldBeanConfig = dependencyManager.unregister(beanName);
			Object i = oldBeanConfig.getKernel().beanInstances.remove(oldBeanConfig);
			fireUnregisterAware(i);
		}
	}

	static class DelegatedBeanConfig extends BeanConfig {

		private final BeanConfig original;

		DelegatedBeanConfig(String localName, BeanConfig src) {
			super(localName, src.getClazz());
			original = src;
		}

		@Override
		public Class<?> getClazz() {
			return original.getClazz();
		}

		@Override
		public BeanConfig getFactory() {
			return original.getFactory();
		}

		@Override
		public Map<Field, Dependency> getFieldDependencies() {
			return original.getFieldDependencies();
		}

		@Override
		public Kernel getKernel() {
			return original.getKernel();
		}

		public BeanConfig getOriginal() {
			return original;
		}

		@Override
		public State getState() {
			return original.getState();
		}

		@Override
		public boolean isExportable() {
			return original.isExportable();
		}

		@Override
		public String toString() {
			return original.toString();
		}
	}

}
