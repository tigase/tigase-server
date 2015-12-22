package tigase.kernel.core;

import java.util.logging.Logger;

import tigase.kernel.KernelException;
import tigase.kernel.Registrar;

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

	public BeanConfigBuilder asClass(Class<?> cls) {
		this.clazz = cls;
		if (this.beanConfig != null)
			throwException(new KernelException("Class or instance is already defined for bean '" + beanName + "'"));

		this.beanConfig = dependencyManager.createBeanConfig(kernel, beanName, cls);
		return this;
	}

	public BeanConfigBuilder asInstance(Object bean) {
		if (this.beanConfig != null)
			throwException(new KernelException("Class or instance is already defined for bean '" + beanName + "'"));

		this.beanConfig = dependencyManager.createBeanConfig(kernel, beanName, bean.getClass());
		this.beanInstance = bean;
		return this;
	}

	public void exec() {
		if (beanConfig == null) {
			log.warning("Bean " + clazz + " cannot be registered, because Kernel cannot create configuration for this bean.");
			return;
		}

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

	public BeanConfigBuilder exportable() {
		beanConfig.setExportable(true);
		return this;
	}

	public String getBeanName() {
		return beanName;
	}

	protected void throwException(KernelException e) {
		kernel.currentlyUsedConfigBuilder = null;
		throw e;
	}

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
