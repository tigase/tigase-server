package tigase.util;

import org.junit.Test;
import tigase.stats.StatisticsList;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by andrzej on 16.03.2016.
 */
public class AllHistoryCacheTest {

	private boolean highMemory = false;

	@Test
	public void testReductionOnHighMemoryUsage() throws NoSuchFieldException, IllegalAccessException {
		int limit = 20;
		List<StatisticsList> entries = new ArrayList<>();
		AllHistoryCache cache = new AllHistoryCache(limit, 95) {
			@Override
			public synchronized void addItem(StatisticsList item) {
				super.addItem(item);
				entries.add(item);
			}

			@Override
			protected boolean isHighMemoryUsage() {
				return highMemory;
			}
		};

		for (int i=0; i<limit + 5; i++) {
			StatisticsList stats = new StatisticsList(Level.FINE);
			cache.addItem(stats);
		}

		assertEquals(20, cache.getCurrentHistory().length);
		for (int i=0; i<5; i++) {
			entries.remove(0);
		}
		assertArrayEquals(entries.toArray(new StatisticsList[entries.size()]), cache.getCurrentHistory());

		highMemory = true;
		cache.addItem(new StatisticsList(Level.FINE));
		while (entries.size() != 10) {
			entries.remove(0);
		}
		assertEquals(10, cache.getCurrentHistory().length);
		assertArrayEquals(entries.toArray(new StatisticsList[entries.size()]), cache.getCurrentHistory());

		cache.addItem(new StatisticsList(Level.FINE));
		while (entries.size() != 5) {
			entries.remove(0);
		}
		assertEquals(5, cache.getCurrentHistory().length);
		assertArrayEquals(entries.toArray(new StatisticsList[entries.size()]), cache.getCurrentHistory());

		highMemory = false;
		cache.addItem(new StatisticsList(Level.FINE));
		assertEquals(6, cache.getCurrentHistory().length);
		assertArrayEquals(entries.toArray(new StatisticsList[entries.size()]), cache.getCurrentHistory());

		highMemory = true;
		cache.addItem(new StatisticsList(Level.FINE));
		while (entries.size() != 5) {
			entries.remove(0);
		}
		assertEquals(5, cache.getCurrentHistory().length);
		assertArrayEquals(entries.toArray(new StatisticsList[entries.size()]), cache.getCurrentHistory());

		Field f = AllHistoryCache.class.getDeclaredField("buffer");
		f.setAccessible(true);
		Object arr = f.get(cache);
		int count =0;
		for (int i=0; i<limit; i++) {
			if (Array.get(arr, i) != null)
				count++;
		}
		assertEquals(5, count);

	}
}
