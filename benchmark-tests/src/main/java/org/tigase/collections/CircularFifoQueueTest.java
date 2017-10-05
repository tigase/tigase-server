/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

package org.tigase.collections;

import com.carrotsearch.sizeof.RamUsageEstimator;
import tigase.collections.CircularFifoQueue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Queue;

class CircularFifoQueueTest {

	public static void main(String[] args) {
		final int limit = 60 * 60 * 24;
		final Queue<Path> paths = new CircularFifoQueue<>(limit, System.out::println);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

		for (int i = 0; i < limit; i++) {
			final ZonedDateTime time = ZonedDateTime.now();
			final Path path = Paths.get(
					"logs/stats/stats_" + time.toInstant().toEpochMilli() + dateTimeFormatter.format(time) + ".txt");
//			if (i % 500 == 0 ) {
//				System.out.printf("[%1$05d] offering: %2$s \n", i, path);
//			}
			paths.offer(path);
		}

		final long bytes = RamUsageEstimator.sizeOf(paths);
		final String humanReadableUnits = RamUsageEstimator.humanReadableUnits(bytes);
		System.out.println(bytes);
		System.out.println(humanReadableUnits);

	}

}
