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
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.util.JID;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Describe class PlainAuth here.
 *
 *
 * Created: Sat Feb 18 13:53:55 2006
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class PlainAuth implements LoginModule {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log = Logger.getLogger("tigase.auth.PlainAuth");

	private Subject subject = null;
	private CallbackHandler callbackHandler = null;
	private Map<String, ?> sharedState = null;
	private Map<String, ?> options = null;
	private CommitHandler commitHandler = null;
	private UserRepository repository = null;

	private String user_name = null;
	private String user_pass = null;
	private String user_raw_pass = null;
	protected XMPPResourceConnection connection = null;

	private boolean loginSuccessful = false;

	// Implementation of javax.security.auth.spi.LoginModule

	/**
	 * Describe <code>initialize</code> method here.
	 *
	 * @param subject a <code>Subject</code> value
	 * @param callbackHandler a <code>CallbackHandler</code> value
	 * @param sharedState a <code>Map</code> value
	 * @param options a <code>Map</code> value
	 */
	public void initialize(final Subject subject,
		final CallbackHandler callbackHandler, final Map<String, ?> sharedState,
		final Map<String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = sharedState;
		this.options = options;
		this.commitHandler =
			(CommitHandler)options.get(CommitHandler.COMMIT_HANDLER_KEY);
		this.repository =
			(UserRepository)options.get(UserRepository.class.getSimpleName());
	}

	/**
	 * Describe <code>abort</code> method here.
	 *
	 * @return a <code>boolean</code> value
	 * @exception LoginException if an error occurs
	 */
	public boolean abort() throws LoginException {
		return true;
	}

	/**
	 * Describe <code>commit</code> method here.
	 *
	 * @return a <code>boolean</code> value
	 * @exception LoginException if an error occurs
	 */
	public boolean commit() throws LoginException {

		connection.authorize(user_name, user_raw_pass);
		commitHandler.handleLoginCommit(user_name, connection);

		return true;
	}

	/**
	 * Describe <code>login</code> method here.
	 *
	 * @return a <code>boolean</code> value
	 * @exception LoginException if an error occurs
	 */
	public boolean login() throws LoginException {

		if (callbackHandler == null) {
	    throw new LoginException("Error: no CallbackHandler available.");
		}
		if (commitHandler == null) {
	    throw new LoginException("Error: no CommitHanlder available.");
		} // end of if (commitHandler == null)
		if (repository == null) {
	    throw new LoginException("Error: no UserRepository available.");
		} // end of if (repository == null)

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
			user_raw_pass =
				repository.getData(JID.getNodeID(user_name,	connection.getDomain()),
					"password");
			if (passwordsEqual(user_pass, user_raw_pass)) {
				loginSuccessful = true;
				log.finest("Login successful.");
			} else {
				log.finest("Login unsuccessful, password incorrect.");
			} // end of else
		} catch (UserNotFoundException e) {
			loginSuccessful = false;
		} catch (NoSuchAlgorithmException e) {
			loginSuccessful = false;
		}

		return loginSuccessful;
	}

	protected boolean passwordsEqual(final String given_password,
		final String db_password) throws NoSuchAlgorithmException {
		log.finest("Comparing passwords, given: " + given_password
			+ ", db: " + db_password);
		return given_password.equals(db_password);
	}

	/**
	 * Describe <code>logout</code> method here.
	 *
	 * @return a <code>boolean</code> value
	 * @exception LoginException if an error occurs
	 */
	public boolean logout() throws LoginException {
		return true;
	}

} // PlainAuth
