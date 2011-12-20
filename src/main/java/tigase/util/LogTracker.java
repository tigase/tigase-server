/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2011 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
 * $Rev: 2411 $
 * Last modified by $Author: kobit $
 * $Date: 2010-10-27 20:27:58 -0600 (Wed, 27 Oct 2010) $
 * 
 */
package tigase.util;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * @author kobit Created Dec 20, 2011
 */
public class LogTracker extends Handler {

	private LogFormatter formatter = new LogFormatter();
	private long lastFlush = 0;
	private long flushIntervals = 2000;
	private String[] trackers = null;
	private PrintWriter writer = null;

	public LogTracker(long flushIntervals, PrintWriter writer, String ... trackers) {
		super();
		this.flushIntervals = flushIntervals;
		this.writer = writer;
		this.trackers = trackers;
	}

	@Override
	public synchronized void publish(LogRecord record) {
		String msg = record.getMessage();
		if (msg != null) {
			boolean matchTracker = false;
			int i = 0;
			while (!(matchTracker = msg.contains(trackers[i++])) && i < trackers.length);
			if (matchTracker) {
				writer.print(formatter.format(record));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#close()
	 */
	@Override
	public void close() throws SecurityException {
		writer.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.logging.Handler#flush()
	 */
	@Override
	public void flush() {
		if (System.currentTimeMillis() - lastFlush > flushIntervals) {
			lastFlush = System.currentTimeMillis();
			writer.flush();
		}
	}

}
