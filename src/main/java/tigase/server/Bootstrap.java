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
import tigase.component.PropertiesBeanConfiguratorWithBackwordCompatibility;
import tigase.conf.ConfiguratorAbstract;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.DependencyGrapher;
import tigase.kernel.core.Kernel;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

	public void init(String[] args) {
		props = new LinkedHashMap<>();
		List<String> settings = new LinkedList<>();
		ConfiguratorAbstract.parseArgs(props, settings, args);
		loadFromPropertyStrings(settings);
	}

	private void loadFromPropertyStrings(List<String> settings) {
		for (String propString : settings) {
			int idx_eq    = propString.indexOf('=');

			// String key = prop[0].trim();
			// Object val = prop[1];
			String key = propString.substring(0, idx_eq);
			Object val = propString.substring(idx_eq + 1);

			if (key.matches(".*\\[[LISBlisb]\\]$")) {
				char c = key.charAt(key.length() - 2);

				key = key.substring(0, key.length() - 3);
				// we do not need to decode value - this will be done by proper BeanConfigurator
				//val = DataTypes.decodeValueType(c, prop[1]);
			}

			props.put(key, val);
		}
	}

	public void setProperties(Map<String,Object> props) {
		this.props = props;
	}


	@Override
	public void start() {
		for (Map.Entry<String, Object> e : props.entrySet()) {
			if (e.getKey().startsWith("--")) {
				String key = e.getKey().substring(2);
				System.setProperty(key, e.getValue().toString());
			}
		}
		// register default types converter and properties bean configurator
		kernel.registerBean(DefaultTypesConverter.class).exec();
		kernel.registerBean(PropertiesBeanConfiguratorWithBackwordCompatibility.class).exec();
		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();

		// moved to AbstractBeanConfigurator
		//registerBeans();

		BeanConfigurator configurator = kernel.getInstance(PropertiesBeanConfiguratorWithBackwordCompatibility.class);
		if (configurator instanceof PropertiesBeanConfigurator) {
			PropertiesBeanConfigurator propertiesBeanConfigurator = (PropertiesBeanConfigurator) configurator;
			propertiesBeanConfigurator.setProperties(props);
		}
		// if null then we register global subbeans
		configurator.registerBeans(null, props);

		DependencyGrapher dg = new DependencyGrapher();
		dg.setKernel(kernel);
		System.out.println(dg.getDependencyGraph());

		MessageRouter mr = kernel.getInstance("message-router");
		mr.start();
	}

	@Override
	public void stop() {
		MessageRouter mr = kernel.getInstance("message-router");
		mr.stop();
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
