/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 */
package tigase.component.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.component.Context;
import tigase.component.exceptions.ComponentException;
import tigase.criteria.Criteria;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;

public class ModulesManager implements ModuleProvider {

	private Context context;

	private Logger log = Logger.getLogger(this.getClass().getName());

	private final ArrayList<Module> modules = new ArrayList<Module>();

	private final HashMap<String, Module> modulesById = new HashMap<String, Module>();

	public ModulesManager(Context context) {
		this.context = context;
	}

	@Override
	public Set<String> getAvailableFeatures() {
		final HashSet<String> features = new HashSet<String>();
		for (Module m : modules) {
			String[] fs = m.getFeatures();
			if (fs != null) {
				for (String string : fs) {
					features.add(string);
				}
			}
		}
		return features;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Module> T getModule(String id) {
		initIfRequired();
		return (T) this.modulesById.get(id);
	}

	public boolean isRegistered(String id) {
		return this.modulesById.containsKey(id);
	}

	public boolean isRegistered(final Module module) {
		return this.modules.contains(module);
	}

	private boolean dirty = true;

	public boolean process(final Packet packet) throws ComponentException, TigaseStringprepException {
		if (dirty)
			initIfRequired();

		boolean handled = false;
		if (log.isLoggable(Level.FINER)) {
			log.finest("Processing packet: " + packet.toString());
		}

		for (Module module : this.modules) {
			Criteria criteria = module.getModuleCriteria();
			if (criteria != null && criteria.match(packet.getElement())) {
				handled = true;
				if (log.isLoggable(Level.FINER)) {
					log.finer("Handled by module " + module.getClass());
				}
				module.process(packet);
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Finished " + module.getClass());
				}
				break;
			}
		}
		return handled;
	}

	private final Set<Module> initializationRequired = Collections.newSetFromMap(new ConcurrentHashMap<Module, Boolean>());

	public synchronized <T extends Module> T register(final String id, final T module) {
		if (log.isLoggable(Level.CONFIG))
			log.config("Register Component module " + id + ": " + module.getClass().getCanonicalName());

		// if (skipIfExists) {
		// @SuppressWarnings("unchecked")
		// T old = getByClass((Class<T>) module.getClass());
		// if (old != null)
		// return old;
		// }

		dirty = true;

		if (modulesById.containsKey(id)) {
			log.config("Module " + id + " already registered. Skipped.");
			return null;
		}

		if (module instanceof ContextAware) {
			((ContextAware) module).setContext(context);
		}

		if (module instanceof InitializingModule) {
			((InitializingModule) module).beforeRegister();
		}

		this.modules.add(module);
		this.modulesById.put(id, module);

		initializationRequired.add(module);

		return module;
	}

	public void initIfRequired() {
		Iterator<Module> it = this.initializationRequired.iterator();
		while (it.hasNext()) {
			Module module = it.next();
			it.remove();
			if (module instanceof InitializingModule) {
				((InitializingModule) module).afterRegistration();
			}
		}
		dirty = false;
	}

	public synchronized void reset() {
		this.modules.clear();
		this.modulesById.clear();
		this.initializationRequired.clear();
	}

	public synchronized void unregister(final Module module) {
		if (log.isLoggable(Level.CONFIG))
			log.config("Unregister Component module: " + module.getClass().getCanonicalName());

		this.modules.remove(module);

		Iterator<Entry<String, Module>> it = this.modulesById.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Module> e = it.next();
			if (e.getValue().equals(module))
				it.remove();
		}
		this.initializationRequired.remove(module);

		if (module instanceof InitializingModule) {
			((InitializingModule) module).unregisterModule();
		}

	}

	public void unregister(String id) {
		Module m = getModule(id);
		unregister(m);
	}

}
