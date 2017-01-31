/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
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
 */

package tigase.vhosts.filter;

import java.util.ArrayList;
import java.util.HashSet;

/**
 *	Enumeration of all possible filtering modes
 */
public enum DomainFilterPolicy {

	/** user can communicate with anyone */
	ALL(false),

	/** user can communicate with other local users
	 * (i.e. of the domains hosted on the same Tigase instance) */
	LOCAL(false),

	/** user can communicate with other users of the same domain */
	OWN(false),

	/** user can communicate with users of the domains within listen domains */
	LIST(true),

	/** user can communicate with anyone except of the users within listed domains */
	BLACKLIST(true),

	/** Custom rules defining communication policies in CSV (using semicolon) in the
	 * format of {@code rule_number;(allow|deny);[type_of_value];[value]}
	 * where {@code type_of_value::(jid)}
	 * <pre>
	 * 1|allow|self;
	 * 2|allow|jid|admin@test2.com;
	 * 3|allow|jid|pubsub@test.com;
	 * 4|deny|all;
	 * </pre>
	 */
	CUSTOM(true),

	/** user can not communicate with anyone, account virtually disabled */
	BLOCK(false);

	boolean domainListRequired;

	DomainFilterPolicy(boolean domainListRequired) {
		this.domainListRequired = domainListRequired;
	}

	private static String[] valuesStr = null;
	private static HashSet<String> valuesDomainsListStr = null;


	/**
	 * Helper method returning proper defaults in case parsed value doesn't
	 * correspond to any of the available modes.
	 *
	 * @param domainFilteringPolicy policy name as string
	 *
	 * @return enum value of corresponding mode, ALL if value doesn't match any mode or null for null parameter.
	 */
	public static DomainFilterPolicy valueof( String domainFilteringPolicy ) {
		if ( domainFilteringPolicy == null ){
			return null;
		}
		try {
			return DomainFilterPolicy.valueOf( domainFilteringPolicy );
		} catch ( Exception e ) {
			return ALL;
		}    // end of try-catch
	}

	/**
	 * Helper method to generate string array with mode values
	 *
	 * @return String array with mode values
	 */
	public static String[] valuesStr() {
		if ( valuesStr == null ){
			DomainFilterPolicy[] vals = values();

			valuesStr = new String[ vals.length ];
			for ( int i = 0 ; i < vals.length ; i++ ) {
				valuesStr[i] = vals[i].name();
			}
		}

		return valuesStr;
	}

	public static HashSet<String> valuePoliciesWithDomainListStr() {
		if ( valuesDomainsListStr == null ){
			DomainFilterPolicy[] vals = values();

			valuesDomainsListStr = new HashSet<>();
			for ( DomainFilterPolicy val : vals ) {
				if ( val.isDomainListRequired() ){
					valuesDomainsListStr.add( val.name() );
				}
			}
		}

		return valuesDomainsListStr;
	}

	public boolean isDomainListRequired() {
		return domainListRequired;
	}

}
