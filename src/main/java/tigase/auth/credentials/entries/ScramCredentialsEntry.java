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
package tigase.auth.credentials.entries;

import tigase.auth.credentials.Credentials;
import tigase.auth.mechanisms.AbstractSaslSCRAM;
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
	private final int iterations = 4096;
	private final byte[] salt;
	private final byte[] saltedPassword;

	public ScramCredentialsEntry(String algorithm, PlainCredentialsEntry entry)
			throws NoSuchAlgorithmException, InvalidKeyException {
		final SecureRandom random = new SecureRandom();
		this.algorithm = algorithm;
		this.salt = new byte[10];
		random.nextBytes(salt);
		this.saltedPassword = AbstractSaslSCRAM.hi(algorithm, AbstractSaslSCRAM.normalize(entry.getPassword()), salt,
												   iterations);
	}

	public ScramCredentialsEntry(String algorithm, byte[] salt, int iterations, byte[] saltedPassword) {
		this.algorithm = algorithm;
		this.salt = salt;
		this.saltedPassword = saltedPassword;
	}

	public byte[] getSalt() {
		return salt;
	}

	public byte[] getSaltedPassword() {
		return saltedPassword;
	}

	public int getIterations() {
		return iterations;
	}

	@Override
	public String getMechanism() {
		return "SCRAM-" + algorithm;
	}

	@Override
	public boolean verifyPlainPassword(String password) {
		try {
			byte[] expSaltedPassword = AbstractSaslSCRAM.hi(algorithm, AbstractSaslSCRAM.normalize(password), salt,
															iterations);
			return Arrays.equals(this.saltedPassword, expSaltedPassword);
		} catch (InvalidKeyException | NoSuchAlgorithmException ex) {
			log.log(Level.FINE, "Password comparison failed", ex);
		}
		return false;
	}

	public static class Decoder
			implements Credentials.Decoder {

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
		public String getName() {
			return name;
		}

		@Override
		public Credentials.Entry decode(BareJID user, String value) {
			byte[] salt = null;
			byte[] saltedPassword = null;
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
				}

				if (x == -1) {
					break;
				} else {
					pos = x + 1;
				}
			}

			return newInstance(salt, iterations, saltedPassword);
		}

		protected Credentials.Entry newInstance(byte[] salt, int iterations, byte[] saltedPassword) {
			return new ScramCredentialsEntry(algorithm, salt, iterations, saltedPassword);
		}
	}

	public static class Encoder
			implements Credentials.Encoder {

		private final SecureRandom random = new SecureRandom();
		@ConfigField(desc = "Hash algorithm")
		private String algorithm;

		@ConfigField(desc = "Number of iterations")
		private int iterations = 4096;
		@ConfigField(desc = "Mechanism name")
		private String name;

		public Encoder() {

		}

		protected Encoder(String algorithm) {
			this.algorithm = algorithm;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String encode(BareJID user, String password) {
			byte[] salt = new byte[10];
			random.nextBytes(salt);
			byte[] saltedPassword = new byte[0];
			try {
				saltedPassword = AbstractSaslSCRAM.hi(algorithm, AbstractSaslSCRAM.normalize(password), salt,
													  iterations);
			} catch (InvalidKeyException | NoSuchAlgorithmException e) {
				throw new RuntimeException("Could not encode password", e);
			}

			return "s=" + tigase.util.Base64.encode(salt) + ",i=" + iterations + ",p=" +
					tigase.util.Base64.encode(saltedPassword);
		}
	}

}
