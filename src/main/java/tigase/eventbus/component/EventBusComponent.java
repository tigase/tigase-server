package tigase.eventbus.component;

import java.util.logging.Level;

import javax.script.ScriptEngineManager;

import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.component.AbstractKernelBasedComponent;
import tigase.component.modules.Module;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.component.modules.impl.JabberVersionModule;
import tigase.component.modules.impl.XmppPingModule;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.component.stores.Affiliation;
import tigase.eventbus.component.stores.AffiliationStore;
import tigase.eventbus.component.stores.SubscriptionStore;
import tigase.kernel.core.Kernel;
import tigase.stats.StatisticsList;
import tigase.xmpp.JID;

public class EventBusComponent extends AbstractKernelBasedComponent implements ClusteredComponentIfc {

	public EventBusComponent() {
	}

	// private final Map<String, ListenerScript> listenersScripts = new
	// ConcurrentHashMap<String, ListenerScript>();

	@Override
	public String getComponentVersion() {
		String version = this.getClass().getPackage().getImplementationVersion();
		return version == null ? "0.0.0" : version;
	}

	@Override
	public String getDiscoCategory() {
		return "pubsub";
	}

	@Override
	public String getDiscoCategoryType() {
		return "service";
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

		if (log.isLoggable(Level.FINE))
			log.fine("Cluster node " + jid + " added to Affiliation Store");
		kernel.getInstance(AffiliationStore.class).putAffiliation(jid, Affiliation.owner);

		Module module = kernel.getInstance(SubscribeModule.ID);
		if (module != null && module instanceof SubscribeModule) {
			((SubscribeModule) module).clusterNodeConnected(jid);
		}

	}

	@Override
	public void onNodeDisconnected(JID jid) {
		super.onNodeDisconnected(jid);

		Module module = kernel.getInstance(SubscribeModule.ID);
		if (module != null && module instanceof SubscribeModule) {
			((SubscribeModule) module).clusterNodeDisconnected(jid);
		}
		kernel.getInstance(AffiliationStore.class).removeAffiliation(jid);
	}

	@Override
	public void processPacket(tigase.server.Packet packet) {
		super.processPacket(packet);
	}

	@Override
	protected void registerModules(Kernel kernel) {
		kernel.registerBean(XmppPingModule.class).exec();
		kernel.registerBean(JabberVersionModule.class).exec();
		kernel.registerBean(AdHocCommandModule.class).exec();
		kernel.registerBean(EventbusDiscoveryModule.class).exec();

		// modules
		kernel.registerBean(SubscribeModule.class).exec();
		kernel.registerBean(UnsubscribeModule.class).exec();
		kernel.registerBean(EventReceiverModule.class).exec();
		kernel.registerBean(EventPublisherModule.class).exec();

		// beans
		// kernel.registerBean(ListenerScriptRegistrar.class).exec();
		kernel.registerBean("scriptEngineManager").asInstance(new ScriptEngineManager()).exec();
		kernel.registerBean(AffiliationStore.class).exec();
		kernel.registerBean("subscriptionStore").asClass(SubscriptionStore.class).exec();
		kernel.registerBean("localEventBus").asInstance(EventBusFactory.getInstance()).exec();

		// ad-hoc commands
		// kernel.registerBean(AddListenerScriptCommand.class).exec();
		// kernel.registerBean(RemoveListenerScriptCommand.class).exec();
	}

	@Override
	public void setClusterController(ClusterControllerIfc cl_controller) {
	}

}
