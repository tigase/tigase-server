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
import java.util.Map;
import tigase.xmpp.XMPPIOService;

/**
 * Created: Oct 2, 2009 12:15:43 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface StreamOpenHandler {

	public static final String XMLNS_KEY = "xmlns";

	String[] getXMLNSs();

	String streamOpened(XMPPIOService<List<ComponentConnection>> s,
			Map<String, String> attribs, ComponentProtocolHandler handler);

	String serviceStarted(XMPPIOService<List<ComponentConnection>> s);

}
