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
package tigase.server.amp;

/**
 * Created: Apr 28, 2010 5:38:24 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface AmpFeatureIfc {

	public static final String AMP_MSG_REPO_CLASS_PROP_KEY = "amp-repo-class";

	public static final String AMP_MSG_REPO_CLASS_PARAM = "--amp-repo-class";

	public static final String AMP_MSG_REPO_URI_PARAM = "--amp-repo-uri";

	public static final String AMP_MSG_REPO_URI_PROP_KEY = "amp-repo-uri";

	public static final String AMP_XMLNS = "http://jabber.org/protocol/amp";

	public static final String FROM_CONN_ID = "from-conn-id";

	public static final String SESSION_JID = "from-session-jid";

	public static final String TO_CONN_ID = "to-conn-id";

	public static final String TO_RES = "to-res";

	public static final String EXPIRED = "expired";

	public static final String OFFLINE = "offline";

	public static final String MSG_OFFLINE_PROP_KEY = "msg-offline";

	public static final String CONDITION_ATT = "condition";

	public static final String ACTION_ATT = "action";

	String getName();
}

