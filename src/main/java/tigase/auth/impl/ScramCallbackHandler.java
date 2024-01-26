/*
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

import tigase.auth.*;
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
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.auth.CallbackHandlerFactory.AUTH_JID;
import static tigase.auth.credentials.Credentials.DEFAULT_CREDENTIAL_ID;

/**
 * Implementation of CallbackHandler to support authentication using SASL SCRAM-* authentication mechanism.
 */
public class ScramCallbackHandler
		implements CallbackHandler, AuthRepositoryAware, SessionAware, DomainAware, MechanismNameAware {

	private static final Logger log = Logger.getLogger(ScramCallbackHandler.class.getCanonicalName());
	private String credentialId = null;
	private ScramCredentialsEntry credentialsEntry;
	private boolean credentialsFetched;
	private String domain;
	private BareJID jid = null;
	private boolean loggingInForbidden = false;
	private String mechanismName;
	private AuthRepository repo;
	private XMPPResourceConnection session;

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
	public void setAuthRepository(AuthRepository repo) {
		this.repo = repo;
	}

	@Override
	public void setDomain(String domain) {
		this.domain = domain;
	}

	@Override
	public void setMechanismName(String mechanismName) {
		this.mechanismName = mechanismName;
	}

	@Override
	public void setSession(XMPPResourceConnection session) {
		this.session = session;
	}

	protected void handleAuthorizeCallback(AuthorizeCallback authCallback) throws SaslException {
		String authenId = authCallback.getAuthenticationID();

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "AuthorizeCallback: authenId: {0}", authenId);
		}

		fetchCredentials();
		if (loggingInForbidden) {
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
		credentialId = DEFAULT_CREDENTIAL_ID;
		BareJID jid = BareJID.bareJIDInstanceNS(nc.getDefaultName());
		if (jid.getLocalpart() == null || !domain.equalsIgnoreCase(jid.getDomain())) {
			jid = BareJID.bareJIDInstanceNS(nc.getDefaultName(), domain);
		}
		setJid(jid);
		nc.setName(jid.toString());
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "NameCallback: {0}", credentialId);
		}
	}

	protected void handlePBKDIterationsCallback(PBKDIterationsCallback callback) throws SaslException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "PBKDIterationsCallback: {0}", jid);
		}
		fetchCredentials();
		if (credentialsEntry != null) {
			callback.setInterations(credentialsEntry.getIterations());
		}
	}

	protected void handleSaltCallback(SaltCallback callback) throws SaslException {
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

	private void fetchCredentials() throws SaslException {
		if (credentialsFetched) {
			return;
		}

		try {
			Credentials credentials = repo.getCredentials(jid, credentialId);

			log.log(Level.FINE,
					"Fetched credentials for: " + jid + " with credentialsId: " + credentialId + ", credentials: " +
							credentials);

			if (credentials == null) {
				loggingInForbidden = true;
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

				loggingInForbidden = !credentials.canLogin();
				if (loggingInForbidden) {
					throw XmppSaslException.getExceptionFor(credentials.getAccountStatus());
				}
			}
		} catch (SaslException e) {
			throw e;
		} catch (Exception ex) {
			log.log(Level.FINE, "Could not retrieve credentials for user " + jid + " with credentialId " + credentialId,
					ex);
		}
		credentialsFetched = true;
	}

	private void handleAuthorizationIdCallback(AuthorizationIdCallback callback) {
		if (!AbstractSasl.isAuthzIDIgnored() && callback.getAuthzId() != null &&
				!callback.getAuthzId().equals(jid.toString())) {
			try {
				credentialId = jid.getLocalpart();
				setJid(BareJID.bareJIDInstance(callback.getAuthzId()));
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		} else {
			credentialId = DEFAULT_CREDENTIAL_ID;
			callback.setAuthzId(jid.toString());
		}
	}

	private void handleChannelBindingCallback(ChannelBindingCallback callback) {
		if (callback.getRequestedBindType() == AbstractSaslSCRAM.BindType.tls_exporter) {
			callback.setBindingData((byte[]) session.getSessionData(AbstractSaslSCRAM.TLS_EXPORTER_KEY));
		} else if (callback.getRequestedBindType() == AbstractSaslSCRAM.BindType.tls_unique) {
			callback.setBindingData((byte[]) session.getSessionData(AbstractSaslSCRAM.TLS_UNIQUE_ID_KEY));
		} else if (callback.getRequestedBindType() == AbstractSaslSCRAM.BindType.tls_server_end_point) {
			try {
				X509Certificate cert = (X509Certificate) session.getSessionData(
						AbstractSaslSCRAM.LOCAL_CERTIFICATE_KEY);
				String usealgo;
				final String algo = cert.getSigAlgName();
				int withIdx = algo.indexOf("with");
				if (withIdx <= 0) {
					throw new RuntimeException("Unable to parse SigAlgName: " + algo);
				}
				usealgo = algo.substring(0, withIdx);
				if (usealgo.equalsIgnoreCase("MD5") || usealgo.equalsIgnoreCase("SHA1")) {
					usealgo = "SHA-256";
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

	private void handleServerKeyCallback(ServerKeyCallback callback) {

		// TODO
	}

	private void handleStoredKeyCallback(StoredKeyCallback callback) {
		// TODO
	}

	private void setJid(BareJID jid) {
		this.jid = jid;
		if (jid != null) {
			this.session.putSessionData(AUTH_JID, jid);
		}
	}

}
