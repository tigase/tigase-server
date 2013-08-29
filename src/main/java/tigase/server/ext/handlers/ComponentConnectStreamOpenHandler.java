/*
 * ComponentConnectStreamOpenHandler.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

/**
 * Created: Oct 7, 2009 5:50:34 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ComponentConnectStreamOpenHandler
				implements StreamOpenHandler {
	/** Field description */
	public static final String XMLNS = "jabber:component:connect";

	//~--- fields ---------------------------------------------------------------

	private String[] xmlnss = new String[] { XMLNS };

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param s
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String serviceStarted(ComponentIOService s) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 * @param attribs
	 * @param handler
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String streamOpened(ComponentIOService serv, Map<String, String> attribs,
			ComponentProtocolHandler handler) {

		// Not sure if this is really used. For sure it is not well documented
		// and perhaps it is not worth implementing, unless someone requests it
		throw new UnsupportedOperationException("Not supported yet.");
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 */
	@Override
	public String[] getXMLNSs() {
		return xmlnss;
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
