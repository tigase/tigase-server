package tigase.disteventbus;

import tigase.disteventbus.impl.LocalEventBus;

public class EventBusFactory {

	private final static LocalEventBus eventBus = new LocalEventBus();

	private EventBusFactory() {
	}

	public static EventBus getInstance() {
		return eventBus;
	}

}
