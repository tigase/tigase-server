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

import tigase.auth.CredentialsDecoderBean;
import tigase.auth.CredentialsEncoderBean;
import tigase.kernel.beans.Bean;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ScramSha1CredentialsEntry
		extends ScramCredentialsEntry {

	private static final String ALGORITHM = "SHA1";

	public ScramSha1CredentialsEntry(PlainCredentialsEntry entry) throws NoSuchAlgorithmException, InvalidKeyException {
		super("SHA-1", entry);
	}

	public ScramSha1CredentialsEntry(byte[] salt, int iterations, byte[] saltedPassword)
			throws NoSuchAlgorithmException, InvalidKeyException {
		super("SHA-1", salt, iterations, saltedPassword);
	}

	public ScramSha1CredentialsEntry(byte[] salt, int iterations, byte[] storedKey, byte[] serverKey) {
		super("SHA-1", salt, iterations, storedKey, serverKey);
	}

	@Bean(name = "SCRAM-SHA-1", parent = CredentialsDecoderBean.class, active = true)
	public static class Decoder
			extends ScramCredentialsEntry.Decoder {

		public Decoder() {
			super(ALGORITHM);
		}

		@Override
		protected ScramSha1CredentialsEntry newInstance(byte[] salt, int iterations, byte[] saltedPassword) {
			try {
				return new ScramSha1CredentialsEntry(salt, iterations, saltedPassword);
			} catch (NoSuchAlgorithmException | InvalidKeyException e) {
				throw new RuntimeException(e);
			}
		}

		protected ScramSha1CredentialsEntry newInstance(byte[] salt, int iterations, byte[] storedKey,
														byte[] serverKey) {
			return new ScramSha1CredentialsEntry(salt, iterations, storedKey, serverKey);
		}
	}

	@Bean(name = "SCRAM-SHA-1", parent = CredentialsEncoderBean.class, active = true)
	public static class Encoder
			extends ScramCredentialsEntry.Encoder {

		public Encoder() {
			super(ALGORITHM);
		}

	}
}
