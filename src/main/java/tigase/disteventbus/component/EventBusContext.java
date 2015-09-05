package tigase.disteventbus.component;

import java.util.Collection;

import tigase.component.Context;
import tigase.disteventbus.component.stores.AffiliationStore;
import tigase.disteventbus.component.stores.SubscriptionStore;
import tigase.disteventbus.impl.LocalEventBus;
import tigase.xmpp.JID;

public interface EventBusContext extends Context {

	AffiliationStore getAffiliationStore();

	Collection<JID> getConnectedNodes();

	LocalEventBus getEventBusInstance();

	SubscriptionStore getSubscriptionStore();
}
