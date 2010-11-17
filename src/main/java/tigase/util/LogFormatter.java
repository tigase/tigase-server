/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.util;

//~--- JDK imports ------------------------------------------------------------

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class LogFormatter here.
 *
 *
 * Created: Thu Jan  5 22:58:02 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class LogFormatter extends Formatter {

	/** Field description */
	public static final Map<Integer, LogWithStackTraceEntry> errors =
		new ConcurrentSkipListMap<Integer, LogWithStackTraceEntry>();
	private static int MED_LEN = 55;
	private static int LEVEL_OFFSET = 12;

	//~--- fields ---------------------------------------------------------------

	private Calendar cal = Calendar.getInstance();

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>LogFormatter</code> instance.
	 *
	 */
	public LogFormatter() {}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param record
	 *
	 * @return
	 */
	@Override
	public synchronized String format(LogRecord record) {
		StringBuilder sb = new StringBuilder(100);

		cal.setTimeInMillis(record.getMillis());
		sb.append(String.format("%1$tF %1$tT", cal));

		if (record.getSourceClassName() != null) {
			String clsName = record.getSourceClassName();
			int idx = clsName.lastIndexOf('.');

			if (idx >= 0) {
				clsName = clsName.substring(idx + 1);
			}    // end of if (idx >= 0)

			sb.append("  ").append(clsName);
		}      // end of if (record.getSourceClassName() != null)

		if (record.getSourceMethodName() != null) {
			sb.append(".").append(record.getSourceMethodName()).append("()");
		}    // end of if (record.getSourceMethodName() != null)

		while (sb.length() < MED_LEN) {
			sb.append(' ');
		}    // end of while (sb.length() < MEDIUM_LEN)

		sb.append("  ").append(record.getLevel()).append(": ");

		while (sb.length() < MED_LEN + LEVEL_OFFSET) {
			sb.append(' ');
		}    // end of while (sb.length() < MEDIUM_LEN)

		sb.append(formatMessage(record));

		if (record.getThrown() != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);

			record.getThrown().printStackTrace(pw);

			StackTraceElement[] stackTrace = record.getThrown().getStackTrace();

			// Do not put the first line of the stack trace to the 'stack' used for
			// hashcode calculation because sometimes the same stack contains different
			// message and we do not want it to be used for the hashcode calculation
			// to make sure we do not have memory leak here.
			sb.append('\n').append(record.getThrown().toString());

			if ((stackTrace.length > 0) || (record.getThrown().getCause() != null)) {
				StringBuilder st_sb = new StringBuilder(1024);

				getStackTrace(st_sb, record.getThrown());
				sb.append(st_sb.toString());
				addError(record.getThrown(), st_sb.toString(), sb.toString());
			} else {
				addError(record.getThrown(), "", sb.toString());
			}
		}

		return sb.toString() + "\n";
	}

	private void addError(Throwable thrown, String stack, String log_msg) {
		Integer code = stack.hashCode();
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


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
