package tigase.kernel.beans.config;

import tigase.kernel.core.BeanConfig;

public interface BeanConfigurator {

	public static final String DEFAULT_CONFIGURATOR_NAME = "defaultBeanConfigurator";

	void configure(BeanConfig beanConfig, Object bean);

}
