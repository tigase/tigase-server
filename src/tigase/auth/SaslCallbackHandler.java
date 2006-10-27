/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.auth;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.db.TigaseDBException;
import tigase.util.JID;
import tigase.xmpp.Authorization;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Describe class SaslCallbackHandler here.
 *
 *
 * Created: Sun Feb 19 10:28:11 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SaslCallbackHandler implements CallbackHandler {

  /**
   * Private logger for class instancess.
   */
  private static final Logger log =
		Logger.getLogger("tigase.auth.SaslCallbackHandler");

	private Map<String, ?> options = null;
	private CommitHandler commitHandler = null;
	private UserRepository repository = null;
	protected XMPPResourceConnection connection = null;

	private String user_name = null;
	private String realm = null;
	private String user_raw_pass = null;

	/**
	 * Creates a new <code>SaslCallbackHandler</code> instance.
	 *
	 */
	public SaslCallbackHandler(final Map<String, ?> options,
		final XMPPResourceConnection connection) {
		this.connection = connection;
		this.options = options;
		this.commitHandler =
			(CommitHandler)options.get(CommitHandler.COMMIT_HANDLER_KEY);
		this.repository =
			(UserRepository)options.get(UserRepository.class.getSimpleName());
	}

	// Implementation of javax.security.auth.callback.CallbackHandler

	/**
	 * Describe <code>handle</code> method here.
	 *
	 * @param callbacks a <code>Callback[]</code> value
	 * @exception IOException if an error occurs
	 * @exception UnsupportedCallbackException if an error occurs
	 */
	public void handle(final Callback[] callbacks)
		throws IOException, UnsupportedCallbackException {

		for (int i = 0; i < callbacks.length; i++) {
			log.finest("Callback: " + callbacks[i].getClass().getSimpleName());
			if (callbacks[i] instanceof RealmCallback) {
				RealmCallback rc = (RealmCallback)callbacks[i];
				realm = rc.getText();
				if (realm == null) {
					realm = rc.getDefaultText();
				} // end of if (realm == null)
				log.finest("RealmCallback: " + realm);
			} else if (callbacks[i] instanceof NameCallback) {
				NameCallback nc = (NameCallback)callbacks[i];
				String jid_tmp = nc.getName();
				if (jid_tmp == null) {
					jid_tmp = nc.getDefaultName();
				} // end of if (name == null)
          // Sometimes realm is given in separate RealmCallback
          // and sometimes is given as part of jid in NameCallback
				if (realm == null || realm.equals("")) {
					realm = JID.getNodeHost(jid_tmp);
				} // end of if (realm == null || realm.equals(""))
				user_name = JID.getNodeNick(jid_tmp);
				if (user_name == null || user_name.equals("")) {
					user_name = jid_tmp;
				} // end of if (name == null || name.equals(""))
				log.finest("NameCallback: " + user_name);
			} else if (callbacks[i] instanceof PasswordCallback) {
				PasswordCallback pc = (PasswordCallback)callbacks[i];
				try {
					user_raw_pass =
						repository.getData(JID.getNodeID(user_name, realm), "password");
					pc.setPassword(user_raw_pass.toCharArray());
					log.finest("PasswordCallback: " + user_raw_pass);
				} catch (UserNotFoundException e) {
					throw new IOException(e.toString(), e);
				} catch (TigaseDBException e) {
					log.log(Level.SEVERE, "Repository access exception.", e);
					throw new IOException("Repository access exception.", e);
				} // end of try-catch
			} else if (callbacks[i] instanceof AuthorizeCallback) {
				AuthorizeCallback authCallback = ((AuthorizeCallback)callbacks[i]);
				String authenId = authCallback.getAuthenticationID();
				log.finest("AuthorizeCallback: authenId: " + authenId);
				String authorId = authCallback.getAuthorizationID();
				log.finest("AuthorizeCallback: authorId: " + authorId);
				if (authenId.equals(authorId)) {
					CallbackHandler cbh =
						new AuthCallbackHandler(user_name, user_raw_pass, null, connection);
					LoginContext lc = null;
					try {
						lc = new LoginContext("auth-plain", cbh);
						connection.setLoginContext(lc);
						Authorization result = connection.login();
						log.finest("Authorization result: " + result);
						authCallback.setAuthorized(true);
						authCallback.setAuthorizedID(authorId);
					} catch (Exception e) {
					}
				} // end of if (authenId.equals(authorId))
			} else {
				throw new UnsupportedCallbackException
					(callbacks[i], "Unrecognized Callback");
			}
		}
	}

  private class AuthCallbackHandler implements CallbackHandler {

		private String name = null;
		private String password = null;
		private String resource = null;
		private XMPPResourceConnection connection = null;

		public AuthCallbackHandler(final String name, final String pass,
			final String resource, final XMPPResourceConnection connection) {
			// Some Jabber/XMPP clients send user name as full JID, others
			// send just nick name. Let's resolve this here.
			this.name = JID.getNodeNick(name);
			if (this.name == null || this.name.equals("")) {
				this.name = name;
			} // end of if (name == null || name.equals(""))
			this.password = pass;
			this.resource = resource;
			this.connection = connection;
		}

		public void handle(final Callback[] callbacks)
      throws IOException, UnsupportedCallbackException {

      for (int i = 0; i < callbacks.length; i++) {
        log.finest("Callback: " + callbacks[i].getClass().getSimpleName());
				if (callbacks[i] instanceof NameCallback) {
          log.finest("NameCallback: " + name);
					NameCallback nc = (NameCallback)callbacks[i];
					nc.setName(name);
        } else if (callbacks[i] instanceof PasswordCallback) {
          log.finest("NameCallback: " + password);
					PasswordCallback pc = (PasswordCallback)callbacks[i];
					pc.setPassword(password.toCharArray());
        } else if (callbacks[i] instanceof ResourceConnectionCallback) {
          log.finest("ResourceConnectionCallback: "
						+ connection.getConnectionId());
					ResourceConnectionCallback rcc =
						(ResourceConnectionCallback)callbacks[i];
					rcc.setResourceConnection(connection);
        } else {
          throw new UnsupportedCallbackException(callbacks[i],
						"Unrecognized Callback");
        }
      }

    }

  }

} // SaslCallbackHandler
