package tigase.auth.impl;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import tigase.auth.SessionAware;
import tigase.auth.callbacks.ValidateCertificateData;
import tigase.auth.mechanisms.SaslEXTERNAL;
import tigase.cert.CertificateUtil;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.BareJID;
import tigase.xmpp.XMPPResourceConnection;

public class CertBasedCallbackHandler implements CallbackHandler, SessionAware {

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
					final String[] authJIDs = CertificateUtil.extractXmppAddrs((X509Certificate) cert).toArray(new String[] {});

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
