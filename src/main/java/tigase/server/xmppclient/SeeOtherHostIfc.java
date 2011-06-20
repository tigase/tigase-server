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
 * @author Wojtek
 */
public interface SeeOtherHostIfc {
    public static final String CM_SEE_OTHER_HOST_CLASS_PROPERTY = "--cm-see-other-host";

    public static final String CM_SEE_OTHER_HOST_CLASS_PROP_KEY = "cm-see-other-host";

    public static final String CM_SEE_OTHER_HOST_CLASS_PROP_DEF_VAL = "tigase.server.xmppclient.SeeOtherHost";

    // default properties
    public static final String CM_SEE_OTHER_HOST_DEFAULT_HOST = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "defaul-host";
    public static final String CM_SEE_OTHER_HOST_DEFAULT_PORT = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "defaul-port";

    //~--- methods -------------------------------------------------------------
    /**
     * Finds an appropriate host for a given JID
     *
     * @param jid is a user JID extracted from the stream open attributes
     * @param host is "this" host to which the user is now connected and which calls the method
     * @return BareJID of possible host to which the user should connect or NULL
     */
    BareJID findHostForJID(BareJID jid, BareJID host);

    //~--- properties ----------------------------------------------------------
//    Map<String, Object> getDefaults(Map<String, Object> params);

    void getDefaults(Map<String, Object> defs, Map<String, Object> params);

    void setProperties(Map<String, Object> props);
}
