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
package tigase.server.xmppserver.proc;

import tigase.cert.CertCheckResult;
import tigase.cert.CertificateUtil;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Packet;
import tigase.server.xmppserver.*;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "sasl-external", parent = S2SConnectionManager.class, active = true)
public class SaslExternal
		extends S2SAbstractProcessor {

	protected static final String[] FEATURES_SASL_PATH = {FEATURES_EL, "mechanisms"};
	private static final String METHOD_NAME = "SASL-EXTERNAL";
	private final static String XMLNS_SASL = "urn:ietf:params:xml:ns:xmpp-sasl";

	private static final Logger log = Logger.getLogger(SaslExternal.class.getName());
	private static Element successElement = new Element("success", new String[]{"xmlns"}, new String[]{XMLNS_SASL});

	@ConfigField(desc = "Skip SASL-EXTERNAL for defined domains", alias = "skip-for-domains")
	private String[] skipForDomains;

	private static boolean isAnyMechanismsPresent(Packet p) {
		final List<Element> childrenStaticStr = p.getElement().getChildrenStaticStr(FEATURES_SASL_PATH);
		return p.isXMLNSStaticStr(FEATURES_SASL_PATH, XMLNS_SASL) && childrenStaticStr != null &&
				!childrenStaticStr.isEmpty();
	}

	private static boolean isTlsEstablished(final CertCheckResult certCheckResult) {
		return (certCheckResult == CertCheckResult.trusted || certCheckResult == CertCheckResult.untrusted ||
				certCheckResult == CertCheckResult.self_signed);
	}

	private boolean isSkippedDomain(String domain ) {
		if (domain != null && skipForDomains != null && skipForDomains.length > 0) {
			for (String skipForDomain : skipForDomains) {
				if (domain.contains(skipForDomain)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void streamFeatures(S2SIOService serv, List<Element> results) {
		Element mechanisms = new Element("mechanisms", new Element[]{new Element("mechanism", "EXTERNAL")},
										 new String[]{"xmlns"}, new String[]{XMLNS_SASL});

		CertCheckResult certCheckResult = (CertCheckResult) serv.getSessionData().get(S2SIOService.CERT_CHECK_RESULT);

		final CID cid = (CID) serv.getSessionData().get("cid");

		// "Server2 advertises SASL mechanisms. If the 'from' attribute of the stream header sent by Server1 can be
		// matched against one of the identifiers provided in the certificate following the matching rules from
		// RFC 6125, Server2 SHOULD advertise the SASL EXTERNAL mechanism. If no match is found, Server2 MAY either
		// close Server1's TCP connection or continue with a Server Dialback (XEP-0220) [8] negotiation."
		// If there was no `from` in the incomming stream then we should not advertise SASL-EXTERNAL and let
		// other party possibly continue with Diallback
		final boolean skipDomain = cid != null && (isSkippedDomain(cid.getLocalHost()) || isSkippedDomain(cid.getRemoteHost()));
		final boolean tlsEstablished = isTlsEstablished(certCheckResult);
		final boolean canAddSaslToFeatures =
				tlsEstablished && !serv.isAuthenticated() && !skipDomain;

		if (!canAddSaslToFeatures & log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, Not adding SASL-EXTERNAL feature, isTlsEstablished: {1}, skipDomain: {2}",
					new Object[]{serv, tlsEstablished, skipDomain});
		}

		if (canAddSaslToFeatures) {
			results.add(mechanisms);
		}
	}

	public int order() {
		return Order.SaslExternal.ordinal();
	}

	@Override
	public boolean process(Packet p, S2SIOService serv, Queue<Packet> results) {
		try {
			final CID cid = (CID) serv.getSessionData().get("cid");
			final boolean skipTLS = (cid != null) && skipTLSForHost(cid.getRemoteHost());

			if (cid != null && (isSkippedDomain(cid.getLocalHost()) || isSkippedDomain(cid.getRemoteHost()))) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Skipping SASL-EXTERNAL for domain: {1} because it was ignored", new Object[]{serv, cid});
				}
				return true;
			}

			if (p.isElement(FEATURES_EL, FEATURES_NS) && p.getElement().getChildren() != null &&
					!p.getElement().getChildren().isEmpty()) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Stream features received packet: {1}", new Object[]{serv, p});
				}

				// Some servers send empty SASL list!
				if (!isAnyMechanismsPresent(p)) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "{0}, No SASL mechanisms found in features. Skipping SASL.",
								new Object[]{serv, p});
					}
					return true;
				}

				CertCheckResult certCheckResult = (CertCheckResult) serv.getSessionData()
						.get(S2SIOService.CERT_CHECK_RESULT);

				if (p.isXMLNSStaticStr(FEATURES_STARTTLS_PATH, START_TLS_NS) && (certCheckResult == null) && !skipTLS) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "{0}, Waiting for starttls, packet: {1}", new Object[]{serv, p});
					}

					return true;
				}

				String method = (String) serv.getSessionData().get(S2S_METHOD_USED);
				if (method != null && method != METHOD_NAME) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "{0}, Another method {1} is used. Skipping.", new Object[]{serv, method});
					}
					return true;
				}

				if (!serv.isAuthenticated() && serv.getSessionData().get(S2S_METHOD_USED) == null) {
					sendAuthRequest(p, serv, results);
					return true;
				}

			} else if (p.isElement("auth", XMLNS_SASL)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Received auth request: {1}", new Object[]{serv, p});
				}
				processAuth(p, serv, results);
				return true;
			} else if (p.isElement("success", XMLNS_SASL)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Received success response: {1}", new Object[]{serv, p});
				}
				processSuccess(p, serv, results);
				return true;
			} else if (p.isElement("failure", XMLNS_SASL)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Received failure response: {1}", new Object[]{serv, p});
				}

				serv.forceStop();

				return true;
			}
		} catch (Exception e) {
			log.log(Level.WARNING, e, () -> String.format("{0}, Error while processing packet: {1}", serv, p));
			serv.forceStop();
			return true;
		}

		return false;
	}

	private void sendAuthRequest(Packet p, S2SIOService serv, Queue<Packet> results) throws TigaseStringprepException {
		Element auth = new Element("auth", "=", new String[]{"xmlns", "mechanism"},
								   new String[]{XMLNS_SASL, "EXTERNAL"});
		results.add(Packet.packetInstance(auth));
		serv.getSessionData().put(S2S_METHOD_USED, METHOD_NAME);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, Starting SASL EXTERNAL: {1}", new Object[]{serv, auth});
		}
	}

	private void processSuccess(Packet p, S2SIOService serv, Queue<Packet> results)
			throws TigaseStringprepException, LocalhostException, NotLocalhostException {
		final CID cid = (CID) serv.getSessionData().get("cid");

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, Sending new stream", new Object[]{serv});
		}
		// old ejabberd has problem if we first send `xmlns` and then `xmlns:stream` so we have to do it reversed...
		String data = "<stream:stream" + " xmlns:stream='http://etherx.jabber.org/streams'" + " xmlns='jabber:server'" +
				" from='" + cid.getLocalHost() + "'" + " to='" + cid.getRemoteHost() + "'" + " version='1.0'>";

		serv.xmppStreamOpen(data);

		CIDConnections cid_conns = handler.getCIDConnections(cid, true);
		cid_conns.connectionAuthenticated(serv, cid);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, Making connection authenticated. cid={1} ", new Object[]{serv, cid});
		}
	}

	private void processAuth(Packet p, S2SIOService serv, Queue<Packet> results)
			throws TigaseStringprepException, LocalhostException, NotLocalhostException {
		final X509Certificate peerCertificate = (X509Certificate) serv.getPeerCertificate();

		if (peerCertificate == null) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "{0}, No peer certificate!", new Object[]{serv});
			}
			results.add(Packet.packetInstance(failureElement("No peer certificate")));
			serv.forceStop();
			return;
		}

		CertCheckResult certCheckResult = (CertCheckResult) serv.getSessionData().get(S2SIOService.CERT_CHECK_RESULT);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, Trust: {1} for peer certificate: {2}, AltNames: {3}",
					new Object[]{serv, certCheckResult, peerCertificate.getSubjectDN(),
								 CertificateUtil.getCertAltCName(peerCertificate)});
		}

		if (certCheckResult != CertCheckResult.trusted && certCheckResult != CertCheckResult.self_signed) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "{0}, Certificate is not trusted", new Object[]{serv});
			}
			results.add(Packet.packetInstance(failureElement("Certificate is not trusted")));
			serv.forceStop();
			return;
		}

		final CID cid = (CID) serv.getSessionData().get("cid");
		if (cid == null) {
			// can't process such request
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "{0}, CID is unknown, can''t proceed",
						new Object[]{serv});
			}
			results.add(Packet.packetInstance(failureElement("Unknown origin hostname (lack of `from` element)")));
			serv.forceStop();
			return;
		}

		boolean nameValid;
		try {
			nameValid = CertificateUtil.verifyCertificateForDomain(peerCertificate, cid.getRemoteHost());
		} catch (CertificateParsingException e) {
			nameValid = false;
		}

		if (!nameValid) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "{0}, Certificate name doesn't match to '{1}'",
						new Object[]{serv, cid.getRemoteHost()});
			}
			results.add(Packet.packetInstance(failureElement("Certificate name doesn't match to domain name")));
			serv.forceStop();
			return;
		}

		CIDConnections cid_conns = handler.getCIDConnections(cid, true);
		cid_conns.connectionAuthenticated(serv, cid);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0}, Making connection authenticated. cid={1} ", new Object[]{serv, cid});
		}

		results.add(Packet.packetInstance(successElement));
	}

	private Element failureElement(String description) {
		Element result = new Element("failure", new Element[]{new Element("invalid-authzid")}, new String[]{"xmlns"},
									 new String[]{XMLNS_SASL});

		if (description != null) {
			result.addChild(new Element("text", description));
		}

		return result;
	}

}
