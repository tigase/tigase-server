package tigase.kernel.beans.config;

import tigase.kernel.core.BeanConfig;

import java.util.Map;

/**
 * Bean configurator.
 * <p>
 * Newly created beans should be configured: it means put specific values to
 * fields, etc. Configurator no need to inject dependencies. This interface
 * allows to create any kind of configurator for beans.
 * </p>
 * <p>
 * Note, that {@link BeanConfig} parameter is just internal metadata used to
 * identify and keep dependencies, etc.
 * </p>
 */
public interface BeanConfigurator {

	/**
	 * Name of default configurator. It will be used by default during creating
	 * new beans by Kernel.
	 */
	String DEFAULT_CONFIGURATOR_NAME = "defaultBeanConfigurator";

	/**
	 * Configure bean.
	 * 
	 * @param beanConfig
	 *            internal bean configuration.
	 * @param bean
	 *            bean to configure.
	 */
	void configure(BeanConfig beanConfig, Object bean);

	void registerBeans(BeanConfig beanConfig, Map<String, Object> valeus);

}
