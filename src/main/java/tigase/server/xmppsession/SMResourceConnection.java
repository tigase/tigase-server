/*
 * SMResourceConnection.java
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



package tigase.server.xmppsession;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.AuthRepository;
import tigase.db.UserRepository;

import tigase.util.TigaseStringprepException;

import tigase.vhosts.VHostItem;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Feb 27, 2010 8:02:11 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SMResourceConnection
				extends XMPPResourceConnection {
	/**
	 * Constructs ...
	 *
	 *
	 * @param connectionId
	 * @param rep
	 * @param authRepo
	 * @param loginHandler
	 */
	public SMResourceConnection(JID connectionId, UserRepository rep,
			AuthRepository authRepo, SessionManagerHandler loginHandler) {
		super(connectionId, rep, authRepo, loginHandler);
		try {
			setDomain(new VHostItem(loginHandler.getComponentId().getDomain())
					.getUnmodifiableVHostItem());
		} catch (TigaseStringprepException ex) {
			Logger.getLogger(SMResourceConnection.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Returns information whether this is a server (SessionManager) session or normal user
	 * session. The server session is used to handle packets addressed to the server itself
	 * (local domain name).
	 *  a <code>boolean</code> value of <code>true</code> if this is the server session
	 * and <code>false</code> otherwise.
	 *
	 * @return a value of boolean
	 */
	@Override
	public boolean isServerSession() {
		return true;
	}

	/**
	 * Method description
	 *
	 *
	 * @param bareJID
	 *
	 *
	 *
	 * @return a value of boolean
	 */
	@Override
	public boolean isUserId(BareJID bareJID) {
		return isLocalDomain(bareJID.toString(), false);
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
