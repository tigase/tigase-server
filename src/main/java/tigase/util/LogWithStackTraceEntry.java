/*
 * LogWithStackTraceEntry.java
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

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.LogRecord;

/**
 *
 */
public class LogWithStackTraceEntry {
	private long   counter = 0;
	private String msg     = null;
	private String record  = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param record
	 * @param msg
	 */
	public LogWithStackTraceEntry(String msg, String record) {
		this.msg    = msg;
		this.record = record;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long increment() {
		return ++counter;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCounter() {
		return counter;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of String
	 */
	public String getMessage() {
		return msg;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of String
	 */
	public String getRecord() {
		return record;
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
