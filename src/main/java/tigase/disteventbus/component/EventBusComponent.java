package tigase.disteventbus.component;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.component.AbstractComponent;
import tigase.component.AbstractContext;
import tigase.component.modules.Module;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.component.modules.impl.DiscoveryModule;
import tigase.component.modules.impl.JabberVersionModule;
import tigase.component.modules.impl.XmppPingModule;
import tigase.disteventbus.EventBusFactory;
import tigase.disteventbus.LocalEventBus;
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
		public Collection<String> getConnectedNodes() {
			return connectedNodes;
		}

		@Override
		public LocalEventBus getEventBusInstance() {
			return eventBusInstance;
		}

		@Override
		public SubscriptionStore getNonClusterSubscriptionStore() {
			return nonClusterSubscriptionStore;
		}

		@Override
		public SubscriptionStore getSubscriptionStore() {
			return subscriptionStore;
		}
	}

	public static final String COMPONENT_EVENTS_XMLNS = "tigase:eventbus";

	private final Set<String> connectedNodes = new HashSet<String>();

	/**
	 * For non-cluster nodes. For example: standalone clients (Psi?) separated
	 * components, etc.
	 */
	private final SubscriptionStore<NonClusterSubscription> nonClusterSubscriptionStore = new SubscriptionStore<NonClusterSubscription>();

	/**
	 * For cluster nodes.
	 */
	private final SubscriptionStore<JID> subscriptionStore = new SubscriptionStore<JID>();

	public EventBusComponent() {
	}

	@Override
	protected EventBusContext createContext() {
		return new EventBusContextImpl(this);
	}

	@Override
	public synchronized void everyMinute() {
		super.everyMinute();

		Date t = new Date();
		Element event = new Element("Time", new String[] { "xmlns" }, new String[] { COMPONENT_EVENTS_XMLNS });
		event.addChild(new Element("time", "" + t.getTime()));
		event.addChild(new Element("timeDesc", t.toString()));

		context.getEventBus().fire(event);

	}

	@Override
	public synchronized void everySecond() {
		super.everySecond();

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
	public boolean isDiscoNonAdmin() {
		return false;
	}

	@Override
	public boolean isSubdomain() {
		return false;
	}

	@Override
	public void nodeConnected(String node) {
		connectedNodes.add(node);

		Module module = modulesManager.getModule(SubscribeModule.ID);
		if (module != null && module instanceof SubscribeModule) {
			((SubscribeModule) module).clusterNodeConnected(node);
		}

	}

	@Override
	public void nodeDisconnected(String node) {
		connectedNodes.remove(node);
		Module module = modulesManager.getModule(SubscribeModule.ID);
		if (module != null && module instanceof SubscribeModule) {
			((SubscribeModule) module).clusterNodeDisconnected(node);
		}
	}

	@Override
	public void processPacket(tigase.server.Packet packet) {
		System.out.println(getComponentId());
		super.processPacket(packet);
	}

	@Override
	public void setClusterController(ClusterControllerIfc cl_controller) {
	}

}
