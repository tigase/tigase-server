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
package tigase.auth.callbacks;

import javax.security.auth.callback.Callback;

/**
 * Class for validate password. Called by SASL mechanisms. If given password is valid then {@linkplain
 * VerifyPasswordCallback#setVerified(boolean) setVerified(true)} must be called.
 */
public class VerifyPasswordCallback
		implements Callback {

	private final String password;

	private boolean verified = false;

	public VerifyPasswordCallback(final String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public boolean isVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
	}

}
