/*
 * XTigase XMPP Server
 * Copyright (C) 2011 "Andrzej Wójcik" <andrzej@hi-low.eu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * Last modified by $Author: andrzej $
 */
package tigase.osgi;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.conf.Configurable;
import tigase.conf.Configurator;
import tigase.server.ServerComponent;
import tigase.server.XMPPServer;
import tigase.xmpp.XMPPImplIfc;

/**
 *
 * @author andrzej
 */
public class ModulesManagerImpl implements ModulesManager {

        private static ModulesManagerImpl instance = null;
        private Map<String, XMPPImplIfc> plugins = null;
        private Map<String, Class<? extends Configurable>> componentsClasses = null;
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
        public void update() {
                //synchronized (this) {
                if (active) {
                        ((Configurator) XMPPServer.getConfigurator()).updateMessageRouter();
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
