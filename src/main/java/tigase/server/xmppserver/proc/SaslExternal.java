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
import tigase.stats.StatisticsList;
import tigase.util.Base64;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "sasl-external", parent = S2SConnectionManager.class, active = true)
public class SaslExternal
		extends AuthenticationProcessor {

	protected static final String[] FEATURES_SASL_PATH = {FEATURES_EL, "mechanisms"};
	private static final String METHOD_NAME = "SASL-EXTERNAL";
	private final static String XMLNS_SASL = "urn:ietf:params:xml:ns:xmpp-sasl";
	private static final Logger log = Logger.getLogger(SaslExternal.class.getName());
	private static Element successElement = new Element("success", new String[]{"xmlns"}, new String[]{XMLNS_SASL});
	@ConfigField(desc = "Enable compatibility with legacy servers", alias = "legacy-compat")
	private boolean legacyCompat = true;
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

	@Override
	public String getMethodName() {
		return METHOD_NAME;
	}

	public void setSkipForDomains(String[] skipForDomains) {
		this.skipForDomains = skipForDomains != null
							  ? Arrays.stream(skipForDomains).map(String::toLowerCase).toArray(String[]::new)
							  : null;
	}

	@Override
	public void streamFeatures(S2SIOService serv, List<Element> results) {
		Element mechanisms = new Element("mechanisms", new Element[]{new Element("mechanism", "EXTERNAL")},
										 new String[]{"xmlns"}, new String[]{XMLNS_SASL});

		final boolean canAddSaslToFeatures = canAddSaslToFeatures(serv);

		if (canAddSaslToFeatures) {
			results.add(mechanisms);
			authenticatorSelectorManager.getAuthenticationProcessors(serv).add(this);
		}
	}

	public int order() {
		return Order.SaslExternal.ordinal();
	}

	@Override
	public void restartAuth(Packet p, S2SIOService serv, Queue<Packet> results) {
		try {
			sendAuthRequest(serv, results);
		} catch (Exception e) {
			log.log(Level.WARNING, e, () -> String.format("%s, Error while restarting authentication", serv));
			results.add(failurePacket(null));
			authenticatorSelectorManager.authenticationFailed(p, serv, this, results);
		}
	}

	@Override
	public boolean canHandle(Packet p, S2SIOService serv, Queue<Packet> results) {
		final CID cid = (CID) serv.getSessionData().get("cid");
		final boolean skipTLS = (cid != null) && skipTLSForHost(cid.getRemoteHost());

		if (cid != null && (isSkippedDomain(cid.getLocalHost()) || isSkippedDomain(cid.getRemoteHost()))) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Skipping SASL-EXTERNAL for domain: {1} because it was ignored [{0}]",
						new Object[]{serv, cid});
			}
			return false;
		}

		if (p.isElement(FEATURES_EL, FEATURES_NS) && p.getElement().getChildren() != null &&
				!p.getElement().getChildren().isEmpty()) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Stream features received packet: {1} [{0}]", new Object[]{serv, p});
			}

			// Some servers send empty SASL list!
			if (!isAnyMechanismsPresent(p)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "No SASL mechanisms found in features. Skipping SASL. [{0}]",
							new Object[]{serv, p});
				}
				return false;
			}

			CertCheckResult certCheckResult = (CertCheckResult) serv.getSessionData()
					.get(S2SIOService.CERT_CHECK_RESULT);

			if (p.isXMLNSStaticStr(FEATURES_STARTTLS_PATH, START_TLS_NS) && (certCheckResult == null) && !skipTLS) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Waiting for starttls, packet: {1} [{0}]", new Object[]{serv, p});
				}

				return false;
			}

			// it is reasonable to skip SASL EXTERNAL for handshaking only connections
			if (certCheckResult == CertCheckResult.invalid || serv.isHandshakingOnly()) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Connection is handshaking only: {1}, certCheckResult: {2}, packet: {3} [{0}]",
							new Object[]{serv, serv.isHandshakingOnly(), certCheckResult, p});
				}
				return false;
			}

			return true;

		}
		return false;
	}

	@Override
	public void getStatistics(String compName, StatisticsList list) {
		super.getStatistics(compName, list);
		authenticatorSelectorManager.getStatistics(compName, list);
	}

	@Override
	public boolean process(Packet p, S2SIOService serv, Queue<Packet> results) {
		try {
			if (authenticatorSelectorManager.isAllowed(p, serv, this, results)) {
				sendAuthRequest(serv, results);
				return true;
			} else if (p.isElement("auth", XMLNS_SASL)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Received auth request: {1} [{0}]", new Object[]{serv, p});
				}
				processAuth(p, serv, results);
				return true;
			} else if (p.isElement("success", XMLNS_SASL)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Received success response: {1} [{0}]", new Object[]{serv, p});
				}
				processSuccess(p, serv, results);
				return true;
			} else if (p.isElement("failure", XMLNS_SASL)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Received failure response: {1} [{0}]", new Object[]{serv, p});
				}

				authenticatorSelectorManager.authenticationFailed(p, serv, this, results);

				return true;
			}
		} catch (Exception e) {
			log.log(Level.WARNING, e, () -> String.format("%s, Error while processing packet: %s", serv, p));
			results.add(failurePacket(null));
			authenticatorSelectorManager.authenticationFailed(p, serv, this, results);
			return true;
		}

		return false;
	}

	private boolean isSkippedDomain(String domain) {
		return domain != null && skipForDomains != null &&
				Arrays.binarySearch(skipForDomains, domain.toLowerCase()) >= 0;
	}

	/**
	 * "Server2 advertises SASL mechanisms. If the 'from' attribute of the stream header sent by Server1 can be
	 * matched against one of the identifiers provided in the certificate following the matching rules from
	 * RFC 6125, Server2 SHOULD advertise the SASL EXTERNAL mechanism. If no match is found, Server2 MAY either
	 * close Server1's TCP connection or continue with a Server Dialback (XEP-0220) [8] negotiation."
	 * If there was no `from` in the incomming stream then we should not advertise SASL-EXTERNAL and let
	 * other party possibly continue with Diallback
	 *
	 * @param serv for which to determine if SASL-EXTERNAL can be added to features
	 *
	 * @return {@code true} if TLS is established, local certificate is valid and domains have not been excluded
	 */
	private boolean canAddSaslToFeatures(S2SIOService serv) {
		final ConcurrentMap<String, Object> sessionData = serv.getSessionData();
		CID cid = (CID) sessionData.get("cid");
		boolean skipDomain =
				cid != null && (isSkippedDomain(cid.getLocalHost()) || isSkippedDomain(cid.getRemoteHost()));
		CertCheckResult certCheckResult = (CertCheckResult) sessionData.get(S2SIOService.CERT_CHECK_RESULT);
		boolean tlsEstablished = isTlsEstablished(certCheckResult);
		CertCheckResult localCertCheckResult = (CertCheckResult) sessionData.get(S2SIOService.LOCAL_CERT_CHECK_RESULT);
		boolean localCertTrusted = localCertCheckResult == CertCheckResult.trusted;
		boolean canAddSaslToFeatures =
				tlsEstablished && localCertTrusted && !serv.isAuthenticated() && !serv.isHandshakingOnly() &&
						!skipDomain;

		if (!canAddSaslToFeatures && log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
					"Not adding SASL-EXTERNAL feature, tlsEstablished: {1} (result: {2}), skipDomain: {3}, localCertTrusted: {4} (result: {5}) [{0}]",
					new Object[]{serv, tlsEstablished, certCheckResult, skipDomain, localCertTrusted,
								 localCertCheckResult});
		}
		return canAddSaslToFeatures;
	}

	private void sendAuthRequest(S2SIOService serv, Queue<Packet> results) throws TigaseStringprepException {
		String cdata = "=";
		CID cid = (CID) serv.getSessionData().get("cid");
		if (cid != null && legacyCompat) {
			cdata = Base64.encode(cid.getLocalHost().getBytes(StandardCharsets.UTF_8));
		}
		Element auth = new Element("auth", cdata, new String[]{"xmlns", "mechanism"},
								   new String[]{XMLNS_SASL, "EXTERNAL"});
		results.add(Packet.packetInstance(auth));
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Starting SASL EXTERNAL: {1} [{0}]", new Object[]{serv, auth});
		}
	}

	private void processSuccess(Packet p, S2SIOService serv, Queue<Packet> results)
			throws TigaseStringprepException, LocalhostException, NotLocalhostException {
		final CID cid = (CID) serv.getSessionData().get("cid");

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Sending new stream [{0}]", new Object[]{serv});
		}
		// old ejabberd has problem if we first send `xmlns` and then `xmlns:stream` so we have to do it reversed...
		String data = "<stream:stream" + " xmlns:stream='http://etherx.jabber.org/streams'" + " xmlns='jabber:server'" +
				" from='" + cid.getLocalHost() + "'" + " to='" + cid.getRemoteHost() + "'" + " version='1.0'>";

		serv.xmppStreamOpen(data);

		CIDConnections cid_conns = handler.getCIDConnections(cid, true);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Making connection authenticated. cid={1}  [{0}]", new Object[]{serv, cid});
		}
		authenticatorSelectorManager.authenticateConnection(serv, cid_conns, cid);
	}

	private void processAuth(Packet p, S2SIOService serv, Queue<Packet> results)
			throws TigaseStringprepException, LocalhostException, NotLocalhostException {
		final X509Certificate peerCertificate = (X509Certificate) serv.getPeerCertificate();

		if (peerCertificate == null) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "No peer certificate! [{0}]", new Object[]{serv});
			}
			results.add(failurePacket("No peer certificate"));
			authenticatorSelectorManager.authenticationFailed(p, serv, this, results);
			return;
		}

		CertCheckResult certCheckResult = (CertCheckResult) serv.getSessionData().get(S2SIOService.CERT_CHECK_RESULT);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Trust: {1} for peer certificate: {2}, AltNames: {3} [{0}]",
					new Object[]{serv, certCheckResult, peerCertificate.getSubjectDN(),
								 CertificateUtil.getCertAltCName(peerCertificate)});
		}

		if (certCheckResult != CertCheckResult.trusted && certCheckResult != CertCheckResult.self_signed) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Certificate is not trusted [{0}]", new Object[]{serv});
			}
			results.add(failurePacket("Certificate is not trusted"));
			authenticatorSelectorManager.authenticationFailed(p, serv, this, results);
			return;
		}

		final CID cid = (CID) serv.getSessionData().get("cid");
		if (cid == null) {
			// can't process such request
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "CID is unknown, can''t proceed [{0}]", new Object[]{serv});
			}
			results.add(failurePacket("Unknown origin hostname (lack of `from` element)"));
			authenticatorSelectorManager.authenticationFailed(p, serv, this, results);
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
				log.log(Level.FINE, "Certificate name doesn't match to '{1}' [{0}]",
						new Object[]{serv, cid.getRemoteHost()});
			}
			results.add(failurePacket("Certificate name doesn't match to domain name"));
			authenticatorSelectorManager.authenticationFailed(p, serv, this, results);
			return;
		}

		CIDConnections cid_conns = handler.getCIDConnections(cid, true);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Making connection authenticated. cid={1} [{0}]", new Object[]{serv, cid});
		}
		authenticatorSelectorManager.authenticateConnection(serv, cid_conns, cid);

		results.add(Packet.packetInstance(successElement));
	}

	private Packet failurePacket(String description) {
		Element result = new Element("failure", new Element[]{new Element("invalid-authzid")}, new String[]{"xmlns"},
									 new String[]{XMLNS_SASL});

		if (description != null) {
			result.addChild(new Element("text", description));
		}

		return Packet.packetInstance(result, null, null);
	}

}
