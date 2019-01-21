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
package tigase.db.util;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.logging.*;

/**
 * @author andrzej
 */
public class SchemaManagerLogHandler
		extends Handler {

	private final ArrayDeque<LogRecord> queue = new ArrayDeque<LogRecord>();

	private static Formatter simpleLogFormatter = new SchemaLogFormatter();

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

	public Optional<String> getMessage() {
		LogRecord rec;
		StringBuilder sb = null;
		while ((rec = poll()) != null) {
			if (rec.getLevel().intValue() <= Level.CONFIG.intValue()) {
				continue;
			}
			if (rec.getMessage() == null) {
				continue;
			}
			if (sb == null) {
				sb = new StringBuilder();
			} else {
				sb.append("\n");
			}
			sb.append(simpleLogFormatter.format(rec));
		}
		return (sb == null ? Optional.empty() : Optional.of(sb.toString()));
	}

	private static class SchemaLogFormatter extends Formatter {

		@Override
		public String format(LogRecord record) {
			return formatMessage(record);
		}
	}
}
