package tigase.auth;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import javax.security.sasl.SaslServerFactory;

import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.SaslAuth;

/**
 * Interface for implementing selectors of SASL mechanisms.
 * 
 */
public interface MechanismSelector {

	/**
	 * Method filters all available SASL mechanisms from
	 * {@linkplain SaslServerFactory factories} with current
	 * {@linkplain XMPPResourceConnection session} state.
	 * 
	 * @param serverFactories
	 *            {@linkplain SaslServerFactory SaslServerFactory} enumeration.
	 * @param session
	 *            current session
	 * @return collection of all SASL mechanisms available in given session (and
	 *         current XMPP Stream).
	 */
	Collection<String> filterMechanisms(Enumeration<SaslServerFactory> serverFactories, XMPPResourceConnection session);

	/**
	 * Initialize selector.
	 * 
	 * @param settings
	 *            settings of {@linkplain SaslAuth} plugin.
	 */
	void init(Map<String, Object> settings);

}
