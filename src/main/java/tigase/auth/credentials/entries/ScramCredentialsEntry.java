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
package tigase.auth.credentials.entries;

import tigase.auth.credentials.Credentials;
import tigase.auth.mechanisms.SCRAMHelper;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.Base64;
import tigase.xmpp.jid.BareJID;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScramCredentialsEntry
		implements Credentials.Entry {

	private static final Logger log = Logger.getLogger(ScramCredentialsEntry.class.getCanonicalName());

	private final String algorithm;
	private final int iterations;
	private final byte[] salt;
	private final byte[] serverKey;
	private final byte[] storedKey;

	public ScramCredentialsEntry(String algorithm, PlainCredentialsEntry entry)
			throws NoSuchAlgorithmException, InvalidKeyException {
		final SecureRandom random = new SecureRandom();
		this.algorithm = algorithm;
		this.iterations = 4096;
		this.salt = new byte[10];
		random.nextBytes(salt);

		var authData = SCRAMHelper.encodePlainPassword(algorithm, salt, iterations, entry.getPassword());
		this.storedKey = authData.storedKey();
		this.serverKey = authData.serverKey();
	}

	public ScramCredentialsEntry(String algorithm, byte[] salt, int iterations, byte[] saltedPassword)
			throws NoSuchAlgorithmException, InvalidKeyException {
		this.algorithm = algorithm;
		this.iterations = iterations;
		this.salt = salt;

		var authData = SCRAMHelper.transcode(algorithm, saltedPassword);
		this.storedKey = authData.storedKey();
		this.serverKey = authData.serverKey();
	}

	public ScramCredentialsEntry(String algorithm, byte[] salt, int iterations, byte[] storedKey, byte[] serverKey) {
		this.algorithm = algorithm;
		this.iterations = iterations;
		this.salt = salt;
		this.storedKey = storedKey;
		this.serverKey = serverKey;
	}

	public int getIterations() {
		return iterations;
	}

	@Override
	public String getMechanism() {
		return "SCRAM-" + algorithm;
	}

	public byte[] getSalt() {
		return salt;
	}

	public byte[] getServerKey() {
		return serverKey;
	}

	public byte[] getStoredKey() {
		return storedKey;
	}

	@Override
	public boolean verifyPlainPassword(String password) {
		try {
			var expAuthData = SCRAMHelper.encodePlainPassword(algorithm, salt, iterations, password);
			return Arrays.equals(this.serverKey, expAuthData.serverKey()) &&
					Arrays.equals(this.storedKey, expAuthData.storedKey());
		} catch (InvalidKeyException | NoSuchAlgorithmException ex) {
			log.log(Level.FINE, "Password comparison failed", ex);
		}
		return false;
	}

	public static class Decoder
			implements Credentials.Decoder<ScramCredentialsEntry> {

		@ConfigField(desc = "Hash algorithm")
		private String algorithm;
		@ConfigField(desc = "Mechanism name")
		private String name;

		public Decoder() {

		}

		protected Decoder(String algorithm) {
			this.algorithm = algorithm;
		}

		@Override
		public ScramCredentialsEntry decode(BareJID user, String value) {
			byte[] salt = null;
			byte[] saltedPassword = null;
			byte[] storedKey = null;
			byte[] serverKey = null;
			int iterations = 0;

			int pos = 0;
			while (pos < value.length()) {
				char c = value.charAt(pos);
				int x = value.indexOf(",", pos + 2);
				String part = value.substring(pos + 2, x == -1 ? value.length() : x);

				switch (c) {
					case 's':
						salt = Base64.decode(part);
						break;
					case 'i':
						iterations = Integer.parseInt(part);
						break;
					case 'p':
						saltedPassword = Base64.decode(part);
						break;
					case 't':
						storedKey = Base64.decode(part);
						break;
					case 'e':
						serverKey = Base64.decode(part);
						break;
				}

				if (x == -1) {
					break;
				} else {
					pos = x + 1;
				}
			}
			if ((storedKey == null || serverKey == null) && saltedPassword != null) {
				return newInstance(salt, iterations, saltedPassword);
			} else if (storedKey != null && serverKey != null) {
				return newInstance(salt, iterations, storedKey, serverKey);
			} else {
				throw new RuntimeException("saltedPassword or storedKey&serverKey pair must be not null.");
			}
		}

		@Override
		public String getName() {
			return name;
		}

		protected ScramCredentialsEntry newInstance(byte[] salt, int iterations, byte[] saltedPassword) {
			try {
				return new ScramCredentialsEntry(algorithm, salt, iterations, saltedPassword);
			} catch (NoSuchAlgorithmException | InvalidKeyException e) {
				throw new RuntimeException(e);
			}
		}

		protected ScramCredentialsEntry newInstance(byte[] salt, int iterations, byte[] storedKey, byte[] serverKey) {
			return new ScramCredentialsEntry(algorithm, salt, iterations, storedKey, serverKey);
		}
	}

	public static class Encoder
			implements Credentials.Encoder<ScramCredentialsEntry> {

		@ConfigField(desc = "Number of iterations")
		private final int iterations = 4096;
		private final SecureRandom random = new SecureRandom();
		@ConfigField(desc = "Hash algorithm")
		private String algorithm;
		@ConfigField(desc = "Mechanism name")
		private String name;

		public Encoder() {

		}

		protected Encoder(String algorithm) {
			this.algorithm = algorithm;
		}

		public static String encode(byte[] salt, int iterations, byte[] storedKey, byte[] serverKey) {
			return "s=" + tigase.util.Base64.encode(salt) + ",i=" + iterations + ",t=" +
					tigase.util.Base64.encode(storedKey) + ",e=" +
					tigase.util.Base64.encode(serverKey);
		}

		public static String encode(ScramCredentialsEntry entry) {
			return encode(entry.getSalt(), entry.getIterations(), entry.getStoredKey(), entry.getServerKey());
		}

		@Override
		public String encode(BareJID user, ScramCredentialsEntry entry) {
			return encode(entry);
		}

		@Override
		public String encode(BareJID user, String password) {
			byte[] salt = new byte[10];
			random.nextBytes(salt);
			SCRAMHelper.AuthenticationData authData;
			try {
				authData = SCRAMHelper.encodePlainPassword(algorithm, salt, iterations, password);
			} catch (InvalidKeyException | NoSuchAlgorithmException e) {
				throw new RuntimeException("Could not encode password", e);
			}

			return encode(salt, iterations, authData.storedKey(), authData.serverKey());
		}

		@Override
		public String getName() {
			return name;
		}
	}

}
