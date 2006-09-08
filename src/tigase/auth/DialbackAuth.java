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
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Describe class DialbackAuth here.
 *
 *
 * Created: Mon Aug 28 11:45:17 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DialbackAuth implements LoginModule {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log = Logger.getLogger("tigase.auth.DigestAuth");

	private CallbackHandler callbackHandler = null;
	private CommitHandler commitHandler = null;
	private String user_name = null;
	private String user_pass = null;
	private String user_raw_pass = null;
	protected XMPPResourceConnection connection = null;

	private boolean loginSuccessful = false;

	public void initialize(final Subject subject,
		final CallbackHandler callbackHandler, final Map<String, ?> sharedState,
		final Map<String, ?> options) {

		this.commitHandler =
			(CommitHandler)options.get(CommitHandler.COMMIT_HANDLER_KEY);

	}

	public boolean login() throws LoginException {

		if (callbackHandler == null) {
	    throw new LoginException("Error: no CallbackHandler available.");
		}
		if (commitHandler == null) {
	    throw new LoginException("Error: no CommitHanlder available.");
		} // end of if (commitHandler == null)

		Callback[] callbacks = new Callback[3];
		NameCallback nc = new NameCallback("User name: ");
		callbacks[0] = nc;
		PasswordCallback pc = new PasswordCallback("Password: ", false);
		callbacks[1] = pc;
		ResourceConnectionCallback rcc = new ResourceConnectionCallback();
		callbacks[2] = rcc;

		try {
			callbackHandler.handle(callbacks);
			user_name = nc.getName();
			char[] password = pc.getPassword();
			if (password == null) {
				password = new char[0];
			} // end of if (password == null)
			user_pass = new String(password);
			pc.clearPassword();
			connection = rcc.getResourceConnection();
			log.finest("User name: " + user_name + ", password: " + user_pass
				+ ", connection: " + connection.getConnectionId());
		} catch (IOException ioe) {
	    throw new LoginException(ioe.toString());
		} catch (UnsupportedCallbackException uce) {
	    throw new LoginException("Error: " + uce.getCallback().toString() +
				" handler not available, can not authenticate user.");
		}

		try {
			if (passwordsEqual(user_pass, user_raw_pass)) {
				loginSuccessful = true;
				log.finest("Login successful.");
			} else {
				log.finest("Login unsuccessful, password incorrect.");
			} // end of else

		} catch (NoSuchAlgorithmException e) {
			loginSuccessful = false;
		}

		return loginSuccessful;
	}

	public boolean abort() throws LoginException {
		return true;
	}

	public boolean commit() throws LoginException {

		//		connection.authorize(user_name, user_raw_pass);
		commitHandler.handleLoginCommit(user_name, connection);

		return true;
	}

	public boolean logout() throws LoginException {
		commitHandler.handleLogout(user_name, connection);
		return true;
	}

	protected boolean passwordsEqual(final String given_password,
		final String db_password) throws NoSuchAlgorithmException {
		return true;
	}

} // DialbackAuth
