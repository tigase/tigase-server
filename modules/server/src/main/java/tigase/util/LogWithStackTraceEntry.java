/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.util;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.LogRecord;

//~--- classes ----------------------------------------------------------------

/**
 *
 */
public class LogWithStackTraceEntry {
	private long counter = 0;
	private String msg = null;
	private String record = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param record
	 * @param msg
	 */
	public LogWithStackTraceEntry(String msg, String record) {
		this.msg = msg;
		this.record = record;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public long getCounter() {
		return counter;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getMessage() {
		return msg;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getRecord() {
		return record;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public long increment() {
		return ++counter;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
