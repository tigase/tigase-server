package tigase.auth.mechanisms;

import java.util.Map;
import java.util.UUID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.sasl.SaslException;

import tigase.auth.XmppSaslException;
import tigase.auth.XmppSaslException.SaslError;

/**
 * SASL-ANONYMOUS mechanism.
 * <br>
 * Called {@linkplain Callback callbacks} in order:
 * <ul>
 * <li>{@link NameCallback}</li>
 * </ul>
 */
public class SaslANONYMOUS extends AbstractSasl {

	public static final String IS_ANONYMOUS_PROPERTY = "IS_ANONYMOUS";

	private static final String MECHANISM = "ANONYMOUS";

	SaslANONYMOUS(Map<? super String, ?> props, CallbackHandler callbackHandler) {
		super(props, callbackHandler);
		negotiatedProperty.put(IS_ANONYMOUS_PROPERTY, Boolean.TRUE);
	}

	@Override
	public byte[] evaluateResponse(byte[] response) throws SaslException {
		NameCallback nc = new NameCallback("ANONYMOUS identity", UUID.randomUUID().toString());
		handleCallbacks(nc);

		this.authorizedId = nc.getName() != null ? nc.getName() : nc.getDefaultName();

		if (this.authorizedId == null) {
			throw new XmppSaslException(SaslError.temporary_auth_failure);
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
