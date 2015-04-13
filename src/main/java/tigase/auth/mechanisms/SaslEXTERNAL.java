package tigase.auth.mechanisms;

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;

import tigase.auth.XmppSaslException;
import tigase.auth.XmppSaslException.SaslError;
import tigase.auth.callbacks.ValidateCertificateData;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.BareJID;

public class SaslEXTERNAL extends AbstractSasl {

	private static final String MECHANISM = "EXTERNAL";

	public static final String PEER_CERTIFICATE_KEY = "PEER_CERTIFICATE_ENTRY_KEY";

	SaslEXTERNAL(Map<? super String, ?> props, CallbackHandler callbackHandler) {
		super(props, callbackHandler);
	}

	@Override
	public byte[] evaluateResponse(byte[] response) throws SaslException {
		BareJID jid;
		try {
			if (response != null && response.length > 0) {
				jid = BareJID.bareJIDInstance(new String(response));
			} else {
				jid = null;
			}
		} catch (TigaseStringprepException e) {
			throw new XmppSaslException(SaslError.malformed_request);
		}

		final ValidateCertificateData ac = new ValidateCertificateData(jid);
		handleCallbacks(ac);

		if (ac.isAuthorized() == true) {
			authorizedId = ac.getAuthorizedID();
		} else {
			throw new XmppSaslException(SaslError.invalid_authzid);
		}

		complete = true;

		return null;
	}

	@Override
	public String getAuthorizationID() {
		return authorizedId;
	}

	@Override
	public String getMechanismName() {
		return MECHANISM;
	}

	@Override
	public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
		return null;
	}

	@Override
	public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
		return null;
	}

}
