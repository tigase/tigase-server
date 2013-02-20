package tigase.auth.mechanisms;

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;

import tigase.auth.XmppSaslException;
import tigase.auth.XmppSaslException.SaslError;
import tigase.auth.callbacks.ValidateCertificateData;

public class SaslEXTERNAL extends AbstractSasl {

	public static final String SESSION_AUTH_JIDS_KEY = "SESSION_AUTH_JIDS_KEY";

	private static final String MECHANISM = "EXTERNAL";

	public static final String SASL_EXTERNAL_ALLOWED = "SASL_EXTERNAL_ALLOWED";

	SaslEXTERNAL(Map<? super String, ?> props, CallbackHandler callbackHandler) {
		super(props, callbackHandler);
	}

	@Override
	public byte[] evaluateResponse(byte[] response) throws SaslException {
		final ValidateCertificateData ac = new ValidateCertificateData();
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
