package tigase.eventbus;

public abstract class AbstractListenerHandler<T> extends AbstractHandler {

	protected final T listener;

	protected AbstractListenerHandler(final String packageName, final String eventName, T listener) {
		super(packageName, eventName);
		this.listener = listener;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || !(o instanceof AbstractListenerHandler))
			return false;

		AbstractListenerHandler that = (AbstractListenerHandler) o;

		return listener.equals(that.listener);

	}

	public T getListener() {
		return listener;
	}

	@Override
	public int hashCode() {
		return listener.hashCode();
	}

}
