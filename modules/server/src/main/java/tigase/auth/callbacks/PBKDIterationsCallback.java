package tigase.auth.callbacks;

import javax.security.auth.callback.Callback;

public class PBKDIterationsCallback implements Callback, java.io.Serializable {

	private static final long serialVersionUID = -4342673378785456908L;

	private int interations;

	private String prompt;

	public PBKDIterationsCallback(String prompt) {
		this.prompt = prompt;
	}

	/**
	 * @return the interations
	 */
	public int getInterations() {
		return interations;
	}

	/**
	 * @param interations
	 *            the interations to set
	 */
	public void setInterations(int interations) {
		this.interations = interations;
	}

}
