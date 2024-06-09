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
package tigase.auth.mechanisms;

import tigase.auth.SaslInvalidLoginExcepion;
import tigase.auth.XmppSaslException;
import tigase.auth.callbacks.AuthorizationIdCallback;
import tigase.auth.callbacks.ReplaceServerKeyCallback;
import tigase.auth.callbacks.ServerKeyCallback;
import tigase.auth.callbacks.SharedSecretKeyCallback;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;

public class SaslXTOKEN
		extends AbstractSasl {

	public static final String NAME = "XTOKEN-HMAC-SHA-256";

	private static final SecureRandom secureRandom = new SecureRandom();
	public static byte[] generateSecretKey() {
		byte[] data = new byte[32];
		secureRandom.nextBytes(data);
		return data;
	}

	private Step step = Step.firstMessage;

	enum Step {
		firstMessage,
		secondMessage,
		finished
	}

	SaslXTOKEN(Map<? super String, ?> props, CallbackHandler callbackHandler) {
		super(props, callbackHandler);
	}
	
	@Override
	public String getMechanismName() {
		return NAME;
	}

	@Override
	public byte[] evaluateResponse(byte[] response) throws SaslException {
		if (response.length <= 64 || response[32] !=  0x00 || response[65] != 0x00) {
			throw new XmppSaslException(XmppSaslException.SaslError.malformed_request, "Invalid token format - too short");
		}

		byte[] data = Arrays.copyOfRange(response, 0, 32);
		byte[] token = Arrays.copyOfRange(response, 33, 65);

		String authcid = new String(Arrays.copyOfRange(response, 66, response.length), StandardCharsets.UTF_8);

		final NameCallback nc = new NameCallback("Authentication identity", authcid);
		final AuthorizationIdCallback ai = new AuthorizationIdCallback("Authorization identity", null);
		final ServerKeyCallback vtc = new ServerKeyCallback(null);
		final SharedSecretKeyCallback sskc = new SharedSecretKeyCallback();
		handleCallbacks(nc, ai, vtc, sskc);

		if (vtc.getServerKey() == null) {
			throw new SaslInvalidLoginExcepion(XmppSaslException.SaslError.not_authorized, nc.getName(), PASSWORD_NOT_VERIFIED_MSG);
		}

		try {
			SecretKeySpec secretKeySpec = new SecretKeySpec(vtc.getServerKey(), "SHA-256");
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(secretKeySpec);
			mac.update(data);
			if (sskc.getSecret() != null) {
				mac.update(sskc.getSecret());
			}
			byte[] hmac = mac.doFinal();
			boolean proofMatch = Arrays.equals(hmac, token);

			if (!proofMatch) {
				throw new SaslInvalidLoginExcepion(XmppSaslException.SaslError.not_authorized, authcid,
												   PASSWORD_NOT_VERIFIED_MSG);
			}

			final String authorizationJID = ai.getAuthzId() == null ? nc.getName() : ai.getAuthzId();

			final AuthorizeCallback ac = new AuthorizeCallback(nc.getName(), authorizationJID);
			handleCallbacks(ac);

			if (ac.isAuthorized() == true) {
				authorizedId = ac.getAuthorizedID();
			} else {
				throw new SaslInvalidLoginExcepion(XmppSaslException.SaslError.invalid_authzid, nc.getName(),
												   getMechanismName() + ": " + authcid + " is not authorized to act as " +
														   authorizationJID);
			}

			final ReplaceServerKeyCallback rtc = new ReplaceServerKeyCallback();
			handleCallbacks(rtc);
			complete = true;

			byte[] authzidData = authorizedId.getBytes(StandardCharsets.UTF_8);
			if (rtc.getNewServerKey() != null) {
				byte[] result = new byte[authzidData.length + 1 + rtc.getNewServerKey().length];
				System.arraycopy(authzidData, 0, result, 0, authzidData.length);
				result[authzidData.length] = 0x00;
				System.arraycopy(rtc.getNewServerKey(), 0, result, authzidData.length + 1, rtc.getNewServerKey().length);
				return result;
			}
			return authzidData;
		} catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			throw new SaslInvalidLoginExcepion(XmppSaslException.SaslError.invalid_authzid, nc.getName(),
											   getMechanismName() + ": " + ex.getMessage());
		}
	}

	@Override
	public String getAuthorizationID() {
		return authorizedId;
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
