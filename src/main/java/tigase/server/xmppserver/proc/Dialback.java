
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.server.xmppserver.proc;

//~--- non-JDK imports --------------------------------------------------------

import tigase.cert.CertCheckResult;

import tigase.net.ConnectionType;

import tigase.server.Packet;
import tigase.server.xmppserver.CID;
import tigase.server.xmppserver.CIDConnections;
import tigase.server.xmppserver.LocalhostException;
import tigase.server.xmppserver.NotLocalhostException;
import tigase.server.xmppserver.S2SIOService;

import tigase.util.Algorithms;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

//~--- JDK imports ------------------------------------------------------------

import java.security.NoSuchAlgorithmException;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Dec 9, 2010 2:00:52 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Dialback extends S2SAbstractProcessor {
	private static final Logger log = Logger.getLogger(Dialback.class.getName());
	private static final Element features = new Element("dialback", new String[] { "xmlns" },
		new String[] { "urn:xmpp:features:dialback" });
	private static final Element features_required = new Element("dialback",
		new Element[] { new Element("required") }, new String[] { "xmlns" },
		new String[] { "urn:xmpp:features:dialback" });

	//~--- fields ---------------------------------------------------------------

	private long authenticationTimeOut = 30;

	// Ejabberd does not request dialback after TLS (at least some versions don't)
	private boolean ejabberd_bug_workaround_active = false;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	public Dialback() {
		super();

		if (System.getProperty("s2s-ejabberd-bug-workaround-active") == null) {
			System.setProperty("s2s-ejabberd-bug-workaround-active", "true");
		}

		ejabberd_bug_workaround_active = Boolean.getBoolean("s2s-ejabberd-bug-workaround-active");
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param p
	 * @param serv
	 * @param results
	 *
	 * @return
	 */
	@Override
	public boolean process(Packet p, S2SIOService serv, Queue<Packet> results) {
		CID cid = (CID) serv.getSessionData().get("cid");
		boolean skipTLS = (cid == null) ? false : skipTLSForHost(cid.getRemoteHost());

		// If this is a dialback packet, process it accordingly
		if (p.getXMLNS() == XMLNS_DB_VAL) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Processing dialback packet: {1}", new Object[] { serv, p });
			}

			processDialback(p, serv);

			return true;
		}

		// If this is stream features, then it depends....
		if (p.isElement(FEATURES_EL, FEATURES_NS)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Stream features received packet: {1}",
						new Object[] { serv, p });
			}

			CertCheckResult certCheckResult =
				(CertCheckResult) serv.getSessionData().get(S2SIOService.CERT_CHECK_RESULT);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, TLS Certificate check: {1}, packet: {2}", new Object[] { serv,
						certCheckResult, p });
			}

			// If TLS is not yet started (announced in stream features) then it is not
			// the right time for dialback yet
			// Some servers send starttls in stream features, even if TLS is already
			// initialized....
			if (p.isXMLNS(FEATURES_EL + "/" + START_TLS_EL, START_TLS_NS) && (certCheckResult == null)
					&&!skipTLS) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Waiting for starttls, packet: {1}", new Object[] { serv, p });
				}

				return true;
			}

			// If TLS has been started and it is a trusted peer, we do not need dialback here
			// but... sometimes the remote server may request dialback anyway, especially if they
			// do not trust us.
			if ((certCheckResult == CertCheckResult.trusted)
					&&!(p.isXMLNS(FEATURES_EL + "/" + DIALBACK_TLS_EL, DIALBACK_TLS_NS))) {
				if (ejabberd_bug_workaround_active) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								"{0}, Ejabberd bug workaround active, proceeding to dialback anyway, packet: {1}",
									new Object[] { serv,
								p });
					}
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST,
								"{0}, TLS trusted peer, no dialback needed or requested, packet: {1}",
									new Object[] { serv,
								p });
					}

					CIDConnections cid_conns;

					try {
						cid_conns = handler.getCIDConnections(cid, true);
						cid_conns.connectionAuthenticated(serv);
					} catch (NotLocalhostException ex) {

						// Should not happen....
						log.log(Level.INFO, "{0}, Incorrect local hostname, packet: {1}", new Object[] { serv,
								p });
						serv.forceStop();
					} catch (LocalhostException ex) {

						// Should not happen....
						log.log(Level.INFO, "{0}, Incorrect remote hostname name, packet: {1}",
								new Object[] { serv,
								p });
						serv.forceStop();
					}

					return true;
				}
			}

			// Nothing else can be done right now except the dialback
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Initializing dialback, packet: {1}", new Object[] { serv, p });
			}

			initDialback(serv, serv.getSessionId());
		}

		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	@Override
	public void serviceStarted(S2SIOService serv) {
		handler.addTimerTask(new AuthenticationTimer(serv), authenticationTimeOut, TimeUnit.SECONDS);
	}

	/**
	 * Method description
	 *
	 *
	 *
	 * @param serv
	 * @param results
	 */
	@Override
	public void streamFeatures(S2SIOService serv, List<Element> results) {
		CertCheckResult certCheckResult =
			(CertCheckResult) serv.getSessionData().get(S2SIOService.CERT_CHECK_RESULT);

		if (certCheckResult == CertCheckResult.trusted) {
			results.add(features);
		} else {
			results.add(features_required);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 * @param attribs
	 *
	 * @return
	 */
	@Override
	public String streamOpened(S2SIOService serv, Map<String, String> attribs) {
		if (attribs.containsKey("version")) {

			// Let's wait for stream features
			return null;
		}

		switch (serv.connectionType()) {
			case connect :
				initDialback(serv, attribs.get("id"));

				break;

			default :

			// Ignore
		}

		return null;
	}

	private void initDialback(S2SIOService serv, String remote_id) {
		try {
			CID cid = (CID) serv.getSessionData().get("cid");
			CIDConnections cid_conns = handler.getCIDConnections(cid, false);

			// It must be always set for connect connection type
			String uuid = UUID.randomUUID().toString();
			String key = null;

			try {
				key = Algorithms.hexDigest(remote_id, uuid, "SHA");
			} catch (NoSuchAlgorithmException e) {
				key = uuid;
			}    // end of try-catch

			serv.setDBKey(key);
			cid_conns.addDBKey(remote_id, key);

			if ( !serv.isHandshakingOnly()) {
				Element elem = new Element(DB_RESULT_EL_NAME, key, new String[] { XMLNS_DB_ATT },
					new String[] { XMLNS_DB_VAL });

				serv.getS2SConnection().addControlPacket(Packet.packetInstance(elem,
						JID.jidInstanceNS(cid.getLocalHost()), JID.jidInstanceNS(cid.getRemoteHost())));
			}

			serv.getS2SConnection().sendAllControlPackets();
		} catch (NotLocalhostException ex) {
			generateStreamError(false, "host-unknown", serv);
		} catch (LocalhostException ex) {
			generateStreamError(false, "invalid-from", serv);
		}
	}

	private void processDialback(Packet p, S2SIOService serv) {

		// Get the cid for which the connection has been created, the cid calculated
		// from the packet may be different though if the remote server tries to multiplexing
		CID cid_main = (CID) serv.getSessionData().get("cid");
		CID cid_packet = new CID(p.getStanzaTo().getDomain(), p.getStanzaFrom().getDomain());

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, DIALBACK packet: {1}, CID_packet: {2}", new Object[] { serv, p,
					cid_packet });
		}

		CIDConnections cid_conns = null;

		// Some servers (ejabberd) do not send from/to attributes in the stream:open which
		// violates the spec, they seem not to care though, so here we handle the case.
		if (cid_main == null) {

			// This actually can only happen for 'accept' connection type
			// what we did not get in stream open we can get from here
			cid_main = cid_packet;
			serv.getSessionData().put("cid", cid_main);

			// For debuging purposes only....
			serv.getSessionData().put("local-hostname", cid_main.getLocalHost());
			serv.getSessionData().put("remote-hostname", cid_main.getRemoteHost());
		}

		try {
			cid_conns = handler.getCIDConnections(cid_main, true);
		} catch (NotLocalhostException ex) {
			log.log(Level.FINER, "{0} Incorrect local hostname: {1}", new Object[] { serv, p });
			generateStreamError(false, "host-unknown", serv);

			return;
		} catch (LocalhostException ex) {
			log.log(Level.FINER, "{0} Incorrect remote hostname: {1}", new Object[] { serv, p });
			generateStreamError(false, "invalid-from", serv);

			return;
		}

		if (serv.connectionType() == ConnectionType.accept) {
			cid_conns.addIncoming(serv);
		}

		String remote_key = p.getElemCData();

		// Dummy dialback implementation for now....
		if ((p.getElemName() == RESULT_EL_NAME) || (p.getElemName() == DB_RESULT_EL_NAME)) {
			if (p.getType() == null) {
				String conn_sessionId = serv.getSessionId();

				handler.sendVerifyResult(DB_VERIFY_EL_NAME, cid_main, cid_packet, null, conn_sessionId,
						null, p.getElemCData(), true);
			} else {
				if (p.getType() == StanzaType.valid) {

					// serv.addCID(new CID(p.getStanzaTo().getDomain(), p.getStanzaFrom().getDomain()));
					cid_conns.connectionAuthenticated(serv);
				} else {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE,
								"Invalid result for DB authentication: {0}, stopping connection: {1}",
									new Object[] { cid_packet,
								serv });
					}

					serv.stop();
				}
			}
		}

		if ((p.getElemName() == VERIFY_EL_NAME) || (p.getElemName() == DB_VERIFY_EL_NAME)) {
			if (p.getType() == null) {
				String local_key = handler.getLocalDBKey(cid_main, cid_packet, remote_key, p.getStanzaId(),
					serv.getSessionId());

				if (local_key == null) {
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "The key is not available for connection CID: {0}, "
								+ "or the packet CID: {1} maybe it is "
									+ "located on a different node...", new Object[] { cid_main,
								cid_packet });
					}
				} else {
					handler.sendVerifyResult(DB_VERIFY_EL_NAME, cid_main, cid_packet,
							local_key.equals(remote_key), p.getStanzaId(), serv.getSessionId(), null, false);
				}
			} else {
				handler.sendVerifyResult(DB_RESULT_EL_NAME, cid_main, cid_packet,
						(p.getType() == StanzaType.valid), null, p.getStanzaId(), null, false);
				cid_conns.connectionAuthenticated(p.getStanzaId());

				if (serv.isHandshakingOnly()) {
					serv.stop();
				}
			}
		}
	}

	//~--- inner classes --------------------------------------------------------

	private class AuthenticationTimer extends TimerTask {
		private S2SIOService serv = null;

		//~--- constructors -------------------------------------------------------

		private AuthenticationTimer(S2SIOService serv) {
			this.serv = serv;
		}

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 */
		@Override
		public void run() {
			if ( !serv.isAuthenticated() && serv.isConnected()) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Connection not authenticated within timeout, stopping: {0}", serv);
				}

				serv.stop();
			}
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
