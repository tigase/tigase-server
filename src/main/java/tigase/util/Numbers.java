/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.util;

/**
 * Created: May 28, 2009 7:39:07 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Numbers {

	//public static char[] sizeChars = {'k', 'K', 'm', 'M', 'g', 'G', 't', 'T'};

	public static int parseSizeInt(String size, int def) {
		if (size == null) {
			return def;
		}
		int result = def;
		String toParse = size;
		int multiplier = 1;
		try {
			switch (size.charAt(size.length() - 1)) {
				case 'k':
				case 'K':
					multiplier = 1024;
					toParse = size.substring(0, size.length() - 1);
					break;
				case 'm':
				case 'M':
					multiplier = 1024 * 1024;
					toParse = size.substring(0, size.length() - 1);
					break;
				case 'g':
				case 'G':
					multiplier = 1024 * 1024 * 1024;
					toParse = size.substring(0, size.length() - 1);
					break;
			}
			result = Integer.parseInt(toParse) * multiplier;
		} catch (Exception e) {
			return def;
		}
		return result;
	}

}
