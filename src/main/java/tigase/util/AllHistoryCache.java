/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
*
* $Rev: 2411 $
* Last modified by $Author: kobit $
* $Date: 2010-10-27 20:27:58 -0600 (Wed, 27 Oct 2010) $
 * 
 */
package tigase.util;

import tigase.stats.StatisticsList;

/**
 * @author Artur Hefczyc
 * Created May 28, 2011
 */
public class AllHistoryCache {

	private StatisticsList[] buffer = null;
	private int start = 0;
	private int count = 0;

	public AllHistoryCache(int limit) {
		buffer = new StatisticsList[limit];
	}

	public synchronized void addItem(StatisticsList item) {
		int ix = (start + count) % buffer.length;
		buffer[ix] = item;
		if (count < buffer.length) {
			count++;
		} else {
			start++;
			start %= buffer.length;
		}
	}

	public synchronized StatisticsList[] getCurrentHistory() {
		StatisticsList[] result = new StatisticsList[count];
		for (int i = 0; i < count; i++) {
			int ix = (start + i) % buffer.length;
			result[i] = buffer[ix];
		}
		return result;
	}


}
