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
