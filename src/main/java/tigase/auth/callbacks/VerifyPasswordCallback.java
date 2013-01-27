package tigase.auth.callbacks;

import javax.security.auth.callback.Callback;

/**
 * Class for validate password. Called by SASL mechanisms. If given password is
 * valid then {@linkplain VerifyPasswordCallback#setVerified(boolean)
 * setVerified(true)} must be called.
 */
public class VerifyPasswordCallback implements Callback {

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
