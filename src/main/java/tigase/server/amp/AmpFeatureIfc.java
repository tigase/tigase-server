
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
package tigase.server.amp;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- interfaces -------------------------------------------------------------

/**
 * Created: Apr 28, 2010 5:38:24 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface AmpFeatureIfc {

	/** Field description */
	public static final String AMP_MSG_REPO_CLASS_PROP_KEY = "amp-repo-class";
	
	/** Field description */
	public static final String AMP_MSG_REPO_CLASS_PARAM = "--amp-repo-class";
	
	/** Field description */
	public static final String AMP_MSG_REPO_URI_PARAM = "--amp-repo-uri";

	/** Field description */
	public static final String AMP_MSG_REPO_URI_PROP_KEY = "amp-repo-uri";

	/** Field description */
	public static final String AMP_XMLNS = "http://jabber.org/protocol/amp";
	
	/** Field description */
	public static final String FROM_CONN_ID = "from-conn-id";

	public static final String SESSION_JID = "from-session-jid";

	/** Field description */
	public static final String TO_CONN_ID = "to-conn-id";

	/** Field description */
	public static final String TO_RES = "to-res";

	/** Field description */
	public static final String EXPIRED = "expired";

	/** Field description */
	public static final String OFFLINE = "offline";

	/** Field description */
	public static final String MSG_OFFLINE_PROP_KEY = "msg-offline";

	/** Field description */
	public static final String CONDITION_ATT = "condition";

	/** Field description */
	public static final String ACTION_ATT = "action";

	//~--- get methods ----------------------------------------------------------

	String getName();
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
