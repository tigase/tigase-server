/*
 * AbstractKernelBasedComponent.java
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

package tigase.component;

import javax.script.ScriptEngineManager;

import tigase.component.adhoc.AdHocCommandManager;
import tigase.component.modules.StanzaProcessor;
import tigase.component.modules.impl.config.ConfiguratorCommand;
import tigase.component.responses.AsyncCallback;
import tigase.component.responses.ResponseManager;
import tigase.conf.ConfigurationException;
import tigase.disco.XMPPService;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.Kernel;
import tigase.server.AbstractMessageReceiver;
import tigase.server.DisableDisco;
import tigase.server.Packet;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractKernelBasedComponent extends AbstractMessageReceiver implements XMPPService, DisableDisco {

	protected final Kernel kernel = new Kernel();
	/**
	 * Logger
	 */
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	protected final EventBus eventBus = EventBusFactory.getInstance();
	private StanzaProcessor stanzaProcessor;

	protected void changeRegisteredBeans(Map<String, Object> props) throws ConfigurationException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (Entry<String, Object> e : props.entrySet()) {
			if (e.getKey().startsWith("modules/")) {
				final String id = e.getKey().substring(8);
				kernel.registerBean(id).asClass((Class<?>) e.getValue()).exec();
			}
		}
	}

	@Override
	protected ScriptEngineManager createScriptEngineManager() {
		ScriptEngineManager result = super.createScriptEngineManager();
		result.setBindings(new BindingsKernel(kernel));
		return result;
	}

	public abstract String getComponentVersion();

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> result = super.getDefaults(params);
		if (kernel.isBeanClassRegistered(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)) {
			BeanConfigurator bc = kernel.getInstance(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME);
			if (bc instanceof PropertiesBeanConfigurator) {
				result.putAll(((PropertiesBeanConfigurator) bc).getCurrentConfigurations());
			}
		}

		return result;
	}

	public Kernel getKernel() {
		return this.kernel;
	}

	/**
	 * Is this component discoverable by disco#items for domain by non admin
	 * users.
	 *
	 * @return <code>true</code> - if yes
	 */
	public abstract boolean isDiscoNonAdmin();

	@Override
	public void processPacket(Packet packet) {
		stanzaProcessor.processPacket(packet);
	}

	protected abstract void registerModules(Kernel kernel);

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		if (props.size() <= 1)
			return;

		kernel.registerBean(DefaultTypesConverter.class).exec();
		kernel.registerBean("component").asInstance(this).exec();
		kernel.registerBean("adHocCommandManager").asClass(AdHocCommandManager.class).exec();
		kernel.registerBean("eventBus").asInstance(eventBus).exec();
		kernel.registerBean("eventBusRegistrar").asInstance(EventBusFactory.getRegistrar()).exec();
		kernel.registerBean("scriptCommandProcessor").asClass(ComponenScriptCommandProcessor.class).exec();
		kernel.registerBean("writer").asClass(DefaultPacketWriter.class).exec();
		kernel.registerBean("stanzaProcessor").asClass(StanzaProcessor.class).exec();
		kernel.registerBean("responseManager").asClass(ResponseManager.class).exec();
		kernel.registerBean(PropertiesBeanConfigurator.class).exec();
		kernel.registerBean(ConfiguratorCommand.class).exec();

		registerModules(kernel);

		PropertiesBeanConfigurator configurator = kernel.getInstance(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME);
		configurator.setProperties(props);

		super.setProperties(props);

		try {
			changeRegisteredBeans(props);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problem during modifing beans set", e);
		}

		this.stanzaProcessor = kernel.getInstance("stanzaProcessor");
	}

	@Override
	public void updateServiceEntity() {
		super.updateServiceEntity();
		this.updateServiceDiscoveryItem(getName(), null, getDiscoDescription(), !isDiscoNonAdmin());
	}

	@Bean(name = "writer")
	public static final class DefaultPacketWriter implements PacketWriter {

		protected final Logger log = Logger.getLogger(this.getClass().getName());
		@Inject(nullAllowed = false)
		private AbstractKernelBasedComponent component;
		@Inject(nullAllowed = false)
		private ResponseManager responseManager;

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
			component.addOutPacket(packet);
		}

	}

}
