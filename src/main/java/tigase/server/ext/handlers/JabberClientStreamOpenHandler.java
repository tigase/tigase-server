/*
 * JabberClientStreamOpenHandler.java
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
import tigase.server.ext.CompRepoItem;
import tigase.server.ext.StreamOpenHandler;

import tigase.xml.Element;

import static tigase.server.ext.ComponentProtocolHandler.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.UUID;

/**
 * Created: Oct 7, 2009 3:17:09 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberClientStreamOpenHandler
				implements StreamOpenHandler {
	/** Field description */
	public static final String XMLNS = "jabber:client";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(JabberClientStreamOpenHandler.class
			.getName());

	//~--- fields ---------------------------------------------------------------

	private String[] xmlnss = new String[] { XMLNS };

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String serviceStarted(ComponentIOService serv) {
		switch (serv.connectionType()) {
		case connect :
			CompRepoItem repoItem = (CompRepoItem) serv.getSessionData().get(REPO_ITEM_KEY);
			String       r_host   = (String) serv.getSessionData().get("remote-host");

			// Send init xmpp stream here
			serv.getSessionData().put(ComponentIOService.HOSTNAME_KEY, r_host);

			// This should be done only, after authentication is completed
			// addComponentConnection(hostname, serv);
			String data = "<stream:stream" + " xmlns='" + XMLNS + "'" +
					" xmlns:stream='http://etherx.jabber.org/streams'" + " version ='1.0'" +
					" xml:lang='en'" + " from='" + repoItem.getDomain() + "'" + " to ='" + r_host +
					"'" + ">";

			return data;

		default :

			// Do nothing, more/some data should come soon...
			break;
		}    // end of switch (service.connectionType())

		return null;
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
		serv.getSessionData().put(XMLNS_KEY, XMLNS);
		switch (serv.connectionType()) {
		case connect : {
			String id = attribs.get("id");

			if (id != null) {
				serv.getSessionData().put(ComponentIOService.SESSION_ID_KEY, id);
			}

			// Do nothing, stream features should come first
			return null;
		}

		case accept : {
			String       from_hostname = attribs.get("from");
			String       to_hostname   = attribs.get("to");
			CompRepoItem repoItem      = handler.getCompRepoItem(from_hostname);

			serv.getSessionData().put(REPO_ITEM_KEY, repoItem);
			serv.getSessionData().put(ComponentIOService.HOSTNAME_KEY, from_hostname);

			String id = UUID.randomUUID().toString();

			serv.getSessionData().put(ComponentIOService.SESSION_ID_KEY, id);

			// This should be done only, after authentication is completed
			// addComponentConnection(hostname, serv);
			StringBuilder sb = new StringBuilder();

			sb.append("<stream:stream" + " xmlns='" + XMLNS + "'" +
					" xmlns:stream='http://etherx.jabber.org/streams'" + " version ='1.0'" +
					" xml:lang='en'" + " from='" + to_hostname + "'" + " to='" + from_hostname +
					"'" + " id='" + id + "'" + "><stream:features>");

			List<Element> features = handler.getStreamFeatures(serv);

			for (Element element : features) {
				sb.append(element.toString());
			}
			sb.append("</stream:features>");

			return sb.toString();
		}

		default :

			// Do nothing, more data should come soon...
			break;
		}    // end of switch (service.connectionType())

		return null;
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
