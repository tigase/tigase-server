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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import static tigase.util.StringUtilities.JUSTIFY.LEFT;
import static tigase.util.StringUtilities.JUSTIFY.RIGHT;
import static tigase.util.StringUtilities.padStringToColumn;

public class LogFormatter
		extends Formatter {

	public static final Map<Integer, LogWithStackTraceEntry> errors = new ConcurrentSkipListMap<>();
	final static DateFormat simple = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss:SSS");
	private static int DATE_TIME_LEN = 26;
	private static int LEVEL_OFFSET = 7;
	private static int METHOD_OFFSET = 37;
	private static int THREAD_OFFSET = 25;
	protected Date timestamp = new Date();

	public LogFormatter() {
	}

	@Override
	public synchronized String format(LogRecord record) {
		StringBuilder sb = new StringBuilder(200);

		timestamp.setTime(record.getMillis());
		sb.append('[').append(simple.format(timestamp)).append(']');
		padStringToColumn(sb, record.getLevel().toString(), LEFT, DATE_TIME_LEN + LEVEL_OFFSET, ' ', " [", "]");
		padStringToColumn(sb, Thread.currentThread().getName(), RIGHT, DATE_TIME_LEN + LEVEL_OFFSET + THREAD_OFFSET,
						  ' ', " [", " ]");
		padStringToColumn(sb, getClassMethodName(record), LEFT,
						  DATE_TIME_LEN + LEVEL_OFFSET + THREAD_OFFSET + METHOD_OFFSET, ' ', " ", ": ");
		sb.append(formatMessage(record));
		if (record.getThrown() != null) {
			final String stackTrace = fillThrowable(record);
			sb.append(stackTrace);
			addError(record.getThrown(), stackTrace, sb.toString());
		}
		return sb.append("\n").toString();
	}

	protected void addError(Throwable thrown, String stack, String log_msg) {
		errors.computeIfAbsent(stack.hashCode(), integer -> {
			String msg = thrown.getMessage();

			if (msg == null) {
				msg = thrown.toString();
			}
			return new LogWithStackTraceEntry(msg, log_msg);
		}).increment();
	}

	private String fillThrowable(LogRecord record) {
		if (record.getThrown() != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			pw.println();
			record.getThrown().printStackTrace(pw);
			pw.close();
			return sw.toString();
		} else {
			return null;
		}
	}

	private String getClassMethodName(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		if (record.getSourceClassName() != null) {
			String className = "";
			className = record.getSourceClassName();
			int idx = className.lastIndexOf('.');
			if (idx >= 0) {
				className = className.substring(idx + 1);
			}
			sb.append(className);
		}
		if (record.getSourceMethodName() != null) {
			sb.append(".").append(record.getSourceMethodName()).append("()");
		}
		return sb.toString();
	}
}

