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

//~--- non-JDK imports --------------------------------------------------------

import tigase.auth.AuthRepositoryAware;
import tigase.auth.callbacks.VerifyPasswordCallback;
import tigase.auth.DomainAware;

import tigase.db.AuthRepository;

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;

/**
 * This is implementation of {@linkplain CallbackHandler} to use with old
 * {@linkplain AuthRepository AuthRepositories}. Callback
 * {@linkplain VerifyPasswordCallback} uses method
 * {@linkplain AuthRepository#plainAuth(BareJID, String)} to password
 * verification.
 */
public class AuthRepoPlainCallbackHandler
				implements CallbackHandler, AuthRepositoryAware, DomainAware {
	private BareJID jid = null;

	/** Field description */
	protected Logger log = Logger.getLogger(this.getClass().getName());
	private String domain;
	private AuthRepository repo;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param callbacks
	 *
	 * @throws IOException
	 * @throws UnsupportedCallbackException
	 */
	@Override
	public void handle(Callback[] callbacks)
					throws IOException, UnsupportedCallbackException {
		for (int i = 0; i < callbacks.length; i++) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Callback: {0}", callbacks[i].getClass().getSimpleName());
			}
			if (callbacks[i] instanceof RealmCallback) {
				RealmCallback rc = (RealmCallback) callbacks[i];
				String realm     = domain;

				if (realm != null) {
					rc.setText(realm);
				}    // end of if (realm == null)
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "RealmCallback: {0}", realm);
				}
			} else if (callbacks[i] instanceof NameCallback) {
				NameCallback nc  = (NameCallback) callbacks[i];
				String user_name = nc.getDefaultName();

				nc.setName(user_name);
				jid = BareJID.bareJIDInstanceNS(user_name, domain);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "NameCallback: {0}", user_name);
				}
			} else if (callbacks[i] instanceof VerifyPasswordCallback) {
				VerifyPasswordCallback pc = (VerifyPasswordCallback) callbacks[i];
				String passwd             = new String(pc.getPassword());

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
				String authenId                = authCallback.getAuthenticationID();

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

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 */
	@Override
	public void setAuthRepository(AuthRepository repo) {
		this.repo = repo;
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 */
	@Override
	public void setDomain(String domain) {
		this.domain = domain;
	}
}


//~ Formatted in Tigase Code Convention on 13/03/04
