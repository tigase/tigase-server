/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2013 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev: $
 * Last modified by $Author: $
 * $Date: $
 */
package tigase.util;

import java.util.concurrent.ScheduledFuture;

/**
 * TimerTask class is basic implementation of java.util.TimerTask class which
 * is used with ScheduledExecutorService in AbstractMessageRecevier.
 */
public abstract class TimerTask implements Runnable {

	private ScheduledFuture<?> future = null;
	
	public void setScheduledFuture(ScheduledFuture<?> future) {
		this.future = future;
	}

	public boolean isScheduled() {
		return future != null && !future.isCancelled() && !future.isDone();
	}
	
	public void cancel() {
		if (future != null) {
			future.cancel(false);
		}
	}
	
}
