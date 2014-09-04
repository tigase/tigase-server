package tigase.disteventbus.component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import tigase.disteventbus.EventsNameMap;
import tigase.disteventbus.LocalEventBus.EventName;

public class SubscriptionStore<TYPE> {

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	private final EventsNameMap<TYPE> subscribers = new EventsNameMap<TYPE>();

	public void addSubscription(String name, String xmlns, TYPE jid) {
		subscribers.put(name, xmlns, jid);
	}

	public Set<EventName> getSubscribedEvents() {
		return subscribers.getAllListenedEvents();
	}

	public Collection<TYPE> getSubscribersJIDs(String name, String xmlns) {
		final HashSet<TYPE> handlers = new HashSet<TYPE>();
		handlers.addAll(subscribers.get(name, xmlns));
		handlers.addAll(subscribers.get(null, xmlns));
		return handlers;
	}

	public boolean hasSubscriber(String eventName, String eventXmlns) {
		return subscribers.hasData(eventName, eventXmlns);
	}

	public void remove(TYPE jid) {
		subscribers.delete(jid);
	}

	public void removeSubscription(String eventName, String eventXmlns, TYPE jidInstanceNS) {
		subscribers.delete(eventName, eventXmlns, jidInstanceNS);
	}

}
