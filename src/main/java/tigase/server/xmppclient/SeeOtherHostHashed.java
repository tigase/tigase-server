/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2011 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 * Last modified by $Author:$
 * $Date$
 *
 */
package tigase.server.xmppclient;

//~--- non-JDK imports --------------------------------------------------------

import tigase.util.TigaseStringprepException;
import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Default and basic implementation of SeeOtherHost returning same host as the
 * initial one
 *
 * @author Wojtek
 */
public class SeeOtherHostHashed implements SeeOtherHostIfc {

    BareJID defaulHost = null;
    private List<BareJID> connectedNodes = new CopyOnWriteArrayList<BareJID>();
    private static final Logger log = Logger.getLogger(SeeOtherHostHashed.class.getName());

    @Override
    public BareJID findHostForJID(BareJID jid, BareJID host) {
	return connectedNodes.get( Math.abs(jid.hashCode() ) % connectedNodes.size() );
    }

    @Override
    public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
    }

    @Override
    public void setProperties(Map<String, Object> props) {
    }

    @Override
    public void setNodes( List<BareJID> connectedNodes) {
	this.connectedNodes = connectedNodes;
    }
}
