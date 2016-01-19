package tigase.eventbus;

public interface EventListener<E> {

	void onEvent(E event);

}
