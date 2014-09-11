package tigase.disteventbus.component.stores;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import tigase.disteventbus.impl.EventName;
import tigase.disteventbus.impl.EventsNameMap;

public class SubscriptionStore {

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	private final EventsNameMap<Subscription> subscribers = new EventsNameMap<Subscription>();

	public void addSubscription(String name, String xmlns, Subscription subscription) {
		subscribers.put(name, xmlns, subscription);
	}

	public Collection<Subscription> getAllData() {
		return subscribers.getAllData();
	}

	public Set<EventName> getSubscribedEvents() {
		return subscribers.getAllListenedEvents();
	}

	public Collection<Subscription> getSubscribersJIDs(String name, String xmlns) {
		final HashSet<Subscription> handlers = new HashSet<Subscription>();
		handlers.addAll(subscribers.get(name, xmlns));
		handlers.addAll(subscribers.get(null, xmlns));
		return handlers;
	}

	public boolean hasSubscriber(String eventName, String eventXmlns) {
		return subscribers.hasData(eventName, eventXmlns);
	}

	public void remove(Subscription jid) {
		subscribers.delete(jid);
	}

	public void removeSubscription(String eventName, String eventXmlns, Subscription jidInstanceNS) {
		subscribers.delete(eventName, eventXmlns, jidInstanceNS);
	}

}
