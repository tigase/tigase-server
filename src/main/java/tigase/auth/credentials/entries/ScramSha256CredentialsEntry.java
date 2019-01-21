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

import tigase.auth.CredentialsDecoderBean;
import tigase.auth.CredentialsEncoderBean;
import tigase.auth.credentials.Credentials;
import tigase.kernel.beans.Bean;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ScramSha256CredentialsEntry
		extends ScramCredentialsEntry {

	private static final String ALGORITHM = "SHA-256";

	public ScramSha256CredentialsEntry(PlainCredentialsEntry entry)
			throws NoSuchAlgorithmException, InvalidKeyException {
		super(ALGORITHM, entry);
	}

	public ScramSha256CredentialsEntry(byte[] salt, int iterations, byte[] saltedPassword) {
		super(ALGORITHM, salt, iterations, saltedPassword);
	}

	@Bean(name = "SCRAM-SHA-256", parent = CredentialsDecoderBean.class, active = true)
	public static class Decoder
			extends ScramCredentialsEntry.Decoder {

		public Decoder() {
			super(ALGORITHM);
		}

		@Override
		protected Credentials.Entry newInstance(byte[] salt, int iterations, byte[] saltedPassword) {
			return new ScramSha256CredentialsEntry(salt, iterations, saltedPassword);
		}
	}

	@Bean(name = "SCRAM-SHA-256", parent = CredentialsEncoderBean.class, active = true)
	public static class Encoder
			extends ScramCredentialsEntry.Encoder {

		public Encoder() {
			super(ALGORITHM);
		}

	}
}
