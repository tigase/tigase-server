package tigase.auth;

import javax.security.auth.callback.CallbackHandler;

/**
 * Interface should be implemented by {@linkplain CallbackHandler} instance if
 * domain name rom current XMPP Session should be injected.
 */
public interface DomainAware extends Aware {

	/**
	 * Sets name of domain from XMPP Stream.
	 * 
	 * @param domain
	 *            domain name
	 */
	void setDomain(String domain);

}
