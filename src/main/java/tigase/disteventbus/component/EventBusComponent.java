package tigase.disteventbus.component;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import javax.script.ScriptEngineManager;

import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.component.AbstractComponent;
import tigase.component.AbstractContext;
import tigase.component.modules.Module;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.component.modules.impl.DiscoveryModule;
import tigase.component.modules.impl.JabberVersionModule;
import tigase.component.modules.impl.XmppPingModule;
import tigase.conf.ConfigurationException;
import tigase.disteventbus.EventBusFactory;
import tigase.disteventbus.component.stores.AffiliationStore;
import tigase.disteventbus.component.stores.SubscriptionStore;
import tigase.disteventbus.impl.LocalEventBus;
import tigase.stats.StatisticsList;
import tigase.xml.Element;
import tigase.xmpp.JID;

public class EventBusComponent extends AbstractComponent<EventBusContext> implements ClusteredComponentIfc {

	private class EventBusContextImpl extends AbstractContext implements EventBusContext {

		private final LocalEventBus eventBusInstance;

		public EventBusContextImpl(AbstractComponent<?> component) {
			super(component);
			this.eventBusInstance = (LocalEventBus) EventBusFactory.getInstance();
		}

		@Override
		public AffiliationStore getAffiliationStore() {
			return affiliationStore;
		}

		@Override
		public Collection<JID> getConnectedNodes() {
			return Collections.unmodifiableCollection(getConnectedNodes());
		}

		@Override
		public LocalEventBus getEventBusInstance() {
			return eventBusInstance;
		}

		@Override
		public SubscriptionStore getSubscriptionStore() {
			return subscriptionStore;
		}
	}

	public static final String COMPONENT_EVENTS_XMLNS = "tigase:eventbus";

	private static long counter = 0;

	private final AffiliationStore affiliationStore = new AffiliationStore();

	private final Map<String, ListenerScript> listenersScripts = new ConcurrentHashMap<String, ListenerScript>();

	private ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

	private ListenerScriptRegistrar scriptsRegistrar;

	/**
	 * For cluster nodes.
	 */
	private final SubscriptionStore subscriptionStore = new SubscriptionStore();

	public EventBusComponent() {
	}

	@Override
	protected EventBusContext createContext() {
		return new EventBusContextImpl(this);
	}

	@Override
	public String getComponentVersion() {
		String version = this.getClass().getPackage().getImplementationVersion();
		return version == null ? "0.0.0" : version;
	}

	@Override
	protected Map<String, Class<? extends Module>> getDefaultModulesList() {
		final Map<String, Class<? extends Module>> result = new HashMap<String, Class<? extends Module>>();

		result.put(SubscribeModule.ID, SubscribeModule.class);
		result.put(UnsubscribeModule.ID, UnsubscribeModule.class);
		result.put(EventReceiverModule.ID, EventReceiverModule.class);
		result.put(EventPublisherModule.ID, EventPublisherModule.class);

		result.put(XmppPingModule.ID, XmppPingModule.class);
		result.put(JabberVersionModule.ID, JabberVersionModule.class);
		result.put(AdHocCommandModule.ID, AdHocCommandModule.class);
		result.put(DiscoveryModule.ID, DiscoveryModule.class);

		return result;
	}

	@Override
	public String getDiscoCategory() {
		return "component";
	}

	@Override
	public String getDiscoCategoryType() {
		return "generic";
	}

	@Override
	public String getDiscoDescription() {
		return "Distributed EventBus";
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
	}

	@Override
	public boolean isDiscoNonAdmin() {
		return false;
	}

	@Override
	public boolean isSubdomain() {
		return false;
	}

	@Override
	protected void onNodeConnected(JID jid) {
		super.onNodeConnected(jid);

		Module module = modulesManager.getModule(SubscribeModule.ID);
		if (module != null && module instanceof SubscribeModule) {
			((SubscribeModule) module).clusterNodeConnected(jid);
		}
	}

	@Override
	public void onNodeDisconnected(JID jid) {
		super.onNodeDisconnected(jid);

		Module module = modulesManager.getModule(SubscribeModule.ID);
		if (module != null && module instanceof SubscribeModule) {
			((SubscribeModule) module).clusterNodeDisconnected(jid);
		}
	}

	@Override
	public void processPacket(tigase.server.Packet packet) {
		super.processPacket(packet);
	}

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		super.setProperties(props);

		scriptsRegistrar = new ListenerScriptRegistrar(listenersScripts, context, scriptEngineManager);

		AdHocCommandModule<?> adHocCommandModule = getModuleProvider().getModule(AdHocCommandModule.ID);

		adHocCommandModule.register(new AddListenerScriptCommand(scriptEngineManager, scriptsRegistrar));
		adHocCommandModule.register(new RemoveListenerScriptCommand(listenersScripts, scriptsRegistrar));

		scriptsRegistrar.load();
	}

}
