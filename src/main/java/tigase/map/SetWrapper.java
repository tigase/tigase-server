package tigase.map;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;

class SetWrapper<E> implements Set<E> {

	private final Set<E> set;

	SetWrapper(Set<E> set) {
		this.set = set;
	}

	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean contains(Object o) {
		return set.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	@Override
	public boolean equals(Object o) {
		return set.equals(o);
	}

	@Override
	public int hashCode() {
		return set.hashCode();
	}

	public boolean isEmpty() {
		return set.isEmpty();
	}

	public Iterator<E> iterator() {
		return new IteratorWrapper<>(set.iterator());
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
		return set.retainAll(c);
	}

	public int size() {
		return set.size();
	}

	public Spliterator<E> spliterator() {
		return set.spliterator();
	}

	public <T> T[] toArray(T[] a) {
		return set.toArray(a);
	}

	public Object[] toArray() {
		return set.toArray();
	}

	@Override
	public String toString() {
		return set.toString();
	}
}
