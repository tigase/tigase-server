/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

package tigase.stats;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Optional;

/**
 * A queue implementation which stores highest added value on a given day
 * and has limited size.
 *
 * @param <E>
 */
public class MaxDailyCounterQueue<E extends Number & Comparable<E>>
		extends ArrayDeque<E> {

	private final int maxQueueLength;
	private LocalDate lastDailyStatsReset = LocalDate.now();
	private String toString = "[]";

	public MaxDailyCounterQueue(int maxQueueLength) {
		this.maxQueueLength = maxQueueLength;
	}

	@Override
	public boolean add(E added) {
		if (isNextItem() || this.peekLast() == null) {
			super.add(added);
		} else if (this.peekLast().compareTo(added) < 0) {
			this.removeLast();
			super.add(added);
		}
		while (size() > maxQueueLength) {
			super.remove();
		}
		toString = super.toString();
		return true;
	}

	public Optional<E> getMaxValue() {
		return getMaxValueInRange(maxQueueLength);
	}

	public Optional<E> getMaxValueInRange(int range) {
		range = Math.min(range, this.maxQueueLength);

		E result = null;
		final Iterator<E> iterator = this.descendingIterator();
		while (iterator.hasNext() && range > 0) {
			range--;

			final E next = iterator.next();
			if (result == null || next.compareTo(result) > 0) {
				result = next;
			}
		}
		return Optional.ofNullable(result);
	}

	/**
	 * Check if <b>any</b> item in the collection surpass the limit
	 *
	 * @param limit against which items should be check
	 *
	 * @return indicating whether <b>any</b> item in the collection surpass the limit
	 */
	public boolean isLimitSurpassed(E limit) {
		return isLimitSurpassed(this.maxQueueLength, limit);
	}

	/**
	 * Check if <b>any</b> item within range surpass the limit
	 *
	 * @param range number of items to check
	 * @param limit against which items should be check
	 *
	 * @return indicating whether <b>any</b> item within range surpass the limit
	 */
	public boolean isLimitSurpassed(int range, E limit) {
		return getMaxValueInRange(range).filter(e -> e.compareTo(limit) > 0).isPresent();
	}

	/**
	 * Check if all and every item in the collection surpass the limit
	 *
	 * @param limit against which items should be check
	 *
	 * @return indicating whether all items in the collection surpass the limit
	 */
	public boolean isLimitSurpassedAllItems(E limit) {
		return isLimitSurpassedAllItems(this.maxQueueLength, limit);
	}

	/**
	 * Check if all and every item within range surpass the limit
	 *
	 * @param range number of items to check
	 * @param limit against which items should be check
	 *
	 * @return indicating whether all items <b>within range</b> surpass the limit
	 */
	public boolean isLimitSurpassedAllItems(int range, E limit) {
		boolean result = true;
		range = Math.min(range, this.maxQueueLength);

		final Iterator<E> iter = this.descendingIterator();
		while (iter.hasNext() && range > 0) {
			range--;

			final E next = iter.next();
			if (next.compareTo(limit) <= 0) {
				result &= false;
			}
		}
		return result;
	}

	public ArrayDeque<E> subQueue(int range) {
		final ArrayDeque<E> result = new ArrayDeque<E>(range);
		range = Math.min(range, this.maxQueueLength);

		final Iterator<E> iter = this.descendingIterator();
		while (iter.hasNext() && range > 0) {
			range--;
			result.add(iter.next());
		}
		return result;
	}

	protected boolean isNextItem() {
		LocalDate now = LocalDate.now();
		if (now.getYear() != lastDailyStatsReset.getYear() ||
				now.getDayOfYear() != lastDailyStatsReset.getDayOfYear()) {
			lastDailyStatsReset = LocalDate.now();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return toString;
	}

//	private String toStringReversed() {
//		Iterator<E> it = descendingIterator();
//		if (!it.hasNext()) {
//			return "[]";
//		}
//
//		StringBuilder sb = new StringBuilder();
//		sb.append('[');
//		for (; ; ) {
//			E e = it.next();
//			sb.append(e == this ? "(this Collection)" : e);
//			if (!it.hasNext()) {
//				return sb.append(']').toString();
//			}
//			sb.append(',').append(' ');
//		}
//	}
}
