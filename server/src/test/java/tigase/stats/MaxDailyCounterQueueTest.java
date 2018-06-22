/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2018 "Tigase, Inc." <office@tigase.com>
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

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class MaxDailyCounterQueueTest {

	@Test
	public void getValueOfEmptyQueue() {
		MaxDailyCounterQueue<Integer> lq = new MaxDailyCounterQueue<>(5);
		Assert.assertEquals(Optional.empty(), lq.getMaxValue());

		lq.add(5);
		Assert.assertNotEquals(Optional.empty(), lq.getMaxValue());
	}

	@Test
	public void getMaxValueInRange() {

		final int limit = 5;
		MaxDailyCounterQueue<Integer> lq = new MaxDailyCounterQueueEveryXItems<>(limit, 1);

		lq.add(1);
		Assert.assertEquals(1, lq.getMaxValueInRange(limit).get().intValue());

		lq.add(2);
		Assert.assertEquals(2, lq.getMaxValueInRange(limit).get().intValue());

		lq.add(4);
		Assert.assertEquals(4, lq.getMaxValueInRange(limit).get().intValue());

		lq.add(5);
		Assert.assertEquals(5, lq.getMaxValueInRange(limit).get().intValue());

		lq.add(6);
		Assert.assertEquals(6, lq.getMaxValueInRange(limit).get().intValue());

		lq.add(1);
		Assert.assertEquals(6, lq.getMaxValueInRange(3).get().intValue());

		Assert.assertEquals(1, lq.getMaxValueInRange(1).get().intValue());
		lq.add(1);
		lq.add(1);
		Assert.assertEquals(1, lq.getMaxValueInRange(3).get().intValue());

		MaxDailyCounterQueue<Integer> lq2 = new MaxDailyCounterQueueEveryXItems<>(1, 4);
		lq2.add(4);
		Assert.assertEquals(4, lq2.peek().intValue());
		lq2.add(3);
		Assert.assertEquals(4, lq2.peek().intValue());
		lq2.add(2);
		Assert.assertEquals(4, lq2.peek().intValue());
		lq2.add(1);
		Assert.assertEquals(4, lq2.peek().intValue());
		lq2.add(8);
		Assert.assertEquals(8, lq2.peek().intValue());
	}

	@Test
	public void isLimitSurpassed() {

		final int collectionSize = 5;
		final int limit = 5;
		MaxDailyCounterQueue<Integer> lq = new MaxDailyCounterQueueEveryXItems<>(collectionSize, 1);

		lq.add(1);
		Assert.assertFalse(lq.isLimitSurpassed(3, limit));

		lq.add(limit + 1);
		Assert.assertTrue(lq.isLimitSurpassed(3, limit));

		lq.add(1);
		Assert.assertTrue(lq.isLimitSurpassed(3, limit));

		lq.add(1);
		Assert.assertTrue(lq.isLimitSurpassed(3, limit));

		lq.add(1);
		Assert.assertFalse(lq.isLimitSurpassed(3, limit));
		Assert.assertTrue(lq.isLimitSurpassed(4, limit));
	}

	@Test
	public void isLimitSurpassedAllItems() {

		final int collectionSize = 5;
		final int limit = 5;
		MaxDailyCounterQueue<Integer> lq = new MaxDailyCounterQueueEveryXItems<>(collectionSize, 1);

		lq.add(1);
		Assert.assertFalse(lq.isLimitSurpassedAllItems(3, limit));

		lq.add(limit + 1);
		Assert.assertFalse(lq.isLimitSurpassedAllItems(3, limit));

		lq.add(limit + 1);
		Assert.assertFalse(lq.isLimitSurpassedAllItems(3, limit));

		lq.add(limit + 1);
		Assert.assertTrue(lq.isLimitSurpassedAllItems(3, limit));

		lq.add(1);
		Assert.assertFalse(lq.isLimitSurpassedAllItems(3, limit));

		lq.add(1);
		Assert.assertFalse(lq.isLimitSurpassedAllItems(3, limit));

	}

	class MaxDailyCounterQueueEveryXItems<E extends Number & Comparable<E>>
			extends MaxDailyCounterQueue<E> {

		private final int modulo;
		int i = 0;

		MaxDailyCounterQueueEveryXItems(int limit, int everyXItem) {
			super(limit);
			modulo = everyXItem;
		}

		@Override
		protected boolean isNextItem() {
			return (i++) % modulo == 0;
		}
	}
}