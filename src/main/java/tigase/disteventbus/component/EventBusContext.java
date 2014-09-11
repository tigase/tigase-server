package tigase.disteventbus.component;

import java.util.Collection;

import tigase.component.Context;
import tigase.disteventbus.component.stores.AffiliationStore;
import tigase.disteventbus.component.stores.SubscriptionStore;
import tigase.disteventbus.impl.LocalEventBus;

public interface EventBusContext extends Context {

	AffiliationStore getAffiliationStore();

	Collection<String> getConnectedNodes();

	LocalEventBus getEventBusInstance();

	SubscriptionStore getSubscriptionStore();
}
