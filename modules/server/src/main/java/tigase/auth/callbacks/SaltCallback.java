package tigase.auth.callbacks;

import javax.security.auth.callback.Callback;

public class SaltCallback implements Callback, java.io.Serializable {

	private static final long serialVersionUID = -4342673378785456908L;

	private String prompt;

	private byte[] salt;

	public SaltCallback(String prompt) {
		this.prompt = prompt;
	}

	/**
	 * @return the salt
	 */
	public byte[] getSalt() {
		return salt;
	}

	/**
	 * @param salt
	 *            the salt to set
	 */
	public void setSalt(byte[] salt) {
		this.salt = salt;
	}

}
