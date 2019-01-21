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

import tigase.kernel.core.BeanConfig;

import java.util.Map;

/**
 * Bean configurator. <p> Newly created beans should be configured: it means put specific values to fields, etc.
 * Configurator no need to inject dependencies. This interface allows to create any kind of configurator for beans. </p>
 * <p> Note, that {@link BeanConfig} parameter is just internal metadata used to identify and keep dependencies, etc.
 * </p>
 */
public interface BeanConfigurator {

	/**
	 * Name of default configurator. It will be used by default during creating new beans by Kernel.
	 */
	String DEFAULT_CONFIGURATOR_NAME = "defaultBeanConfigurator";

	/**
	 * Notify bean configurator that configuration was changed and beans needs to be reconfigured
	 */
	void configurationChanged();

	/**
	 * Configure bean.
	 *
	 * @param beanConfig internal bean configuration.
	 * @param bean bean to configure.
	 */
	void configure(BeanConfig beanConfig, Object bean);

	/**
	 * Looks for and registers beans which should be registered due to initialization of passed bean.
	 *
	 * List of beans to register may come from config (<code>values</code>), annotations, etc.
	 *
	 * @param beanConfig bean config of initializing bean
	 * @param bean instance of initializing bean
	 * @param valeus configuration for the initializing bean
	 */
	void registerBeans(BeanConfig beanConfig, Object bean, Map<String, Object> valeus);

}
