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

import javax.security.sasl.SaslException;

public class XmppSaslException
		extends SaslException {

	private static final long serialVersionUID = 1L;

	public enum SaslError {
		/**
		 * The receiving entity acknowledges that the authentication handshake has been aborted by the initiating
		 * entity.
		 */
		aborted("aborted"),
		/**
		 * The account of the initiating entity has been temporarily disabled.
		 */
		account_disabled("account-disabled"),
		/**
		 * The authentication failed because the initiating entity provided credentials that have expired.
		 */
		credentials_expired("credentials-expired"),
		/**
		 * The mechanism requested by the initiating entity cannot be used unless the confidentiality and integrity of
		 * the underlying stream are protected (typically via TLS).
		 */
		encryption_required("encryption-required"),
		/**
		 * The data provided by the initiating entity could not be processed because the base 64 encoding is incorrect.
		 */
		incorrect_encoding("incorrect-encoding"),
		/**
		 * The authzid provided by the initiating entity is invalid, either because it is incorrectly formatted or
		 * because the initiating entity does not have permissions to authorize that ID.
		 */
		invalid_authzid("invalid-authzid"),
		/**
		 * The initiating entity did not specify a mechanism, or requested a mechanism that is not supported by the
		 * receiving entity.
		 */
		invalid_mechanism("invalid-mechanism"),
		/**
		 * The request is malformed (e.g., the {@code <auth/>} element includes initial response data but the mechanism
		 * does not allow that, or the data sent violates the syntax for the specified SASL mechanism).
		 */
		malformed_request("malformed-request"),
		/**
		 * The mechanism requested by the initiating entity is weaker than server policy permits for that initiating
		 * entity.
		 */
		mechanism_too_weak("mechanism-too-weak"),
		/**
		 * The authentication failed because the initiating entity did not provide proper credentials, or because some
		 * generic authentication failure has occurred but the receiving entity does not wish to disclose specific
		 * information about the cause of the failure.
		 */
		not_authorized("not-authorized"),
		/**
		 * The authentication failed because of a temporary error condition within the receiving entity, and it is
		 * advisable for the initiating entity to try again later.
		 */
		temporary_auth_failure("temporary-auth-failure");

		private final String elementName;

		SaslError(String elementName) {
			this.elementName = elementName;
		}

		public String getElementName() {
			return elementName;
		}

	}
	private SaslError saslError;

	public XmppSaslException(SaslError saslError) {
		super();
		this.saslError = saslError;
	}

	public XmppSaslException(SaslError saslError, String detail) {
		super(detail);
		this.saslError = saslError;
	}

	public SaslError getSaslError() {
		return saslError;
	}

	public String getSaslErrorElementName() {
		return saslError == null ? null : saslError.getElementName();
	}

}
