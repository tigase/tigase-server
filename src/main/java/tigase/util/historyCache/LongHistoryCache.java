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
package tigase.util.historyCache;

/**
 * Created: Sep 8, 2009 7:39:27 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class LongHistoryCache {

	private long[] buffer = null;
	private int count = 0;
	private int start = 0;

	public LongHistoryCache(int limit) {
		buffer = new long[limit];
	}

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

	public synchronized long[] getCurrentHistory() {
		long[] result = new long[count];

		for (int i = 0; i < count; i++) {
			int ix = (start + i) % buffer.length;

			result[i] = buffer[ix];
		}

		return result;
	}
}

