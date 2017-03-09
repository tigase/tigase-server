package tigase.db.ldap;

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import tigase.db.AuthRepository;
import tigase.db.AuthorizationException;
import tigase.db.DBInitException;
import tigase.db.Repository;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.util.Base64;
import tigase.xmpp.BareJID;

@Repository.Meta( supportedUris = { "ldaps?:.*" } )
public class LdapAuthProvider implements AuthRepository {

	private class SaslPLAINLdap implements SaslServer {

		private boolean authOk = false;

		private final String serverName;

		private BareJID userId;

		public SaslPLAINLdap(String serverName) {
			this.serverName = serverName;
		}

		@Override
		public void dispose() throws SaslException {
		}

		@Override
		public byte[] evaluateResponse(byte[] byteArray) throws SaslException {
			int auth_idx = 0;

			while ((byteArray[auth_idx] != 0) && (auth_idx < byteArray.length)) {
				++auth_idx;
			}

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
				userId = BareJID.bareJIDInstance(user_id, serverName);
				authOk = doBindAuthentication(userId, passwd);
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

		public BareJID getUser_id() {
			return userId;
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

	}

	private static final Logger log = Logger.getLogger(LdapAuthProvider.class.getName());

	protected static final String[] non_sasl_mechs = { "password" };

	protected static final String[] sasl_mechs = { "PLAIN" };

	/**
	 * Example: <code>uid=%s,ou=people,dc=xmpp-test,dc=org</code>
	 */
	public static final String USER_DN_PATTERN_KEY = "user-dn-pattern";

	private String providerUrl;

	private String userDnPattern;

	@Override
	public void addUser(BareJID user, String password) throws UserExistsException, TigaseDBException {
		throw new TigaseDBException("Not available");
	}

	@Override
	@Deprecated
	public boolean digestAuth(BareJID user, String digest, String id, String alg) throws UserNotFoundException,
			TigaseDBException, AuthorizationException {
		throw new AuthorizationException("Not supported.");
	}

	private boolean doBindAuthentication(BareJID userId, final String password) throws UserNotFoundException,
			TigaseDBException, AuthorizationException {
		try {
			Hashtable<Object, Object> env = new Hashtable<Object, Object>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, this.providerUrl);

			if (log.isLoggable(Level.FINE))
				log.fine("Authenticating user '" + userId + "' with password ******");

			final String dn = String.format(this.userDnPattern, userId.getLocalpart(), userId.getDomain(), userId.toString());

			if (log.isLoggable(Level.FINER))
				log.finer("Using DN:" + dn);

			env.put(Context.SECURITY_AUTHENTICATION, "simple");
			env.put(Context.SECURITY_PRINCIPAL, dn);
			env.put(Context.SECURITY_CREDENTIALS, password);

			// Create the initial context
			DirContext ctx = new InitialDirContext(env);
			ctx.close();
			if (log.isLoggable(Level.FINE))
				log.fine("User " + userId + " authenticated.");
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
	public String getResourceUri() {
		return providerUrl;
	}

	@Override
	public long getUsersCount() {
		return -1;
	};

	@Override
	public long getUsersCount(String domain) {
		return -1;
	}

	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		this.userDnPattern = params.get(USER_DN_PATTERN_KEY);
		this.providerUrl = resource_uri;
		if (log.isLoggable(Level.CONFIG)) {
			log.config("User DN Pattern: " + this.userDnPattern);
			log.config("LDAP URL: " + this.providerUrl);
		}
	}

	@Override
	public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {
	}

	@Override
	public boolean otherAuth(Map<String, Object> props) throws UserNotFoundException, TigaseDBException, AuthorizationException {
		String proto = (String) props.get(PROTOCOL_KEY);

		if (proto.equals(PROTOCOL_VAL_SASL)) {
			if (props.get(MACHANISM_KEY).equals("PLAIN")) {
				return saslAuth(props);
			}
		} else if (proto.equals(PROTOCOL_VAL_NONSASL)) {
			String password = (String) props.get(PASSWORD_KEY);
			BareJID user_id = (BareJID) props.get(USER_ID_KEY);
			boolean auth = doBindAuthentication(user_id, password);
			if (auth) {
				props.put(USER_ID_KEY, user_id);
			}
			return auth;
		}

		throw new AuthorizationException("Protocol is not supported.");
	}

	@Override
	@Deprecated
	public boolean plainAuth(BareJID user, String password) throws UserNotFoundException, TigaseDBException,
			AuthorizationException {
		throw new AuthorizationException("Not supported.");
	}

	@Override
	public void queryAuth(Map<String, Object> authProps) {
		String protocol = (String) authProps.get(PROTOCOL_KEY);

		if (protocol.equals(PROTOCOL_VAL_NONSASL)) {
			authProps.put(RESULT_KEY, non_sasl_mechs);
		}

		if (protocol.equals(PROTOCOL_VAL_SASL)) {
			authProps.put(RESULT_KEY, sasl_mechs);
		}
	}

	@Override
	public void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException {
		throw new TigaseDBException("Not available");
	}

	private boolean saslAuth(final Map<String, Object> props) throws AuthorizationException {
		try {
			SaslPLAINLdap ss = new SaslPLAINLdap((String) props.get(SERVER_NAME_KEY));

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
				props.put(USER_ID_KEY, ss.getUser_id());
				return true;
			} else {
				return false;
			} // end of if (ss.isComplete()) else
		} catch (SaslException e) {
			throw new AuthorizationException("Sasl exception.", e);
		}
	}

	@Override
	public void updatePassword(BareJID user, String password) throws UserNotFoundException, TigaseDBException {
		throw new TigaseDBException("Not available");
	}

	@Override
	public String getPassword(BareJID user) throws UserNotFoundException, TigaseDBException {
		throw new TigaseDBException("Not available");
	}

	@Override
	public boolean isUserDisabled(BareJID user) 
					throws UserNotFoundException, TigaseDBException {
		return false;
	}
	
	@Override
	public void setUserDisabled(BareJID user, Boolean value) 
					throws UserNotFoundException, TigaseDBException {
		throw new TigaseDBException("Feature not supported");
	}
}
