package tigase.eventbus;

public interface EventSourceListener<E> {

	void onEvent(E event, Object source);

}
