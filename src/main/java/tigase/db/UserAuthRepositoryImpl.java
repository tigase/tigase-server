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
package tigase.db;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import tigase.util.Base64;
import tigase.util.Algorithms;
import tigase.util.JIDUtils;

import static tigase.db.UserAuthRepository.*;

/**
 * Describe class UserAuthRepositoryImpl here.
 *
 *
 * Created: Sat Nov 11 21:46:50 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UserAuthRepositoryImpl implements UserAuthRepository {

  private static final Logger log =
    Logger.getLogger("tigase.db.UserAuthRepositoryImpl");

	private static final String PASSWORD_KEY = "password";

	private static final String[] non_sasl_mechs = {
		"password"
		, "digest"
	};
	private static final String[] sasl_mechs =
	{
		"PLAIN"
		,	"DIGEST-MD5"
		, "CRAM-MD5"
	};

	private UserRepository repo = null;

	/**
	 * Creates a new <code>UserAuthRepositoryImpl</code> instance.
	 *
	 */
	public UserAuthRepositoryImpl(UserRepository repo) {
		this.repo = repo;
	}

	public String getResourceUri() { return repo.getResourceUri(); }

	private String getPassword(final String user)
		throws UserNotFoundException, TigaseDBException {
		return repo.getData(user, PASSWORD_KEY);
	}

	// Implementation of tigase.db.UserAuthRepository

	/**
	 * Describe <code>queryAuth</code> method here.
	 *
	 * @param authProps a <code>Map</code> value
	 */
	public void queryAuth(final Map<String, Object> authProps) {
		String protocol = (String)authProps.get(PROTOCOL_KEY);
		if (protocol.equals(PROTOCOL_VAL_NONSASL)) {
			authProps.put(RESULT_KEY, non_sasl_mechs);
		} // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))
		if (protocol.equals(PROTOCOL_VAL_SASL)) {
			authProps.put(RESULT_KEY, sasl_mechs);
		} // end of if (protocol.equals(PROTOCOL_VAL_NONSASL))
	}

	/**
	 * Describe <code>initRepository</code> method here.
	 *
	 * @param string a <code>String</code> value
	 * @exception DBInitException if an error occurs
	 */
	public void initRepository(final String string) throws DBInitException { }

	/**
	 * Describe <code>plainAuth</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public boolean plainAuth(final String user, final String password)
		throws UserNotFoundException, TigaseDBException {
		String db_password = getPassword(user);
		return password != null && db_password != null &&
			db_password.equals(password);
	}

	/**
	 * Describe <code>digestAuth</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param digest a <code>String</code> value
	 * @param id a <code>String</code> value
	 * @param alg a <code>String</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 * @exception AuthorizationException if an error occurs
	 */
	public boolean digestAuth(final String user, final String digest,
		final String id, final String alg)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		final String db_password = getPassword(user);
		try {
			final String digest_db_pass =	Algorithms.hexDigest(id, db_password, alg);
			log.finest("Comparing passwords, given: " + digest
				+ ", db: " + digest_db_pass);
			return digest.equals(digest_db_pass);
		} catch (NoSuchAlgorithmException e) {
			throw new AuthorizationException("No such algorithm.", e);
		} // end of try-catch
	}

	/**
	 * Describe <code>otherAuth</code> method here.
	 *
	 * @param props a <code>Map</code> value
	 * @return a <code>boolean</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 * @exception AuthorizationException if an error occurs
	 */
	public boolean otherAuth(final Map<String, Object> props)
		throws UserNotFoundException, TigaseDBException, AuthorizationException {
		String proto = (String)props.get(PROTOCOL_KEY);
		if (proto.equals(PROTOCOL_VAL_SASL)) {
			return saslAuth(props);
		} // end of if (proto.equals(PROTOCOL_VAL_SASL))
		throw new AuthorizationException("Protocol is not supported.");
	}

	public void logout(final String user) {}

	/**
	 * Describe <code>addUser</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @exception UserExistsException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public void addUser(final String user, final String password)
		throws UserExistsException, TigaseDBException {
		repo.addUser(user);
		log.info("Repo user added: " + user);
		updatePassword(user, password);
		log.info("Password updated: " + user + ":" + password);
	}

	/**
	 * Describe <code>updatePassword</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @param password a <code>String</code> value
	 * @exception UserExistsException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public void updatePassword(final String user, final String password)
		throws TigaseDBException {
		repo.setData(user, PASSWORD_KEY, password);
	}

	/**
	 * Describe <code>removeUser</code> method here.
	 *
	 * @param user a <code>String</code> value
	 * @exception UserNotFoundException if an error occurs
	 * @exception TigaseDBException if an error occurs
	 */
	public void removeUser(final String user)
		throws UserNotFoundException, TigaseDBException {
		repo.removeUser(user);
	}

	private boolean saslAuth(final Map<String, Object> props)
		throws AuthorizationException {
		try {
			SaslServer ss = (SaslServer)props.get("SaslServer");
			if (ss == null) {
				Map<String, String> sasl_props = new TreeMap<String, String>();
				sasl_props.put(Sasl.QOP, "auth");
				ss = Sasl.createSaslServer((String)props.get(MACHANISM_KEY),
					"xmpp",	(String)props.get(SERVER_NAME_KEY),
					sasl_props, new SaslCallbackHandler(props));
				props.put("SaslServer", ss);
			} // end of if (ss == null)
			String data_str = (String)props.get(DATA_KEY);
			byte[] in_data =
				(data_str != null ? Base64.decode(data_str) : new byte[0]);
			log.finest("response: " + new String(in_data));
			byte[] challenge = ss.evaluateResponse(in_data);
			log.finest("challenge: " +
				(challenge != null ? new String(challenge) : "null"));
			String challenge_str = (challenge != null && challenge.length > 0
				? Base64.encode(challenge) : null);
			props.put(RESULT_KEY, challenge_str);
			if (ss.isComplete()) {
				return true;
			} else {
				return false;
			} // end of if (ss.isComplete()) else
		} catch (SaslException e) {
			throw new AuthorizationException("Sasl exception.", e);
		} // end of try-catch
	}

	private class SaslCallbackHandler implements CallbackHandler {

		private Map<String, Object> options = null;

		private SaslCallbackHandler(final Map<String, Object> options) {
			this.options = options;
		}

		// Implementation of javax.security.auth.callback.CallbackHandler
		/**
		 * Describe <code>handle</code> method here.
		 *
		 * @param callbacks a <code>Callback[]</code> value
		 * @exception IOException if an error occurs
		 * @exception UnsupportedCallbackException if an error occurs
		 */
		public void handle(final Callback[] callbacks)
			throws IOException, UnsupportedCallbackException {

			String jid = null;

			for (int i = 0; i < callbacks.length; i++) {
				log.finest("Callback: " + callbacks[i].getClass().getSimpleName());
				if (callbacks[i] instanceof RealmCallback) {
					RealmCallback rc = (RealmCallback)callbacks[i];
					String realm = (String)options.get(REALM_KEY);
					if (realm != null) {
						rc.setText(realm);
					} // end of if (realm == null)
					log.finest("RealmCallback: " + realm);
				} else if (callbacks[i] instanceof NameCallback) {
					NameCallback nc = (NameCallback)callbacks[i];
					String user_name = nc.getName();
					if (user_name == null) {
						user_name = nc.getDefaultName();
					} // end of if (name == null)
					jid = JIDUtils.getNodeID(user_name, (String)options.get(REALM_KEY));
					options.put(USER_ID_KEY, jid);
					log.finest("NameCallback: " + user_name);
				} else if (callbacks[i] instanceof PasswordCallback) {
					PasswordCallback pc = (PasswordCallback)callbacks[i];
					try {
						String passwd = getPassword(jid);
						pc.setPassword(passwd.toCharArray());
						log.finest("PasswordCallback: " +	passwd);
					} catch (Exception e) {
						throw new IOException("Password retrieving problem.", e);
					} // end of try-catch
				} else if (callbacks[i] instanceof AuthorizeCallback) {
					AuthorizeCallback authCallback = ((AuthorizeCallback)callbacks[i]);
					String authenId = authCallback.getAuthenticationID();
					log.finest("AuthorizeCallback: authenId: " + authenId);
					String authorId = authCallback.getAuthorizationID();
					log.finest("AuthorizeCallback: authorId: " + authorId);
					if (authenId.equals(authorId)) {
						authCallback.setAuthorized(true);
					} // end of if (authenId.equals(authorId))
				} else {
					throw new UnsupportedCallbackException
						(callbacks[i], "Unrecognized Callback");
				}
			}
		}
	}

} // UserAuthRepositoryImpl