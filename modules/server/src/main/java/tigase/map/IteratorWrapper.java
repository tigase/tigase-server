package tigase.map;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Created by bmalkow on 04.12.2015.
 */
class IteratorWrapper<E> implements Iterator<E> {

	private final Iterator<E> iterator;

	IteratorWrapper(Iterator<E> iterator) {
		this.iterator = iterator;
	}

	public void forEachRemaining(Consumer<? super E> action) {
		iterator.forEachRemaining(action);
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	public E next() {
		return iterator.next();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
