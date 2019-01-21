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
package tigase.auth.mechanisms;

import tigase.auth.SaslInvalidLoginExcepion;
import tigase.auth.XmppSaslException;
import tigase.auth.XmppSaslException.SaslError;
import tigase.auth.callbacks.AuthorizationIdCallback;
import tigase.auth.callbacks.VerifyPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import java.util.Map;

/**
 * SASL-PLAIN mechanism. <br> Called {@linkplain Callback callbacks} in order: <ul> <li>{@link NameCallback}</li>
 * <li>{@link VerifyPasswordCallback}</li> <li>{@link AuthorizeCallback}</li> </ul>
 */
public class SaslPLAIN
		extends AbstractSasl {

	public static final String NAME = "PLAIN";

	SaslPLAIN(Map<? super String, ?> props, CallbackHandler callbackHandler) {
		super(props, callbackHandler);
	}

	@Override
	public byte[] evaluateResponse(byte[] response) throws SaslException {

		String[] data = split(response, "");

		if (data.length != 3) {
			throw new XmppSaslException(SaslError.malformed_request, "Invalid number of message parts");
		}

		final String authzid = data[0];
		final String authcid = data[1];
		final String passwd = data[2];

		if (authcid.length() < 1) {
			throw new XmppSaslException(SaslError.malformed_request, "Authentication identity string is empty");
		}

		if (authcid.length() > 255) {
			throw new XmppSaslException(SaslError.malformed_request, "Authentication identity string is too long");
		}

		if (!isEmpty(authzid) && authzid.length() > 255) {
			throw new XmppSaslException(SaslError.malformed_request, "Authorization identity string is too long");
		}

		if (passwd.length() > 255) {
			throw new XmppSaslException(SaslError.malformed_request, "Password string is too long");
		}

		final NameCallback nc = new NameCallback("Authentication identity", authcid);
		final AuthorizationIdCallback ai = new AuthorizationIdCallback("Authorization identity",
																	   isEmpty(authzid) ? null : authzid);
		final VerifyPasswordCallback vpc = new VerifyPasswordCallback(passwd);

		handleCallbacks(nc, ai, vpc);

		if (vpc.isVerified() == false) {
			throw new SaslInvalidLoginExcepion(SaslError.not_authorized, nc.getName(), PASSWORD_NOT_VERIFIED_MSG);
		}

		final String authorizationJID = ai.getAuthzId() == null ? nc.getName() : ai.getAuthzId();

		final AuthorizeCallback ac = new AuthorizeCallback(nc.getName(), authorizationJID);
		handleCallbacks(ac);

		if (ac.isAuthorized() == true) {
			authorizedId = ac.getAuthorizedID();
		} else {
			throw new SaslInvalidLoginExcepion(SaslError.invalid_authzid, nc.getName(),
											   "PLAIN: " + authcid + " is not authorized to act as " +
													   authorizationJID);
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
		return NAME;
	}

	@Override
	public byte[] unwrap(byte[] incoming, int offset, int len) {
		return null;
	}

	@Override
	public byte[] wrap(byte[] outgoing, int offset, int len) {
		return null;
	}

}
