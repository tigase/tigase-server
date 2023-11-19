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
package tigase.xmpp.impl;

import tigase.kernel.beans.Bean;
import tigase.util.Base64;
import tigase.xmpp.XMPPResourceConnection;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

@Bean(name = "CaptchaProvider", parent = JabberIqRegister.class, active = true)
public class CaptchaProvider {

	private Random random = new SecureRandom();

	public CaptchaItem generateCaptcha(XMPPResourceConnection connection) {
		return new SimpleTextCaptcha(random, connection);
	}

	public CaptchaItem getCaptchaByID(String id) {
		if (id == null) {
			return null;
		}

		try {
			String[] parts = id.split("\\.");
			String type = parts[0];
			if (!"simple-text".equals(type)) {
				return null;
			}

			return new SimpleTextCaptcha(parts);
		} catch (Throwable ex) {
			// could not parse captcha data
			return null;
		}
	}

	public interface CaptchaItem {

		String getID();

		String getCaptchaRequest(XMPPResourceConnection session);

		int getErrorCounter();

		void incraseErrorCounter();

		boolean isResponseValid(XMPPResourceConnection session, String response);

	}

	private class SimpleTextCaptcha
			implements CaptchaItem {

		private static final Duration TIMEOUT = Duration.ofMinutes(5);
		private static final int NONCE_LENGTH = 16;

		private final int a;

		private final int b;
		private final int result;
		private final byte[] nonce;
		private final long time;
		private final byte[] hmac;
		private int errorCounter;
		private final Duration timeout = TIMEOUT;

		public static byte[] calculateHMac(byte[] data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
			SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(secretKeySpec);
			return mac.doFinal(data);
		}

		public static String getSecret(XMPPResourceConnection connection) {
			return Optional.ofNullable(connection.getDomain().getS2sSecret())
					.orElse(connection.getDomainAsJID().toString());
		}

		SimpleTextCaptcha(Random random, XMPPResourceConnection connection) {
			this.a = 1 + random.nextInt(31);
			this.b = 1 + random.nextInt(31);
			this.result = a + b;
			this.nonce = new byte[NONCE_LENGTH];
			random.nextBytes(this.nonce);
			this.time = System.currentTimeMillis();

			try {
				this.hmac = calculateHMac((getPrefix() + "." + result).getBytes(StandardCharsets.UTF_8),
										  getSecret(connection));
			} catch (Throwable ex) {
				throw new RuntimeException("Could not generate HMAC", ex);
			}
		}

		SimpleTextCaptcha(String[] parts) {
			this.nonce = new byte[NONCE_LENGTH];
			ByteBuffer tmp = ByteBuffer.wrap(Base64.decode(parts[1]));
			tmp.get(nonce);
			this.a = tmp.getInt();
			this.b = tmp.getInt();
			this.time = tmp.getLong();
			this.result = a + b;
			this.hmac = Base64.decode(parts[2]);
		}

		@Override
		public String getID() {
			return getPrefix() + "." + Base64.encode(hmac);
		}

		protected String getPrefix() {
			ByteBuffer tmp = ByteBuffer.allocate(nonce.length + 4 + 4 + 8);
			tmp.put(nonce);
			tmp.putInt(a);
			tmp.putInt(b);
			tmp.putLong(time);
			return "simple-text" + "." + Base64.encode(tmp.array());
		}

		@Override
		public String getCaptchaRequest(XMPPResourceConnection session) {
			return "Solve: " + String.valueOf(a) + " + " + String.valueOf(b);
		}

		@Override
		public int getErrorCounter() {
			return errorCounter;
		}

		@Override
		public void incraseErrorCounter() {
			++this.errorCounter;
		}

		@Override
		public boolean isResponseValid(XMPPResourceConnection session, String response) {
			if (response == null) {
				return false;
			}
			try {
				String responseResult = response.trim();
				// check if response matches
				if (!String.valueOf(this.result).equals(responseResult)) {
					return false;
				}
				// check if it is not expired
				if (time > System.currentTimeMillis() || (System.currentTimeMillis() - time) > timeout.toMillis()) {
					return false;
				}
				// then check if anyone tampered with token
				byte[] calculated = calculateHMac((getPrefix() + "." + responseResult).getBytes(StandardCharsets.UTF_8),
												  getSecret(session));
				return Arrays.equals(hmac, calculated);
			} catch (Throwable ex) {
				return false;
			}
		}

	}

}
