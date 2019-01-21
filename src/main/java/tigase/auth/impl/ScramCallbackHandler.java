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
package tigase.auth.impl;

import tigase.auth.AuthRepositoryAware;
import tigase.auth.DomainAware;
import tigase.auth.MechanismNameAware;
import tigase.auth.SessionAware;
import tigase.auth.callbacks.*;
import tigase.auth.credentials.Credentials;
import tigase.auth.credentials.entries.PlainCredentialsEntry;
import tigase.auth.credentials.entries.ScramCredentialsEntry;
import tigase.auth.mechanisms.AbstractSasl;
import tigase.auth.mechanisms.AbstractSaslSCRAM;
import tigase.db.AuthRepository;
import tigase.util.Base64;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.auth.CallbackHandlerFactory.AUTH_JID;
import static tigase.auth.credentials.Credentials.DEFAULT_USERNAME;

/**
 * Implementation of CallbackHandler to support authentication using SASL SCRAM-* authentication mechanism.
 */
public class ScramCallbackHandler
		implements CallbackHandler, AuthRepositoryAware, SessionAware, DomainAware, MechanismNameAware {

	private static final Logger log = Logger.getLogger(ScramCallbackHandler.class.getCanonicalName());
	private boolean accountDisabled = false;
	private ScramCredentialsEntry credentialsEntry;
	private boolean credentialsFetched;
	private String domain;
	private BareJID jid = null;
	private String mechanismName;
	private AuthRepository repo;
	private XMPPResourceConnection session;
	private String username = null;

	public ScramCallbackHandler() {
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

	@Override
	public void setMechanismName(String mechanismName) {
		this.mechanismName = mechanismName;
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

	protected void handleAuthorizeCallback(AuthorizeCallback authCallback) {
		String authenId = authCallback.getAuthenticationID();

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "AuthorizeCallback: authenId: {0}", authenId);
		}

		fetchCredentials();
		if (accountDisabled) {
			authCallback.setAuthorized(false);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "User {0} is disabled", jid);
			}
			return;
		}

		String authorId = authCallback.getAuthorizationID();

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "AuthorizeCallback: authorId: {0}", authorId);
		}
		authCallback.setAuthorized(true);
		session.removeSessionData(AUTH_JID);
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
		} else if (callback instanceof AuthorizationIdCallback) {
			handleAuthorizationIdCallback((AuthorizationIdCallback) callback);
		} else if (callback instanceof SaltCallback) {
			handleSaltCallback((SaltCallback) callback);
		} else if (callback instanceof AuthorizeCallback) {
			handleAuthorizeCallback((AuthorizeCallback) callback);
		} else {
			throw new UnsupportedCallbackException(callback, "Unrecognized Callback " + callback);
		}
	}

	protected void handleNameCallback(NameCallback nc) throws IOException {
		username = DEFAULT_USERNAME;//nc.getDefaultName();
		setJid(BareJID.bareJIDInstanceNS(nc.getDefaultName(), domain));
		nc.setName(jid.toString());
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "NameCallback: {0}", username);
		}
	}

	protected void handlePBKDIterationsCallback(PBKDIterationsCallback callback) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "PBKDIterationsCallback: {0}", jid);
		}
		fetchCredentials();
		if (credentialsEntry != null) {
			callback.setInterations(credentialsEntry.getIterations());
		}
	}

	protected void handleSaltCallback(SaltCallback callback) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "SaltCallback: {0}", jid);
		}

		fetchCredentials();
		if (credentialsEntry != null) {
			callback.setSalt(credentialsEntry.getSalt());
		} else {
			callback.setSalt(null);
		}
	}

	protected void handleSaltedPasswordCallbackCallback(SaltedPasswordCallback callback) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "PasswordCallback: {0}", jid);
		}

		fetchCredentials();
		if (credentialsEntry != null) {
			callback.setSaltedPassword(credentialsEntry.getSaltedPassword());
		} else {
			callback.setSaltedPassword(null);
		}
	}

	private void handleAuthorizationIdCallback(AuthorizationIdCallback callback) {
		if (!AbstractSasl.isAuthzIDIgnored() && callback.getAuthzId() != null &&
				!callback.getAuthzId().equals(jid.toString())) {
			try {
				username = jid.getLocalpart();
				setJid(BareJID.bareJIDInstance(callback.getAuthzId()));
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		} else {
			username = DEFAULT_USERNAME;
			callback.setAuthzId(jid.toString());
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
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Channel binding {0}: {1} in session-id {2}",
					new Object[]{callback.getRequestedBindType(),
								 callback.getBindingData() == null ? "null" : Base64.encode(callback.getBindingData()),
								 session});
		}
	}

	private void fetchCredentials() {
		if (credentialsFetched) {
			return;
		}

		try {
			Credentials credentials = repo.getCredentials(jid, username);

			if (credentials == null) {
				accountDisabled = true;
			} else {
				String mech = mechanismName.endsWith("-PLUS") ? mechanismName.substring(0, mechanismName.length() -
						"-PLUS".length()) : mechanismName;

				Credentials.Entry entry = credentials.getEntryForMechanism(mech);
				if (entry == null) {
					entry = credentials.getEntryForMechanism("PLAIN");
				}
				if (entry instanceof ScramCredentialsEntry) {
					credentialsEntry = (ScramCredentialsEntry) entry;
				} else if (entry instanceof PlainCredentialsEntry) {
					credentialsEntry = new ScramCredentialsEntry(mech.replace("SCRAM-", ""),
																 (PlainCredentialsEntry) entry);
				}

				accountDisabled = credentials.isAccountDisabled();
			}
		} catch (Exception ex) {
			log.log(Level.FINE, "Could not retrieve credentials for user " + jid + " with username " + username, ex);
		}
		credentialsFetched = true;
	}

	private void setJid(BareJID jid) {
		this.jid = jid;
		if (jid != null) {
			this.session.putSessionData(AUTH_JID, jid);
		}
	}

}
