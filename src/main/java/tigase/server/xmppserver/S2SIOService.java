/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
package tigase.server.xmppserver;

import tigase.xmpp.XMPPIOService;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Jun 14, 2010 12:30:53 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class S2SIOService
		extends XMPPIOService<Object> {

	public static final String S2S_CONNECTION_KEY = "s2s-connection-key";
	public static final String HANDSHAKING_DOMAIN_KEY = "handshaking-domain-key";
	protected static final String HANDSHAKING_ONLY_KEY = "handshaking-only-key";
	private static final Logger log = Logger.getLogger(S2SIOService.class.getName());

	/**
	 * This structure keeps a set of all CIDs reusing this connection. If the connection goes down all CIDs must be
	 * notified.
	 */
	private Set<CID> authenticatedCIDs = new CopyOnWriteArraySet<CID>();
	private CIDConnections cid_conns = null;
	private String dbKey = null;
	private S2SConnection s2s_conn = null;

	private String session_id = null;

	/**
	 * Adds another connection id (CID) to the authenticated list for this connection
	 *
	 * @param cid
	 */
	public void addCID(CID cid) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, Adding CID to authenticated: {1}", new Object[]{this, cid});
		}

		authenticatedCIDs.add(cid);
	}

	public Set<CID> getCIDs() {
		return authenticatedCIDs;
	}

	public S2SConnection getS2SConnection() {
		return s2s_conn;
	}

	public void setS2SConnection(S2SConnection s2s_conn) {
		this.s2s_conn = s2s_conn;
	}

	public String getSessionId() {
		return session_id;
	}

	public void setSessionId(String session_id) {
		this.session_id = session_id;
	}

	public boolean isAuthenticated(CID cid) {
		return authenticatedCIDs.contains(cid);
	}

	public boolean isAuthenticated() {
		return authenticatedCIDs.size() > 0;
	}

	public boolean isHandshakingOnly() {
		return getSessionData().get(HANDSHAKING_ONLY_KEY) != null;
	}

	public void setDBKey(String key) {
		dbKey = key;
	}

	@Override
	public String toString() {
		CID cid = (CID) getSessionData().get("cid");

		return "CID: " + cid + ", " + super.toString();
	}
}
