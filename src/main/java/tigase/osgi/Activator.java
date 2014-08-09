/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.osgi;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.bridge.SLF4JBridgeHandler;
import tigase.conf.ConfiguratorAbstract;
import tigase.db.Repository;
import tigase.db.RepositoryFactory;
import tigase.osgi.ModulesManager;
import tigase.osgi.util.ClassUtil;
import tigase.server.XMPPServer;
import tigase.xmpp.XMPPImplIfc;

public class Activator implements BundleActivator {

        private static final Logger log = Logger.getLogger(Activator.class.getCanonicalName());

        private static Bundle bundle = null;
        
        @Override
        public void start(BundleContext bc) throws Exception {
                try {
                        bundle = bc.getBundle();
                        
                        if (!SLF4JBridgeHandler.isInstalled()) {
                                SLF4JBridgeHandler.install();
                        }
                        
                        XMPPServer.setOSGi(true);
                        
                        try {
                                Set<Class<XMPPImplIfc>> procs = ClassUtil.getClassesImplementing(XMPPImplIfc.class);
                                ArrayList<String> elems = new ArrayList<String>(32);
                                
                                for (Class<XMPPImplIfc> cproc : procs) {
                                        if (!Modifier.isPublic(cproc.getModifiers())) {
                                                continue;
                                        }

                                        if (cproc != null) {
                                                ModulesManagerImpl.getInstance().registerPluginClass(cproc);
                                        }
                                }
                        }
                        catch (Exception e) {
                                log.log(Level.SEVERE, "Plugin loading excepton", e);
                        }
                        
						try {
								Set<Class<Repository>> repos = ClassUtil.getClassesImplementing(Repository.class);
								RepositoryFactory.initialize(repos);
						} catch (Exception e) {
								log.log(Level.SEVERE, "Could not initialize properly ResourceFactory", e);
						}
						
						// we need to export this before we start, so if start will fail due to missing
						// dependencies we would be able to add them later and recorver from this
						ModulesManagerImpl.getInstance().setActive(true);
                        bc.registerService(ModulesManager.class.getName(), ModulesManagerImpl.getInstance(), new Hashtable());
						
                        XMPPServer.start(new String[]{
                                ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY, "etc/init.properties"
                        });
                        
                        // if it is not too late
                        SLF4JBridgeHandler.install();
                }
                catch (Exception ex) {
                        log.log(Level.SEVERE, "Error starting bundle: ", ex);
                        throw ex;
                }
        }

        @Override
        public void stop(BundleContext bc) throws Exception {
                try {
                        ModulesManagerImpl.getInstance().setActive(false);
                        XMPPServer.stop();
                }
                catch (Exception ex) {
                        log.log(Level.SEVERE, "Error stopping bundle: ", ex);
                        throw ex;
                }
        }
        
        public static Bundle getBundle() {
                return bundle;
        }
}
