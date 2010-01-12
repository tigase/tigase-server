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

import java.util.Arrays;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.security.NoSuchAlgorithmException;
import tigase.util.Algorithms;
import tigase.xmpp.BareJID;

/**
 * Describe class SaslPLAIN here.
 *
 *
 * Created: Mon Nov  6 09:02:31 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SaslPLAIN implements SaslServer {

	public static final String ENCRYPTION_KEY = "password-encryption";
	public static final String ENCRYPTION_PLAIN = "PLAIN";
	public static final String ENCRYPTION_MD5 = "MD5";
	/**
	 * This is LibreSource variant of encoding MD5 sum. The calculation is done
	 * the same way but HEX representation of the sum is different:
	 * <pre>
	 *	StringBuilder sb = new StringBuilder();
	 *	for (byte b: md5) {
	 *    sb.append(Integer.toHexString(b));
	 *	}
	 *	</pre>
	 *
	 */
	public static final String ENCRYPTION_LS_MD5 = "LS-MD5";
	public static final String ENCRYPTION_SHA = "SHA";

	private static final String MECHANISM = "PLAIN";

	private Map<? super String, ?> props = null;
	private CallbackHandler callbackHandler = null;

	private boolean auth_ok = false;

	/**
	 * Creates a new <code>SaslPLAIN</code> instance.
	 *
	 */
	public SaslPLAIN() {}

	protected SaslPLAIN(Map<? super String, ?> props,
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

// 	/**
// 	 * This is not fully correct HEX representation of digest sum but
// 	 * this is how Libre Source does it so I have to be compatible with them.
// 	 *
// 	 * @param passwd a <code>String</code> value
// 	 * @return a <code>String</code> value
// 	 */
// 	private String ls_digest(String passwd) throws NoSuchAlgorithmException {
// 		byte[] md5 = Algorithms.digest("", passwd, "MD5");
// 		StringBuilder sb = new StringBuilder();
// 		for (byte b: md5) {
// 			sb.append(Integer.toHexString(b));
// 		}
// 		return sb.toString();
// 	}

	/**
	 * Describe <code>evaluateResponse</code> method here.
	 *
	 * @param byteArray a <code>byte[]</code> value
	 * @return a <code>byte[]</code> value
	 * @exception SaslException if an error occurs
	 */
	public byte[] evaluateResponse(final byte[] byteArray) throws SaslException {

		//StringBuilder authz = new StringBuilder();
		// fields are separated with \0 char so let's look for the char
		// position....
		int auth_idx = 0;
		while (byteArray[auth_idx] != 0 && auth_idx < byteArray.length)
		{ ++auth_idx;	}
		String authoriz = new String(byteArray, 0, auth_idx);
		int user_idx = ++auth_idx;
		while (byteArray[user_idx] != 0 && user_idx < byteArray.length)
		{ ++user_idx;	}
		String user_id = new String(byteArray, auth_idx, user_idx - auth_idx);
		++user_idx;
		String passwd =
			new String(byteArray, user_idx, byteArray.length - user_idx);
		if (passwd != null) {
			String alg = (String)props.get(ENCRYPTION_KEY);
			if (alg != null) {
				try {
					if (alg.equals(ENCRYPTION_MD5) || alg.equals(ENCRYPTION_SHA)) {
						passwd = Algorithms.hexDigest("", passwd, alg);
					} // end of if (alg != null && !alg.equals())
// 					if (alg.equals(ENCRYPTION_LS_MD5)) {
// 						passwd = ls_digest(passwd);
// 					} // end of if (alg != null && !alg.equals())
				} catch (NoSuchAlgorithmException e) {
					throw
						new SaslException("Password encrypting algorithm is not supported.",
							e);
				} // end of try-catch
		}
		} // end of if (passwd != null)

		if (callbackHandler == null) {
			throw new SaslException("Error: no CallbackHandler available.");
		}
		Callback[] callbacks = new Callback[3];
		NameCallback nc = new NameCallback("User name", user_id);
		PasswordCallback pc = new PasswordCallback("User password", false);
		RealmCallback rc = new RealmCallback("Put domain as realm.");
		callbacks[0] = nc;
		callbacks[1] = pc;
		callbacks[2] = rc;
		try {
			callbackHandler.handle(callbacks);
			char[] real_password = pc.getPassword();
			if (!Arrays.equals(real_password, passwd.toCharArray())) {
				throw new SaslException("Password missmatch.");
			}
			if (authoriz != null && !authoriz.isEmpty()) {
				String realm = rc.getText();
				callbacks = new Callback[1];
				AuthorizeCallback ac =
					new AuthorizeCallback(BareJID.toString(user_id, realm), authoriz);
				callbacks[0] = ac;
				callbackHandler.handle(callbacks);
				if (ac.isAuthorized()) {
					auth_ok = true;
				} else {
					throw new SaslException("Not authorized.");
				} // end of else
			} else {
				auth_ok = true;
			} // end of if (authoriz != null && !authoriz.empty()) else
		} catch (Exception e) {
			throw new SaslException("Authorization error.", e);
		} // end of try-catch

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

} // SaslPLAIN