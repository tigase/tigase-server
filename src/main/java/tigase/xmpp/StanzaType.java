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
package tigase.xmpp;

/**
 * Describe class StanzaType here.
 *
 *
 * Created: Fri Feb 10 13:13:50 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public enum StanzaType {

	error,                                      // Common type
		get, set, result,                         // iq types
		available, unavailable, probe, subscribe, // presence types
		subscribed, unsubscribe, unsubscribed,    // presence types
		chat, groupchat, headline, normal,        // message types
		valid, invalid,                           // Dialback verification packets
    invisible;                                // Other unknown types...

	public static StanzaType valueof(String cmd) {
		try {
			return StanzaType.valueOf(cmd);
		} catch (IllegalArgumentException e) {
			return null;
		} // end of try-catch
	}

} // StanzaType