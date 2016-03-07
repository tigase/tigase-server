/*
 * Bootstrap.java
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

package tigase.server;

import tigase.component.PropertiesBeanConfigurator;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManager;
import tigase.osgi.ModulesManagerImpl;
import tigase.util.ClassUtil;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bootstrap class is responsible for initialization of Kernel to start Tigase XMPP Server.
 *
 * Created by andrzej on 05.03.2016.
 */
public class Bootstrap implements Lifecycle {

	private static final Logger log = Logger.getLogger(Bootstrap.class.getCanonicalName());

	private final Kernel kernel;
	private Map<String, Object> props;

	public Bootstrap() {
		kernel = new Kernel("root");
	}

	public void setProperties(Map<String,Object> props) {
		this.props = props;
	}


	@Override
	public void start() {
		// register default types converter and properties bean configurator
		kernel.registerBean(DefaultTypesConverter.class).exec();
		kernel.registerBean(PropertiesBeanConfigurator.class).exec();

		// moved to AbstractBeanConfigurator
		//registerBeans();

		BeanConfigurator configurator = kernel.getInstance(PropertiesBeanConfigurator.class);
		if (configurator instanceof PropertiesBeanConfigurator) {
			PropertiesBeanConfigurator propertiesBeanConfigurator = (PropertiesBeanConfigurator) configurator;
			propertiesBeanConfigurator.setProperties(props);
		}
		// if null then we register global subbeans
		configurator.registerBeans(null, props);

	}

	@Override
	public void stop() {

	}

	// moved to AbstractBeanConfigurator
//	public void registerBeans() {
//	}
//
//	protected void registerBeans(Set<Class<?>> classes) {
//		for (Class<?> cls : classes) {
//			Bean annotation = shouldRegister(cls, this.getClass());
//			if (annotation != null) {
//				kernel.registerBean(cls);
//			}
//		}
//	}
//
//	protected Bean shouldRegister(Class<?> cls, Class<?> requiredClass) {
//		Bean annotation = cls.getAnnotation(Bean.class);
//		if (annotation == null)
//			return null;
//
//		Class parent = annotation.parent();
//		if (parent == Object.class)
//			return null;
//
//		return parent.isAssignableFrom(requiredClass) ? annotation : null;
//	}

	protected Kernel getKernel() {
		return kernel;
	}
}
