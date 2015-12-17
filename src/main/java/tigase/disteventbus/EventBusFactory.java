package tigase.disteventbus;

public class EventBusFactory {

	private final static CombinedEventBus eventBus = new CombinedEventBus();

	private EventBusFactory() {
	}

	public static EventBus getInstance() {
		return eventBus;
	}

}
