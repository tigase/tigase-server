/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.io;

//~--- non-JDK imports --------------------------------------------------------

import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.eventbus.HandleEvent;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Oct 15, 2010 2:40:49 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SSLContextContainer extends SSLContextContainerAbstract {

	private static final Logger log = Logger.getLogger(SSLContextContainer.class.getName());

	protected final EventBus eventBus = EventBusFactory.getInstance();
	private final SSLContextContainerIfc parent;

	protected Map<String, SSLHolder> sslContexts = new ConcurrentSkipListMap<>();

	/**
	 * Constructor used to create root SSLContextContainer instance which should cache only SSLContext instances where
	 * array of TrustManagers is not set - common for all ConnectionManagers. This instance is kept by TLSUtil class.
	 *
	 * @param certContainer
	 */
	public SSLContextContainer(CertificateContainerIfc certContainer) {
		this(certContainer, null);
	}

	/**
	 * Constructor used to create instances for every ConnectionManager so that every connection manager can have
	 * different TrustManagers and SSLContext instance will still be cached.
	 *
	 * @param certContainer
	 * @param parent
	 */
	public SSLContextContainer(CertificateContainerIfc certContainer, SSLContextContainerIfc parent) {
		super(certContainer);
		this.parent = parent;
	}

	@Override
	public SSLContext getSSLContext(String protocol, String hostname, boolean clientMode, TrustManager[] tms) {
		SSLHolder holder = null;

		String alias = hostname;

		try {
			if (tms == null) {
				if (parent != null) {
					return parent.getSSLContext(protocol, hostname, clientMode, tms);
				} else {
					tms = getTrustManagers();
				}
			}

			if (alias == null) {
				alias = getDefCertAlias();
			} // end of if (hostname == null)

			holder = find(sslContexts, alias);

			if (holder == null || !holder.isValid(tms)) {
				SSLContext sslContext = createContext(protocol,hostname, alias, clientMode, tms);
				if (clientMode)
					return sslContext;
				holder = new SSLHolder(tms, sslContext);
				sslContexts.put(alias, holder);
			}


		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not initialize SSLContext for domain: " + alias + ", protocol: " + protocol, e);
			holder = null;
		}
                
		return holder != null ? holder.getSSLContext() : null;
	}

	@Override
	public KeyStore getTrustStore() {
		return null;
	}

	@Override
	public void start() {
		eventBus.registerAll(this);
	}

	@Override
	public void stop() {
		eventBus.unregisterAll(this);
	}

	/**
	 * Method handles <code>CertificateChanged</code> event emitted by CertificateContainer
	 * and removes cached instance of SSLContext for domain for which certificate has
	 * changed.
	 *
	 * @param event
	 */
	@HandleEvent
	private void onCertificateChange(CertificateContainer.CertificateChanged event) {
		String alias = event.getAlias();
		sslContexts.remove(alias);
	}

	private class SSLHolder {
		private final TrustManager[] tms;
		private final SSLContext sslContext;

		public SSLHolder(TrustManager[] tms, SSLContext sslContext) {
			this.tms = tms;
			this.sslContext = sslContext;
		}

		public SSLContext getSSLContext() {
			return sslContext;
		}

		public boolean isValid(TrustManager[] tms) {
			return tms == this.tms;
		}
	}
}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
