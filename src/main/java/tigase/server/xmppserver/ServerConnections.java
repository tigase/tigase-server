/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.server.xmppserver;

import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import java.util.logging.Level;

import tigase.xmpp.XMPPIOService;
import tigase.server.Packet;

/**
 * Describe class ServerConnections here.
 *
 *
 * Created: Wed Jun 11 14:26:53 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ServerConnections {

	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppserver.ServerConnections");

	public enum OutgoingState {
		NULL, CONNECTING, HANDSHAKING, OK;
	}

	/**
	 * Outgoing (connect) service for data packets.
	 */
	private XMPPIOService outgoing = null;
	private OutgoingState conn_state = OutgoingState.NULL;

// 	/**
// 	 * Incoming (accept) services session:id. Some servers (EJabberd) opens
// 	 * many connections for each domain, especially when in cluster mode.
// 	 */
// 	private ConcurrentSkipListMap<String, XMPPIOService> incoming =
//     new ConcurrentSkipListMap<String, XMPPIOService>();

	/**
	 * Normal packets between users on different servers
	 */
	private ConcurrentLinkedQueue<Packet> waitingPackets =
    new ConcurrentLinkedQueue<Packet>();

	/**
	 * Controll packets for s2s connection establishing
	 */
	private ConcurrentLinkedQueue<Packet> waitingControlPackets =
		new ConcurrentLinkedQueue<Packet>();

	private ConcurrentSkipListMap<String, String> db_keys =
    new ConcurrentSkipListMap<String, String>();

	private long sentPackets = 0;
	private long receivedPackets = 0;
	private String cid = null;

	private ConnectionHandlerIfc handler = null;

	/**
	 * Keeps the creation time. After some time the queue and all
	 * packets waiting to send should become outdated and they
	 * should be returned to sender and no more attempts to connect
	 * to the remote server should be performed.
	 */
	private long creationTime = System.currentTimeMillis();

	/**
	 * Creates a new <code>ServerConnections</code> instance.
	 *
	 *
	 * @param handler 
	 */
	public ServerConnections(ConnectionHandlerIfc handler, String cid) {
		this.handler = handler;
		this.cid = cid;
	}

	public String getCID() {
		return cid;
	}

	/**
	 * Describe <code>sendPacket</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @return a <code>boolean</code> value
	 */
	public synchronized boolean sendPacket(Packet packet) {
		boolean result = false;
		if (outgoing != null && outgoing.isConnected()
			&& conn_state == OutgoingState.OK) {
			result = handler.writePacketToSocket(outgoing, packet);
			if (!result) {
				outgoing.forceStop();
				outgoing = null;
			} else {
				++sentPackets;
			}
		}
		if (!result) {
			addDataPacket(packet);
		}
		return result;
	}

	public OutgoingState getOutgoingState() {
		return conn_state;
	}

	public int getDBKeysSize() {
		return db_keys.size();
	}

	public synchronized boolean sendControlPacket(Packet packet) {
		boolean result = false;
		if (outgoing != null && outgoing.isConnected()
			&& (conn_state == OutgoingState.OK
				|| conn_state == OutgoingState.HANDSHAKING)) {
			result = handler.writePacketToSocket(outgoing, packet);
			if (!result) {
				outgoing.forceStop();
				outgoing = null;
			}
		}
		if (!result) {
			addControlPacket(packet);
			if (log.isLoggable(Level.FINEST)) {
    			log.finest("Inserted to waiting queue packet: " + packet.toString());
            }
		}
		return result;
	}

	public boolean outgoingIsNull() {
		return outgoing == null;
	}

	public void addDataPacket(Packet packet) {
		if (waitingPackets.size() == 0) {
			creationTime = System.currentTimeMillis();
		}
		waitingPackets.offer(packet);
	}

	public long waitingTime() {
		return (System.currentTimeMillis() - creationTime);
	}

	public void addControlPacket(Packet packet) {
		waitingControlPackets.offer(packet);
	}

	public boolean needsConnection() {
		return (conn_state == OutgoingState.NULL);
	}

	public void setConnecting() {
		conn_state = OutgoingState.CONNECTING;
	}

	public void setValid() {
		conn_state = OutgoingState.OK;
	}

	public synchronized boolean sendAllControlPackets() {
		handler.writePacketsToSocket(outgoing, waitingControlPackets);
		return true;
	}


	public synchronized boolean handleDialbackSuccess() {
		if (outgoing != null && conn_state == OutgoingState.HANDSHAKING) {
			setValid();
			LinkedList<Packet> all = new LinkedList<Packet>();
			Packet packet = null;
			while ((packet = waitingControlPackets.poll()) != null) {
				all.offer(packet);
			}
			sentPackets += waitingPackets.size();
			while ((packet = waitingPackets.poll()) != null) {
				all.offer(packet);
			}
			handler.writePacketsToSocket(outgoing, all);
			return true;
		} else {
			log.warning("Something wrong, the method was called when the outgoing connection is null.");
			outgoing = null;
			conn_state = OutgoingState.NULL;
			return false;
		}
	}

	public synchronized void handleDialbackFailure() {
		if (outgoing != null) {
			outgoing.forceStop();
			outgoing = null;
		}
		conn_state = OutgoingState.NULL;
	}

	public Queue<Packet> getWaitingPackets() {
		return waitingPackets;
	}

	public void stopAll() {
		if (outgoing != null) {
			outgoing.forceStop();
			outgoing = null;
		}
		conn_state = OutgoingState.NULL;
// 		Set<Map.Entry<String, XMPPIOService>> set = incoming.entrySet();
// 		for (Map.Entry<String, XMPPIOService> entry: set) {
// 			entry.getValue().forceStop();
// 			set.remove(entry);
// 		}
	}

	public void putDBKey(String sessionId, String dbKey) {
		db_keys.put(sessionId, dbKey);
	}

	public String getDBKey(String sessionId) {
		return db_keys.get(sessionId);
	}

	public synchronized void addOutgoing(XMPPIOService serv) {
		XMPPIOService old = outgoing;
		if (outgoing != serv) {
			outgoing = serv;
			conn_state = OutgoingState.HANDSHAKING;
		}
		if (old != null) {
			log.info("Old outgoing connection replaced with new one!");
			old.forceStop();
		}
	}

