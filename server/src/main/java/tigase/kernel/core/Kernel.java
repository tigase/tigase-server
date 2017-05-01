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
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.BeanConfig.State;
import tigase.sys.TigaseRuntime;
import tigase.util.ExceptionUtilities;
import tigase.util.ReflectionHelper;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Main class of Kernel.
 */
public class Kernel {

	protected final static Logger log = Logger.getLogger(Kernel.class.getName());

	private static final ThreadLocal<DelayedDependencyInjectionQueue> DELAYED_DEPENDENCY_INJECTION = new ThreadLocal<>();

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

		log.setLevel(Level.CONFIG);

		BeanConfig bc = dependencyManager.createBeanConfig(this, "kernel", Kernel.class);
		bc.setPinned(true);
		dependencyManager.register(bc);
		putBeanInstance(bc, this);
		bc.setState(State.initialized);
	}

	protected static void initBean(BeanConfig tmpBC, Set<BeanConfig> createdBeansConfig, int deep)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		final BeanConfig beanConfig = tmpBC instanceof DelegatedBeanConfig ? ((DelegatedBeanConfig) tmpBC).original : tmpBC;

		if (log.isLoggable(Level.CONFIG)) {
			log.log(Level.CONFIG, "[{0}] Initialising bean, bc={1}, createdBeansConfig={2}, deep={3}",
			        new Object[] {tmpBC.getBeanName(), tmpBC, createdBeansConfig, deep});
		}

		if (beanConfig.getState() == State.initialized)
			return;

		DelayedDependencyInjectionQueue queue = beanConfig.getKernel().beginDependencyDelayedInjection();

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

		if (bean instanceof RegistrarBean) {
			((RegistrarBean) bean).register(beanConfig.getKernel());
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
		} else {
			AbstractBeanConfigurator.registerBeansForBeanOfClass(beanConfig.getKernel(), bean.getClass());
		}
		
		beanConfig.getKernel().finishDependecyDelayedInjection(queue);


		for (final Dependency dep : beanConfig.getFieldDependencies().values()) {
			beanConfig.getKernel().injectDependencies(bean, dep, createdBeansConfig, deep, false);
		}

		// there is no need to wait to initialize parent beans, it there any?
		if (bean instanceof Initializable) {
			((Initializable) bean).initialize();
		}
		tmpBC.setState(State.initialized);
//		if (deep == 0) {
//			for (BeanConfig bc : createdBeansConfig) {
//				Object bi = bc.getKernel().getInstance(bc);
//				bc.setState(State.initialized);
//				if (bi instanceof Initializable) {
//					((Initializable) bi).initialize();
//				}
//			}
//
////			if (bean instanceof RegistrarBean) {
////				Kernel parent = beanConfig.getKernel().getParent();
////				parent.ln(beanConfig.getBeanName(), beanConfig.getKernel(), beanConfig.getBeanName());
////			}
//
//		}
	}

	private Object createNewInstance(BeanConfig beanConfig) {
		try {
			if (beanConfig.getFactory() != null) {
				BeanFactory<?> factory = beanConfig.getKernel().getInstance(beanConfig.getFactory());
				return factory.createInstance();
			} else {
				if (log.isLoggable(Level.FINER)) {
					log.finer("[" + getName() + "] Creating instance of bean " + beanConfig.getBeanName());
				}
				Class<?> clz = beanConfig.getClazz();

				return clz.newInstance();
			}
		} catch (NoClassDefFoundError e) {
			if (e.getMessage() != null && e.getMessage().contains("licence")) {
				final String[] msg = {"ERROR! ACS strategy was enabled with following class configuration",
									  "--sm-cluster-strategy-class=tigase.server.cluster.strategy.OnlineUsersCachingStrategy",
									  "but required libraries are missing!",
									  "",
									  "Please make sure that all tigase-acs*.jar and licence-lib.jar",
									  "files are available in the classpath or disable ACS strategy!",
									  "(by commenting out above line)",
									  "",
									  "For more information please peruse ACS documentation.",};
				TigaseRuntime.getTigaseRuntime().shutdownTigase(msg);
			}
			throw new KernelException("Can't create instance of bean '" + beanConfig.getBeanName()  + "' (class: " + beanConfig.getClazz() + ")", e);
		} catch (Exception e) {
			throw new KernelException("Can't create instance of bean '" + beanConfig.getBeanName() + "' (class: " + beanConfig.getClazz() + ")" , e);
		}
	}

	private void fireUnregisterAware(Object i) {
		if (i != null && i instanceof UnregisterAware) {
			try {
				((UnregisterAware) i).beforeUnregister();
			} catch (Exception e) {
//				e.printStackTrace();
				log.log(Level.WARNING, "Problem during unregistering bean", e);
			}
		}
	}

	public void gc() {
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "Start GC for unused beans.");
		}

		dependencyManager.getBeanConfigs()
				.stream()
				.filter(beanConfig -> beanConfig.getState() == State.instanceCreated)
				.forEach(beanConfig -> {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Removing instance of unused bean " + beanConfig.getBeanName());
					}
					beanConfig.getKernel().beanInstances.remove(beanConfig);
					beanConfig.setState(State.registered);
				});

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

		dependencyManager.getBeanConfigs()
				.stream()
				.filter(beanConfig -> Kernel.class.isAssignableFrom(beanConfig.getClazz()) &&
						beanConfig.getState() == State.initialized)
				.forEach(new Consumer<BeanConfig>() {
					@Override
					public void accept(BeanConfig beanConfig) {
						Kernel k = getInstance(beanConfig);
						if (k != Kernel.this) {
							k.gc();
						}
					}
				});
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
		while (beanConfig instanceof DelegatedBeanConfig) {
			beanConfig = ((DelegatedBeanConfig) beanConfig).original;
		}
