package tigase.auth.callbacks;

import javax.security.auth.callback.Callback;

public class SaltedPasswordCallback implements Callback, java.io.Serializable {

	private static final long serialVersionUID = -4342673378785456908L;

	private String prompt;

	private byte[] saltedPassword;

	public SaltedPasswordCallback(String prompt) {
		this.prompt = prompt;
	}

	/**
	 * @return the salt
	 */
	public byte[] getSaltedPassword() {
		return saltedPassword;
	}

	/**
	 * @param salt
	 *            the salt to set
	 */
	public void setSaltedPassword(byte[] salt) {
		this.saltedPassword = salt;
	}

}
