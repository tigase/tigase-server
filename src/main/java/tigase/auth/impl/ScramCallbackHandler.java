package tigase.auth.impl;

import tigase.auth.AuthRepositoryAware;
import tigase.auth.DomainAware;
import tigase.auth.SessionAware;
import tigase.auth.callbacks.*;
import tigase.auth.mechanisms.AbstractSasl;
import tigase.auth.mechanisms.AbstractSaslSCRAM;
import tigase.db.AuthRepository;
import tigase.util.Base64;
import tigase.xmpp.BareJID;
import tigase.xmpp.XMPPResourceConnection;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScramCallbackHandler implements CallbackHandler, AuthRepositoryAware, SessionAware, DomainAware {

	private final SecureRandom random = new SecureRandom();
	private final byte[] salt;
	protected BareJID jid = null;
	protected Logger log = Logger.getLogger(this.getClass().getName());
	private String domain;
	private int pbkd2Iterations = 4096;
	private AuthRepository repo;
	private XMPPResourceConnection session;

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
		if (AbstractSasl.isAuthzIDIgnored() || authenId.equals(authorId)) {
			authCallback.setAuthorized(true);
		}
	}

	protected void handleCallback(Callback callback) throws UnsupportedCallbackException, IOException {
		if (callback instanceof XMPPSessionCallback) {
			((XMPPSessionCallback) callback).setSession(session);
		} else if (callback instanceof ChannelBindingCallback) {
			handleChannelBindingCallback((ChannelBindingCallback) callback);
		} else if (callback instanceof PBKDIterationsCallback) {
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

	private void handleChannelBindingCallback(ChannelBindingCallback callback) {
		if (callback.getRequestedBindType() == AbstractSaslSCRAM.BindType.tls_unique) {
			callback.setBindingData((byte[]) session.getSessionData(AbstractSaslSCRAM.TLS_UNIQUE_ID_KEY));
		} else if (callback.getRequestedBindType() == AbstractSaslSCRAM.BindType.tls_server_end_point) {
			try {
				Certificate cert = (Certificate) session.getSessionData(AbstractSaslSCRAM.LOCAL_CERTIFICATE_KEY);
				final String usealgo;
				final String algo = cert.getPublicKey().getAlgorithm();
				if (algo.equals("MD5") || algo.equals("SHA-1")) {
					usealgo = "SHA-256";
				} else {
					usealgo = algo;
				}
				final MessageDigest md = MessageDigest.getInstance(usealgo);
				final byte[] der = cert.getEncoded();
				md.update(der);
				callback.setBindingData(md.digest());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		if (log.isLoggable(Level.FINEST))
			log.log(Level.FINEST, "Channel binding {0}: {1} in session-id {2}", new Object[]{callback.getRequestedBindType(), callback.getBindingData() == null ? "null" : Base64.encode(callback.getBindingData()), session});
	}

	protected void handleNameCallback(NameCallback nc) throws IOException {
		String user_name = nc.getDefaultName();
		jid = BareJID.bareJIDInstanceNS(user_name, domain);
		nc.setName(jid.toString());
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
				callback.setSaltedPassword(new byte[]{});
			} else {

				byte[] saltedPassword = AbstractSaslSCRAM.hi("SHA1", AbstractSaslSCRAM.normalize(pwd), salt, pbkd2Iterations);

				callback.setSaltedPassword(saltedPassword);
			}
		} catch (Exception e) {
			callback.setSaltedPassword(null);
			log.log(Level.FINE, "Can't retrieve user password.", e);
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

	@Override
	public void setSession(XMPPResourceConnection session) {
		this.session = session;
	}
}
