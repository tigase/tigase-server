package tigase.disteventbus.component;

import java.util.Collection;

import tigase.component.Context;
import tigase.disteventbus.LocalEventBus;
import tigase.xmpp.JID;

public interface EventBusContext extends Context {

	Collection<String> getConnectedNodes();

	LocalEventBus getEventBusInstance();

	SubscriptionStore<NonClusterSubscription> getNonClusterSubscriptionStore();

	SubscriptionStore<JID> getSubscriptionStore();
}
