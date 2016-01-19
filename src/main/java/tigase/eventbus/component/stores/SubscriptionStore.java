package tigase.eventbus.component.stores;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import tigase.eventbus.EventName;
import tigase.eventbus.EventsNameMap;
import tigase.kernel.beans.Bean;

@Bean(name = "subscriptionStore")
public class SubscriptionStore {

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	private final EventsNameMap<Subscription> subscribers = new EventsNameMap<Subscription>();

	public void addSubscription(String eventName, String eventPackage, Subscription subscription) {
		subscribers.put(eventName, eventPackage, subscription);
	}

	public Collection<Subscription> getAllData() {
		return subscribers.getAllData();
	}

	public Set<EventName> getSubscribedEvents() {
		return subscribers.getAllListenedEvents();
	}

	public Collection<Subscription> getSubscribersJIDs(String eventName, String eventPackage) {
		final HashSet<Subscription> handlers = new HashSet<Subscription>();
		handlers.addAll(subscribers.get(eventName, eventPackage));
		handlers.addAll(subscribers.get(null, eventPackage));
		return handlers;
	}

	public boolean hasSubscriber(String eventName, String eventPackage) {
		return subscribers.hasData(eventName, eventPackage);
	}

	public void remove(Subscription jid) {
		subscribers.delete(jid);
	}

	public void removeSubscription(String eventName, String eventPackage, Subscription jidInstanceNS) {
		subscribers.delete(eventName, eventPackage, jidInstanceNS);
	}

}
