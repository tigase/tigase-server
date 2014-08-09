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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.conf.Configurable;
import tigase.conf.Configurator;
import tigase.server.ServerComponent;
import tigase.server.XMPPServer;
import tigase.util.ClassUtil;
import tigase.xmpp.XMPPImplIfc;

/**
 *
 * @author andrzej
 */
public class ModulesManagerImpl implements ModulesManager {

		private static final Logger log = Logger.getLogger(ModulesManagerImpl.class.getCanonicalName());
	
        private static ModulesManagerImpl instance = null;
        private Map<String, XMPPImplIfc> plugins = null;
        private Map<String, Class<? extends Configurable>> componentsClasses = null;
				private ConcurrentHashMap<String, Class<?>> classes = null;
        private boolean active = false;

        public static ModulesManagerImpl getInstance() {
                if (instance == null) {
                        instance = new ModulesManagerImpl();
                }
                return instance;
        }

        private ModulesManagerImpl() {
                plugins = new ConcurrentHashMap<String, XMPPImplIfc>();
                componentsClasses = new ConcurrentHashMap<String, Class<? extends Configurable>>();
				classes = new ConcurrentHashMap<String, Class<?>>();
        }

        @Override
        public void registerPluginClass(Class<? extends XMPPImplIfc> pluginCls) {
                synchronized (this) {
                        try {
                                XMPPImplIfc plugin = pluginCls.newInstance();

//                                // is it really needed?
//                                XMPPImplIfc oldPlugin = pluginsClasses.get(plugin.id());
//                                if (oldPlugin != null) {
//                                        if (!oldPlugin.getClass().getCanonicalName().startsWith("tigase.xmpp.impl")) {
//                                                return;
//                                        }
//                                        if (plugin.getClass().getCanonicalName().startsWith("tigase.xmpp.impl")) {
//                                                return;
//                                        }
//                                }
                                
                                plugins.put(plugin.id(), plugin);
                        } catch (InstantiationException ex) {
                                Logger.getLogger(ModulesManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IllegalAccessException ex) {
                                Logger.getLogger(ModulesManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                }
        }

        @Override
        public void unregisterPluginClass(Class<? extends XMPPImplIfc> pluginClass) {
                synchronized (this) {
                        String key = null;
                        
                        for (Iterator<Entry<String, XMPPImplIfc>> it = plugins.entrySet().iterator(); it.hasNext();) {
                                Entry<String,XMPPImplIfc> entry = it.next();
                                if(pluginClass.equals(entry.getValue())) {
                                        key = entry.getKey();
                                }
                        }
                        
                        if (key != null) {
                                plugins.remove(key);
                        }
                }
        }

//        public static Set<String> getActivePlugins() {
//                HashSet<String> active = new HashSet<String>();
//                for (Entry<String, XMPPImplIfc> entry : getInstance().pluginsClasses.entrySet()) {
//                        if (entry.getValue().getClass().getCanonicalName().startsWith("tigase.xmpp.impl.")) {
//                                continue;
//                        }
//
//                        active.add(entry.getKey());
//                }
//                return active;
//        }
//
//        public static Set<String> getActiveComponentsNames() {
//                return new HashSet<String>(getInstance().activeComponents.keySet());
//        }
//
//        public static String getComponentClassNameByName(String name) {
//                return getInstance().activeComponents.get(name);
//        }
//
//        public static ServerComponent getComponentInstance(String cls_name) {
//                return getInstance().componentsClasses.get(cls_name);
//        }

        @Override
        public void registerServerComponentClass(Class<? extends Configurable> compCls) {
                synchronized (this) {
                        componentsClasses.put(compCls.getCanonicalName(), compCls);
                }
        }

        @Override
        public void unregisterServerComponentClass(Class<? extends Configurable> compCls) {
                synchronized (this) {
                        componentsClasses.remove(compCls.getCanonicalName());
                }
        }
		
		@Override
		public void registerClass(Class<?> cls) {
			synchronized (this) {
				String clsName = cls.getCanonicalName();
				classes.put(clsName, cls);
				if (XMPPImplIfc.class.isAssignableFrom(cls)) {
					registerPluginClass((Class<? extends XMPPImplIfc>) cls);
				}
				if (Configurable.class.isAssignableFrom(cls)) {
					registerServerComponentClass((Class<? extends Configurable>) cls);
				}
			}
		}
		
		@Override
		public void unregisterClass(Class<?> cls) {
			synchronized (this) {
				String clsName = cls.getCanonicalName();
				classes.remove(clsName, cls);
				if (XMPPImplIfc.class.isAssignableFrom(cls)) {
					unregisterPluginClass((Class<? extends XMPPImplIfc>) cls);
				}
				if (Configurable.class.isAssignableFrom(cls)) {
					unregisterServerComponentClass((Class<? extends Configurable>) cls);
				}
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
		
		public <T extends Class> Set<T> getImplementations(T cls) {
			return ClassUtil.getClassesImplementing(classes.values(), cls);
		}
		
        @Override
        public void update() {
                //synchronized (this) {
                if (active) {
					Configurator configurator = ((Configurator) XMPPServer.getConfigurator());
					if (configurator != null)
						configurator.updateMessageRouter();
                }
                //}
        }

        public XMPPImplIfc getPlugin(String plug_id) throws InstantiationException, IllegalAccessException {
                return plugins.get(plug_id);                
        }
        
        public boolean hasPluginForId(String plug_id) {
                return plugins.containsKey(plug_id);
        }
        
        public Class<? extends ServerComponent> getServerComponentClass(String className) {
                return componentsClasses.get(className);
        }
        
        public ServerComponent getServerComponent(String className) throws InstantiationException, IllegalAccessException {
                Class<? extends Configurable> compCls = componentsClasses.get(className);
                
                if (compCls == null) {
                        return null;
                }
                
                return compCls.newInstance();
        }
        
        public boolean hasClassForServerComponent(String className) {
                return componentsClasses.containsKey(className);
        }
        
        public void setActive(boolean active) {
                this.active = active;
        }
}
