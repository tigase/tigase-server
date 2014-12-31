/*
 * LogFormatter.java
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

import java.util.Calendar;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.Map;

/**
 * Describe class LogFormatter here.
 *
 *
 * Created: Thu Jan  5 22:58:02 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class LogFormatter
				extends Formatter {
	/** Field description */
	public static final Map<Integer, LogWithStackTraceEntry> errors =
			new ConcurrentSkipListMap<Integer, LogWithStackTraceEntry>();
	private static int DATE_TIME_LEN = 24;
	private static int LEVEL_OFFSET  = 12;
	private static int MED_LEN       = 35;
	private static int TH_NAME_LEN   = 17;

	//~--- fields ---------------------------------------------------------------

	private Calendar cal = Calendar.getInstance();

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>LogFormatter</code> instance.
	 *
	 */
	public LogFormatter() {}

	//~--- methods --------------------------------------------------------------

	@Override
	public synchronized String format(LogRecord record) {
		StringBuilder sb = new StringBuilder(200);

		cal.setTimeInMillis(record.getMillis());
		sb.append(String.format("%1$tF %1$tT.%1$tL", cal));

		String th_name = Thread.currentThread().getName();

		sb.append(" [").append(th_name).append("]");
		while (sb.length() < DATE_TIME_LEN + TH_NAME_LEN) {
			sb.append(' ');
		}    // end of while (sb.length() < MEDIUM_LEN)
		if (record.getSourceClassName() != null) {
			String clsName = record.getSourceClassName();
			int    idx     = clsName.lastIndexOf('.');

			if (idx >= 0) {
				clsName = clsName.substring(idx + 1);
			}    // end of if (idx >= 0)
			sb.append("  ").append(clsName);
		}      // end of if (record.getSourceClassName() != null)
		if (record.getSourceMethodName() != null) {
			sb.append(".").append(record.getSourceMethodName()).append("()");
		}    // end of if (record.getSourceMethodName() != null)
		while (sb.length() < DATE_TIME_LEN + TH_NAME_LEN + MED_LEN) {
			sb.append(' ');
		}    // end of while (sb.length() < MEDIUM_LEN)
		sb.append("  ").append(record.getLevel()).append(": ");
		while (sb.length() < DATE_TIME_LEN + TH_NAME_LEN + MED_LEN + LEVEL_OFFSET) {
			sb.append(' ');
		}    // end of while (sb.length() < MEDIUM_LEN)
		sb.append(formatMessage(record));
		if (record.getThrown() != null) {
			sb.append('\n').append(record.getThrown().toString());

			StringBuilder st_sb = new StringBuilder(1024);

			getStackTrace(st_sb, record.getThrown());
			sb.append(st_sb.toString());
			addError(record.getThrown(), st_sb.toString(), sb.toString());
		}

		return sb.toString() + "\n";
	}

	private void addError(Throwable thrown, String stack, String log_msg) {
		Integer                code  = stack.hashCode();
		LogWithStackTraceEntry entry = errors.get(code);

		if (entry == null) {
			String msg = thrown.getMessage();

			if (msg == null) {
				msg = thrown.toString();
			}
			entry = new LogWithStackTraceEntry(msg, log_msg);
			errors.put(code, entry);
		}
		entry.increment();
	}

	//~--- get methods ----------------------------------------------------------

	private void getStackTrace(StringBuilder sb, Throwable th) {
		if (sb.length() > 0) {
			sb.append("\nCaused by: ").append(th.toString());
		}

		StackTraceElement[] stackTrace = th.getStackTrace();

		if ((stackTrace != null) && (stackTrace.length > 0)) {
			for (int i = 0; i < stackTrace.length; i++) {
				sb.append("\n\tat ").append(stackTrace[i].toString());
			}
		}

		Throwable cause = th.getCause();

		if (cause != null) {
			getStackTrace(sb, cause);
		}
	}
}    // LogFormatter


//~ Formatted in Tigase Code Convention on 13/05/27
