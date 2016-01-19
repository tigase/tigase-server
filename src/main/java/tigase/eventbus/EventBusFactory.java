package tigase.eventbus;

public class EventBusFactory {

	private final static EventBusImplementation eventBus = new EventBusImplementation();

	private EventBusFactory() {
	}

	public static EventBus getInstance() {
		return eventBus;
	}

}