//		return (T) beanConfig.getKernel().beanInstances.get(beanConfig);
		return (T) beanConfig.getKernel().beanInstances.get(beanConfig);
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
		//TODO - check if null should be passed here
		final List<BeanConfig> bcs = dependencyManager.getBeanConfigs(beanClass, null, null, allowNonExportable);

		if (bcs.size() > 1) {
			throw new KernelException("Too many beans implemented class " + beanClass);
		}
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
//				e.printStackTrace();
				log.log(Level.SEVERE, "Exception getting instance", e);
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

		if (log.isLoggable(Level.CONFIG)) {
			log.log(Level.CONFIG, "[{0}] Creating instance of bean {1}: bc={2}, parent={3}, state={4}",
			        new Object[] {getName(), beanName, bc, parent, (bc != null ? bc.getState() : "n/a")});
		}

		if (bc == null && parent != null && parent.isBeanClassRegistered(beanName)) {
			return parent.getInstance(beanName);
		}

		if (bc == null)
			throw new KernelException("Unknown bean '" + beanName + "'.");

		if (bc.getState() != State.initialized) {
			try {
				bc.getKernel().initBean(bc, new HashSet<BeanConfig>(), 0);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception getting instance", e);
				throw new KernelException(e);
			}
			injectIfRequired(bc);
		}

		Object result = bc.getKernel().getInstance(bc);

		return (T) result;
	}

	public <T> T getInstanceIfExistsOr(String beanName, Function<BeanConfig, T> function) {
		BeanConfig bc = dependencyManager.getBeanConfig(beanName);

		if (bc == null && parent != null && parent.isBeanClassRegistered(beanName)) {
			return parent.getInstanceIfExistsOr(beanName, function);
		}

		if (bc == null)
			throw new KernelException("Unknown bean '" + beanName + "'.");

		Object result = bc.getKernel().getInstance(bc);
		if (result == null) {
			result = function.apply(bc);
		}

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

	public void registerLinks(String beanName) {
		Link l = this.registeredLinks.get(beanName);
		if (l != null) {
			lnInternal(l.exportingBeanName, l.destinationKernel, l.destinationName);
		}
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
		List<BeanConfig> bcs = dependencyManager.getBeanConfigs(beanType, null, null);
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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Kernel{");
		sb.append("dependencyManager=").append(dependencyManager);
		sb.append(", currentlyUsedConfigBuilder=").append(currentlyUsedConfigBuilder);
		sb.append(", forceAllowNull=").append(forceAllowNull);
		sb.append(", name='").append(name).append('\'');
		sb.append(", parent=").append(parent);
		sb.append('}');
		return sb.toString();
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

	/**
	 * Injects data to bean.
	 *
	 * @param data data to be injected.
	 * @param dependency dependency definition.
	 * @param toBean destination bean.
	 * @param forceNullInjection if <code>true</code> then null will be injected even if null is not allowed for this
	 * dependency definition. In this case, Exception "Can't inject <null>" will not be throwed.
	 *
	 * @return <code>true</code> if injection was successfull, <code>false</code> only in case of forcing null injection
	 * on not-null dependency.
	 *
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private boolean inject(Object[] data, Dependency dependency, Object toBean, final boolean forceNullInjection)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {

		if (!forceNullInjection && !this.forceAllowNull && !dependency.isNullAllowed() &&
				(data == null || data.length == 0)) {
			throw new KernelException(
					"Can't inject <null> to field " + dependency.getField().getDeclaringClass().getName() + "." +
							dependency.getField().getName());
		}

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
				if (l > 1) {
					throw new KernelException("Can't put many objects to single field " + dependency.getField());
				}
				if (l == 0) {
					o = null;
				} else {
					o = Array.get(data, 0);
				}
			}

			valueToSet = o;
		}

		BeanUtils.setValue(toBean, dependency.getField(), valueToSet);

		return !(forceNullInjection && !this.forceAllowNull && !dependency.isNullAllowed() &&
				(data == null || data.length == 0));
	}

	private boolean injectDependencies(Object bean, Dependency dep, Set<BeanConfig> createdBeansConfig, int deep, boolean forceNullInjection)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		BeanConfig[] dependentBeansConfigs = dependencyManager.getBeanConfig(dep);
		ArrayList<Object> dataToInject = new ArrayList<Object>();

		if (log.isLoggable(Level.CONFIG)) {
			log.log(Level.CONFIG, "[{0}] Injecting dependencies, bean: {1}, dep: {2}, createdBeansConfig: {3}, deep: {4}",
			        new Object[] {getName(), bean, dep, createdBeansConfig, deep});
		}

		for (BeanConfig b : dependentBeansConfigs) {
			if (b == null) {
				continue;
			} else {
				Object beanToInject = b.getKernel().getInstance(b);
				if (beanToInject == null) {
					try {
						initBean(b, createdBeansConfig, deep + 1);
					} catch (InvocationTargetException|KernelException|InstantiationException ex) {
						log.log(Level.WARNING, "Could not initialize bean " + b.getBeanName() + " (class: " + b.getClazz() + ")" + ", skipping injection of this bean");
						log.log(Level.CONFIG, "Could not initialize bean " + b.getBeanName() + " (class: " + b.getClazz() + ")" + ", skipping injection of this bean", ex);
						Object i = b.getKernel().beanInstances.remove(b);
						if (i instanceof RegistrarBean) {
							((RegistrarBean) i).unregister(b.getKernel());
							Kernel parent = b.getKernel().getParent();
							parent.unregister(b.getBeanName() + "#KERNEL");
							b.setKernel(parent);
						}
						b.setState(State.registered);
						continue;
					}
					beanToInject = b.getKernel().getInstance(b);
 				}
				if (beanToInject == null) // && dep.getType() != null && (Collection.class.isAssignableFrom(dep.getType()) || dep.getType().isArray()))
					continue;
				// it may happen that we create link in parent kernel to bean in subkernel and both are marked
				// as exportable - then we have 2 bean configs pointing to same instance - so we need it to
				// detect this - remove duplicated instances from dataToInject list
				if (!dataToInject.contains(beanToInject))
					dataToInject.add(beanToInject);
			}
		}
		Object[] d;
		if (dataToInject.isEmpty()) {
			d = new Object[]{};
		} else if (dep.getType() != null) {
			Class<?> type = dep.getType();
			if (Collection.class.isAssignableFrom(type)) {
				Type t = ReflectionHelper.getCollectionParamter(dep.getGenericType(), dep.getBeanConfig().getClazz());
				if (t instanceof ParameterizedType) {
					type = (Class) ((ParameterizedType) t).getRawType();
				} else if (t instanceof TypeVariable) {
					type = (Class) ((TypeVariable) t).getBounds()[0];
				} else {
					type = (Class) t;
				}
			}
			Object[] z = (Object[]) Array.newInstance(type, 1);
			d = dataToInject.toArray(z);
		} else {
			d = dataToInject.toArray();
		}
		if (log.isLoggable(Level.FINER))
			log.finer("[" + getName() + "] Injecting " + Arrays.toString(d) + " to " + dep.getBeanConfig() + "#" + dep);

		return inject(d, dep, bean, forceNullInjection);
	}

	private boolean isThereSomethingWaitingFor(final BeanConfig beanConfig) {
		// current kernel
//		Collection<Dependency> dps = dependencyManager.getDependenciesTo(beanConfig);
//		for (Dependency dp : dps) {
//			if(dp.getBeanConfig().getState()==State.initialized){
//				// initialized bean is waiting of beanConfig.
//				return true;
//			} else {
//				boolean x = isThereSomethingWaitingFor(dp.getBeanConfig());
//				if(x) return true;
//			}
//		}

		final Set<BeanConfig> related = dependencyManager.getDependenciesTo(beanConfig)
				.stream()
				.map(d -> d.getBeanConfig())
				.collect(Collectors.toSet());
		while (true) {
			HashSet<BeanConfig> toAdd = new HashSet<>();

			for (BeanConfig config : related) {
				for (Dependency dependency : dependencyManager.getDependenciesTo(config)) {
					if (!related.contains(dependency.getBeanConfig())) {
						toAdd.add(dependency.getBeanConfig());
					}
				}
			}

			if (toAdd.size() == 0) {
				break;
			}
			related.addAll(toAdd);
		}

		for (BeanConfig config : related) {
			if (config.getState() == State.initialized) {
				return true;
			}
		}

//		if (dps.stream().filter(dependency -> dependency.getBeanConfig().getState() == State.initialized).count() > 0) {
//			return true;
//		}

		if (beanConfig.isExportable()) {
			long r = getDependencyManager().getBeanConfigs()
					.stream()
					.filter(bc -> Kernel.class.isAssignableFrom(bc.getClazz()) && bc.getState() == State.initialized)
					.map(p -> (Kernel) getInstance(p))
					.filter(k -> !k.equals(Kernel.this))
					.map(kernel -> (Boolean) kernel.isThereSomethingWaitingFor(beanConfig))
					.filter(Boolean::booleanValue)
					.count();//					.forEach(k -> {
//						boolean r = k.isThereSomethingWaitingFor(beanConfig);
//						if(r) return true;
//					})
			return r > 0;
		}

		return false;
	}

	void injectIfRequired(final BeanConfig beanConfig) {
		try {
			if (!isThereSomethingWaitingFor(beanConfig)) {
				// nothing is waiting for this bean. Skipping initialization.
				return;
			}
			Collection<Dependency> dps = dependencyManager.getDependenciesTo(beanConfig);

			for (Dependency dep : dps) {
				BeanConfig depbc = dep.getBeanConfig();

				if (depbc.getState() != State.initialized && depbc.getState() != State.inactive) {
					//depbc.getState() != State.inactive && depbc.getState() != State.instanceCreated
					//if (isThereSomethingWaitingFor(depbc)) {
						try {
							if (depbc.getState() != State.instanceCreated) {
								initBean(depbc, new HashSet<BeanConfig>(), 0);
							}
							injectIfRequired(depbc);
						} catch (Exception e) {
							log.log(Level.SEVERE, "Exception injecting bean if required", e);
						}
					//}
				}

				if (depbc.getState() == State.initialized) {
					if (beanConfig.getState() != State.initialized) {
						try {
							initBean(beanConfig, new HashSet<BeanConfig>(), 0);
						} catch (Exception e) {
							// cannot initialize beanconfig -- skipping injecting
							log.log(Level.SEVERE, "Exception injecting bean if required", e);
							return;
						}
					}
					Object bean = depbc.getKernel().getInstance(depbc);

					injectDependencies(bean, dep, new HashSet<BeanConfig>(), 0, false);
				}
			}
			if (beanConfig.isExportable()) {
				getDependencyManager().getBeanConfigs()
						.stream()
						.filter(bc -> Kernel.class.isAssignableFrom(bc.getClazz()) &&
								bc.getState() == State.initialized)
						.map(p -> (Kernel) getInstance(p))
						.filter(k -> !k.equals(Kernel.this))
						.forEach(k -> {
							k.injectIfRequired(beanConfig);
						});

			}
		} catch (Exception e) {
//			e.printStackTrace();
			log.log(Level.SEVERE, "Exception", e);
			throw new KernelException("Can't inject bean " + beanConfig + " to dependend beans.", e);
		}
	}

	void injectDependency(Dependency dep)
			throws IllegalAccessException, InstantiationException, InvocationTargetException {
		if (log.isLoggable(Level.CONFIG)) {
			log.log(Level.CONFIG, "[{0}] Injecting dependency, dep: {1}",
					new Object[] {getName(), dep});
		}

		BeanConfig depbc = dep.getBeanConfig();
		if (depbc.getState() == State.initialized || depbc.getState() == State.instanceCreated) {
			Object bean = depbc.getKernel().getInstance(depbc);
			if (bean == null) {
				log.log(Level.FINEST, "skipping injection of dependencies to " + dep + " as there is no bean instance");
				return;
			}

			injectDependencies(bean, dep, new HashSet<BeanConfig>(), 0, false);
		}
	}

	void injectDependencies(Collection<Dependency> dps) {

		if (log.isLoggable(Level.CONFIG)) {
			log.log(Level.CONFIG, "[{0}] Injecting dependencies, dps: {1}",
			        new Object[] {getName(), dps});
		}

		for (Dependency dep : dps) {
			BeanConfig depbc = dep.getBeanConfig();

			try {
				injectDependency(dep);
			} catch (Exception e) {
				log.log(Level.WARNING, "Can't inject dependency to bean " + depbc.getBeanName() + " (class: " + depbc.getClazz() + ")" + " unloading bean " + depbc.getBeanName() + ExceptionUtilities
						.getExceptionRootCause(e, true));
				                                                                                                                                                                                             	
				log.log(Level.CONFIG, "Can't inject dependency to bean " + depbc.getBeanName() + " (class: " + depbc.getClazz() + ")" + " unloading bean " + depbc.getBeanName(), e);
				try {
					Object i = depbc.getKernel().beanInstances.remove(depbc);
					State oldState = depbc.getState();
					depbc.setState(State.inactive);
					if (oldState == State.initialized)
						fireUnregisterAware(i);
					unloadInjectedBean(depbc);
				} catch (Exception ex) {
					throw new KernelException("Can't unload bean " + depbc.getBeanName(), ex);
				} finally {
					depbc.setState(State.registered);
				}
			}
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
		return isBeanClassRegistered(beanName, true);
	}

	public boolean isBeanClassRegistered(final String beanName, boolean checkInParent) {
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
		Link link = new Link();
		link.exportingBeanName = exportingBeanName;
		link.destinationKernel = destinationKernel;
		link.destinationName = destinationName;
		this.registeredLinks.put(exportingBeanName, link);

		BeanConfig dbc = lnInternal(exportingBeanName, destinationKernel, destinationName);
		//destinationKernel.injectIfRequired(dbc);
	}

	BeanConfig lnInternal(String exportingBeanName, Kernel destinationKernel, String destinationName){
		final BeanConfig sbc = dependencyManager.getBeanConfig(exportingBeanName);
		// Object bean = getInstance(sbc.getBeanName());
		if (sbc == null)
			throw new KernelException("Can't export bean " + exportingBeanName + " as there is no such bean");

		BeanConfig dbc = new DelegatedBeanConfig(destinationName, sbc);

		destinationKernel.dependencyManager.register(dbc);

		return dbc;
	}

	private Map<String, Link> registeredLinks = new HashMap<>();

	private class Link{
		String exportingBeanName;
		Kernel destinationKernel;
		String destinationName;
	}

	void putBeanInstance(BeanConfig beanConfig, Object beanInstance) {
		Object oldBeanInstance = this.beanInstances.put(beanConfig, beanInstance);
		if (oldBeanInstance instanceof UnregisterAware && oldBeanInstance != beanInstance) {
			((UnregisterAware) oldBeanInstance).beforeUnregister();
		}
		if (beanInstance instanceof Kernel && beanInstance != this) {
			((Kernel) beanInstance).setParent(this);
		}
		//beanConfig.setState(State.initialized);
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
		if (annotation.exportable())
			builder.exportable();
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

	protected BeanConfig registerBean(BeanConfig beanConfig, BeanConfig factoryBeanConfig, Object beanInstance) {
		BeanConfig parent = null;
		if (beanConfig.getSource() == BeanConfig.Source.annotation && !beanConfig.getRegisteredBy().isEmpty()) {
			BeanConfig bc = dependencyManager.getBeanConfig(beanConfig.getBeanName());
			parent = beanConfig.getRegisteredBy().iterator().next();
			if (bc != null && bc.getClazz().equals(beanConfig.getClazz())) {
				bc.addRegisteredBy(parent);
				parent.addRegisteredBean(bc);
				currentlyUsedConfigBuilder = null;
				return bc;
			}
		}

		if (factoryBeanConfig != null) {
			factoryBeanConfig.setPinned(beanConfig.isPinned());
			factoryBeanConfig.setState(beanConfig.getState());
			unregisterInt(factoryBeanConfig.getBeanName());
			dependencyManager.register(factoryBeanConfig);
		}

		BeanConfig oldBeanConfig = dependencyManager.getBeanConfig(beanConfig.getBeanName());
		Collection<Dependency> oldDeps = oldBeanConfig == null ? null : dependencyManager.getDependenciesTo(oldBeanConfig);

		unregisterInt(beanConfig.getBeanName());
		dependencyManager.register(beanConfig);
		if (parent != null) {
			parent.addRegisteredBean(beanConfig);
		}

		if (beanInstance != null) {
			putBeanInstance(beanConfig, beanInstance);
			beanConfig.setState(State.initialized);
		}

		Collection<Dependency> deps = dependencyManager.getDependenciesTo(beanConfig);
		if (oldDeps != null) {
			deps.addAll(oldDeps.stream().filter(od -> {
				Field f = od.getField();
				return !deps.stream().anyMatch(nd -> nd.getField().equals(f));
			}).collect(Collectors.toSet()));
		}

		currentlyUsedConfigBuilder = null;

		if (!queueForDelayedDependencyInjection(deps)) {
			injectDependencies(deps);
		}

		return beanConfig;
	}

	private boolean queueForDelayedDependencyInjection(Collection<Dependency> deps) {
		DelayedDependencyInjectionQueue queue = DELAYED_DEPENDENCY_INJECTION.get();
		if (queue == null) {
			return false;
		}

		if (deps.isEmpty())
			return true;

		queue.offer(new DelayedDependenciesInjection(deps));
		return true;
	}

	public DelayedDependencyInjectionQueue beginDependencyDelayedInjection() {
		DelayedDependencyInjectionQueue queue = DELAYED_DEPENDENCY_INJECTION.get();
		if (queue == null) {
			queue = new DelayedDependencyInjectionQueue();
			DELAYED_DEPENDENCY_INJECTION.set(queue);
			return queue;
		}
		return null;
	}

	public void finishDependecyDelayedInjection(DelayedDependencyInjectionQueue queue)
			throws IllegalAccessException, InstantiationException, InvocationTargetException {
		if (log.isLoggable(Level.CONFIG)) {
			log.log(Level.CONFIG, "[{0}] Finishing injecting dependencies, queue: {1}",
			        new Object[] {getName(), queue});
		}

		if (queue == null) {
			return;
		}
		DELAYED_DEPENDENCY_INJECTION.remove();

		for (DelayedDependenciesInjection item : queue.getQueue()) {
			item.inject();
		}
	}


	public void setBeanActive(String beanName, boolean value) {
		BeanConfig beanConfig = dependencyManager.getBeanConfig(beanName);

		if (beanConfig == null) {
			throw new KernelException("Unknown bean '" + beanName + "'.");
		}

		if (beanConfig.getKernel() != this) {
			if (RegistrarBean.class.isAssignableFrom(beanConfig.getClazz())) {
				beanName = "service";
			}
			beanConfig.getKernel().setBeanActive(beanName, value);
			return;
		}

		if (value && beanConfig.getState() == State.inactive) {
			// activing bean
			if (log.isLoggable(Level.FINER)) {
				log.finer("[" + getName() + "] Making bean " + beanName + " active");
			}
			beanConfig.setState(State.registered);
			try {
				injectIfRequired(beanConfig);
			} catch (KernelException e) {
				log.fine("Cannot initialize " + beanConfig.getBeanName() + ". Leaving in state " +
								 beanConfig.getState());
			}
		}
		if (!value && beanConfig.getState() != State.inactive) {
			// deactiving bean
			if (log.isLoggable(Level.FINER)) {
				log.finer("[" + getName() + "] Making bean " + beanName + " inactive");
			}
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
		final HashSet<BeanConfig> beansToRemove = new HashSet<>();

		for (BeanConfig bc : dependencyManager.getBeanConfigs()) {
			if (bc.getState() != State.initialized) {
				continue;
			}
			Object ob = bc.getKernel().getInstance(bc);
			if (ob == null) {
				continue;
			}

			for (Dependency d : bc.getFieldDependencies().values()) {
				if (DependencyManager.match(d, beanConfig)) {
					try {
						BeanConfig[] cbcs = dependencyManager.getBeanConfig(d);
						if (cbcs.length == 0) {
							boolean r = inject(null, d, ob, true);
							if (!r) {
								beansToRemove.add(bc);
							}
//						} else if (cbcs.length == 1) {// Clearing single-instance
//							// dependency. Like single field.
//							// BeanConfig cbc = cbcs[0];
//							// if (cbc != null && cbc.equals(removingBC)) {
//							inject(null, d, ob);
//							// }
//						} else if (cbcs.length > 1) { // Clearing multi-instance
						} else {
							// dependiency. Like
							// collections and arrays.

							boolean r = injectDependencies(ob, d, new HashSet<BeanConfig>(), 0, true);
							if (!r) {
								beansToRemove.add(bc);
							}
						}
					} catch (KernelException ex) {
						log.log(Level.WARNING, "Can't set null to " + d + " unloading bean " + d.getBeanName(), ex);
						unloadInjectedBean(d.getBeanConfig());
					}
				}
			}
		}

		for (BeanConfig config : beansToRemove) {
			log.log(Level.INFO, "Removing " + config.getBeanName() + " because of dependency violation");
			if (dependencyManager.getBeanConfig(config.getBeanName()) != null) {
				setBeanActive(config.getBeanName(), false);
			}
//			setBeanActive(config.getBeanName(), true);
		}
		for (BeanConfig config : beansToRemove) {
			if (dependencyManager.getBeanConfig(config.getBeanName()) != null) {
				setBeanActive(config.getBeanName(), true);
			}
		}

	}

	/**
	 * Removes bean from Kernel.
	 *
	 * @param beanName name of bean to be removed.
	 */
	public void unregister(final String beanName) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("[" + getName() + "] Unregistering bean " + beanName);
		}
		BeanConfig unregisteredBeanConfig = dependencyManager.getBeanConfig(beanName);
		// bean can be unregistered already
		if (unregisteredBeanConfig == null) {
			return;
		}

		if (!(unregisteredBeanConfig instanceof DelegatedBeanConfig) && unregisteredBeanConfig.getKernel() != this) {
			if (!RegistrarBean.class.isAssignableFrom(unregisteredBeanConfig.getClazz())) {
				unregisteredBeanConfig.getKernel().unregister(beanName);
				return;
			}
		}

		Map<Kernel, ArrayList<BeanConfig>> lklklk = new HashMap<>();
		getDependencyManager().getBeanConfigs(Kernel.class, null, null)
				.stream()
				.filter(beanConfig -> beanConfig.getState() == State.initialized)
				.map(new Function<BeanConfig, Kernel>() {
					@Override
					public Kernel apply(BeanConfig beanConfig) {
						return getInstance(beanConfig);
					}
				})
				.forEach(kernel -> {
					Collection<Dependency> links = kernel.getDependencyManager()
							.getDependenciesTo(unregisteredBeanConfig);
					ArrayList<BeanConfig> toRemove = new ArrayList<BeanConfig>();
					for (Dependency link : links) {
						BeanConfig[] bc = kernel.getDependencyManager().getBeanConfig(link);
						toRemove.addAll(Arrays.asList(bc));
					}

					lklklk.put(kernel, toRemove);
				});

		unregisterInt(beanName);
		try {
			unloadInjectedBean(unregisteredBeanConfig);
		} catch (Exception e) {
//			e.printStackTrace();
			log.log(Level.SEVERE, "Exception during unregistering", e);
			throw new KernelException("Can't unload bean " + beanName + " from depenent beans", e);
		} finally {
			dependencyManager.unregister(beanName);
		}

		getDependencyManager().getBeanConfigs(Kernel.class, null, null)
				.stream()
				.filter(beanConfig -> beanConfig.getState() == State.initialized)
				.map(new Function<BeanConfig, Kernel>() {
					@Override
					public Kernel apply(BeanConfig beanConfig) {
						return getInstance(beanConfig);
					}
				})
				.forEach(kernel -> {
					BeanConfig[] links = kernel.getDependencyManager().findDelegationTo(unregisteredBeanConfig);
					for (BeanConfig link : links) {
						kernel.unregister(link.getBeanName());
					}
				});

		if (parent != null) {
			BeanConfig[] links = parent.getDependencyManager().findDelegationTo(unregisteredBeanConfig);
			if (links != null) {
				for (BeanConfig link : links) {
					parent.unregister(link.getBeanName());
				}
			}

		}

		for (Map.Entry<Kernel, ArrayList<BeanConfig>> en : lklklk.entrySet()) {
			Kernel kernel = en.getKey();
			for (BeanConfig beanConfig : en.getValue()) {
				try {
					kernel.unloadInjectedBean(beanConfig);
				} catch (Exception e) {
//					e.printStackTrace();
					log.log(Level.SEVERE, "Exception during un-registering", e);
					throw new KernelException(
							"Can't unload bean " + beanConfig.getBeanName() + " from depenent beans in kernel " +
									kernel.getName(), e);
				}
			}
		}
	}

	void unregisterInt(String beanName) {
		if (dependencyManager.isBeanClassRegistered(beanName)) {
			// unregistering
			if (log.isLoggable(Level.FINER))
				log.finer("[" + getName() + "] Found registred bean " + beanName + ". Unregistering...");

			BeanConfig oldBeanConfig = dependencyManager.unregister(beanName);
			Object i = oldBeanConfig.getKernel().beanInstances.remove(oldBeanConfig);
			if (oldBeanConfig.getState() == State.initialized)
				fireUnregisterAware(i);

			for (BeanConfig bc : oldBeanConfig.getRegisteredBeans()) {
				if (bc.removeRegisteredBy(oldBeanConfig)) {
					bc.getKernel().unregisterInt(bc.getBeanName());
				}
			}
			if (RegistrarBean.class.isAssignableFrom(oldBeanConfig.getClazz())) {
				if (oldBeanConfig.getKernel().getParent() != null) {
					oldBeanConfig.getKernel().getParent().unregister(oldBeanConfig.getBeanName() + "#KERNEL");
				}
			}
		}
	}

	public static class DelegatedBeanConfig extends BeanConfig {

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

	public class DelayedDependencyInjectionQueue {
		private final ArrayDeque<DelayedDependenciesInjection> queue = new ArrayDeque<>();

		public boolean offer(DelayedDependenciesInjection item) {
			DelayedDependenciesInjection last = queue.peekLast();
			if (last != null && last.equals(item)) {
				return true;
			}
			return queue.offer(item);
		}

		public Queue<DelayedDependenciesInjection> getQueue() {
			return queue;
		}

		public boolean checkStartingKernel(Kernel kernel) {
			return Kernel.this == kernel;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("DelayedDependencyInjectionQueue{");
			sb.append("queue=").append(queue);
			sb.append('}');
			return sb.toString();
		}
	}

	private class DelayedDependenciesInjection {

		private final Collection<Dependency> dependencies;

		public DelayedDependenciesInjection(Collection<Dependency> deps) {
			this.dependencies = deps;
		}

		public void inject() throws IllegalAccessException, InvocationTargetException, InstantiationException {
			for (Dependency dep : dependencies) {
				Kernel.this.injectDependency(dep);
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DelayedDependenciesInjection) {
				DelayedDependenciesInjection o = (DelayedDependenciesInjection) obj;
				if (o.dependencies.size() != dependencies.size()) {
					return false;
				}
				return o.dependencies.containsAll(dependencies) && dependencies.containsAll(o.dependencies);
			}
			return super.equals(obj);
		}
	}
}
