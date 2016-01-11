/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.org>
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
 */
package tigase.server.xmppclient;

import tigase.xmpp.BareJID;

import tigase.util.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostManagerIfc;
import tigase.xml.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventBusFactory;
import tigase.disteventbus.clustered.EventHandler;
import tigase.xmpp.JID;

//~--- classes ----------------------------------------------------------------

/**
 * Default and basic implementation of SeeOtherHost returning same host as the
 * initial one
 *
 * @author Wojtek
 */
public class SeeOtherHost implements SeeOtherHostIfc {

	private static final Logger log = Logger.getLogger(SeeOtherHost.class.getName());
	public static final String REDIRECTION_ENABLED = "see-other-host-redirect-enabled";
	
	protected List<BareJID> defaultHost = null;
	protected EventBus eventBus = EventBusFactory.getInstance();
	private ArrayList<Phase> active = new ArrayList<Phase>();
	protected VHostManagerIfc vHostManager = null;
	protected EventHandler shutdownEventHandler = (String name, String xmlns, Element e) -> {
		String nodeJid = e.getAttributeStaticStr("node");
		nodeShutdown(nodeJid);
	};
	private Set<String> shutdownNodes = new CopyOnWriteArraySet<String>();

	@Override
	public BareJID findHostForJID(BareJID jid, BareJID host) {
		if (defaultHost != null && !defaultHost.isEmpty()) {
			return defaultHost.get( 0 );
		} else {
			return host;
		}
	}

	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		List<VHostItem.DataType> types = new ArrayList<VHostItem.DataType>();
		types.add(new VHostItem.DataType( REDIRECTION_ENABLED, "see-other-host redirection enabled",
																			 Boolean.class, Boolean.TRUE ) );
		VHostItem.registerData( types );
	}

	@Override
	public void setProperties(final Map<String, Object> props) {
		if (props.containsKey(SeeOtherHostIfc.CM_SEE_OTHER_HOST_ACTIVE)) {
			
			String[] phase =  ((String)props.get(SeeOtherHostIfc.CM_SEE_OTHER_HOST_ACTIVE)).split( ";");
			for ( String ph : phase) {
				try {
					active.add( Phase.valueOf( ph ) );
				} catch ( IllegalArgumentException e ) {
					log.log( Level.FINEST, "unsupported phase configuration item: " + ph + e  );
				}
			}

		} else {
			active.add( Phase.OPEN );
		}
		log.log( Level.CONFIG, props.get( "component-id" ) + " :: see-other-redirect active in: " + Arrays.asList( active ) );

		if ((props.containsKey(SeeOtherHostIfc.CM_SEE_OTHER_HOST_DEFAULT_HOST))
			&& !props.get(SeeOtherHostIfc.CM_SEE_OTHER_HOST_DEFAULT_HOST).toString().trim().isEmpty()) {
			defaultHost = new ArrayList<BareJID>();
			for (String host : ((String) props.get(SeeOtherHostIfc.CM_SEE_OTHER_HOST_DEFAULT_HOST)).split(",")) {
				try {
					defaultHost.add(BareJID.bareJIDInstance(host));
				} catch (TigaseStringprepException ex) {
					log.log(Level.CONFIG, "From JID violates RFC6122 (XMPP:Address Format): ", ex);
				}
			}
			Collections.sort(defaultHost);
		} else {
			defaultHost = null;
		}
	}

	@Override
	public void setNodes(List<JID> nodes) {
		// log.log(Level.CONFIG, "Action invalid for current implementation.");
		synchronized (this) {
			List<String> toRemove = new ArrayList<String>();
			Iterator<String> it = shutdownNodes.iterator();
			while (it.hasNext()) {
				String shutdownNode = it.next();
				boolean found = false;
				for (JID node : nodes) {
					found |= shutdownNode.equals(node.getDomain());
				}
				// remove node from nodes during shutdown if nodes was disconnected 
				// as it probably was shutdown and should reconnect after restart
				if (!found) {
					toRemove.add(shutdownNode);
				}
			}
			shutdownNodes.removeAll(toRemove);
		}
	}

	@Override
	public Element getStreamError( String xmlns, BareJID destination ) {
		Element error = new Element( "stream:error" );
		Element seeOtherHost = new Element( "see-other-host", destination.toString() );

		seeOtherHost.setXMLNS( xmlns );
		error.addChild( seeOtherHost );

		return error;
	}

	@Override
	public boolean isEnabled(VHostItem vHost, Phase ph) {
		return (boolean) vHost.getData( REDIRECTION_ENABLED )
					 && active.contains( ph );
	}
	
	@Override
	public void start() {
		eventBus.addHandler("shutdown", "tigase:server", shutdownEventHandler);
	}
	
	@Override
	public void stop() {
		eventBus.removeHandler("shutdown", "tigase:server", shutdownEventHandler);
	}

	protected boolean isNodeShutdown(BareJID jid) {
		return jid != null && shutdownNodes.contains(jid.getDomain());
	}
	
	protected void nodeShutdown(String node) {
		synchronized (this) {
			shutdownNodes.add(node);
		}
	}
	
}
