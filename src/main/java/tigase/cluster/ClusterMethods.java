/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.cluster;

/**
 * Describe class ClusterMethods here.
 *
 *
 * Created: Mon Jun 16 21:17:03 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public enum ClusterMethods {

	// Put the most expected methods first to parse them quickly
	USER_CONNECTED, USER_DISCONNECTED, USER_INITIAL_PRESENCE,
	SYNC_ONLINE, OTHER,
	CHECK_DB_KEY
	// These should really be removed as they are not used anymore.
	//UPDATE_NODES,  SESSION_TRANSFER, PACKET_REDIRECT
	;

	public static ClusterMethods parseMethod(String method) {
		// For some reason below code is really slow
		// I am implementing less pretty but more efficient way then.
//		try {
//			return ClusterMethods.valueOf(method);
//		} catch (Exception e) {
//			return OTHER;
//		}

		for (ClusterMethods meth : values()) {
			// Code below assumes the 'method' is got from intern() so we can use
			// reference equality here.
			if (method == meth.name()) {
				return meth;
			}
		}
		return OTHER;
	}

}
