/**
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
package tigase.db;

import tigase.annotations.TigaseDeprecated;
import tigase.auth.CredentialsDecoderBean;
import tigase.auth.CredentialsEncoderBean;
import tigase.auth.credentials.Credentials;
import tigase.auth.credentials.entries.PlainCredentialsEntry;
import tigase.xmpp.jid.BareJID;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interface <code>AuthRepository</code> defines a proxy bridge between user authentication data storage and the Tigase
 * server authentication logic. Important thing about the authentication repository is that it not only stores login
 * credentials but also performs actual user authentication. This is because available authentication mechanisms depend
 * on the way data are stored in the repository (database).
 * <br>
 * Created: Sun Nov  5 21:15:46 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface AuthRepository
		extends Repository {

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide an extra authentication data by
	 * the client to the authentication logic. Please note the <code>RESULT_KEY</code> property key is used to provide
	 * authentication data from the server to the client. This property is used to provide authentication data from the
	 * client to the server.
	 */
	String DATA_KEY = "data";

	String DIGEST_ID_KEY = "digest-id";

	String DIGEST_KEY = "digest";

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide desired authentication mechanism
	 * to the authentication logic.
	 */
	String MACHANISM_KEY = "mechanism";

	String PASSWORD_KEY = "password";

	// Query params (and otherAuth)

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide desired authentication protocol
	 * to the authentication logic.
	 */
	String PROTOCOL_KEY = "protocol";

	/**
	 * Property value for <code>otherAuth</code> method call. It is used to provide desired authentication NON-SASL
	 * protocol to the authentication logic.
	 */
	String PROTOCOL_VAL_NONSASL = "nonsasl";

	/**
	 * Property value for <code>otherAuth</code> method call. It is used to provide desired authentication SASL protocol
	 * to the authentication logic.
	 */
	String PROTOCOL_VAL_SASL = "sasl";

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide authentication realm to the
	 * authentication logic. In most cases, the realm is just a domain name.
	 */
	String REALM_KEY = "realm";

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide authentication handshaking data
	 * during login process. Some authentication mechanisms require exchanging requests between the client and the
	 * server. This property key points back to the data which need to be sent back to the client.
	 */
	String RESULT_KEY = "result";

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide authentication domain to the
	 * authentication logic. It is highly recommended that this property is always set, even if the authentication
	 * protocol/mechanism does not need it strictly.
	 */
	String SERVER_NAME_KEY = "server-name";

	/**
	 * Property key name for <code>otherAuth</code> method call. It is used to provide a user ID on successful user
	 * login. Please note, the key points to the object of <code>BareJID</code> type.
	 */
	String USER_ID_KEY = "user-id";

	String USERNAME_KEY = "username";

	enum AccountStatus {
		active(1),
		disabled(0),
		pending(-2),
		system(-1),
		vip(2),
		paid(3);

		private static final HashMap<Integer, AccountStatus> statuses = new HashMap<>();

		static {
			for (AccountStatus v : AccountStatus.values()) {
				statuses.put(v.getValue(), v);
			}
		}

		private final int value;

		public static AccountStatus byValue(int value) {
			return statuses.get(value);
		}

		AccountStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	void addUser(BareJID user, String password) throws TigaseDBException;

	AccountStatus getAccountStatus(BareJID user) throws TigaseDBException;

	default Credentials getCredentials(BareJID user, String username) throws TigaseDBException {
		String password = getPassword(user);
		if (password != null) {
			return new SingleCredential(user, getAccountStatus(user), new PlainCredentialsEntry(password));
		}
		return null;
	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	String getPassword(BareJID user) throws TigaseDBException;

	/**
	 * <code>getResourceUri</code> method returns database connection string.
	 *
	 * @return a <code>String</code> value of database connection string.
	 */
	String getResourceUri();

	default Collection<String> getUsernames(BareJID user) throws TigaseDBException {
		return Collections.emptyList();
	}

	/**
	 * This method is only used by the server statistics component to report number of registered users.
	 *
	 * @return a <code>long</code> number of registered users in the repository.
	 */
	long getUsersCount();

	/**
	 * This method is only used by the server statistics component to report number of registered users for given
	 * domain.
	 *
	 * @param domain for which get the statistics
	 *
	 * @return a <code>long</code> number of registered users in the repository.
	 */
	long getUsersCount(String domain);

	default boolean isMechanismSupported(String domain, String mechanism) {
		return "PLAIN".equals(mechanism);
	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	default boolean isUserDisabled(BareJID user) throws TigaseDBException {
		AccountStatus s = getAccountStatus(user);
		return s == AccountStatus.disabled;
	}

	/**
	 * Do some actions on repository, when user logs in. (for example update <code>last_login_time</code>)
	 *
	 * @param jid JID of logged user.
	 *
	 * @throws TigaseDBException if an error occurs
	 */
	void loggedIn(BareJID jid) throws TigaseDBException;

	void logout(BareJID user) throws TigaseDBException;

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	boolean otherAuth(Map<String, Object> authProps)
			throws TigaseDBException, AuthorizationException;

	/**
	 * <code>queryAuth</code> returns mechanisms available for authentication.
	 *
	 * @param authProps a <code>Map</code> value with parameters for authentication.
	 */
	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	void queryAuth(Map<String, Object> authProps);

	default void removeCredential(BareJID user, String username) throws TigaseDBException {

	}

	void removeUser(BareJID user) throws TigaseDBException;

	void setAccountStatus(BareJID user, AccountStatus status) throws TigaseDBException;

	default void setCredentialsCodecs(CredentialsEncoderBean encoder, CredentialsDecoderBean decoder) {

	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	default void setUserDisabled(BareJID user, Boolean value) throws TigaseDBException {
		AccountStatus status = getAccountStatus(user);
		if (status == AccountStatus.active || status == AccountStatus.disabled) {
			setAccountStatus(user, value ? AccountStatus.disabled : AccountStatus.active);
		}
	}

	default void updateCredential(BareJID user, String username, String password)
			throws TigaseDBException {
		updatePassword(user, password);
	}

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	void updatePassword(BareJID user, String password) throws TigaseDBException;

	class DefaultCredentials
			implements Credentials {

		private static final Logger log = Logger.getLogger(DefaultCredentials.class.getCanonicalName());
		private final AccountStatus accountStatus;
		private final CredentialsDecoderBean decoder;
		private final List<RawEntry> entries;
		private final BareJID user;

		public DefaultCredentials(BareJID user, AccountStatus accountStatus, List<RawEntry> entries,
								  CredentialsDecoderBean decoderBean) {
			this.accountStatus = accountStatus;
			this.user = user;
			this.entries = entries;
			this.decoder = decoderBean;
		}

		@Override
		public Credentials.Entry getEntryForMechanism(String mechanism) {
			for (RawEntry entry : entries) {
				if (entry.isForMechanism(mechanism)) {
					try {
						return decoder.decode(user, mechanism, entry.getValue());
					} catch (NoSuchAlgorithmException ex) {
						log.log(Level.WARNING, "Could not decode credentials for " + mechanism, ex);
					}
				}
			}
			return null;
		}

		@Override
		public Credentials.Entry getFirst() {
			if (entries.isEmpty()) {
				return null;
			}
			RawEntry entry = entries.get(0);
			try {
				return decoder.decode(user, entry.getMechanism(), entry.getValue());
			} catch (NoSuchAlgorithmException ex) {
				log.log(Level.WARNING, "Could not decode credentials for " + entry.getMechanism(), ex);
				return null;
			}
		}

		@Override
		public BareJID getUser() {
			return user;
		}

		public boolean isAccountDisabled() {
			return accountStatus == AccountStatus.disabled;
		}

		public static class RawEntry
				implements Credentials.RawEntry {

			private final String mechanism;
			private final String value;

			public RawEntry(String mechanism, String value) {
				this.mechanism = mechanism;
				this.value = value;
			}

			public String getMechanism() {
				return mechanism;
			}

			public String getValue() {
				return value;
			}

		}
	}

	class SingleCredential
			implements Credentials {

		private final AccountStatus accountStatus;
		private final Credentials.Entry entry;
		private final BareJID user;

		public SingleCredential(BareJID user, AccountStatus accountStatus, Credentials.Entry entry) {
			this.user = user;
			this.entry = entry;
			this.accountStatus = accountStatus;
		}

		@Override
		public Entry getEntryForMechanism(String mechanism) {
			if (mechanism.equals(entry.getMechanism())) {
				return entry;
			}
			return null;
		}

		@Override
		public Entry getFirst() {
			return entry;
		}

		@Override
		public BareJID getUser() {
			return user;
		}

		@Override
		public boolean isAccountDisabled() {
			return accountStatus == AccountStatus.disabled;
		}
	}

}    // AuthRepository

