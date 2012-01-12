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

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import tigase.xmpp.JID;
import tigase.xmpp.XMPPSession;

/**
 * @author kobit Created Dec 20, 2011
 */
public class LogUserFilter implements Filter {

	private XMPPSession tracker = null;
	private String id = null;

	public LogUserFilter(String id, XMPPSession tracker) {
		this.id = id;
		this.tracker = tracker;
	}

	public String getId() {
		return id;
	}

	@Override
	public boolean isLoggable(LogRecord record) {
		boolean matchTracker = false;
		String msg = record.getMessage();
		if (msg != null) {
			JID[] trackers = tracker.getJIDs();
			if (trackers != null && trackers.length > 0) {
				int i = 0;
				while (!matchTracker && i < trackers.length) {
					matchTracker = msg.contains(trackers[i++].toString());
				}
			}
			if (!matchTracker) {
				trackers = tracker.getConnectionIds();
				if (trackers != null && trackers.length > 0) {
					int i = 0;
					while (!matchTracker && i < trackers.length) {
						matchTracker = msg.contains(trackers[i++].toString());
					}
				}
			}
		}
		return matchTracker;
	}

}
