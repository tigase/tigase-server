/*
 * TimingRecorder.java
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



/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.util;

//~--- JDK imports ------------------------------------------------------------

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Map;

/**
 *
 * @author kobit
 */
public class TimingRecorder {
	/** Field description */
	public static final int ACCURACY_PROP_DEF = 10;

	/** Field description */
	public static final String                     ACCURACY_PROP_KEY = "timing-accuracy";
	private static final Map<String, TimingRecord> timings =
			new ConcurrentSkipListMap<String, TimingRecord>();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param id
	 */
	public static void endTiming(String id) {
		timings.get(id).recordEnd();
	}

	/**
	 * Method description
	 *
	 *
	 * @param id
	 * @param accuracy
	 */
	public static void initTiming(String id, int accuracy) {
		timings.put(id, new TimingRecord(id, accuracy));
	}

	/**
	 * Method description
	 *
	 *
	 * @param id
	 */
	public static void startTiming(String id) {
		timings.get(id).recordStart();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param id
	 *
	 * 
	 */
	public static long getAverageTiming(String id) {
		return timings.get(id).getAverageTiming();
	}

	/**
	 * Method description
	 *
	 *
	 * @param id
	 *
	 * 
	 */
	public static long getMaxRecordedTiming(String id) {
		return timings.get(id).getMaxRecordedTiming();
	}
}


//~ Formatted in Tigase Code Convention on 13/04/24
