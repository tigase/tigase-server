/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.map;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

class CollectionWrapper<E>
		implements Collection<E> {

	private final Collection<E> collection;

	CollectionWrapper(Collection<E> collection) {
		this.collection = collection;
	}

	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		collection.clear();
	}

	public boolean contains(Object o) {
		return collection.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return collection.containsAll(c);
	}

	@Override
	public boolean equals(Object o) {
		return collection.equals(o);
	}

	@Override
	public int hashCode() {
		return collection.hashCode();
	}

	public boolean isEmpty() {
		return collection.isEmpty();
	}

	public Iterator<E> iterator() {
		return new IteratorWrapper<>(collection.iterator());
	}

	public Stream<E> parallelStream() {
		return collection.parallelStream();
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean removeIf(Predicate<? super E> filter) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public int size() {
		return collection.size();
	}

	public Spliterator<E> spliterator() {
		return collection.spliterator();
	}

	public Stream<E> stream() {
		return collection.stream();
	}

	public <T> T[] toArray(T[] a) {
		return collection.toArray(a);
	}

	public Object[] toArray() {
		return collection.toArray();
	}

	@Override
	public String toString() {
		return collection.toString();
	}
}
