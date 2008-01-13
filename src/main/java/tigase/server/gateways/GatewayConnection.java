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

import java.util.List;
import tigase.server.Packet;

/**
 * Describe interface GatewayConnection here.
 *
 *
 * Created: Mon Nov 12 15:07:33 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface GatewayConnection {

	void setLogin(String username, String password);

	void setGatewayDomain(String domain);

	void addJid(String jid);

	void removeJid(String jid);

	String[] getAllJids();

	void setGatewayListener(GatewayListener listener);

	void init() throws GatewayException;

	void login() throws LoginGatewayException;

	void logout();

	void sendMessage(Packet message) throws GatewayException;

	void addBuddy(String id, String nick) throws GatewayException;

	void removeBuddy(String id) throws GatewayException;

	String getType();

	String getName();

	String getPromptMessage();

	List<RosterItem> getRoster();

}
