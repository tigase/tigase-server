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

import tigase.auth.AuthRepositoryAware;
import tigase.auth.DomainAware;
import tigase.auth.SessionAware;
import tigase.auth.XmppSaslException;
import tigase.auth.callbacks.AuthorizationIdCallback;
import tigase.auth.callbacks.VerifyPasswordCallback;
import tigase.auth.credentials.Credentials;
import tigase.auth.mechanisms.AbstractSasl;
import tigase.db.AuthRepository;
import tigase.db.UserNotFoundException;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.auth.CallbackHandlerFactory.AUTH_JID;
import static tigase.auth.credentials.Credentials.DEFAULT_CREDENTIAL_ID;

/**
 * Implementation of CallbackHandler for authentication with SASL PLAIN or using plaintext password.
 */
public class PlainCallbackHandler
		implements CallbackHandler, AuthRepositoryAware, DomainAware, SessionAware {

	protected String domain;

	protected BareJID jid = null;
	protected Logger log = Logger.getLogger(this.getClass().getName());
	protected AuthRepository repo;
	private boolean loggingInForbidden = false;
	private XMPPResourceConnection session;
	private String credentialId;

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
	public void setSession(XMPPResourceConnection session) {
		this.session = session;
	}

	protected void handleAuthorizeCallback(AuthorizeCallback authCallback) {
		String authenId = authCallback.getAuthenticationID();

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "AuthorizeCallback: authenId: {0}", authenId);
		}

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
		if (callback instanceof RealmCallback) {
			handleRealmCallback((RealmCallback) callback);
		} else if (callback instanceof NameCallback) {
			handleNameCallback((NameCallback) callback);
		} else if (callback instanceof AuthorizationIdCallback) {
			handleAuthorizationIdCallback((AuthorizationIdCallback) callback);
		} else if (callback instanceof VerifyPasswordCallback) {
			handleVerifyPasswordCallback((VerifyPasswordCallback) callback);
		} else if (callback instanceof AuthorizeCallback) {
			handleAuthorizeCallback((AuthorizeCallback) callback);
		} else {
			throw new UnsupportedCallbackException(callback, "Unrecognized Callback");
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

	protected void handleRealmCallback(RealmCallback rc) throws IOException {
		String realm = domain;

		if (realm != null) {
			rc.setText(realm);
		} // end of if (realm == null)
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "RealmCallback: {0}", realm);
		}
	}

	protected void handleVerifyPasswordCallback(VerifyPasswordCallback pc) throws IOException {
		final String password = pc.getPassword();
		try {
			Credentials credentials = repo.getCredentials(jid, credentialId);

			log.log(Level.FINE,
					"Fetched credentials for: " + jid + " with credentialsId: " + credentialId + ", credentials: " +
							credentials);

			Credentials.Entry entry = credentials.getEntryForMechanism("PLAIN");
			if (entry == null) {
				entry = credentials.getFirst();
			}

			loggingInForbidden = !credentials.canLogin();

			if (loggingInForbidden) {
				throw XmppSaslException.getExceptionFor(credentials.getAccountStatus());
			}

			final boolean verified = !loggingInForbidden && entry != null && entry.verifyPlainPassword(password);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Verification result: {0}, loggingInForbidden: {1}, entry: {2}, credentials: {3}",
						new Object[]{verified, loggingInForbidden, entry, credentials});
			}
			pc.setVerified(verified);
		} catch (UserNotFoundException e) {
			log.log(Level.FINE, "User not found: " + e);
			pc.setVerified(false);
		} catch (SaslException e) {
			log.log(Level.FINE, "User inactive: " + e);
			throw e;
		} catch (Exception e) {
			throw new IOException("Password verification problem.", e);
		}
	}

	private void handleAuthorizationIdCallback(AuthorizationIdCallback callback) throws XmppSaslException {
		if (!AbstractSasl.isAuthzIDIgnored() && callback.getAuthzId() != null &&
				!callback.getAuthzId().equals(jid.toString())) {
			try {
				credentialId = jid.getLocalpart();
				setJid(BareJID.bareJIDInstance(callback.getAuthzId()));
			} catch (TigaseStringprepException ex) {
				log.warning("Malformed AuthorizationId: " + ex.getMessage());
				throw new XmppSaslException(XmppSaslException.SaslError.invalid_authzid);
			}
		} else {
			credentialId = DEFAULT_CREDENTIAL_ID;
			callback.setAuthzId(jid.toString());
		}
	}

	private void setJid(BareJID jid) {
		this.jid = jid;
		if (jid != null) {
			this.session.putSessionData(AUTH_JID, jid);
		}
	}

}
