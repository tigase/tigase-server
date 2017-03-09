package tigase.auth.mechanisms;

import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;

import tigase.auth.XmppSaslException;
import tigase.auth.XmppSaslException.SaslError;
import tigase.auth.callbacks.VerifyPasswordCallback;

/**
 * SASL-PLAIN mechanism.
 * <br>
 * Called {@linkplain Callback callbacks} in order:
 * <ul>
 * <li>{@link NameCallback}</li>
 * <li>{@link VerifyPasswordCallback}</li>
 * <li>{@link AuthorizeCallback}</li>
 * </ul>
 */
public class SaslPLAIN extends AbstractSasl {

	private static final String MECHANISM = "PLAIN";

	SaslPLAIN(Map<? super String, ?> props, CallbackHandler callbackHandler) {
		super(props, callbackHandler);
	}

	@Override
	public byte[] evaluateResponse(byte[] response) throws SaslException {

		String[] data = split(response, "");

		if (data.length != 3)
			throw new XmppSaslException(SaslError.malformed_request, "Invalid number of message parts");

		final String authzid = data[0];
		final String authcid = data[1];
		final String passwd = data[2];

		if (authcid.length() < 1)
			throw new XmppSaslException(SaslError.malformed_request, "Authentication identity string is empty");

		if (authcid.length() > 255)
			throw new XmppSaslException(SaslError.malformed_request, "Authentication identity string is too long");

		if (!isEmpty(authzid) && authzid.length() > 255)
			throw new XmppSaslException(SaslError.malformed_request, "Authorization identity string is too long");

		if (passwd.length() > 255)
			throw new XmppSaslException(SaslError.malformed_request, "Password string is too long");

		final NameCallback nc = new NameCallback("Authentication identity", authcid);
		final VerifyPasswordCallback vpc = new VerifyPasswordCallback(passwd);

		handleCallbacks(nc, vpc);

		if (vpc.isVerified() == false) {
			throw new XmppSaslException(SaslError.not_authorized, "Password not verified");
		}

		final String authorizationJID = isEmpty(authzid) ? nc.getName() : authzid;

		final AuthorizeCallback ac = new AuthorizeCallback(nc.getName(), authorizationJID);
		handleCallbacks(ac);

		if (ac.isAuthorized() == true) {
			authorizedId = ac.getAuthorizedID();
		} else {
			throw new XmppSaslException(SaslError.invalid_authzid, "PLAIN: " + authcid + " is not authorized to act as "
					+ authorizationJID);
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
