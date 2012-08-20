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

import java.util.ArrayList;
import java.util.Collections;
import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.util.TigaseStringprepException;

//~--- classes ----------------------------------------------------------------

/**
 * Default and basic implementation of SeeOtherHost returning same host as the
 * initial one
 *
 * @author Wojtek
 */
public class SeeOtherHostHashed implements SeeOtherHostIfc {

	protected List<BareJID> defaulHost = null;
	protected List<BareJID> connectedNodes = new CopyOnWriteArrayList<BareJID>();
	private static final Logger log = Logger.getLogger(SeeOtherHostHashed.class.getName());

	@Override
	public BareJID findHostForJID(BareJID jid, BareJID host) {
		int hash = Math.abs(jid.hashCode());
		if (defaulHost !=null
			&& !defaulHost.isEmpty()
			&& connectedNodes.contains( defaulHost.get( hash % defaulHost.size() ) ) ) {
				return defaulHost.get( hash % defaulHost.size() );
		} else if (connectedNodes.size() > 0 ) {
			return connectedNodes.get( hash % connectedNodes.size());
		} else {
			return host;
		}
	}

	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
	}

	@Override
	public void setProperties(final Map<String, Object> props) {
		if ((props.containsKey(SeeOtherHostIfc.CM_SEE_OTHER_HOST_DEFAULT_HOST))
			&& !props.get(SeeOtherHostIfc.CM_SEE_OTHER_HOST_DEFAULT_HOST).toString().trim().isEmpty()) {
			defaulHost = new ArrayList<BareJID>();
			for (String host : ((String) props.get(SeeOtherHostIfc.CM_SEE_OTHER_HOST_DEFAULT_HOST)).split(",")) {
				try {
					defaulHost.add(BareJID.bareJIDInstance(host));
				} catch (TigaseStringprepException ex) {
					log.log(Level.CONFIG, "From JID violates RFC6122 (XMPP:Address Format): ", ex);
				}
			}
			Collections.sort(defaulHost);
		} else {
			defaulHost = null;
		}
	}

	@Override
	public void setNodes(List<BareJID> connectedNodes) {
		this.connectedNodes = connectedNodes;
	}
}
