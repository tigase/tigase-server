package tigase.component.responses;

import tigase.server.Packet;
import tigase.xmpp.StanzaType;

/**
 * Main interface for callback of all <a
 * href='http://xmpp.org/rfcs/rfc6120.html#stanzas-semantics-iq'>IQ</a>
 * asynchronous request-response mechanism.
 * 
 * @author bmalkow
 * 
 */
public interface AsyncCallback {

	/**
	 * Called when received response has type {@linkplain StanzaType#error
	 * error}.
	 * 
	 * @param responseStanza
	 *            received IQ stanza
	 * @param errorCondition 
	 *            error condition
	 */
	void onError(Packet responseStanza, String errorCondition);

	/**
	 * Called when received response has type {@linkplain StanzaType#result
	 * result}.
	 * 
	 * @param responseStanza
	 *            received stanza
	 */
	void onSuccess(Packet responseStanza);

	/**
	 * Called when response wasn't received in given time.
	 */
	void onTimeout();

}