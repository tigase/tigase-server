/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.auth;

import java.util.Map;
import java.util.UUID;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

/**
 * Describe class SaslAnonymous here.
 *
 *
 * Created: Mon Nov  6 09:02:31 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SaslAnonymous implements SaslServer {

	private static final String MECHANISM = "ANONYMOUS";

	private Map<? super String, ?> props = null;
	private CallbackHandler callbackHandler = null;

	private boolean auth_ok = true;

	/**
	 * Creates a new <code>SaslAnonymous</code> instance.
	 *
	 */
	public SaslAnonymous() {}

	protected SaslAnonymous(Map<? super String, ?> props,
		CallbackHandler callbackHandler) {
		this.props = props;
		this.callbackHandler = callbackHandler;
	}

	// Implementation of javax.security.sasl.SaslServer

	/**
	 * Describe <code>unwrap</code> method here.
	 *
	 * @param byteArray a <code>byte[]</code> value
	 * @param n an <code>int</code> value
	 * @param n1 an <code>int</code> value
	 * @return a <code>byte[]</code> value
	 * @exception SaslException if an error occurs
	 */
	public byte[] unwrap(final byte[] byteArray, final int n, final int n1)
		throws SaslException {
		return null;
	}

	/**
	 * Describe <code>evaluateResponse</code> method here.
	 *
	 * @param byteArray a <code>byte[]</code> value
	 * @return a <code>byte[]</code> value
	 * @exception SaslException if an error occurs
	 */
	public byte[] evaluateResponse(final byte[] byteArray) throws SaslException {
		Callback[] callbacks = new Callback[1];
		NameCallback nc = new NameCallback("User name", UUID.randomUUID().toString());
		callbacks[0] = nc;
		try {
			callbackHandler.handle(callbacks);
		} catch (Exception e) {
			throw new SaslException("Authorization error.", e);
		}
		return null;
	}

	/**
	 * Describe <code>getAuthorizationID</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	public String getAuthorizationID() {
		return null;
	}

	/**
	 * Describe <code>getMechanismName</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	public String getMechanismName() {
		return MECHANISM;
	}

	/**
	 * Describe <code>getNegotiatedProperty</code> method here.
	 *
	 * @param string a <code>String</code> value
	 * @return an <code>Object</code> value
	 */
	public Object getNegotiatedProperty(final String string) {
		return null;
	}

	/**
	 * Describe <code>isComplete</code> method here.
	 *
	 * @return a <code>boolean</code> value
	 */
	public boolean isComplete() {
		return auth_ok;
	}

	/**
	 * Describe <code>wrap</code> method here.
	 *
	 * @param byteArray a <code>byte[]</code> value
	 * @param n an <code>int</code> value
	 * @param n1 an <code>int</code> value
	 * @return a <code>byte[]</code> value
	 * @exception SaslException if an error occurs
	 */
	public byte[] wrap(final byte[] byteArray, final int n, final int n1)
		throws SaslException {
		return null;
	}

	/**
	 * Describe <code>dispose</code> method here.
	 *
	 * @exception SaslException if an error occurs
	 */
	public void dispose() throws SaslException {
		props = null;
		callbackHandler = null;
	}

} // SaslAnonymous
