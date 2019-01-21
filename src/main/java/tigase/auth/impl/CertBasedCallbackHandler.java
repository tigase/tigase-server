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
package tigase.auth.impl;

import tigase.auth.SessionAware;
import tigase.auth.callbacks.ValidateCertificateData;
import tigase.auth.mechanisms.SaslEXTERNAL;
import tigase.cert.CertificateUtil;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CertBasedCallbackHandler
		implements CallbackHandler, SessionAware {

	protected Logger log = Logger.getLogger(this.getClass().getName());

	private XMPPResourceConnection session;

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		try {
			for (int i = 0; i < callbacks.length; i++) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Callback: {0}", callbacks[i].getClass().getSimpleName());
				}

				if (callbacks[i] instanceof ValidateCertificateData) {
					ValidateCertificateData authCallback = ((ValidateCertificateData) callbacks[i]);

					final String domain = session.getDomain().getVhost().getDomain();
					final BareJID defaultAuthzid = authCallback.getDefaultAuthzid();
					if (defaultAuthzid != null && !defaultAuthzid.getDomain().equals(domain)) {
						return;
					}

					Certificate cert = (Certificate) session.getSessionData(SaslEXTERNAL.PEER_CERTIFICATE_KEY);
					final String[] authJIDs = CertificateUtil.extractXmppAddrs((X509Certificate) cert)
							.toArray(new String[]{});

					for (String string : authJIDs) {
						if (defaultAuthzid != null) {
							if (string.equals(defaultAuthzid.toString())) {
								authCallback.setAuthorized(true);
								authCallback.setAuthorizedID(string);
							}
						} else if (BareJID.bareJIDInstance(string).getDomain().equals(domain)) {
							authCallback.setAuthorized(true);
							authCallback.setAuthorizedID(string);
						}
					}
				} else {
					throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
				}
			}
		} catch (TigaseStringprepException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setSession(XMPPResourceConnection session) {
		this.session = session;
	}
}
