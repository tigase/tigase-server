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
package tigase.auth;

import tigase.auth.mechanisms.SaslEXTERNAL;
import tigase.auth.mechanisms.SaslSCRAMPlus;
import tigase.auth.mechanisms.TigaseSaslServerFactory;
import tigase.cert.CertificateUtil;
import tigase.db.AuthRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.vhosts.VHostItem;
import tigase.xmpp.XMPPResourceConnection;

import javax.security.sasl.SaslServerFactory;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Stream;

@Bean(name = "mechanism-selector", parent = TigaseSaslProvider.class, active = true)
public class DefaultMechanismSelector
		implements MechanismSelector {

	@ConfigField(desc = "List of allowed SASL mechanisms", alias = "allowed-mechanisms")
	private HashSet<String> allowedMechanisms = new HashSet<String>();

	@ConfigField(desc = "List of SASL mechanisms allowed with non-plain password stored in authentication repository", alias = "non-plain-password-allowed-mechanisms")
	private HashSet<String> allowedMechanismsWithNonPlainPasswordInRepository = new HashSet<>();

	@Inject
	private AuthRepository authRepository;

	public DefaultMechanismSelector() {
		Stream.of("ANONYMOUS", "PLAIN", "EXTERNAL").forEach(allowedMechanismsWithNonPlainPasswordInRepository::add);
	}

	@Override
	public Collection<String> filterMechanisms(Enumeration<SaslServerFactory> serverFactories,
											   XMPPResourceConnection session) {
		final Map<String, ?> props = new HashMap<String, Object>();
		final ArrayList<String> result = new ArrayList<String>();
		while (serverFactories.hasMoreElements()) {
			SaslServerFactory ss = serverFactories.nextElement();
			String[] x = ss.getMechanismNames(props);
			for (String name : x) {
				// JKD9 introduced a change! now if factory implements more than one mechanism it will be returned multiple times!!
				if (result.contains(name)) {
					continue;
				}
				if (match(ss, name, session) && isAllowedForDomain(name, session.getDomain())) {
					result.add(name);
				}
			}
		}
		return result;
	}

	protected boolean isAllowedForDomain(final String mechanismName, final VHostItem vhost) {
		final String[] saslAllowedMechanisms = vhost.getSaslAllowedMechanisms();
		if (saslAllowedMechanisms != null && saslAllowedMechanisms.length > 0) {
			for (String allowed : saslAllowedMechanisms) {
				if (allowed.equals(mechanismName)) {
					return true;
				}
			}
			return false;
		} else if (!allowedMechanisms.isEmpty()) {
			return allowedMechanisms.contains(mechanismName);
		}
		return true;
	}

	protected boolean match(SaslServerFactory factory, String mechanismName, XMPPResourceConnection session) {
		if (session.isTlsRequired() && !session.isEncrypted()) {
			return false;
		}
		if (factory instanceof TigaseSaslServerFactory) {
			switch (mechanismName) {
				case "EXTERNAL":
					return isJIDInCertificate(session);
				case "ANONYMOUS":
					return session.getDomain().isAnonymousEnabled();
				default:
					if (mechanismName.startsWith("SCRAM-") && mechanismName.endsWith("-PLUS") &&
							!SaslSCRAMPlus.isAvailable(session)) {
						return false;
					}

					return authRepository.isMechanismSupported(session.getDomain().getKey(), mechanismName);
			}
		}
		return false;
	}

	private boolean isJIDInCertificate(final XMPPResourceConnection session) {
		Certificate cert = (Certificate) session.getSessionData(SaslEXTERNAL.PEER_CERTIFICATE_KEY);
		if (cert == null) {
			return false;
		}

		final List<String> authJIDs = CertificateUtil.extractXmppAddrs((X509Certificate) cert);
		return authJIDs != null && !authJIDs.isEmpty();
	}
}