// 	public void addIncoming(String session_id, XMPPIOService serv) {
// 		if (serv == outgoing) {
// 			log.info("Adding outgoing connection as incoming, packet received on wrong connection? session_id: " + session_id);
// 			return;
// 		}
// 		XMPPIOService old_serv = incoming.get(session_id);
// 		if (old_serv != null) {
// 			if (old_serv == serv) {
// 				log.info("Adding again the same handshaking service session_id: "
// 					+ session_id + ", unique_id: " + serv.getUniqueId());
// 				return;
// 			} else {
// 				log.info("Adding new handshaking service when the old one for the"
// 					+ " same session_id exists: "
// 					+ session_id + ", new_unique_id: " + serv.getUniqueId()
// 					+ ", old_unique_id: " + old_serv.getUniqueId());
// 				old_serv.forceStop();
// 			}
// 		}
// 		incoming.put(session_id, serv);
// 	}

// 	public boolean sendToIncoming(String session_id, Packet packet) {
// 		XMPPIOService serv = incoming.get(session_id);
// 		if (serv != null) {
// 			return handler.writePacketToSocket(serv, packet);
// 		} else {
// 			return false;
// 		}
// 	}

// 	public void validateIncoming(String session_id, boolean valid) {
// 		XMPPIOService serv = incoming.get(session_id);
// 		if (serv != null) {
// 			serv.getSessionData().put("valid", valid);
// 			if (!valid) {
// 				serv.stop();
// 			}
// 		}
// 	}

// 	public boolean isIncomingValid(String session_id) {
// 		if (session_id == null) {
// 			return false;
// 		}
// 		XMPPIOService serv = incoming.get(session_id);
// 		if (serv == null || serv.getSessionData().get("valid") == null) {
// 			return false;
// 		} else {
// 			return (Boolean)serv.getSessionData().get("valid");
// 		}
// 	}

	public boolean isOutgoingConnected() {
		return outgoing != null && outgoing.isConnected();
	}

	public boolean isOutgoing(XMPPIOService serv) {
		return serv == outgoing;
	}

	public void serviceStopped(XMPPIOService serv) {
		String session_id = 
						(String) serv.getSessionData().get(XMPPIOService.SESSION_ID_KEY);
		if (session_id != null) {
			db_keys.remove(session_id);
		} else {
			log.info("Session_ID is null for: " + serv.getUniqueId());
		}
		if (serv == outgoing) {
			outgoing = null;
			conn_state = OutgoingState.NULL;
            if (log.isLoggable(Level.FINER)) {
                log.finer("Connection removed: " + session_id);
            }
// 			return;
		}
// 		XMPPIOService rem = incoming.remove(session_id);
// 		if (rem == null) {
// 			log.fine("No service with given SESSION_ID: " + session_id);
// 		} else {
// 			log.finer("Connection removed: " + session_id);
// 		}
	}

// 	public int incomingSize() {
// 		return incoming.size();
// 	}

}
