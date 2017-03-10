package tigase.auth.callbacks;

import tigase.xmpp.XMPPResourceConnection;

import javax.security.auth.callback.Callback;

public class XMPPSessionCallback implements Callback, java.io.Serializable {

	private final String prompt;
	private XMPPResourceConnection session;

	public XMPPSessionCallback(String prompt) {
		this.prompt = prompt;
	}

	public String getPrompt() {
		return prompt;
	}

	public XMPPResourceConnection getSession() {
		return session;
	}

	public void setSession(XMPPResourceConnection session) {
		this.session = session;
	}
}
