package tigase.auth;

import javax.security.auth.callback.CallbackHandler;

import tigase.xmpp.XMPPResourceConnection;

/**
 * Interface should be implemented by {@linkplain CallbackHandler} instance if
 * current XMPP Session should be injected.
 */
public interface SessionAware {

	/**
	 * Sets XMPP Session.
	 * 
	 * @param session
	 *            XMPP session.
	 */
	void setSession(XMPPResourceConnection session);

}
