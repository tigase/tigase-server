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

package tigase.server.ext.handlers;

import java.util.List;
import java.util.Map;
import tigase.server.ext.ComponentConnection;
import tigase.server.ext.ComponentProtocolHandler;
import tigase.server.ext.StreamOpenHandler;
import tigase.xmpp.XMPPIOService;

/**
 * Created: Oct 2, 2009 5:18:44 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UnknownXMLNSStreamOpenHandler implements StreamOpenHandler {

	@Override
	public String streamOpened(XMPPIOService<List<ComponentConnection>> serv,
			Map<String, String> attribs, ComponentProtocolHandler handler) {
		String xmlns = attribs.get("xmlns");
		StringBuilder sb =
				new StringBuilder(
				"<stream:stream xmlns:stream='http://etherx.jabber.org/streams'" +
				" version='1.0'" +
				" xml:lang='en'");
		if (xmlns != null) {
			sb.append(" xmlns='").append(xmlns).append("'");
		}
		if (attribs.get("to") != null) {
			sb.append(" from='").append(attribs.get("to")).append("'");
		}
		if (attribs.get("from") != null) {
			sb.append(" to='").append(attribs.get("from")).append("'");
		}
		if (attribs.get("id") != null) {
			sb.append(" id='").append(attribs.get("id")).append("'");
		}
		sb.append('>');
		sb.append("<stream:error>" +
				"<invalid-namespace xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
				"</stream:error>" +
				"</stream:stream>");
		return sb.toString();
	}

	@Override
	public String[] getXMLNSs() {
		return null;
	}

	@Override
	public String serviceStarted(XMPPIOService<List<ComponentConnection>> s) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
