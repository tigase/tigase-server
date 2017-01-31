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
import java.util.concurrent.ThreadLocalRandom;

/**
 * A queue implementation which stores highest added value on a given day
 * and has limited size.
 *
 * @param <E>
 */
public class MaxDailyCounterQueue<E extends Number & Comparable<E>>
		extends ArrayDeque<E> {

	private final int limit;
	private LocalDate lastDailyStatsReset = LocalDate.now();
	private String toString = "[]";

	public static void main(String[] args) {

		MaxDailyCounterQueue<Integer> lq = new MaxDailyCounterQueue<Integer>(15) {
			int i = 0;

			@Override
			protected boolean isNextItem() {
				return (i++) % 4 == 0;
			}
		};

		for (int i = 0; i < 200; i++) {
			final int rand = ThreadLocalRandom.current().nextInt(0, 100);
			lq.add(rand);
			System.out.print(lq.toString());
			System.out.print("       max: " + lq.getMaxValueInRange(3));
			System.out.print("       sup-limit: " + lq.isLimitSurpassedAllItems(3, 50));
			System.out.println();
		}
	}

	public MaxDailyCounterQueue(int limit) {
		this.limit = limit;
	}

	@Override
	public boolean add(E added) {
		if (isNextItem() || this.peekLast() == null) {
			super.add(added);
		} else if (this.peekLast().compareTo(added) < 0) {
			this.removeLast();
			super.add(added);
		}
		while (size() > limit) {
			super.remove();
		}
		toString = super.toString();
		return true;
	}

	public E getMaxValueInRange(int range) {
		range = Math.min(range, this.limit);

		E result = null;
		final Iterator<E> iterator = this.descendingIterator();
		while (iterator.hasNext() && range > 0) {
			range--;

			final E next = iterator.next();
			if (result == null || next.compareTo(result) > 0) {
				result = next;
			}
		}
		return result;
	}

	public boolean isLimitSurpassedAllItems(int range, long limit) {
		boolean result = true;
		range = Math.min(range, this.limit);

		final Iterator<E> iter = this.descendingIterator();
		while (iter.hasNext() && range > 0) {
			range--;

			final E next = iter.next();
			if (next.longValue() <= limit) {
				result &= false;
			}
		}
		return result;
	}

	public ArrayDeque<E> subQueue(int range) {
		final ArrayDeque<E> result = new ArrayDeque<E>(range);
		range = Math.min(range, this.limit);

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
