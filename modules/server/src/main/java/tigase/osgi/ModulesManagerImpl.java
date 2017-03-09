/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2012 "Andrzej Wójcik" <andrzej.wojcik@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * Last modified by $Author: andrzej $
 */
package tigase.osgi;

import tigase.conf.Configurable;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.xmpp.XMPPImplIfc;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @author andrzej
 */
public class ModulesManagerImpl implements ModulesManager {

	private static final Logger log = Logger.getLogger(ModulesManagerImpl.class.getCanonicalName());

	private static ModulesManagerImpl instance = null;
	private AbstractBeanConfigurator beanConfigurator;

	private ConcurrentHashMap<String, Class<?>> classes = null;
	private boolean active = false;

	public static ModulesManagerImpl getInstance() {
		if (instance == null) {
			instance = new ModulesManagerImpl();
		}
		return instance;
	}

	private ModulesManagerImpl() {
		classes = new ConcurrentHashMap<String, Class<?>>();
	}

	public Collection<Class<?>> getClasses() {
		return classes.values();
	}

	@Override
	public void registerPluginClass(Class<? extends XMPPImplIfc> pluginCls) {
		registerClass(pluginCls);
	}

	@Override
	public void unregisterPluginClass(Class<? extends XMPPImplIfc> pluginClass) {
		unregisterClass(pluginClass);
	}

	@Override
	public void registerServerComponentClass(Class<? extends Configurable> compCls) {
		registerClass(compCls);
	}

	@Override
	public void unregisterServerComponentClass(Class<? extends Configurable> compCls) {
		unregisterClass(compCls);
	}

	@Override
	public void registerClass(Class<?> cls) {
		synchronized (this) {
			String clsName = cls.getCanonicalName();
			classes.put(clsName, cls);
		}
	}

	@Override
	public void unregisterClass(Class<?> cls) {
		synchronized (this) {
			String clsName = cls.getCanonicalName();
			classes.remove(clsName, cls);
		}
	}

	@Override
	public Class<?> forName(String className) throws ClassNotFoundException {
		if ("tigase.cluster.strategy.OnlineUsersCachingStrategy".equals(className)) {
			log.warning("You are using old name for SM clustering strategy in property "
					+ "--sm-cluster-strategy-class\nYou are using name: " + className + "\n"
					+ " while name: tigase.server.cluster.strategy.OnlineUsersCachingStrategy"
					+ " should be used.");
			className = "tigase.server.cluster.strategy.OnlineUsersCachingStrategy";
		}
		Class<?> cls = classes.get(className);
		if (cls == null) {
			cls = this.getClass().getClassLoader().loadClass(className);
		}
		return cls;
	}

	@Override
	public void update() {
		//synchronized (this) {
		if (active && beanConfigurator != null) {
			beanConfigurator.configurationChanged();
		}
		//}
	}

	public void setBeanConfigurator(AbstractBeanConfigurator beanConfigurator) {
		this.beanConfigurator = beanConfigurator;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
