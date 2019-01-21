/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.server.xmppsession;

import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

/**
 * Describe interface SessionManagerHandler here.
 * <br>
 * Created: Sat Feb 18 13:27:58 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface SessionManagerHandler {

	public static final String COMMIT_HANDLER_KEY = "LoginHandlerKey";

	JID getComponentId();

	void handleLogin(BareJID userId, XMPPResourceConnection conn);

	void handleDomainChange(String domain, XMPPResourceConnection conn);

	void handleLogout(BareJID userId, XMPPResourceConnection conn);

	void handlePresenceSet(XMPPResourceConnection conn);

	void handleResourceBind(XMPPResourceConnection conn);

	boolean isLocalDomain(String domain, boolean includeComponents);
}
