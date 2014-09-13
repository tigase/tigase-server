/*
 * WebSocketClientConnectionManager.java
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



package tigase.server.websocket;

//~--- non-JDK imports --------------------------------------------------------

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tigase.conf.ConfigurationException;
import tigase.xmpp.XMPPIOService;

/**
 * Class implements basic support allowing clients to connect using WebSocket
 * protocol
 *
 * @author andrzej
 */
public class WebSocketClientConnectionManager
				extends tigase.server.xmppclient.ClientConnectionManager {
	
	private static final String PROTOCOL_VERSIONS_KEY = "protocol-versions";
	private static final String[] PROTOCOL_VERSIONS_DEF = { WebSocketHybi.ID };
	
	private static final WebSocketProtocolIfc[] SUPPORTED_PROTOCOL_VERSIONS = { new WebSocketHybi(), new WebSocketHixie76() };
	
	private WebSocketProtocolIfc[] enabledProtocolVersions = null;
	
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String,Object> defs = super.getDefaults(params);
		defs.put(PROTOCOL_VERSIONS_KEY, PROTOCOL_VERSIONS_DEF);
		return defs;
	}
	
	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getDiscoDescription() {
		return "Websocket connection manager";
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		if (props.containsKey(PROTOCOL_VERSIONS_KEY)) {
			String[] versions = (String[]) props.get(PROTOCOL_VERSIONS_KEY);
			List<WebSocketProtocolIfc> value = new ArrayList<WebSocketProtocolIfc>();
			for (String version : versions) {
				for (WebSocketProtocolIfc v : SUPPORTED_PROTOCOL_VERSIONS) {
					if (version.equals(v.getId())) {
						value.add(v);
					}
				}
			}
			enabledProtocolVersions = value.toArray(new WebSocketProtocolIfc[value.size()]);
		}
		super.setProperties(props);
	}
	
	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>int[]</code>
	 */
	@Override
	protected int[] getDefPlainPorts() {
		return new int[] { 5290 };
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>int[]</code>
	 */
	@Override
	protected int[] getDefSSLPorts() {
		return null;
	}

	/**
	 * Method returns XMPPIOService instance implementing WebSocketXMPPIOService
	 *
	 *
	 *
	 * @return a value of <code>XMPPIOService<Object></code>
	 */
	@Override
	protected XMPPIOService<Object> getXMPPIOServiceInstance() {
		return new WebSocketXMPPIOService<Object>(enabledProtocolVersions);
	}
}


//~ Formatted in Tigase Code Convention on 13/10/15
