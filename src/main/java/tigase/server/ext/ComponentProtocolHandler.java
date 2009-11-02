/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.ext;

import java.util.List;
import tigase.xml.Element;
import tigase.xmpp.XMPPIOService;

/**
 * Created: Oct 7, 2009 5:54:56 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ComponentProtocolHandler {

	public static final String REPO_ITEM_KEY = "repo-item";

	void authenticated(XMPPIOService<ComponentConnection> serv);

	CompRepoItem getCompRepoItem(String hostname);

	List<Element> getStreamFeatures(XMPPIOService<ComponentConnection> serv);

	ExtProcessor getProcessor(String string);

	StreamOpenHandler getStreamOpenHandler(String xmlns);

}
