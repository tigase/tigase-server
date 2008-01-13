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
package tigase.server.gateways;

import tigase.server.Packet;

/**
 * Implementation of this class can listen for an XMPP packet received from
 * somewhere.
 *
 *
 * Created: Mon Nov 12 13:00:01 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface GatewayListener {

	void packetReceived(Packet packet);

	void logout(GatewayConnection gc);

	void loginCompleted(GatewayConnection gc);

	void gatewayException(GatewayConnection gc, Throwable exc);

	void userRoster(GatewayConnection gc);

	void updateStatus(GatewayConnection gc, RosterItem item);

	String formatJID(String legacyName);

	String decodeLegacyName(String jid);

}
