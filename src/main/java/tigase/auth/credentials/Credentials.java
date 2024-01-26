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
package tigase.auth.credentials;

import tigase.annotations.TigaseDeprecated;
import tigase.xmpp.jid.BareJID;

import static tigase.db.AuthRepository.AccountStatus;

/**
 * Interface implemented by classes handling user login credentials. In implementations of this interface multiple
 * credentials for single account may be stored but for single credentialId, ie. different credentials for different
 * authentication mechanisms.
 */
public interface Credentials {

	String DEFAULT_CREDENTIAL_ID = "default";

	@Deprecated
	@TigaseDeprecated(since = "8.1.0", note = "Username changed to CredentialId")
	String DEFAULT_USERNAME = DEFAULT_CREDENTIAL_ID;

	/**
	 * Checks if account can perform logging-in
	 */
	boolean canLogin();

	/**
	 * @return account status of the account
	 */
	AccountStatus getAccountStatus();

	/**
	 * Find a credential for specified encryption mechanism
	 *
	 * @return instance of an entry if available or null
	 */
	Entry getEntryForMechanism(String mechanism);

	/**
	 * Returns first available instance of credentials entry
	 *
	 * @return first available instance of credentials entry
	 */
	Entry getFirst();

	/**
	 * Returns bare jid of an account
	 *
	 * @return bare jid of an account
	 */
	BareJID getUser();

	/**
	 * Checks if account is disabled
	 */
	boolean isAccountDisabled();

	/**
	 * Interface implemented by credentials decoder converting from value stored in database to the form represented by
	 * implementation of Entry interface.
	 */
	interface Decoder<E extends Entry> {

		/**
		 * Decode credentials stored in database to more suitable form.
		 *
		 * @return credentials stored in database in more suitable form
		 */
		E decode(BareJID user, String value);

		/**
		 * Name of the encryption mechanism for which decoder works
		 *
		 * @return name of the encryption mechanism for which decoder works
		 */
		String getName();

	}

	/**
	 * Interface implemented by credentials encoder converting them from plaintext value to encoded form stored in the
	 * database.
	 */
	interface Encoder<E extends Entry> {

		/**
		 * Encodes entry to store in database.
		 *
		 * @param user for which encode
		 * @param password plaintext password to encode
		 *
		 * @return encoded authentication data.
		 */
		String encode(BareJID user, String password);

		/**
		 * Encodes entry to store in database.
		 *
		 * @param user for which encode
		 * @param entry to encode
		 *
		 * @return encoded authentication data.
		 */
		String encode(BareJID user, E entry);

		/**
		 * Name of the encryption mechanism for which encoder works
		 *
		 * @return name of the encryption mechanism for which encoder works
		 */
		String getName();

	}

	/**
	 * Interface required to be implemented by classes representing credential entry.
	 */
	interface Entry {

		/**
		 * Name of the encryption mechanism used to encode stored credentials.
		 *
		 * Note: Value returned by this method may be equal to SASL mechanism name used to encode this value,
		 * but doesn't have to be only one of SASL mechanism name, ie. for passwords encoded for PLAIN mechanism
		 * not stored in plain format in the repository.
		 */
		String getMechanism();

		/**
		 * Check if plaintext password will match stored credential
		 */
		boolean verifyPlainPassword(String plain);

	}

	/**
	 * Interface implemented by classes used as DTO for credentials read from repository.s
	 */
	interface RawEntry {

		/**
		 * Name of the encryption mechanism used to encode stored credentials.
		 *
		 * Note: Value returned by this method may be equal to SASL mechanism name used to encode this value,
		 * but doesn't have to be only one of SASL mechanism name, ie. for passwords encoded for PLAIN mechanism
		 * not stored in plain format in the repository.
		 */
		String getMechanism();

		/**
		 * Encoded value
		 */
		String getValue();

		/**
		 * Checks if the provided string matches the name of the encryption mechanism used to encode data for storage
		 * in the repository.
		 */
		default boolean isForMechanism(String mechanism) {
			return mechanism.equals(getMechanism());
		}

	}

}
