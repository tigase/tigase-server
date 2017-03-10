/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.vhosts;

import tigase.server.ServerComponent;

/**
 * Interface VHostListener needs to be implemented by every class wanting
 * to accept and process XMPP packets sent to virtual domains. It allows
 * for basic component settings and retrieve information and configuration
 * about virtual domains server by the installation.
 *
 *
 * Created: Fri Nov 21 14:29:49 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface VHostListener
				extends ServerComponent {

	/**
	 * Sets the VHostManager which works on this server installation.
	 * The VHostManager allows for accessing vritual domain data.
	 * @param manager is a reference to VHostManager available on the server
	 * installation.
	 */
	void setVHostManager(VHostManagerIfc manager);

	/**
	 * Indicates whether the component accepts packets to all local domains.
	 * The best example would be SM component which usually handles all requests
	 * sent to any local domain.
	 * @return 'true' if the component accepts packets to local domains
	 * 'false' otherwise.
	 */
	boolean handlesLocalDomains();

	/**
	 * Indicates whether the component can handle all packets to
	 * non-local domains. S2s connection manager component is the best
	 * example of the component which handles all requests sent to non-local
	 * domains.
	 * @return 'true' if the component accepts packets to non-local domains
	 * 'false' otherwise.
	 */
	boolean handlesNonLocalDomains();

	/**
	 * Indicates whether the component can handle packets to 'name' subdomain.
	 * Name subdomain is an artificial domain created from the component name
	 * and the domain name. The best example would be a 'MUC' component or
	 * a 'PubSub' component. They are usually named respectively 'muc' and
	 * 'pubsub' and they accept requests sent to domains 'muc.tigase.org' or
	 * 'pubsub.tigase.org', even though a local domain is just 'tigase.org'.
	 * @return 'true' if the component accepts packets to 'name' subdomains
	 * 'false' otherwise.
	 */
	boolean handlesNameSubdomains();
}
