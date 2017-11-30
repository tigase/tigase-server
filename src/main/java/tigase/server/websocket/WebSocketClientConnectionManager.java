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

import tigase.conf.ConfigurationException;
import tigase.net.SocketType;
import tigase.server.xmppclient.XMPPIOProcessor;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.StreamError;
import tigase.xmpp.XMPPIOService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class implements basic support allowing clients to connect using WebSocket
 * protocol
 *
 * @author andrzej
 */
public class WebSocketClientConnectionManager
				extends tigase.server.xmppclient.ClientConnectionManager {
	
	private static final String XMLNS_FRAMING = "urn:ietf:params:xml:ns:xmpp-framing";
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
	
	@Override
	public String getDiscoDescription() {
		return "Websocket connection manager";
	}

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
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
	
	@Override
	protected int[] getDefPlainPorts() {
		return new int[] { 5290 };
	}

	@Override
	protected int[] getDefSSLPorts() {
		return null;
	}

	@Override
	protected XMPPIOService<Object> getXMPPIOServiceInstance() {
		return new WebSocketXMPPIOService<Object>(enabledProtocolVersions);
	}
	
	@Override
	protected String prepareStreamClose(XMPPIOService<Object> serv) {
		if (isPreRFC(serv)) {
			return super.prepareStreamClose(serv);
		}
		return "<close xmlns='urn:ietf:params:xml:ns:xmpp-framing' />";
	}
	
	@Override
	protected String prepareStreamOpen(XMPPIOService<Object> serv, String id, String hostname) {
		if (isPreRFC(serv)) {
			return super.prepareStreamOpen(serv, id, hostname);
		}		
		return "<open" + " xmlns='" + XMLNS_FRAMING + "'" + " from='" + hostname + "'" 
				+ " id='" + id + "'" + " version='1.0' xml:lang='en' />";
	}
	
	@Override
	protected String prepareStreamError(XMPPIOService<Object> serv, List<Element> err_el) {
		if (isPreRFC(serv)) {
			return super.prepareStreamError(serv, err_el);
		}			
		StreamError streamError	= StreamError.getByCondition(err_el.get(0).getName());	
		
		for (XMPPIOProcessor proc : processors) {
			proc.streamError(serv, streamError);
		}		
		
		return "<stream:error xmlns:stream=\"http://etherx.jabber.org/streams\">" + err_el.get(0).toString() + "</stream:error>";
	}
	
	@Override
	protected String[] prepareStreamError(XMPPIOService<Object> serv, StreamError streamError, String hostname) {
		if (isPreRFC(serv)) {
			return super.prepareStreamError(serv, streamError, hostname);
		}	
		for (XMPPIOProcessor proc : processors) {
			proc.streamError(serv, streamError);
		}
		return new String[]{
				"<open" + " xmlns='" + XMLNS_FRAMING + "'" + " from='" + hostname + "'" + " id='tigase-error-tigase'" +
						" version='1.0' xml:lang='en' />",
				"<stream:error xmlns:stream=\"http://etherx.jabber.org/streams\">" + "<" + streamError.getCondition() +
						" xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" + "</stream:error>",
				"<close xmlns='" + XMLNS_FRAMING + "'/>" };
	}
	
	@Override
	protected String[] prepareSeeOtherHost(XMPPIOService<Object> serv, String hostname, BareJID see_other_host) {
		if (isPreRFC(serv)) {
			return super.prepareSeeOtherHost(serv, hostname, see_other_host);
		}		
		for (XMPPIOProcessor proc : processors) {
			proc.streamError(serv, StreamError.SeeOtherHost);
		}				
		boolean ssl = SocketType.ssl == ((SocketType) serv.getSessionData().get("socket"));
		int localPort = serv.getLocalPort();
		String see_other_uri = (ssl ? "wss://" : "ws://") + see_other_host + ":" + localPort + "/";
		return new String[]{"<open" + " xmlns='" + XMLNS_FRAMING + "'" + " from='" +
									(hostname != null ? hostname : getDefVHostItem()) + "'" +
									" id='tigase-error-tigase'" + " version='1.0' xml:lang='en' />",
							"<close xmlns='urn:ietf:params:xml:ns:xmpp-framing' see-other-uri='" + see_other_uri +
									"' />"};
	}	
	
	@Override
	protected void preprocessStreamFeatures(XMPPIOService<Object> serv, Element elem_features) {
		if (!isPreRFC(serv)) {
			elem_features.setAttribute("xmlns:stream", "http://etherx.jabber.org/streams");
		}
	}
	
	private boolean isPreRFC(XMPPIOService<Object> serv) {
		return serv == null || ((WebSocketXMPPIOService<Object>) serv).getWebSocketXMPPSpec() == WebSocketXMPPIOService.WebSocketXMPPSpec.hybi;
	}
}


//~ Formatted in Tigase Code Convention on 13/10/15
