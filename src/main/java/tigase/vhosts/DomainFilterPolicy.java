/*
 * DomainFilterPolicy.java
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



/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.vhosts;

/**
 *
 * @author kobit
 */
public enum DomainFilterPolicy {
	ALL, LOCAL, OWN, BLOCK, LIST;

	private static String[] valuesStr = null;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param domains
	 *
	 * @return
	 */
	public static DomainFilterPolicy valueof(String domains) {
		if (domains == null) {
			return null;
		}
		try {
			return DomainFilterPolicy.valueOf(domains);
		} catch (Exception e) {
			return ALL;
		}    // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public static String[] valuesStr() {
		if (valuesStr == null) {
			DomainFilterPolicy[] vals = values();

			valuesStr = new String[vals.length];
			for (int i = 0; i < vals.length; i++) {
				valuesStr[i] = vals[i].name();
			}
		}

		return valuesStr;
	}
}


//~ Formatted in Tigase Code Convention on 13/03/16
