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
package tigase.auth.mechanisms;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SCRAMHelper {

	/**
	 * Encode plain password to SCRAM authentication data: ServerKey and StoredKey.
	 *
	 * @param algorithm algorithm.
	 * @param salt salt.
	 * @param iterations iterations.
	 * @param plainPassword plain password.
	 *
	 * @return authentication data
	 */
	public static AuthenticationData encodePlainPassword(String algorithm, byte[] salt, int iterations,
														 String plainPassword)
			throws NoSuchAlgorithmException, InvalidKeyException {
		var saltedPassword = AbstractSaslSCRAM.hi(algorithm, AbstractSaslSCRAM.normalize(plainPassword), salt,
												  iterations);
		return transcode(algorithm, saltedPassword);
	}

	/**
	 * Transcode Salted Password (PBKDF2) to SCRAM authentication data: ServerKey and StoredKey.
	 *
	 * @param algorithm algorithm.
	 * @param saltedPassword salted password to transcode.
	 *
	 * @return authentication data.
	 */
	public static AuthenticationData transcode(String algorithm, byte[] saltedPassword)
			throws NoSuchAlgorithmException, InvalidKeyException {
		byte[] ck = AbstractSaslSCRAM.hmac(algorithm, saltedPassword, AbstractSaslSCRAM.DEFAULT_CLIENT_KEY);
		var storedKey = AbstractSaslSCRAM.h(algorithm, ck);
		var serverKey = AbstractSaslSCRAM.hmac(algorithm, saltedPassword, AbstractSaslSCRAM.DEFAULT_SERVER_KEY);
		return new AuthenticationData(algorithm, storedKey, serverKey);
	}

	private SCRAMHelper() {
	}

	public record AuthenticationData(String algorithm, byte[] storedKey, byte[] serverKey) {

	}

}
