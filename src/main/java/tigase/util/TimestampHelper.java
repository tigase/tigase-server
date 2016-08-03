/*
 * TimestampHelper.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author andrzej
 */
public class TimestampHelper {
	
	private final SimpleDateFormat TIMESTAMP_FORMATTER1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX");
	private final SimpleDateFormat TIMESTAMP_FORMATTER2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	private final SimpleDateFormat TIMESTAMP_FORMATTER3 = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSSXX");
	private final SimpleDateFormat TIMESTAMP_FORMATTER4 = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

	public TimestampHelper() {
		TIMESTAMP_FORMATTER1.setTimeZone(TimeZone.getTimeZone("UTC"));
		TIMESTAMP_FORMATTER2.setTimeZone(TimeZone.getTimeZone("UTC"));
		TIMESTAMP_FORMATTER3.setTimeZone(TimeZone.getTimeZone("UTC"));
		TIMESTAMP_FORMATTER4.setTimeZone(TimeZone.getTimeZone("UTC"));
	}	
	
	public Date parseTimestamp(String tmp) throws ParseException {
		if (tmp == null || tmp.isEmpty())
			return null;
		
		Date date = null;

		boolean useXXX = (tmp.charAt(tmp.length()-6) == '+');
		if (tmp.contains(".")) {
			if (useXXX) {
				synchronized (TIMESTAMP_FORMATTER4) {
					date = TIMESTAMP_FORMATTER4.parse(tmp);
				}
			} else {
				synchronized (TIMESTAMP_FORMATTER3) {
					date = TIMESTAMP_FORMATTER4.parse(tmp);
				}
			}
		} else {
			if (useXXX) {
				synchronized (TIMESTAMP_FORMATTER2) {
					date = TIMESTAMP_FORMATTER2.parse(tmp);
				}
			} else {
				synchronized (TIMESTAMP_FORMATTER1) {
					date =TIMESTAMP_FORMATTER1.parse(tmp);
				}
			}
		}

		return date;
	}	
	
	public String format(Date ts) {
		synchronized (TIMESTAMP_FORMATTER1) {
			return TIMESTAMP_FORMATTER1.format(ts);
		}
	}

}
