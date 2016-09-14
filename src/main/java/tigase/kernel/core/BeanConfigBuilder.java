/*
 * BeanConfigBuilder.java
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

import tigase.kernel.KernelException;

import java.util.logging.Logger;

/**
 * Builder to help register beans in Kernel.
 * <p>
 * Usage:
 * <p>
 * <pre>
 * {@code
 *
 *  // If Bean1.class is annotated by @Bean annotation.
 *  registerBean(Bean1.class).exec();
 *
 *  // If Bean2 isn't annotated or should be registered with different name.
 *  krnl.registerBean("bean2").asClass(Bean2.class).exec();
 *
 *  // To register already created variable bean4 as bean "bean4".
 *  krnl.registerBean("bean4").asInstance(bean4).exec();
 *
 *  // If Bean5 have to been created by Bean5Factory.
 *  krnl.registerBean("bean5").asClass(Bean5.class).withFactory(Bean5Factory.class).exec();
 * }
 * </pre>
 * </p>
 */
public class BeanConfigBuilder {

	protected final Logger log = Logger.getLogger(this.getClass().getName());
	private final String beanName;
	private final DependencyManager dependencyManager;
	private final Kernel kernel;
	private BeanConfig beanConfig;
	private Object beanInstance;
	private BeanConfig factoryBeanConfig;
	private Class<?> clazz;

	BeanConfigBuilder(Kernel kernel, DependencyManager dependencyManager, String beanName) {
		this.kernel = kernel;
		this.dependencyManager = dependencyManager;
		this.beanName = beanName;
	}

	/**
	 * Registers bean as type to be created when it will be required.
	 *
	 * @param cls class of bean.
	 * @return {@link BeanConfigBuilder}.
	 */
	public BeanConfigBuilder asClass(Class<?> cls) {
		this.clazz = cls;
		if (this.beanConfig != null)
			throwException(new KernelException("Class or instance is already defined for bean '" + beanName + "'"));

		this.beanConfig = dependencyManager.createBeanConfig(kernel, beanName, cls);
		return this;
	}

	/**
	 * Registers class instance as bean.
	 *
	 * @param bean instance of bean.
	 * @return {@link BeanConfigBuilder}.
	 */
	public BeanConfigBuilder asInstance(Object bean) {
		if (this.beanConfig != null)
			throwException(new KernelException("Class or instance is already defined for bean '" + beanName + "'"));

		this.beanConfig = dependencyManager.createBeanConfig(kernel, beanName, bean.getClass());
		this.beanInstance = bean;
		return this;
	}

	/**
	 * Finishing registration of bean.
	 */
	public void exec() {
		execWithoutInject();
		//kernel.injectIfRequired(beanConfig);
	}

	public BeanConfig execWithoutInject() {
		if (beanConfig == null) {
			log.warning("Bean " + clazz + " cannot be registered, because Kernel cannot create configuration for this bean.");
			return null;
		}

		beanConfig = kernel.registerBean(beanConfig, factoryBeanConfig, beanInstance);

		return beanConfig;
	}

	/**
	 * Mark bean as 'exportable'. It means that bean will be visible for all
	 * child Kernels registered in current Kernel.
	 *
	 * @return {@link BeanConfigBuilder}.
	 */
	public BeanConfigBuilder exportable() {
		beanConfig.setExportable(true);
		return this;
	}

	/**
	 * Returns name of bean.
	 *
	 * @return name of bean.
	 */
	public String getBeanName() {
		return beanName;
	}

	public BeanConfigBuilder setActive(boolean active) {
		if (active) beanConfig.setState(null);
		else
			beanConfig.setState(BeanConfig.State.inactive);
		return this;
	}

	public BeanConfigBuilder setPinned(boolean pinned) {
		beanConfig.setPinned(pinned);
		return this;
	}

	public BeanConfigBuilder setSource(BeanConfig.Source source) {
		beanConfig.setSource(source);
		return this;
	}

	public BeanConfigBuilder registeredBy(BeanConfig parent) {
		if (parent != null) {
			beanConfig.addRegisteredBy(parent);
		}
		return this;
	}

	protected void throwException(KernelException e) {
		kernel.currentlyUsedConfigBuilder = null;
		throw e;
	}

	/**
	 * Defines factory for currently registered bean.
	 *
	 * @param beanFactoryClass bean factory class.
	 * @return {@link BeanConfigBuilder}.
	 */
	public BeanConfigBuilder withFactory(Class<?> beanFactoryClass) {
		if (beanInstance != null)
			throwException(new KernelException("Cannot register factory to bean '" + beanName + "' registered as instance."));
		if (factoryBeanConfig != null)
			throwException(new KernelException("Factory for bean '" + beanName + "' is already registered."));

		this.factoryBeanConfig = dependencyManager.createBeanConfig(kernel, beanName + "#FACTORY", beanFactoryClass);
		beanConfig.setFactory(factoryBeanConfig);

		return this;
	}

}
