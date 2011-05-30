package tigase.db.ldap;

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import tigase.db.AuthRepositoryImpl;
import tigase.db.AuthorizationException;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.util.Base64;
import tigase.xmpp.BareJID;

public class LdapAuthProvider extends AuthRepositoryImpl {

	private class SaslPLAINLdap implements SaslServer {

		private boolean authOk = false;

		@Override
		public void dispose() throws SaslException {
		}

		@Override
		public byte[] evaluateResponse(byte[] byteArray) throws SaslException {
			int auth_idx = 0;

			while ((byteArray[auth_idx] != 0) && (auth_idx < byteArray.length)) {
				++auth_idx;
			}

			final String authoriz = new String(byteArray, 0, auth_idx);
			int user_idx = ++auth_idx;

			while ((byteArray[user_idx] != 0) && (user_idx < byteArray.length)) {
				++user_idx;
			}

			final String user_id = new String(byteArray, auth_idx, user_idx - auth_idx);

			if (log.isLoggable(Level.FINEST)) {
				log.finest("SASL userId: " + user_id);
			}

			++user_idx;

			final String passwd = new String(byteArray, user_idx, byteArray.length - user_idx);

			if (log.isLoggable(Level.FINEST)) {
				log.finest("SASL password: " + passwd);
			}

			try {
				authOk = ldapAuth(user_id, passwd);
			} catch (Exception e) {
				log.log(Level.WARNING, "Can't authenticate user", e);
				authOk = false;
			}

			return null;
		}

		@Override
		public String getAuthorizationID() {
			return null;
		}

		@Override
		public String getMechanismName() {
			return "PLAIN";
		}

		@Override
		public Object getNegotiatedProperty(String propName) {
			return null;
		}

		@Override
		public boolean isComplete() {
			return authOk;
		}

		@Override
		public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
			return null;
		}

		@Override
		public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
			return null;
		}

	};

	protected static final String[] non_sasl_mechs = { "password" };

	public static final String PROVIDER_URL_KEY = "ldap-url";

	protected static final String[] sasl_mechs = { "PLAIN" };

	/**
	 * Example: <code>uid=%s,ou=people,dc=xmpp-test,dc=org</code>
	 */
	public static final String USER_DN_PATTERN_KEY = "user-dn-pattern";

	private String providerUrl;

	private String userDnPattern;

	public LdapAuthProvider(UserRepository repo) {
		super(repo);
	}

	@Override
	public void initRepository(String string, java.util.Map<String, String> params) throws tigase.db.DBInitException {
		super.initRepository(string, params);
		this.userDnPattern = params.get(USER_DN_PATTERN_KEY);
		this.providerUrl = params.get(PROVIDER_URL_KEY);
		if (log.isLoggable(Level.CONFIG)) {
			log.config("User DN Pattern: " + this.userDnPattern);
			log.config("LDAP URL: " + this.providerUrl);
		}
	}

	private boolean ldapAuth(final String username, final String password) throws UserNotFoundException, TigaseDBException,
			AuthorizationException {
		try {
			Hashtable<Object, Object> env = new Hashtable<Object, Object>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, this.providerUrl);

			if (log.isLoggable(Level.FINE))
				log.fine("Authenticating user '" + username + "' with password ******");

			final String dn = String.format(this.userDnPattern, username);

			if (log.isLoggable(Level.FINER))
				log.finer("Using DN:" + dn);

			env.put(Context.SECURITY_AUTHENTICATION, "simple");
			env.put(Context.SECURITY_PRINCIPAL, dn);
			env.put(Context.SECURITY_CREDENTIALS, password);

			// Create the initial context
			DirContext ctx = new InitialDirContext(env);
			ctx.close();
			if (log.isLoggable(Level.FINE))
				log.fine("User " + username + " authenticated.");
			return true;
		} catch (javax.naming.AuthenticationException e) {
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "Authentication error: " + e.getMessage());
			return false;
		} catch (Exception e) {
			if (log.isLoggable(Level.WARNING))
				log.log(Level.WARNING, "Can't authenticate user", e);
			return false;
		}

	}

	@Override
	public boolean otherAuth(Map<String, Object> props) throws UserNotFoundException, TigaseDBException, AuthorizationException {
		String proto = (String) props.get(PROTOCOL_KEY);

		if (proto.equals(PROTOCOL_VAL_SASL)) {
			if (props.get(MACHANISM_KEY).equals("PLAIN")) {
				saslAuth(props);
			}
		} else if (proto.equals(PROTOCOL_VAL_NONSASL)) {
			String password = (String) props.get(PASSWORD_KEY);
			BareJID user_id = (BareJID) props.get(USER_ID_KEY);

			return ldapAuth(user_id.getLocalpart(), password);
		}

		throw new AuthorizationException("Protocol is not supported.");
	}

	@Override
	public void queryAuth(final Map<String, Object> authProps) {
		String protocol = (String) authProps.get(PROTOCOL_KEY);

		if (protocol.equals(PROTOCOL_VAL_NONSASL)) {
			authProps.put(RESULT_KEY, non_sasl_mechs);
		}

		if (protocol.equals(PROTOCOL_VAL_SASL)) {
			authProps.put(RESULT_KEY, sasl_mechs);
		}
	}

	private boolean saslAuth(final Map<String, Object> props) throws AuthorizationException {
		try {
			SaslServer ss = new SaslPLAINLdap();

			String data_str = (String) props.get(DATA_KEY);
			byte[] in_data = ((data_str != null) ? Base64.decode(data_str) : new byte[0]);

			if (log.isLoggable(Level.FINEST)) {
				log.finest("response: " + new String(in_data));
			}

			byte[] challenge = ss.evaluateResponse(in_data);

			if (log.isLoggable(Level.FINEST)) {
				log.finest("challenge: " + ((challenge != null) ? new String(challenge) : "null"));
			}

			String challenge_str = (((challenge != null) && (challenge.length > 0)) ? Base64.encode(challenge) : null);

			props.put(RESULT_KEY, challenge_str);

			if (ss.isComplete()) {
				return true;
			} else {
				return false;
			} // end of if (ss.isComplete()) else
		} catch (SaslException e) {
			throw new AuthorizationException("Sasl exception.", e);
		} // end of try-catch
	}

}
