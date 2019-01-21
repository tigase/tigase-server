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

import tigase.auth.XmppSaslException;
import tigase.auth.XmppSaslException.SaslError;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.sasl.SaslException;
import java.util.Map;
import java.util.UUID;

/**
 * SASL-ANONYMOUS mechanism. <br> Called {@linkplain Callback callbacks} in order: <ul> <li>{@link NameCallback}</li>
 * </ul>
 */
public class SaslANONYMOUS
		extends AbstractSasl {

	public static final String IS_ANONYMOUS_PROPERTY = "IS_ANONYMOUS";
	public static final String NAME = "ANONYMOUS";

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
