package tigase.kernel.core;

import tigase.kernel.KernelException;
import tigase.kernel.Registrar;

/**
 * Builder to help register beans in Kernel.<br/>
 *
 * Usage:<br/>
 * 
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
 */
public class BeanConfigBuilder {

	private BeanConfig beanConfig;

	private Object beanInstance;

	private final String beanName;

	private final DependencyManager dependencyManager;

	private BeanConfig factoryBeanConfig;

	private final Kernel kernel;

	BeanConfigBuilder(Kernel kernel, DependencyManager dependencyManager, String beanName) {
		this.kernel = kernel;
		this.dependencyManager = dependencyManager;
		this.beanName = beanName;
	}

	/**
	 * Registers bean as type to be created when it will be required.
	 * 
	 * @param cls
	 *            class of bean.
	 * @return {@link BeanConfigBuilder}.
	 */
	public BeanConfigBuilder asClass(Class<?> cls) {
		if (this.beanConfig != null)
			throwException(new KernelException("Class or instance is already defined for bean '" + beanName + "'"));

		this.beanConfig = dependencyManager.createBeanConfig(kernel, beanName, cls);
		return this;
	}

	/**
	 * Registers class instance as bean.
	 * 
	 * @param bean
	 *            instance of bean.
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
		if (factoryBeanConfig != null) {
			kernel.unregisterInt(factoryBeanConfig.getBeanName());
			dependencyManager.register(factoryBeanConfig);
		}
		kernel.unregisterInt(beanConfig.getBeanName());
		dependencyManager.register(beanConfig);

		if (beanInstance != null) {
			kernel.putBeanInstance(beanConfig, beanInstance);
		}

		kernel.currentlyUsedConfigBuilder = null;
		kernel.injectIfRequired(beanConfig);

		for (BeanConfig rbc : kernel.getDependencyManager().getBeanConfigs(Registrar.class)) {
			kernel.getInstance(rbc.getBeanName());
		}
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

	protected void throwException(KernelException e) {
		kernel.currentlyUsedConfigBuilder = null;
		throw e;
	}

	/**
	 * Defines factory for currently registered bean.
	 * 
	 * @param beanFactoryClass
	 *            bean factory class.
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
