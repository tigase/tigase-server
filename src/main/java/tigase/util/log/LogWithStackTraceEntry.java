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
package tigase.util.log;

public class LogWithStackTraceEntry {

	private long counter = 0;
	private String msg = null;
	private String record = null;

	public LogWithStackTraceEntry(String msg, String record) {
		this.msg = msg;
		this.record = record;
	}

	public long getCounter() {
		return counter;
	}

	public String getMessage() {
		return msg;
	}

	public String getRecord() {
		return record;
	}

	public long increment() {
		return ++counter;
	}
}

