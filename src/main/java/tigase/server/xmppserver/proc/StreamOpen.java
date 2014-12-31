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
package tigase.server.xmppserver.proc;

//~--- non-JDK imports --------------------------------------------------------

import tigase.net.ConnectionType;

import tigase.server.xmppserver.CID;
import tigase.server.xmppserver.CIDConnections;
import tigase.server.xmppserver.LocalhostException;
import tigase.server.xmppserver.NotLocalhostException;
import tigase.server.xmppserver.S2SConnection;
import tigase.server.xmppserver.S2SIOService;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Dec 9, 2010 1:59:56 PM
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StreamOpen extends S2SAbstractProcessor {
	private static final Logger log = Logger.getLogger(StreamOpen.class.getName());

	// ~--- methods --------------------------------------------------------------

	@Override
	public int order() {
		return Order.StreamOpen.ordinal();
	}
	
	@Override
	public void serviceStarted(S2SIOService serv) {
		switch (serv.connectionType()) {
			case connect:
				CID cid = (CID) serv.getSessionData().get("cid");

				serv.getSessionData().put(S2SIOService.HOSTNAME_KEY, cid.getLocalHost());

				// Send init xmpp stream here
				// XMPPIOService serv = (XMPPIOService)service;
				String data =
						"<stream:stream" + " xmlns:stream='http://etherx.jabber.org/streams'"
								+ " xmlns='jabber:server'" + " xmlns:db='jabber:server:dialback'"
								+ " from='" + cid.getLocalHost() + "'" + " to='" + cid.getRemoteHost()
								+ "'" + " version='1.0'>";

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, sending: {1}", new Object[] { serv, data });
				}

				S2SConnection s2s_conn =
						(S2SConnection) serv.getSessionData().get(S2SIOService.S2S_CONNECTION_KEY);

				if (s2s_conn == null) {
					log.log(Level.WARNING,
							"Protocol error s2s_connection not set for outgoing connection: {0}", serv);
					serv.stop();
				} else {
					s2s_conn.setS2SIOService(serv);
					serv.setS2SConnection(s2s_conn);
				}

				serv.xmppStreamOpen(data);

				break;

			default:

				// Do nothing, more data should come soon...
				break;
		} // end of switch (service.connectionType())
	}

	@Override
	public void serviceStopped(S2SIOService serv) {
		CID cid = (CID) serv.getSessionData().get("cid");

		if (cid == null) {
			if (serv.connectionType() == ConnectionType.connect) {
				log.log(Level.WARNING, "Protocol error cid not set for outgoing connection: {0}",
						serv);
			}

			return;
		}

		try {
			CIDConnections cid_conns = handler.getCIDConnections(cid, false);

			if (cid_conns == null) {
                                if (log.isLoggable(Level.FINE)) {
                                        log.log(Level.FINE,
						"Protocol error cid_conns not found for outgoing connection: {0}", serv);
                                }
				return;
			} else {
				cid_conns.connectionStopped(serv);
			}
		} catch (NotLocalhostException ex) {
			log.log(Level.WARNING, "Not a local host for cid: {0}", cid);
		} catch (LocalhostException ex) {
			log.log(Level.WARNING, "A local host for cid: {0}", cid);
		}
	}

	@Override
	public String streamOpened(S2SIOService serv, Map<String, String> attribs) {
		CID cid = (CID) serv.getSessionData().get("cid");
		String remote_hostname = attribs.get("from");
		String local_hostname = attribs.get("to");
		String version = attribs.get(VERSION_ATT_NAME);
		if (version != null) {
			serv.getSessionData().put(VERSION_ATT_NAME, version);
		}

		if (cid == null) {
			if ((remote_hostname != null) && (local_hostname != null)) {
				cid = new CID(local_hostname, remote_hostname);
			}
		}

		try {
			CIDConnections cid_conns = handler.getCIDConnections(cid, false);

			switch (serv.connectionType()) {
				case connect: {

					// It must be always set for connect connection type
					String remote_id = attribs.get("id");

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "{0}, Connect Stream opened for: {1}, session id{2}",
								new Object[] { serv, cid, remote_id });
					}

					if (cid_conns == null) {

						// This should actually not happen. Let's be clear here about
						// handling unexpected
						// cases.
						log.log(
								Level.WARNING,
								"{0} This might be a bug in s2s code, should not happen."
										+ " Missing CIDConnections for stream open to ''connect'' service type.",
								serv);
						generateStreamError(false, "internal-server-error", serv);

						return null;
					}

					if (log.isLoggable(Level.FINEST)) {
						log.log(
								Level.FINEST,
								"{0}, stream open for cid: {1}, outgoint: {2}, incoming: {3}",
								new Object[] { serv, cid, cid_conns.getOutgoingCount(),
										cid_conns.getIncomingCount() });
					}

					serv.setSessionId(remote_id);

					return null;
				}

				case accept: {
					if (local_hostname != null) {
						serv.getSessionData().put(S2SIOService.HOSTNAME_KEY, local_hostname);
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "{0}, Unknown local hostname.", serv);
						}
					}

					String id = UUID.randomUUID().toString();

					serv.setSessionId(id);

					String stream_open =
							"<stream:stream" + " xmlns:stream='http://etherx.jabber.org/streams'"
									+ " xmlns='jabber:server'" + " xmlns:db='jabber:server:dialback'"
									+ " id='" + id + "'";

					if (cid != null) {
						stream_open +=
								" from='" + cid.getLocalHost() + "'" + " to='" + cid.getRemoteHost()
										+ "'";

						if (cid_conns == null) {
							cid_conns = handler.getCIDConnections(cid, true);
						}

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST,
									"{0}, Accept Stream opened for: {1}, session id: {2}", new Object[] {
											serv, cid, id });
						}

						serv.getSessionData().put("cid", cid);
						cid_conns.addIncoming(serv);
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST,
									"{0}, Accept Stream opened for unknown CID, session id: {1}",
									new Object[] { serv, id });
						}
					}

					// Spec examples show that the version should always be included but
					// this seems to break some servers.
					if (FORCE_VERSION || attribs.containsKey("version")) {
						stream_open += " version='1.0'";
					}
					stream_open += ">";

					return stream_open;
				}

				default:
					log.log(Level.SEVERE, "{0}, Warning, program shouldn't reach that point.", serv);

					break;
			} // end of switch (serv.connectionType())
		} catch (NotLocalhostException ex) {
			generateStreamError(false, "host-unknown", serv);
		} catch (LocalhostException ex) {
			generateStreamError(false, "invalid-from", serv);
		}

		return null;
	}
}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
