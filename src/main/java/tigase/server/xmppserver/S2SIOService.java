/*
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
	private Set<CID> authenticatedCIDsOUT = new CopyOnWriteArraySet<CID>();
	private Set<CID> authenticatedCIDsIN = new CopyOnWriteArraySet<CID>();
	private boolean streamNegotiationCompleted = false;
	private CIDConnections cid_conns = null;
	private String dbKey = null;
	private S2SConnection s2s_conn = null;

	private String session_id = null;

	enum DIRECTION {
		IN,
		OUT,
		BOTH,
		ANY
	}

	/**
	 * Adds another connection id (CID) to the authenticated list for this connection
	 *
	 */
	public void addCID(CID cid) {
		addCID(cid, DIRECTION.BOTH);
	}

	public void addCID(CID cid, DIRECTION direction) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Adding CID to authenticated: {1} [{0}]", new Object[]{this, cid});
		}

		switch (direction) {
			case IN:
				authenticatedCIDsIN.add(cid);
				break;
			case OUT:
				authenticatedCIDsOUT.add(cid);
				break;
			case BOTH:
			case ANY:
			default:
				authenticatedCIDsIN.add(cid);
				authenticatedCIDsOUT.add(cid);
				break;
		}
	}

	public Set<CID> getCIDs() {
		final CopyOnWriteArraySet<CID> cids = new CopyOnWriteArraySet<>();
		cids.addAll(authenticatedCIDsIN);
		cids.retainAll(authenticatedCIDsOUT);
		return cids;
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
		return authenticatedCIDsOUT.contains(cid) && authenticatedCIDsIN.contains(cid);
	}

	public boolean isAuthenticated() {
		return authenticatedCIDsOUT.size() > 0 && authenticatedCIDsIN.size() > 0;
	}

	public boolean isHandshakingOnly() {
		return getSessionData().get(HANDSHAKING_ONLY_KEY) != null;
	}

	public boolean isStreamNegotiationCompleted() {
		return streamNegotiationCompleted;
	}

	public void streamNegotiationCompleted() {
		log.log(Level.FINEST, "Marking the service as negotiated: " + this);
		this.streamNegotiationCompleted = true;
	}

	public void setDBKey(String key) {
		dbKey = key;
	}

	@Override
	public String toString() {
		CID cid = (CID) getSessionData().get("cid");

		return "CID: " + cid + ", IN: " + authenticatedCIDsIN.size() + ", OUT: " + authenticatedCIDsOUT.size() +
				", authenticated: " + isAuthenticated() + ", remote-session-id: " + getSessionId()
				+ ", streamNegotiationCompleted: " + streamNegotiationCompleted + ", " + super.toString();
	}
}
