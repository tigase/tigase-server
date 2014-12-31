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
 */
package tigase.cluster;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.*;

import javax.script.Bindings;

import tigase.cluster.api.*;
import tigase.vhosts.VHostManager;
import tigase.xmpp.JID;

public class VHostManagerClustered extends VHostManager implements ClusteredComponentIfc {

	private static final Logger log = Logger.getLogger( VHostManagerClustered.class.getName() );
	private List<JID> connectedNodes = new CopyOnWriteArrayList<>();
	private String CONNECTED_NODES_VAR = "connectedNodes";

	@Override
	public void setClusterController( ClusterControllerIfc cl_controller ) {}

	@Override
	public void nodeConnected( String node ) {
		JID nodeJID = JID.jidInstanceNS( "vhost-man", node, null );
		log.log( Level.FINEST, "Node connected: " + nodeJID );
		synchronized ( connectedNodes ) {
			if ( !connectedNodes.contains( nodeJID ) ){
				connectedNodes.add( nodeJID );
			}
		}
	}

	@Override
	public void nodeDisconnected( String node ) {
		JID nodeJID = JID.jidInstanceNS( "vhost-man", node, null);
		log.log( Level.FINEST, "Node disconnected: " + nodeJID );
		connectedNodes.remove( nodeJID );
	}

	@Override
	public void initBindings( Bindings binds ) {
		super.initBindings( binds );
		binds.put( CONNECTED_NODES_VAR, connectedNodes );
		log.log( Level.ALL, " == initBindings: " + Arrays.asList( connectedNodes ));
	}
}
