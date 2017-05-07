/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017, "Tigase, Inc." <office@tigase.com>
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
package tigase.db.util;

import java.util.ArrayDeque;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 *
 * @author andrzej
 */
public class SchemaManagerLogHandler
		extends Handler {

	private final ArrayDeque<LogRecord> queue = new ArrayDeque<LogRecord>();
	
	public SchemaManagerLogHandler() {
	}
	
	@Override
	public void publish(LogRecord record) {
		queue.offer(record);
	}

	@Override
	public void flush() {
		queue.clear();
	}

	@Override
	public void close() throws SecurityException {
		flush();
	}
	
	public LogRecord poll() {
		return queue.poll();
	}
	
}
