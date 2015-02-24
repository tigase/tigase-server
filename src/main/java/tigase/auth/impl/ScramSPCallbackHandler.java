package tigase.auth.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;

import tigase.auth.AuthRepositoryAware;
import tigase.auth.DomainAware;
import tigase.auth.callbacks.PBKDIterationsCallback;
import tigase.auth.callbacks.SaltCallback;
import tigase.auth.callbacks.SaltedPasswordCallback;
import tigase.auth.mechanisms.AbstractSasl;
import tigase.db.AuthRepository;
import tigase.util.Base64;
import tigase.xmpp.BareJID;

/**
 * Handler for SCRAM with Salted Password.
 *
 * Password should be encoded as:<br>
 *
 * <pre>
 * base64(salt | saltedPassword)
 * </pre>
 *
 * Where:<br>
 * <code>salt</code> - 20 bytes,<br>
 * <code>saltedPassword</code> - 20 bytes.
 *
 */
public class ScramSPCallbackHandler implements CallbackHandler, AuthRepositoryAware, DomainAware {

	private String domain;

	protected BareJID jid = null;

	protected Logger log = Logger.getLogger(this.getClass().getName());

	private int pbkd2Iterations = 4096;

	private AuthRepository repo;

	private byte[] salt;

	private byte[] saltedPassword;

	public ScramSPCallbackHandler() {
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (int i = 0; i < callbacks.length; i++) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Callback: {0}", callbacks[i].getClass().getSimpleName());
			}
			handleCallback(callbacks[i]);
		}
	}

	protected void handleAuthorizeCallback(AuthorizeCallback authCallback) {
		String authenId = authCallback.getAuthenticationID();

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "AuthorizeCallback: authenId: {0}", authenId);
		}

		String authorId = authCallback.getAuthorizationID();

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "AuthorizeCallback: authorId: {0}", authorId);
		}
		if (AbstractSasl.isAuthzIDIgnored() || authenId.equals(authorId)) {
			authCallback.setAuthorized(true);
		}
	}

	protected void handleCallback(Callback callback) throws UnsupportedCallbackException, IOException {
		if (callback instanceof PBKDIterationsCallback) {
			handlePBKDIterationsCallback((PBKDIterationsCallback) callback);
		} else if (callback instanceof SaltedPasswordCallback) {
			handleSaltedPasswordCallback((SaltedPasswordCallback) callback);
		} else if (callback instanceof NameCallback) {
			handleNameCallback((NameCallback) callback);
		} else if (callback instanceof SaltCallback) {
			handleSaltCallback((SaltCallback) callback);
		} else if (callback instanceof AuthorizeCallback) {
			handleAuthorizeCallback((AuthorizeCallback) callback);
		} else {
			throw new UnsupportedCallbackException(callback, "Unrecognized Callback " + callback);
		}

	}

	protected void handleNameCallback(NameCallback nc) throws IOException {
		String user_name = nc.getDefaultName();
		jid = BareJID.bareJIDInstanceNS(user_name, domain);
		nc.setName(jid.toString());
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "NameCallback: {0}", user_name);
		}
		try {
			String pwd = repo.getPassword(jid);
			if (pwd == null)
				throw new SaslException("User " + jid + " not found.");
			byte[] buffer = Base64.decode(pwd);
			this.salt = new byte[20];
			this.saltedPassword = new byte[20];
			System.arraycopy(buffer, 0, salt, 0, salt.length);
			System.arraycopy(buffer, salt.length, saltedPassword, 0, saltedPassword.length);
		} catch (Exception e) {
			this.salt = null;
			this.saltedPassword = null;
			log.log(Level.WARNING, "Can't retrieve users salted password.", e);
		}
	}

	protected void handlePBKDIterationsCallback(PBKDIterationsCallback callback) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "PBKDIterationsCallback: {0}", jid);
		}
		callback.setInterations(pbkd2Iterations);
	}

	protected void handleSaltCallback(SaltCallback callback) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "SaltCallback: {0}", jid);
		}

		callback.setSalt(salt);
	}

	protected void handleSaltedPasswordCallback(SaltedPasswordCallback callback) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "SaltedPasswordCallback: {0}", jid);
		}

		callback.setSaltedPassword(saltedPassword);
	}

	@Override
	public void setAuthRepository(AuthRepository repo) {
		this.repo = repo;
	}

	@Override
	public void setDomain(String domain) {
		this.domain = domain;
	}

}
