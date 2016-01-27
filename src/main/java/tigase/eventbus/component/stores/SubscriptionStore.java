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

	public void addSubscription(String eventPackage, String eventName, Subscription subscription) {
		subscribers.put(eventPackage, eventName, subscription);
	}

	public Collection<Subscription> getAllData() {
		return subscribers.getAllData();
	}

	public Set<EventName> getSubscribedEvents() {
		return subscribers.getAllListenedEvents();
	}

	public Collection<Subscription> getSubscribersJIDs(String eventPackage, String eventName) {
		final HashSet<Subscription> handlers = new HashSet<Subscription>();
		handlers.addAll(subscribers.get(eventPackage, eventName));
		handlers.addAll(subscribers.get(eventPackage, null));
		return handlers;
	}

	public boolean hasSubscriber(String eventPackage, String eventName) {
		return subscribers.hasData(eventPackage, eventName);
	}

	public void remove(Subscription jid) {
		subscribers.delete(jid);
	}

	public void removeSubscription(String eventPackage, String eventName, Subscription jidInstanceNS) {
		subscribers.delete(eventPackage, eventName, jidInstanceNS);
	}

}
