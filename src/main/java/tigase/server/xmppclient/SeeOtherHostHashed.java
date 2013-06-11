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
 * Last modified by $Author:$
 * $Date$
 *
 */
package tigase.server.xmppclient;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Default implementation for cluster environment of SeeOtherHostIfc returning
 * redirect host based on the hash value of the user's JID
 *
 * @author Wojtek
 */
//public class SeeOtherHostHashed implements SeeOtherHostIfc {
public class SeeOtherHostHashed extends SeeOtherHost {

//	protected List<BareJID> defaultHost = null;
	protected List<BareJID> connectedNodes = new CopyOnWriteArrayList<BareJID>();
	private static final Logger log = Logger.getLogger(SeeOtherHostHashed.class.getName());

	@Override
	public BareJID findHostForJID(BareJID jid, BareJID host) {
		int hash = Math.abs(jid.hashCode());
		if (defaultHost !=null
			&& !defaultHost.isEmpty()
			&& connectedNodes.contains( defaultHost.get( hash % defaultHost.size() ) ) ) {
				return defaultHost.get( hash % defaultHost.size() );
		} else if (connectedNodes.size() > 0 ) {
			return connectedNodes.get( hash % connectedNodes.size());
		} else {
			return host;
		}
	}


	@Override
	public void setNodes(List<BareJID> connectedNodes) {
		this.connectedNodes = connectedNodes;
	}
}
