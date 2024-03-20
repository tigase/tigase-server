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
import tigase.auth.callbacks.ReplaceServerKeyCallback;
import tigase.auth.callbacks.ServerKeyCallback;
import tigase.auth.credentials.Credentials;
import tigase.auth.credentials.entries.XTokenCredentialsEntry;
import tigase.auth.mechanisms.AbstractSasl;
import tigase.auth.mechanisms.SaslXTOKEN;
import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.util.Base64;
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

public class XTokenCallbackHandler implements CallbackHandler, AuthRepositoryAware, DomainAware, SessionAware {

	protected String domain;

	protected BareJID jid = null;
	protected Logger log = Logger.getLogger(this.getClass().getName());
	protected AuthRepository repo;
	private boolean loggingInForbidden = false;
	private XMPPResourceConnection session;
	private String credentialId;
	private XTokenCredentialsEntry entry;

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
		} else if (callback instanceof ServerKeyCallback) {
			handleServerKeyCallback((ServerKeyCallback) callback);
		} else if (callback instanceof AuthorizeCallback) {
			handleAuthorizeCallback((AuthorizeCallback) callback);
		} else if (callback instanceof ReplaceServerKeyCallback) {
			handleReplaceServerKeyCallback((ReplaceServerKeyCallback) callback);
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

	protected void handleServerKeyCallback(ServerKeyCallback pc) throws IOException {
		try {
			Credentials credentials = repo.getCredentials(jid, credentialId);

			log.log(Level.FINE,
					"Fetched credentials for: " + jid + " with credentialsId: " + credentialId + ", credentials: " +
							credentials);

			entry = (XTokenCredentialsEntry) credentials.getEntryForMechanism(SaslXTOKEN.NAME);

			loggingInForbidden = !credentials.canLogin();
			if (loggingInForbidden) {
				throw XmppSaslException.getExceptionFor(credentials.getAccountStatus());
			}

			pc.setServerKey(entry.getSecretKey());
		} catch (SaslException e) {
			log.log(Level.FINE, "User inactive: " + e);
			throw e;
		} catch (Exception ex) {
			log.log(Level.FINE, "Could not retrieve credentials for user " + jid + " with credentialId " + credentialId,
					ex);
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

	private void handleReplaceServerKeyCallback(ReplaceServerKeyCallback callback) throws XmppSaslException {
		try {
			byte[] data = SaslXTOKEN.generateSecretKey();
			repo.updateCredential(jid, credentialId, "XTOKEN", "t=" + Base64.encode(data) + ",o=" + false);
			callback.setNewServerKey(data);
		} catch (TigaseDBException e) {
			throw new XmppSaslException(XmppSaslException.SaslError.temporary_auth_failure);
		}
	}

	private void setJid(BareJID jid) {
		this.jid = jid;
		if (jid != null) {
			this.session.putSessionData(AUTH_JID, jid);
		}
	}

}
