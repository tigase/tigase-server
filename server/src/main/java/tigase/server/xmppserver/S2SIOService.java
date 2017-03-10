
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
*
* $Rev$
* Last modified by $Author$
* $Date$
 */
package tigase.server.xmppserver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.XMPPIOService;

//~--- JDK imports ------------------------------------------------------------

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jun 14, 2010 12:30:53 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class S2SIOService extends XMPPIOService<Object> {
	private static final Logger log = Logger.getLogger(S2SIOService.class.getName());

	/** Field description */
	public static final String S2S_CONNECTION_KEY = "s2s-connection-key";
        public static final String HANDSHAKING_DOMAIN_KEY = "handshaking-domain-key";
	protected static final String HANDSHAKING_ONLY_KEY = "handshaking-only-key";

	//~--- fields ---------------------------------------------------------------

	private CIDConnections cid_conns = null;
	private String dbKey = null;
	private S2SConnection s2s_conn = null;
	private String session_id = null;

///**
// * This structure keeps a set of domains which are authorized to send or
// * receive packets on this connection.
// */
//private Set<String> authenticatedDomains = new CopyOnWriteArraySet<String>();

	/**
	 * This structure keeps a set of all CIDs reusing this connection. If the connection
	 * goes down all CIDs must be notified.
	 */
	private Set<CID> authenticatedCIDs = new CopyOnWriteArraySet<CID>();

	//~--- methods --------------------------------------------------------------

///**
// * Method description
// *
// *
// * @param domain
// */
//public void addAuthenticatedDomain(String domain) {
//  authenticatedDomains.add(domain);
//}

	/**
	 * Adds another connection id (CID) to the authenticated list for this connection
	 *
	 *
	 * @param cid
	 */
	public void addCID(CID cid) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, Adding CID to authenticated: {1}", new Object[] { this, cid });
		}

		authenticatedCIDs.add(cid);
	}

	//~--- get methods ----------------------------------------------------------

///**
// * Method description
// *
// *
// * 
// */
//public Set<String> getAuthenticatedDomains() {
//  return authenticatedDomains;
//}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public Set<CID> getCIDs() {
		return authenticatedCIDs;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public S2SConnection getS2SConnection() {
		return s2s_conn;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getSessionId() {
		return session_id;
	}

///**
// * Method description
// *
// *
// * @param domain
// *
// * 
// */
//public boolean isAuthenticated(String domain) {
//  return authenticatedDomains.contains(domain);
//}

	/**
	 * Method description
	 *
	 *
	 * @param cid
	 *
	 * 
	 */
	public boolean isAuthenticated(CID cid) {
		return authenticatedCIDs.contains(cid);
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public boolean isAuthenticated() {
		return authenticatedCIDs.size() > 0;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public boolean isHandshakingOnly() {
		return getSessionData().get(HANDSHAKING_ONLY_KEY) != null;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param key
	 */
	public void setDBKey(String key) {
		dbKey = key;
	}

	/**
	 * Method description
	 *
	 *
	 * @param s2s_conn
	 */
	public void setS2SConnection(S2SConnection s2s_conn) {
		this.s2s_conn = s2s_conn;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 * @param session_id
	 */
	public void setSessionId(String session_id) {
		this.session_id = session_id;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public String toString() {
		CID cid = (CID) getSessionData().get("cid");

		return "CID: " + cid + ", " + super.toString();
	}
}
