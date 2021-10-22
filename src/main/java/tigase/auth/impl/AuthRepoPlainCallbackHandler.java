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

import tigase.annotations.TigaseDeprecated;
import tigase.auth.AuthRepositoryAware;
import tigase.auth.DomainAware;
import tigase.auth.XmppSaslException;
import tigase.auth.callbacks.VerifyPasswordCallback;
import tigase.auth.mechanisms.AbstractSasl;
import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.xmpp.jid.BareJID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.db.AuthRepository.*;

/**
 * This is implementation of {@linkplain CallbackHandler} to use with old {@linkplain AuthRepository AuthRepositories}.
 * Callback {@linkplain VerifyPasswordCallback} uses method {@linkplain tigase.db.AuthRepositoryImpl#plainAuth(BareJID, String)} to
 * password verification.
 */
@Deprecated
@TigaseDeprecated(since = "8.0.0")
public class AuthRepoPlainCallbackHandler
		implements CallbackHandler, AuthRepositoryAware, DomainAware {

	protected String domain;

	protected BareJID jid = null;
	protected Logger log = Logger.getLogger(this.getClass().getName());
	protected AuthRepository repo;

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

	@SuppressWarnings("unused")
	protected void handleAuthorizeCallback(AuthorizeCallback authCallback) throws SaslException {
		String authenId = authCallback.getAuthenticationID();

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "AuthorizeCallback: authenId: {0}", authenId);
		}

		try {
			final AccountStatus accountStatus = repo.getAccountStatus(jid);
			if (accountStatus.isInactive()) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "User {0} is disabled, status: {1}", new Object[] {jid, accountStatus});
				}
				throw XmppSaslException.getExceptionFor(accountStatus);
			}
		} catch (TigaseDBException e) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Cannot check if user " + jid + " is enabled", e);
			}
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
		if (callback instanceof RealmCallback) {
			handleRealmCallback((RealmCallback) callback);
		} else if (callback instanceof NameCallback) {
			handleNameCallback((NameCallback) callback);
		} else if (callback instanceof VerifyPasswordCallback) {
			handleVerifyPasswordCallback((VerifyPasswordCallback) callback);
		} else if (callback instanceof AuthorizeCallback) {
			handleAuthorizeCallback((AuthorizeCallback) callback);
		} else {
			throw new UnsupportedCallbackException(callback, "Unrecognized Callback");
		}

	}

	protected void handleNameCallback(NameCallback nc) throws IOException {
		String user_name = nc.getDefaultName();
		jid = BareJID.bareJIDInstanceNS(user_name, domain);
		nc.setName(jid.toString());
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "NameCallback: {0}", user_name);
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
		String passwd = pc.getPassword();

		try {
			Map<String, Object> map = new HashMap<String, Object>();

			map.put(PROTOCOL_KEY, PROTOCOL_VAL_NONSASL);
			map.put(USER_ID_KEY, jid);
			map.put(PASSWORD_KEY, passwd);
			map.put(REALM_KEY, jid.getDomain());
			map.put(SERVER_NAME_KEY, jid.getDomain());
			pc.setVerified(repo.otherAuth(map));
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "VerifyPasswordCallback: {0}", "******");
			}
		} catch (Exception e) {
			pc.setVerified(false);

			throw new IOException("Password verification problem.", e);
		}
	}
}