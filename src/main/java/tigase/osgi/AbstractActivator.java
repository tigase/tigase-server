/*
 * AbstractActivator.java
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
package tigase.osgi;

import org.osgi.framework.*;
import tigase.osgi.util.ClassUtil;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Common activator which should be extended by any OSGi module which will be
 * used by Tigase XMPP Server in OSGi mode.
 *
 * Created by andrzej on 08.09.2016.
 */
public abstract class AbstractActivator implements BundleActivator, ServiceListener {

	private static final Logger log = Logger.getLogger(Activator.class.getCanonicalName());
	protected Set<Class<?>> classesToExport = null;
	private BundleContext context = null;
	private ServiceReference serviceReference = null;
	private ModulesManager serviceManager = null;

	@Override
	public void start(BundleContext bc) throws Exception {
		synchronized (this) {
			context = bc;
			bc.addServiceListener(this, "(&(objectClass=" + ModulesManager.class.getName() + "))");
			serviceReference = bc.getServiceReference(ModulesManager.class.getName());
			if (serviceReference != null) {
				serviceManager = (ModulesManager) bc.getService(serviceReference);
				registerAddons();
			}
		}
	}

	@Override
	public void stop(BundleContext bc) throws Exception {
		synchronized (this) {
			if (serviceManager != null) {
				unregisterAddons();
				context.ungetService(serviceReference);
				serviceManager = null;
				serviceReference = null;
			}
		}
	}

	@Override
	public void serviceChanged(ServiceEvent event) {
		if (event.getType() == ServiceEvent.REGISTERED) {
			if (serviceReference == null) {
				serviceReference = event.getServiceReference();
				serviceManager = (ModulesManager) context.getService(serviceReference);
				registerAddons();
			}
		}
		else if (event.getType() == ServiceEvent.UNREGISTERING) {
			if (serviceReference == event.getServiceReference()) {
				unregisterAddons();
				context.ungetService(serviceReference);
				serviceManager = null;
				serviceReference = null;
			}
		}
	}

	private void registerAddons() {
		if (serviceManager != null) {
			if (classesToExport == null) {
				classesToExport = ClassUtil.getClassesFromBundle(context.getBundle()).stream().filter(PUBLIC_AND_NOT_ABSTRACT).collect(Collectors.toSet());
			}
			classesToExport.forEach( cls -> serviceManager.registerClass(cls) );
			serviceManager.update();
		}
	}

	private void unregisterAddons() {
		if (serviceManager != null) {
			if (classesToExport != null) {
				classesToExport.forEach( cls -> serviceManager.unregisterClass(cls) );
			}
			serviceManager.update();
		}
	}

	private static Predicate<Class> PUBLIC_AND_NOT_ABSTRACT =  (cls -> {
			int mod = cls.getModifiers();
			return !Modifier.isAbstract(mod) && Modifier.isPublic(mod);
	});
}