/*
 * AuthRepoPlainCallbackHandler.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */

package tigase.auth.impl;

//~--- JDK imports ------------------------------------------------------------
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.SaslException;

import tigase.auth.AuthRepositoryAware;
import tigase.auth.DomainAware;
import tigase.auth.callbacks.VerifyPasswordCallback;
import tigase.auth.mechanisms.AbstractSasl;
import tigase.auth.mechanisms.AbstractSaslSCRAM;
import tigase.db.AuthRepository;
import tigase.util.Base64;
import tigase.xmpp.BareJID;

/**
 * This is implementation of {@linkplain CallbackHandler} to use with old
 * {@linkplain AuthRepository AuthRepositories}. Callback
 * {@linkplain VerifyPasswordCallback} uses method
 * {@linkplain AuthRepository#plainAuth(BareJID, String)} to password
 * verification.
 */
public class PlainSPCallbackHandler implements CallbackHandler, AuthRepositoryAware, DomainAware {
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
		final String password = pc.getPassword();
		try {
			String pwd = repo.getPassword(jid);
			if (pwd == null)
				throw new SaslException("User " + jid + " not found.");
			byte[] buffer = Base64.decode(pwd);
			byte[] salt = new byte[20];
			byte[] saltedPassword = new byte[20];
			System.arraycopy(buffer, 0, salt, 0, salt.length);
			System.arraycopy(buffer, salt.length, saltedPassword, 0, saltedPassword.length);

			byte[] np = AbstractSaslSCRAM.hi("SHA1", AbstractSaslSCRAM.normalize(password), salt, 4096);

			pc.setVerified(Arrays.equals(saltedPassword, np));
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "VerifyPasswordCallback: {0}", "******");
			}
		} catch (Exception e) {
			pc.setVerified(false);
			throw new IOException("Password verification problem.", e);
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