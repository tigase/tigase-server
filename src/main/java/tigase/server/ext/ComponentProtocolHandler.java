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

package tigase.server.ext;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

//~--- interfaces -------------------------------------------------------------

/**
 * Created: Oct 7, 2009 5:54:56 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ComponentProtocolHandler {

	/** Field description */
	public static final String REPO_ITEM_KEY = "repo-item";

//public static final String AUTHENTICATED_KEY = "authenticated";

	/** Field description */
	public static final String EXTCOMP_BIND_HOSTNAMES_PROP_KEY = "bind-ext-hostnames";

	//~--- methods --------------------------------------------------------------

	void authenticated(ComponentIOService serv);

	void authenticationFailed(ComponentIOService serv, Packet packet);

	void bindHostname(String hostname, ComponentIOService serv);

	//~--- get methods ----------------------------------------------------------

	CompRepoItem getCompRepoItem(String hostname);

	ExtProcessor getProcessor(String string);

	List<Element> getStreamFeatures(ComponentIOService serv);

	StreamOpenHandler getStreamOpenHandler(String xmlns);

	//~--- methods --------------------------------------------------------------

	String newPacketId(String prefix);

	void unbindHostname(String hostname, ComponentIOService serv);
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
