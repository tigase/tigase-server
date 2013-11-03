/*
 * UserConnectedCmd.java
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



/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.cluster.strategy.cmd;

//~--- non-JDK imports --------------------------------------------------------

import tigase.cluster.api.ClusterCommandException;
import tigase.cluster.api.CommandListenerSMAbstract;
import tigase.cluster.strategy.ConnectionRecord;
import tigase.cluster.strategy.DefaultClusteringStrategy;

import tigase.server.Command;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

import static tigase.cluster.strategy.ClusteringStrategyIfc.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author kobit
 */
public class UserConnectedCmd
				extends CommandListenerSMAbstract<ConnectionRecord,
						DefaultClusteringStrategy<ConnectionRecord>> {
	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(UserConnectedCmd.class.getName());
	private static final String USER_CONNECTED_RESPONSE = "user_connected_response";

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param name
	 * @param strat
	 */
	public UserConnectedCmd(String name, final DefaultClusteringStrategy strat) {
		super(name, strat);
	}

	//~--- methods --------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 *
	 * @see tigase.cluster.api.CommandListener#executeCommand(java.util.Map)
	 */

	/**
	 * Method description
	 *
	 *
	 * @param fromNode
	 * @param visitedNodes
	 * @param data
	 * @param packets
	 *
	 * @throws ClusterCommandException
	 */
	@Override
	public void executeCommand(JID fromNode, Set<JID> visitedNodes, Map<String,
			String> data, Queue<Element> packets)
					throws ClusterCommandException {
		incSyncInTraffic();
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
					"Called fromNode: {0}, visitedNodes: {1}, data: {2}, packets: {3}",
					new Object[] { fromNode,
					visitedNodes, data, packets });
		}

		ConnectionRecord rec = getConnectionRecord(fromNode, data);

		// There is one more thing....
		// If the new connection is for the same resource we have here then the
		// old connection must be destroyed.
		XMPPSession session = getStrategy().getSM().getXMPPSessions().get(rec.getUserJid()
				.getBareJID());

		if (session != null) {
			int                    sessions_no = 0;
			XMPPResourceConnection conn = session.getResourceForResource(rec.getUserJid()
					.getResource());

			if (conn != null) {

				// This session is here for now but will be removed soom after
				// we receive a response from a connection manager.
				++sessions_no;
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"Duplicate resource connection, logingout the older connection: {0}", rec);
				}
				try {
					Packet cmd = Command.CLOSE.getPacket(getStrategy().getSM().getComponentId(),
							conn.getConnectionId(), StanzaType.set, conn.nextStanzaId());
					Element err_el = new Element("conflict");

					err_el.setXMLNS("urn:ietf:params:xml:ns:xmpp-streams");
					cmd.getElement().getChild("command").addChild(err_el);
					getStrategy().getSM().fastAddOutPacket(cmd);
				} catch (Exception ex) {

					// TODO Auto-generated catch block
					ex.printStackTrace();
				}
			}

			// If there are other connections for this use we setup a "cluster" type
			// of XMPPResourceConnection to allows for a better support for plugins
			// not aware of a cluster environment.
			if (session.getActiveResourcesSize() > sessions_no) {

				// Make sure the object hasn't been initialized before
				XMPPResourceConnection connection = getStrategy().getSM()
						.getXMPPResourceConnections().get(rec.getConnectionId());

				if (connection == null) {
					connection = getStrategy().getSM().loginUserSession(rec.getConnectionId(), rec
							.getUserJid().getDomain(), rec.getUserJid().getBareJID(), rec.getUserJid()
							.getResource(), rec.getSessionId());
					connection.putSessionData(CLUSTER_NODE, rec.getNode());

					// Now send back to the node on which the user opened a new connection
					// information that we have here other user's sessions
					// But only if this is not yet a response to the even on other node
					if (data.get(USER_CONNECTED_RESPONSE) != null) {
						for (XMPPResourceConnection c : session.getActiveResources()) {
							if ((c != connection) && (c != conn)) {
								try {
									Map<String, String> params = getStrategy().prepareConnectionParams(
											conn);

									params.put(USER_CONNECTED_RESPONSE, USER_CONNECTED_RESPONSE);
									getStrategy().getClusterController().sendToNodes(getName(), params,
											getStrategy().getSM().getComponentId(), new JID[] { rec
											.getNode() });
								} catch (NotAuthorizedException ex) {

									// Ignore, we do not want not authenticated connections to be sent
								} catch (NoConnectionIdException ex) {

									// Ignore, we do not want this to be sent
								}
							}
						}
					}
				} else {
					if (log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING, "Session is already there! Double resource bind? {0}",
								rec);
					}
				}
			}
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "User connected jid: {0}, fromNode: {1}", new Object[] { rec
					.getUserJid(),
					fromNode });
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/11/02
