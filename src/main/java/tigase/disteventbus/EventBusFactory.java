package tigase.disteventbus;

public class EventBusFactory {

	private final static LocalEventBus eventBus = new LocalEventBus();

	public static EventBus getInstance() {
		return eventBus;
	}

	private EventBusFactory() {
		// TODO Auto-generated constructor stub
	}

}
