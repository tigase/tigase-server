package tigase.auth.impl;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;

import tigase.auth.AuthRepositoryAware;
import tigase.auth.DomainAware;
import tigase.auth.callbacks.PBKDIterationsCallback;
import tigase.auth.callbacks.SaltCallback;
import tigase.auth.callbacks.SaltedPasswordCallback;
import tigase.auth.mechanisms.AbstractSaslSCRAM;
import tigase.db.AuthRepository;
import tigase.xmpp.BareJID;

public class ScramCallbackHandler implements CallbackHandler, AuthRepositoryAware, DomainAware {

	private String domain;

	protected BareJID jid = null;

	protected Logger log = Logger.getLogger(this.getClass().getName());

	private int pbkd2Iterations = 4096;

	private final SecureRandom random = new SecureRandom();

	private AuthRepository repo;

	private final byte[] salt;

	public ScramCallbackHandler() {
		this.salt = new byte[10];
		random.nextBytes(salt);
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
		if (authenId.equals(authorId) || authorId.equals(authenId + "@" + domain)) {
			authCallback.setAuthorized(true);
		}
	}

	protected void handleCallback(Callback callback) throws UnsupportedCallbackException, IOException {
		if (callback instanceof PBKDIterationsCallback) {
			handlePBKDIterationsCallback((PBKDIterationsCallback) callback);
		} else if (callback instanceof SaltedPasswordCallback) {
			handleSaltedPasswordCallbackCallback((SaltedPasswordCallback) callback);
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
		nc.setName(user_name);
		jid = BareJID.bareJIDInstanceNS(user_name, domain);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "NameCallback: {0}", user_name);
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

	protected void handleSaltedPasswordCallbackCallback(SaltedPasswordCallback callback) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "PasswordCallback: {0}", jid);
		}

		try {
			String pwd = repo.getPassword(jid);

			if (pwd == null) {
				callback.setSaltedPassword(new byte[] {});
			} else {

				byte[] saltedPassword = AbstractSaslSCRAM.hi("SHA1", AbstractSaslSCRAM.normalize(pwd), salt, pbkd2Iterations);

				callback.setSaltedPassword(saltedPassword);
			}
		} catch (Exception e) {
			callback.setSaltedPassword(null);
			log.log(Level.WARNING, "Can't retrieve user password.", e);
		}
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
