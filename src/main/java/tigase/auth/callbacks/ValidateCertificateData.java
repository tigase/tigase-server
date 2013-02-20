package tigase.auth.callbacks;

import javax.security.auth.callback.Callback;

public class ValidateCertificateData implements Callback {

	private boolean authorized;
	private String authorizedID;

	public boolean isAuthorized() {
		return authorized;
	}

	public String getAuthorizedID() {
		return authorizedID;
	}

	public void setAuthorized(boolean authorized) {
		this.authorized = authorized;
	}

	public void setAuthorizedID(String authorizedID) {
		this.authorizedID = authorizedID;
	}

}
