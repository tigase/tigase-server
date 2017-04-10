/*
 * CaptchaProvider.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
import tigase.xmpp.XMPPResourceConnection;

import java.security.SecureRandom;
import java.util.Random;

@Bean(name = "CaptchaProvider", parent = JabberIqRegister.class, active = true)
public class CaptchaProvider {

	private Random random = new SecureRandom();

	public CaptchaItem generateCaptcha() {
		return new SimpleTextCaptcha(1 + random.nextInt(31), 1 + random.nextInt(31));
	}

	public interface CaptchaItem {

		String getCaptchaRequest(XMPPResourceConnection session);

		int getErrorCounter();

		void incraseErrorCounter();

		boolean isResponseValid(XMPPResourceConnection session, String response);

	}

	private class SimpleTextCaptcha
			implements CaptchaItem {

		private final int a;

		private final int b;
		private int errorCounter;

		SimpleTextCaptcha(int a, int b) {
			this.a = a;
			this.b = b;
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
			final int v = a + b;
			return response.trim().equals(String.valueOf(v));
		}
	}

}
