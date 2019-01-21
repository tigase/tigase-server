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
package tigase.component;

import tigase.annotations.TigaseDeprecated;
import tigase.component.modules.Module;
import tigase.component.modules.StanzaProcessor;
import tigase.component.responses.AsyncCallback;
import tigase.component.responses.ResponseManager;
import tigase.conf.ConfigurationException;
import tigase.disco.XMPPService;
import tigase.eventbus.EventBus;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.server.AbstractMessageReceiver;
import tigase.server.DisableDisco;
import tigase.server.Packet;

import javax.script.Bindings;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for implement XMPP Component.
 *
 * @author bmalkow
 * @deprecated Do not use this class at all. Use {@link AbstractKernelBasedComponent} instead. This class is here just
 * because developer wants to know how some parts of code worked before migration to Kernel Based TCF.
 */
@Deprecated
@TigaseDeprecated(since = "8.0.0")
public abstract class AbstractComponent
		extends AbstractMessageReceiver
		implements XMPPService, DisableDisco {

	protected static final String COMPONENT = "component";
	protected final Logger log = Logger.getLogger(this.getClass().getName());

	@Inject(bean = "eventBus")
	protected EventBus eventBus;

	@Inject
	private Kernel kernel;
	@Inject
	private ResponseManager responseManager;
	@Inject
	private StanzaProcessor stanzaProcessor;

	public AbstractComponent() {
	}

	@Override
	public synchronized void everyMinute() {
		super.everyMinute();
		if (responseManager != null) {
			responseManager.checkTimeouts();
		}
	}

	/**
	 * Returns version of component. Used for Service Discovery purposes.
	 *
	 * @return version of component.
	 */
	public abstract String getComponentVersion();

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		final Map<String, Object> props = super.getDefaults(params);

		Map<String, Class<? extends Module>> modules = getDefaultModulesList();
		if (modules != null) {
			for (Entry<String, Class<? extends Module>> m : modules.entrySet()) {
				props.put("modules/" + m.getKey(), m.getValue());
			}
		}

		return props;
	}

	public Kernel getKernel() {
		return kernel;
	}

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	public ResponseManager getResponseManager() {
		return responseManager;
	}

	public void setResponseManager(ResponseManager responseManager) {
		this.responseManager = responseManager;
	}

	public StanzaProcessor getStanzaProcessor() {
		return stanzaProcessor;
	}

	public void setStanzaProcessor(StanzaProcessor stanzaProcessor) {
		this.stanzaProcessor = stanzaProcessor;
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds); // To change body of generated methods,

		// choose Tools | Templates.
		binds.put(COMPONENT, this);
	}

	/**
	 * Is this component discoverable by disco#items for domain by non admin users.
	 *
	 * @return <code>true</code> - if yes
	 */
	public abstract boolean isDiscoNonAdmin();

	@Override
	public void processPacket(Packet packet) {
		stanzaProcessor.processPacket(packet);
	}

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		try {
			super.setProperties(props);
		} catch (Exception e) {
			log.log(Level.WARNING, "Zjebalo sie", e);
		}
		try {
			initModules(props);
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't initialize modules!", e);
		}
	}

	@Override
	public void updateServiceEntity() {
		super.updateServiceEntity();
		this.updateServiceDiscoveryItem(getName(), null, getDiscoDescription(), !isDiscoNonAdmin());
	}

	EventBus getEventBus() {
		return eventBus;
	}

	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	protected void addOutPacket(Packet packet, AsyncCallback asyncCallback) {
		addOutPacket(packet);
	}

	/**
	 * Returns default map of components. Keys in map are used as component identifiers.<br>
	 * <br>
	 * This map may be modified by <code>config.tdsl</code>:<br> <code>&lt;component_name&gt;/modules/&lt;module_name&gt;[S]=&lt;module_class&gt;</code>
	 *
	 * @return map of default modules.
	 */
	protected abstract Map<String, Class<? extends Module>> getDefaultModulesList();

	protected void initModules(Map<String, Object> props)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (Entry<String, Object> e : props.entrySet()) {
			if (e.getKey().startsWith("modules/")) {
				final String id = e.getKey().substring(8);
				kernel.registerBean(id).asClass((Class<?>) e.getValue()).exec();
			}
		}
	}

	public static class DefaultPacketWriter
			implements PacketWriter {

		protected final Logger log = Logger.getLogger(this.getClass().getName());
		@Inject(bean = "service")
		private AbstractComponent component;
		@Inject
		private ResponseManager responseManager;

		public AbstractComponent getComponent() {
			return component;
		}

		public void setComponent(AbstractComponent component) {
			this.component = component;
		}

		public ResponseManager getResponseManager() {
			return responseManager;
		}

		public void setResponseManager(ResponseManager responseManager) {
			this.responseManager = responseManager;
		}

		@Override
		public void write(Collection<Packet> elements) {
			if (elements != null) {
				for (Packet element : elements) {
					if (element != null) {
						write(element);
					}
				}
			}
		}

		@Override
		public void write(Packet packet) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Sent: " + packet.getElement());
			}
			component.addOutPacket(packet);
		}

		@Override
		public void write(Packet packet, AsyncCallback callback) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Sent: " + packet.getElement());
			}
			responseManager.registerResponseHandler(packet, ResponseManager.DEFAULT_TIMEOUT, callback);
			component.addOutPacket(packet, callback);
		}

	}
}
