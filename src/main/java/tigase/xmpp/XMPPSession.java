/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package tigase.xmpp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import tigase.util.JIDUtils;

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
   * Private logger for class instancess.
   */
  private static Logger log = Logger.getLogger("tigase.xmpp.XMPPSession");

  /**
   * User name - part of user's JID
   */
  private String username = null;

	private ArrayList<XMPPResourceConnection> activeResources = null;
	private long creationTime = 0;

	/**
	 * Creates a new <code>XMPPSession</code> instance.
	 *
	 */
	public XMPPSession(final String username) {
		activeResources = new ArrayList<XMPPResourceConnection>();
		this.username = username;
		this.creationTime = System.currentTimeMillis();
	}

	public long getLiveTime() {
		return (System.currentTimeMillis() - creationTime);
	}

	public void streamClosed(XMPPResourceConnection conn) {
		removeResourceConnection(conn);
	}

	public String getUserName() {
		return username;
	}

	@SuppressWarnings({"unchecked"})
	public List<XMPPResourceConnection> getActiveResources() {
		return (List<XMPPResourceConnection>)activeResources.clone();
	}

	public int getActiveResourcesSize() {
		return activeResources.size();
	}

	/**
	 * This method is called each time the resource is set for connection.
	 */
	public void resourceSet(XMPPResourceConnection conn) {
		activeResources.remove(conn);
		String cur_res = conn.getResource();
		XMPPResourceConnection old_conn = getResourceForResource(cur_res);
		activeResources.add(conn);
		if (old_conn != null) {
			log.finest("Found old resource connection for username : " + username
				+ ", id: "+old_conn.getConnectionId());
			try { old_conn.logout(); } catch (Exception e) {
				log.log(Level.INFO, "Exception during closing old connection, ignoring.", e);
			}
			removeResourceConnection(old_conn);
		} // end of if (old_res != null)
	}

	public void addResourceConnection(XMPPResourceConnection conn) {
		log.finest("Adding resource connection for username : " + username
			+ ", id: " + conn.getConnectionId());
		XMPPResourceConnection old_res = getResourceForResource(conn.getResource());
		// If they are equal, just ignore this. It may happen only for USER_STATUS
		// command where the user session is artificialy created....
		if (old_res != conn) {
			if (old_res != null) {
				log.finest("Found old resource connection, id: "+old_res.getConnectionId());
				removeResourceConnection(old_res);
				try { old_res.logout(); } catch (Exception e) {
					log.log(Level.INFO,
						"Exception during closing old connection, ignoring.", e);
				}
			} // end of if (old_res != null)
			activeResources.add(conn);
			conn.setParentSession(this);
			log.finest("Number of active resources is: " + activeResources.size());
			if (activeResources.size() > 1) {
				int i = 0;
				for (XMPPResourceConnection res: activeResources) {
					log.finest("RES " + (++i) + ": " + res.getResource() + ", "
						+ res.getConnectionId());
				} // end of for (XMPPResourceConnection res: activeResources)
			} // end of if (activeResources.size() > 1)
		}
	}

	public void removeResourceConnection(XMPPResourceConnection conn) {
		activeResources.remove(conn);
		conn.setParentSession(null);
	}

	public String[] getJIDs() throws NotAuthorizedException {
		String[] result = new String[activeResources.size()];
		int idx = 0;
		for (XMPPResourceConnection conn: activeResources) {
			result[idx++] = conn.getJID();
		} // end of for (XMPPResourceConnection conn: activeResources)
		return result;
	}

	public XMPPResourceConnection getResourceForResource(final String resource) {
		if (resource != null && resource.length() > 0) {
			for (XMPPResourceConnection conn: activeResources) {
				if (resource.equals(conn.getResource())) {
					return conn;
				} // end of if (resource.equals(conn.getResource()))
			} // end of for (XMPPResourceConnection conn: activeResources)
		} // end of if (resource.length() > 0)
		return null;
	}

	public XMPPResourceConnection getResourceForJID(final String jid) {
		final String resource = JIDUtils.getNodeResource(jid);
		return getResourceForResource(resource);
	}

	public XMPPResourceConnection getResourceConnection(final String jid) {

		if (activeResources.size() == 0) {
			return null;
		} // end of if (activeResources.size() == 0)

		if (activeResources.size() == 1) {
			XMPPResourceConnection result = activeResources.get(0);
			log.finest("Only 1 active resource: " + result.getResource());
			return result;
		} // end of if (activeResources.size() == 1)

		XMPPResourceConnection conn = getResourceForJID(jid);
		if (conn != null) {
			log.finest("Number of resources: " + activeResources.size()
				+ ", got resource for jid: " + jid);
			return conn;
		} // end of if (conn != null)

		// There is no active resource for this jid, so let's return
		// connection with the highest priority:
		ArrayList<XMPPResourceConnection> al =
			new ArrayList<XMPPResourceConnection>();
		al.add(activeResources.get(0));
		int highest_priority = al.get(0).getPriority();
		for (int i = 1; i < activeResources.size(); ++i) {
			XMPPResourceConnection conn_tmp = activeResources.get(i);
			if (conn_tmp.getPriority() == highest_priority) {
				al.add(conn_tmp);
				continue;
			} // end of if (conn_tmp.getPriority() == highest_priority)
			if (conn_tmp.getPriority() > highest_priority) {
				al.clear();
				al.add(conn_tmp);
				highest_priority = conn_tmp.getPriority();
			}
		} // end of for (XMPPResourceConnection conn: activeResources)

		if (al.size() == 1) {
			// We found 1 connection with highest priority
			return al.get(0);
		} // end of if (al.size() == 1)

		// We have a few connections with the same highest priority
		// Let's return the one which was the most recently used.
		XMPPResourceConnection conn_last = al.get(0);
		long time = conn_last.getLastAccessed();
		for (int i = 1; i < al.size(); ++i) {
			if (al.get(i).getLastAccessed() > time) {
				conn_last = al.get(i);
				time = conn_last.getLastAccessed();
			} // end of if (al.get(i).getLastAccessed() > time)
		}
		return conn_last;
	}

} // XMPPSession
