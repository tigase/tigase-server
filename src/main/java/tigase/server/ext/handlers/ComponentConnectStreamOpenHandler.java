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

package tigase.server.ext.handlers;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.ext.ComponentConnection;
import tigase.server.ext.ComponentIOService;
import tigase.server.ext.ComponentProtocolHandler;
import tigase.server.ext.StreamOpenHandler;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Oct 7, 2009 5:50:34 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ComponentConnectStreamOpenHandler implements StreamOpenHandler {

	/** Field description */
	public static final String XMLNS = "jabber:component:connect";

	//~--- fields ---------------------------------------------------------------

	private String[] xmlnss = new String[] { XMLNS };

	//~--- get methods ----------------------------------------------------------

	@Override
	public String[] getXMLNSs() {
		return xmlnss;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public String serviceStarted(ComponentIOService s) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String streamOpened(ComponentIOService serv, Map<String, String> attribs,
			ComponentProtocolHandler handler) {

		// Not sure if this is really used. For sure it is not well documented
		// and perhaps it is not worth implementing, unless someone requests it
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
