package tigase.auth.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import tigase.auth.SessionAware;
import tigase.auth.callbacks.ValidateCertificateData;
import tigase.auth.mechanisms.SaslEXTERNAL;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.BareJID;
import tigase.xmpp.XMPPResourceConnection;

public class CertBasedCallbackHandler implements CallbackHandler, SessionAware {

	private XMPPResourceConnection session;

	@Override
	public void setSession(XMPPResourceConnection session) {
		this.session = session;
	}

	protected Logger log = Logger.getLogger(this.getClass().getName());

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
					final String[] authJIDs = (String[]) session.getSessionData(SaslEXTERNAL.SESSION_AUTH_JIDS_KEY);

					for (String string : authJIDs) {
						if (BareJID.bareJIDInstance(string).getDomain().equals(domain)) {
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
}
