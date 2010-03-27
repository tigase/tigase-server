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

package tigase.server.xmppsession;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;

//~--- interfaces -------------------------------------------------------------

/**
 * Describe interface SessionManagerHandler here.
 *
 *
 * Created: Sat Feb 18 13:27:58 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface SessionManagerHandler {

	/** Field description */
	public static final String COMMIT_HANDLER_KEY = "LoginHandlerKey";

	//~--- get methods ----------------------------------------------------------

	JID getComponentId();

	//~--- methods --------------------------------------------------------------

	void handleLogin(BareJID userId, XMPPResourceConnection conn);

	void handleLogout(BareJID userId, XMPPResourceConnection conn);

	void handlePresenceSet(XMPPResourceConnection conn);

	void handleResourceBind(XMPPResourceConnection conn);

	//~--- get methods ----------------------------------------------------------

	boolean isLocalDomain(String domain, boolean includeComponents);
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
