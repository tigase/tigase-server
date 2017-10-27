/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
import tigase.util.Algorithms;
import tigase.xmpp.jid.BareJID;

import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MD5PasswordCredentialsEntry implements Credentials.Entry {

	private static final Logger log = Logger.getLogger(MD5PasswordCredentialsEntry.class.getCanonicalName());

	private final String passwordHash;

	public MD5PasswordCredentialsEntry(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	@Override
	public String getMechanism() {
		return "MD5-PASSWORD";
	}

	@Override
	public boolean verifyPlainPassword(String plain) {
		try {
			byte[] hash = MessageDigest.getInstance("MD5").digest(plain.getBytes("UTF-8"));
			return passwordHash.equalsIgnoreCase(Algorithms.bytesToHex(hash));
		} catch (Exception ex) {
			log.log(Level.WARNING, "failed to verify password digest", ex);
		}
		return false;
	}

	@Bean(name = "MD5-PASSWORD", parent = CredentialsEncoderBean.class, active = false)
	public static class Encoder implements Credentials.Encoder {

		@Override
		public String getName() {
			return "MD5-PASSWORD";
		}

		@Override
		public String encode(BareJID user, String password) {
			try {
				byte[] hash = MessageDigest.getInstance("MD5").digest(password.getBytes("UTF-8"));
				return Algorithms.bytesToHex(hash);
			} catch (Exception ex) {
				throw new RuntimeException("failed to generate password hash", ex);
			}
		}
	}

	@Bean(name = "MD5-PASSWORD", parent = CredentialsDecoderBean.class, active = false)
	public static class Decoder implements Credentials.Decoder {

		@Override
		public String getName() {
			return "MD5-PASSWORD";
		}

		@Override
		public Credentials.Entry decode(BareJID user, String value) {
			return new MD5PasswordCredentialsEntry(value);
		}
	}
}
