/*
 * TimingRecord.java
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

/**
 *
 * @author kobit
 */
public class TimingRecord {
	private String id;
	private long[] timings;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param id
	 * @param accuracy
	 */
	public TimingRecord(String id, int accuracy) {
		this.id = id;
		timings = new long[accuracy];
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	public void recordEnd() {
		throw new UnsupportedOperationException(
				"Not supported yet.");    // To change body of generated methods, choose Tools | Templates.
	}

	/**
	 * Method description
	 *
	 */
	public void recordStart() {
		throw new UnsupportedOperationException(
				"Not supported yet.");    // To change body of generated methods, choose Tools | Templates.
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public long getAverageTiming() {
		throw new UnsupportedOperationException(
				"Not supported yet.");    // To change body of generated methods, choose Tools | Templates.
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	long getMaxRecordedTiming() {
		throw new UnsupportedOperationException(
				"Not supported yet.");    // To change body of generated methods, choose Tools | Templates.
	}
}


//~ Formatted in Tigase Code Convention on 13/04/24
