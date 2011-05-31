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

import java.util.Map;
import tigase.xmpp.BareJID;

/**
 * Default and basic implementation of SeeOtherHost returning same host as the
 * initial one
 * @author Wojtek
 */
public class SeeOtherHost implements SeeOtherHostIfc {

    @Override
    public BareJID findHostForJID(BareJID jid, BareJID host) {
	return host;
    }

    @Override
    public void getDefaults(Map<String, Object> defs,
			    Map<String, Object> params) {
    }
    @Override
    public void setProperties(Map<String, Object> props) {
    }


}
