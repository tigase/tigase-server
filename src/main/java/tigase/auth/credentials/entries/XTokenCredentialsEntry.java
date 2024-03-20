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
import tigase.auth.credentials.Credentials;
import tigase.auth.mechanisms.SaslXTOKEN;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.Base64;
import tigase.xmpp.jid.BareJID;

public class XTokenCredentialsEntry
		implements Credentials.Entry {

	private final byte[] secretKey;
	private boolean oneTime;

	public XTokenCredentialsEntry(byte[] secretKey, boolean oneTime) {
		this.secretKey = secretKey;
		this.oneTime = oneTime;
	}

	public byte[] getSecretKey() {
		return secretKey;
	}

	public boolean isOneTime() {
		return oneTime;
	}

	@Override
	public String getMechanism() {
		return SaslXTOKEN.NAME;
	}

	@Override
	public boolean verifyPlainPassword(String plain) {
		return false;
	}

	public String encoded() {
		return "t=" + Base64.encode(getSecretKey()) + ",o=" + isOneTime();
	}

	@Bean(name = SaslXTOKEN.NAME, parent = CredentialsDecoderBean.class, active = false)
	public static class Decoder
			implements Credentials.Decoder<XTokenCredentialsEntry> {

		@ConfigField(desc = "Mechanism name")
		private String name;
		
		@Override
		public XTokenCredentialsEntry decode(BareJID user, String value) {
			byte[] secretKey = null;
			boolean isOT = false;
			int pos = 0;
			while (pos < value.length()) {
				char c = value.charAt(pos);
				int x = value.indexOf(",", pos + 2);
				String part = value.substring(pos + 2, x == -1 ? value.length() : x);

				switch (c) {
					case 't':
						secretKey = Base64.decode(part);
						break;
					case 'o':
						isOT = Boolean.parseBoolean(part);
						break;
				}

				if (x == -1) {
					break;
				} else {
					pos = x + 1;
				}
			}
			if (secretKey == null) {
				throw new RuntimeException("secret key cannot be null!");
			}
			return new XTokenCredentialsEntry(secretKey, isOT);
		}

		@Override
		public String getName() {
			return name;
		}
	}

	@Bean(name = SaslXTOKEN.NAME, parent = CredentialsEncoderBean.class, active = false)
	public static class Encoder
			implements Credentials.Encoder<XTokenCredentialsEntry> {

		@ConfigField(desc = "Mechanism name")
		private String name;
		
		@Override
		public String encode(BareJID user, String password) {
			return null;
		}

		@Override
		public String encode(BareJID user, XTokenCredentialsEntry entry) {
			return entry.encoded();
		}

		@Override
		public String getName() {
			return name;
		}
	}
}
