/*
 * XMPPSession.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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



package tigase.xmpp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.util.TigaseStringprepException;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

/**
 * Describe class XMPPSession here.
 *
 *
 * Created: Wed Feb  8 22:14:28 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPSession {
	/**
	 * Private logger for class instances.
	 */
	private static final Logger log = Logger.getLogger(XMPPSession.class.getName());

	//~--- fields ---------------------------------------------------------------

	private CopyOnWriteArrayList<XMPPResourceConnection> activeResources = null;
	private long creationTime                                            = 0;
	private long packets_counter                                         = 0;
	private Map<String, Object> sessionData                              = null;

	/**
	 * User name - part of user's JID
	 */
	private String username = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>XMPPSession</code> instance.
	 *
	 *
	 * @param username
	 */
	public XMPPSession(final String username) {
		sessionData       = new ConcurrentHashMap<String, Object>();
		activeResources   = new CopyOnWriteArrayList<XMPPResourceConnection>();
		this.username     = username;
		this.creationTime = System.currentTimeMillis();
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * This method is called each time the resource is set for connection.
	 *
	 * @param conn
	 * @throws TigaseStringprepException
	 */
	public void addResourceConnection(XMPPResourceConnection conn)
					throws TigaseStringprepException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Adding resource connection for username : " + username + ", id: " +
								 conn);
		}

		// There is a bug somewhere which causes to allow for 2 or more connections
		// with the same resource. Let's try to catch the case here and fix it....
		String resource = conn.getResource();

		if (resource != null) {
			ArrayDeque<XMPPResourceConnection> old_ress =
				new ArrayDeque<XMPPResourceConnection>();

			for (XMPPResourceConnection act_conn : activeResources) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Resource checking: " + act_conn.getResource() +
										 ", connectionID: " + act_conn);
				}
				if (resource.equalsIgnoreCase(act_conn.getResource())) {
					old_ress.add(act_conn);
				}    // end of if (resource.equals(conn.getResource()))
			}      // end of for (XMPPResourceConnection conn: activeResources)

			XMPPResourceConnection old_res = null;

			while ((old_res = old_ress.poll()) != null) {

				// If they are equal, just ignore this. It may happen only for USER_STATUS
				// command where the user session is artificially created....
				if (old_res != conn) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Found old resource connection, id: " + old_res);
					}
					try {
						old_res.putSessionData(XMPPResourceConnection.ERROR_KEY, "conflict");
						old_res.logout();
					} catch (Exception e) {
						log.log(Level.INFO, "Exception during closing old connection, ignoring.", e);
					}
					removeResourceConnection(old_res);
				}
			}
		}

		// The connection could have been already added with null resource
		// to avoid adding it twice let's check if it is already there
		XMPPResourceConnection old_res;

		try {
			old_res = getResourceForConnectionId(conn.getConnectionId());
		} catch (NoConnectionIdException ex) {
			old_res = null;
		}
		if (old_res == null) {
			activeResources.add(conn);
			conn.setParentSession(this);
		}
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Number of active resources is: " + activeResources.size());
			if (activeResources.size() > 1) {
				int i = 0;

				for (XMPPResourceConnection res : activeResources) {
					log.finest("RES " + (++i) + ": " + res);
				}    // end of for (XMPPResourceConnection res: activeResources)
			}      // end of if (activeResources.size() > 1)
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	public List<XMPPResourceConnection> getActiveResourcesClone() {
		return (List<XMPPResourceConnection>) activeResources.clone();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public List<XMPPResourceConnection> getActiveResources() {
		return (List<XMPPResourceConnection>) activeResources;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int getActiveResourcesSize() {
		return activeResources.size();
	}

	/**
	 *
	 * @param key
	 * @return
	 */
	public Object getCommonSessionData(String key) {
		return sessionData.get(key);
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 *
	 */
	public JID[] getConnectionIds() {
		JID[] result = new JID[activeResources.size()];
		int idx      = 0;

		for (XMPPResourceConnection conn : activeResources) {
			try {
				result[idx] = conn.getConnectionId();
				++idx;
			} catch (NoConnectionIdException ex) {

				// Skip connection with no connectionId set
			}
		}    // end of for (XMPPResourceConnection conn: activeResources)

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 *
	 */
	public JID[] getJIDs() {
		JID[] result = new JID[activeResources.size()];
		int idx      = 0;

		for (XMPPResourceConnection conn : activeResources) {
			result[idx++] = conn.getjid();
		}    // end of for (XMPPResourceConnection conn: activeResources)

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getLiveTime() {
		return (System.currentTimeMillis() - creationTime);
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 * @return
	 */
	public synchronized XMPPResourceConnection getResourceConnection(JID jid) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Called for: " + jid);
		}
		if (activeResources.size() == 0) {
			return null;
		}    // end of if (activeResources.size() == 0)
		if (activeResources.size() == 1) {
			XMPPResourceConnection result = activeResources.get(0);

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Only 1 active resource: " + result.getResource());
			}

			return result;
		}    // end of if (activeResources.size() == 1)

		XMPPResourceConnection conn = getResourceForJID(jid);

		if (conn != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Number of resources: " + activeResources.size() +
									 ", got resource for jid: " + jid);
			}

			return conn;
		}    // end of if (conn != null)

		// There is no active resource for this jid, so let's return
		// connection with the highest priority:
		ArrayList<XMPPResourceConnection> al = new ArrayList<XMPPResourceConnection>();

//  al.add(activeResources.get(0));
//  int highest_priority = al.get(0).getPriority();
		int highest_priority = 0;

		for (Iterator<XMPPResourceConnection> it =
						activeResources.iterator(); it.hasNext(); ) {
			XMPPResourceConnection conn_tmp = it.next();

			if (!conn_tmp.isAuthorized()) {
				log.info("Old XMPP connection which is not authorized anymore, removing..." +
								 conn_tmp);
				activeResources.remove(conn_tmp);
			}
			if (conn_tmp.getPriority() == highest_priority) {
				al.add(conn_tmp);

				continue;
			}    // end of if (conn_tmp.getPriority() == highest_priority)
			if (conn_tmp.getPriority() > highest_priority) {
				al.clear();
				al.add(conn_tmp);
				highest_priority = conn_tmp.getPriority();
			}
		}
		if (al.size() == 1) {

			// We found 1 connection with highest priority
			return al.get(0);
		}    // end of if (al.size() == 1)

		// We have a few connections with the same highest priority
		// Let's return the one which was the most recently used.
		XMPPResourceConnection conn_last = al.get(0);
		long time                        = conn_last.getLastAccessed();

		for (int i = 1; i < al.size(); ++i) {
			if (al.get(i).getLastAccessed() > time) {
				conn_last = al.get(i);
				time      = conn_last.getLastAccessed();
			}    // end of if (al.get(i).getLastAccessed() > time)
		}

		return conn_last;
	}

	/**
	 * Method description
	 *
	 *
	 * @param connectionId
	 *
	 * @return
	 */
	public XMPPResourceConnection getResourceForConnectionId(JID connectionId) {
		try {
			for (XMPPResourceConnection conn : activeResources) {
				if (connectionId.equals(conn.getConnectionId())) {
					return conn;
				}    // end of if (resource.equals(conn.getResource()))
			}      // end of for (XMPPResourceConnection conn: activeResources)
		} catch (NoConnectionIdException ex) {

			// Logger.getLogger(XMPPSession.class.getName()).log(Level.SEVERE, null, ex);
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 * @return
	 */
	public XMPPResourceConnection getResourceForJID(JID jid) {
		final String resource = jid.getResource();

		return getResourceForResource(resource);
	}

	/**
	 * Method description
	 *
	 *
	 * @param resource
	 *
	 * @return
	 */
	public XMPPResourceConnection getResourceForResource(String resource) {
		if ((resource != null) && (resource.length() > 0)) {
			for (XMPPResourceConnection conn : activeResources) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Resource checking: " + conn.getResource() + ", connectionID: " +
										 conn);
				}
				if (resource.equalsIgnoreCase(conn.getResource())) {
					return conn;
				}    // end of if (resource.equals(conn.getResource()))
			}      // end of for (XMPPResourceConnection conn: activeResources)
		}        // end of if (resource.length() > 0)

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getUserName() {
		return username;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param conn
	 */
	public void removeResourceConnection(XMPPResourceConnection conn) {
		activeResources.remove(conn);
		conn.removeParentSession(null);
	}

	/**
	 * Method description
	 *
	 *
	 * @param conn
	 */
	public void streamClosed(XMPPResourceConnection conn) {
		removeResourceConnection(conn);
	}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 * @param value
	 */
	protected void putCommonSessionData(String key, Object value) {
		sessionData.put(key, value);
	}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 *
	 * @return
	 */
	protected Object removeCommonSessionData(String key) {
		return sessionData.remove(key);
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("XMPPSession=[");
		sb.append("username: ").append(username);
		sb.append(", resources: ").append(activeResources.toString());
		sb.append("]");

		return sb.toString();
	}

	/**
	 *
	 */
	public void incPacketsCounter() {
		++packets_counter;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getPacketsCounter() {
		return packets_counter;
	}
}    // XMPPSession


//~ Formatted in Tigase Code Convention on 13/02/19
