/*
 * XMPPSession.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XMPPSession class is a container class for all {@link XMPPResourceConnection} objects for
 * particular user (i.e. all user's connected resources)
 */
public class XMPPSession {
	/**
	 * Private logger for class instances.
	 */
	private static final Logger log = Logger.getLogger(XMPPSession.class.getName());

	//~--- fields ---------------------------------------------------------------

	private CopyOnWriteArrayList<XMPPResourceConnection> activeResources = null;
	private long                                         creationTime    = 0;
	private long                                         packets_counter = 0;
	private Map<String, Object>                          sessionData     = null;

	/**
	 * User name - localpart of user's JID
	 */
	private String username = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>XMPPSession</code> instance.
	 *
	 * @param username - localpart of user's JID
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
	 * Method performs checking whether there is no collision of the resources.
	 *
	 * @param conn {@link XMPPResourceConnection} that is being added.
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
					log.finest("Resource checking for: " + username + " :: " + act_conn.getResource() +
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
						log.finest("Found old resource connection for: " + username + ", id: " + old_res);
					}
					try {
						old_res.putSessionData(XMPPResourceConnection.ERROR_KEY, "conflict");
						old_res.logout();
					} catch (NotAuthorizedException e) {
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
			log.finest("Number of active resources of [" + username + "] = "
								 + activeResources.size() + " : " + activeResources);
		}
	}

	/**
	 * Increments counter of processed packet for the given
	 * user session (i.e. all users connections in total)
	 */
	public void incPacketsCounter() {
		++packets_counter;
	}

	/**
	 * Removes particular {@link XMPPResourceConnection} user's resource connection
	 * from the list of all active user connections within given {@link XMPPSession}
	 * and detaches {@link XMPPSession} from the removed {@link XMPPResourceConnection}
	 *
	 * @param conn
	 */
	public void removeResourceConnection(XMPPResourceConnection conn) {
		if (activeResources.remove(conn))
			conn.removeParentSession(null);
	}

	/**
	 * Method is called upon closing stream connection and removes particular
	 * {@link XMPPResourceConnection}
	 *
	 * @param conn - {@link XMPPResourceConnection} for which stream was closed.
	 */
	public void streamClosed(XMPPResourceConnection conn) {
		removeResourceConnection(conn);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("XMPPSession=[");
		sb.append( "hash=" + System.identityHashCode( this ) );
		sb.append( ", username: " ).append( username );
		sb.append(", resources: ").append(activeResources.toString());
		sb.append("];");

		return sb.toString();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method returns a List of all {@link XMPPResourceConnection} objects related
	 * to this {@link XMPPSession} object
	 */
	public List<XMPPResourceConnection> getActiveResources() {
		return (List<XMPPResourceConnection>) activeResources;
	}

	/**
	 * Method returns a cloned List of all {@link XMPPResourceConnection} objects
	 * related to this {@link XMPPSession} object
	 */
	@SuppressWarnings({ "unchecked" })
	public List<XMPPResourceConnection> getActiveResourcesClone() {
		return (List<XMPPResourceConnection>) activeResources.clone();
	}

	/**
	 * Method returns number of all {@link XMPPResourceConnection} objects
	 * related to this {@link XMPPSession} object
	 */
	public int getActiveResourcesSize() {
		return activeResources.size();
	}

	/**
	 * Method returns a data for particular {@code key} which is common to all resource
	 * connections within this {@link XMPPSession}
	 *
	 * related to this {@link XMPPSession} object
	 *
	 * @param key for which data should be returned
	 */
	public Object getCommonSessionData(String key) {
		return sessionData.get(key);
	}

	/**
	 * Method returns an array of all ConnectionIDs related to this
	 * {@link XMPPSession}
	 */
	public JID[] getConnectionIds() {
		JID[] result = new JID[activeResources.size()];
		int   idx    = 0;

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
	 * Method returns an array of all FullJIDs related to this
	 * {@link XMPPSession}
	 */
	public JID[] getJIDs() {
		JID[] result = new JID[activeResources.size()];
		int   idx    = 0;

		for (XMPPResourceConnection conn : activeResources) {
			result[idx++] = conn.getjid();
		}    // end of for (XMPPResourceConnection conn: activeResources)

		return result;
	}

	/**
	 * Method returns time of how long the session is active (in milliseconds)
	 */
	public long getLiveTime() {
		return (System.currentTimeMillis() - creationTime);
	}

	/**
	 * Returns number of processed packet for the given
	 * user session (i.e. all users connections in total)
	 */
	public long getPacketsCounter() {
		return packets_counter;
	}

	/**
	 * Method returns {@link XMPPResourceConnection} for particular FullJID. In
	 * case there are no sessions {@code null} is returned, in case there is more
	 * than one active session a session with the highest priority is returned. In
	 * case there are still more than one connections with the same priority then
	 * the latest active one is returned.
	 *
	 * @param jid FullJID for which a {@code XMPPResourceConnection} should be
	 *            returned
	 */
	public synchronized XMPPResourceConnection getResourceConnection(JID jid) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Called for: " + jid);
		}
		if (activeResources.size() == 0) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("No active resources found!");
			}
			return null;
		}    // end of if (activeResources.size() == 0)
		if (jid.getResource() != null) {
			return this.getResourceForResource(jid.getResource());
		}
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

		// There is no single active resource for this jid, so let's return
		// connection with the highest priority:
		ArrayList<XMPPResourceConnection> al = new ArrayList<XMPPResourceConnection>();

		int highest_priority = 0;
		int priority;

		for ( XMPPResourceConnection conn_tmp : activeResources ) {
			if (!conn_tmp.isAuthorized()) {
				if (log.isLoggable(Level.FINE)) {
					log.finest("Connection either not yet authorized or already gone, ignoring while processing: " + conn_tmp);
				}
				continue;
			}

			// if connections priority matches current highest priority add it only, otherwise if
			// it's greater clear current list and set new highest priority
			priority = conn_tmp.getPriority();
			if (priority == highest_priority) {
				al.add(conn_tmp);
			}
			if (priority > highest_priority) {
				al.clear();
				al.add(conn_tmp);
				highest_priority = priority;
			}
		}
		if (al.size() == 0) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("No active resources found!");
			}
			return null;
		}
		if (al.size() == 1) {
			// We found 1 connection with highest priority
			return al.get(0);
		}    // end of if (al.size() == 1)

		// We have a few connections with the same highest priority
		// Let's return the one which was the most recently used.
		XMPPResourceConnection conn_last = al.get(0);
		long                   time      = conn_last.getLastAccessed();

		for (int i = 1; i < al.size(); ++i) {
			if (al.get(i).getLastAccessed() > time) {
				conn_last = al.get(i);
				time      = conn_last.getLastAccessed();
			}    // end of if (al.get(i).getLastAccessed() > time)
		}

		return conn_last;
	}

	/**
	 * Method returns {@link XMPPResourceConnection} for particular ConnectionID.
	 * In case there is no session that match given ConnectionID then {@code null}
	 * is returned.
	 *
	 * @param connectionId  ConnectionID for which {@code XMPPResourceConnection} should be
	 *            returned
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
	 * Method returns {@link XMPPResourceConnection} for particular FullJID (using
	 * {@code resource} name as determinant. In case there is no session that
	 * match - {@code null} is returned.
	 *
	 * @param jid FullJID for which a {@code XMPPResourceConnection} should be
	 *            returned
	 */
	public XMPPResourceConnection getResourceForJID(JID jid) {
		final String resource = jid.getResource();

		return getResourceForResource(resource);
	}

	/**
	 * Method returns {@link XMPPResourceConnection} for particular
	 * {@code resource}. In case there is no session that match - {@code null} is
	 * returned.
	 *
	 * @param resource resource string for which a {@code XMPPResourceConnection} should be
	 *            returned
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
	 * Method returns username that is related to this {@code XMPPSession} (i.e.
	 * mostly localpart of JID)
	 */
	public String getUserName() {
		return username;
	}

	//~--- methods --------------------------------------------------------------

	protected Object computeCommonSessionDataIfAbsent(String key, Function<String,Object> valueFactory) {
		return sessionData.computeIfAbsent(key, valueFactory);
	}
	
	/**
	 * Method used to store data common for all connections of the user.
	 *
	 * @param key under which data should be store
	 * @param value data to be stored
	 */
	protected void putCommonSessionData(String key, Object value) {
		sessionData.put(key, value);
	}

	protected Object putCommonSessionDataIfAbsent(String key, Object value) {
		return sessionData.putIfAbsent(key, value);
	}
	
	/**
	 * Method used to remove data common for all connections of the user.
	 *
	 * @param key for which data should be removed
	 */
	protected Object removeCommonSessionData(String key) {
		return sessionData.remove(key);
	}
}    // XMPPSession
