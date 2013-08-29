/*
 * TimeUtils.java
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

/**
 * This is too slow.
 *
 *
 * Created: Tue Oct 28 21:08:58 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class TimeUtils {
	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int
	 */
	public static int getHourNow() {
		Calendar cal = Calendar.getInstance();

		return cal.get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int
	 */
	public static int getMinuteNow() {
		Calendar cal = Calendar.getInstance();

		return cal.get(Calendar.MINUTE);
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
