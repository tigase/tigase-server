
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
*
* $Rev$
* Last modified by $Author$
* $Date$
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

//~--- classes ----------------------------------------------------------------

/**
 * Created: Feb 27, 2010 8:02:11 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SMResourceConnection extends XMPPResourceConnection {

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
			setDomain(new VHostItem(loginHandler.getComponentId().getDomain()).getUnmodifiableVHostItem());
		} catch (TigaseStringprepException ex) {
			Logger.getLogger(SMResourceConnection.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public boolean isServerSession() {
		return true;
	}

	@Override
	public boolean isUserId(BareJID bareJID) {
		return isLocalDomain(bareJID.toString(), false);
	}
}
