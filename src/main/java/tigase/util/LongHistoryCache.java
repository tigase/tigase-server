/*
 * LongHistoryCache.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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
 *
 */



package tigase.util;

/**
 * Created: Sep 8, 2009 7:39:27 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class LongHistoryCache {
	private long[] buffer = null;
	private int    count  = 0;
	private int    start  = 0;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param limit
	 */
	public LongHistoryCache(int limit) {
		buffer = new long[limit];
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param item is a <code>long</code>
	 */
	public synchronized void addItem(long item) {
		int ix = (start + count) % buffer.length;

		buffer[ix] = item;
		if (count < buffer.length) {
			count++;
		} else {
			start++;
			start %= buffer.length;
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>long[]</code>
	 */
	public synchronized long[] getCurrentHistory() {
		long[] result = new long[count];

		for (int i = 0; i < count; i++) {
			int ix = (start + i) % buffer.length;

			result[i] = buffer[ix];
		}

		return result;
	}
}


//~ Formatted in Tigase Code Convention on 13/11/29
