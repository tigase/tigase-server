package tigase.auth.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

import tigase.auth.AuthRepositoryAware;
import tigase.auth.DomainAware;
import tigase.auth.callbacks.VerifyPasswordCallback;
import tigase.db.AuthRepository;
import tigase.xmpp.BareJID;

/**
 * This is implementation of {@linkplain CallbackHandler} to use with old
 * {@linkplain AuthRepository AuthRepositories}. Callback
 * {@linkplain VerifyPasswordCallback} uses method
 * {@linkplain AuthRepository#plainAuth(BareJID, String)} to password
 * veryfication.
 */
public class AuthRepoPlainCallbackHandler implements CallbackHandler, AuthRepositoryAware, DomainAware {

	private String domain;

	private BareJID jid = null;

	protected Logger log = Logger.getLogger(this.getClass().getName());

	private AuthRepository repo;

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

		for (int i = 0; i < callbacks.length; i++) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Callback: {0}", callbacks[i].getClass().getSimpleName());
			}

			if (callbacks[i] instanceof RealmCallback) {
				RealmCallback rc = (RealmCallback) callbacks[i];
				String realm = domain;

				if (realm != null) {
					rc.setText(realm);
				} // end of if (realm == null)

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "RealmCallback: {0}", realm);
				}
			} else if (callbacks[i] instanceof NameCallback) {
				NameCallback nc = (NameCallback) callbacks[i];
				String user_name = nc.getDefaultName();

				nc.setName(user_name);

				jid = BareJID.bareJIDInstanceNS(user_name, domain);

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "NameCallback: {0}", user_name);
				}
			} else if (callbacks[i] instanceof VerifyPasswordCallback) {
				VerifyPasswordCallback pc = (VerifyPasswordCallback) callbacks[i];
				String passwd = new String(pc.getPassword());

				try {

					try {
						pc.setVerified(repo.plainAuth(jid, passwd));
					} catch (Exception e) {
						pc.setVerified(false);
					}

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "VerifyPasswordCallback: {0}", passwd);
					}
				} catch (Exception e) {
					throw new IOException("Password verification problem.", e);
				}
			} else if (callbacks[i] instanceof AuthorizeCallback) {
				AuthorizeCallback authCallback = ((AuthorizeCallback) callbacks[i]);
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
			} else {
				throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
			}
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
