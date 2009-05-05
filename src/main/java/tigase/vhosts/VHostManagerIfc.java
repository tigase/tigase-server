/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
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
 * This is VHostManagerIfc interface which allows to access data for
 * virtual domains server by this installation. There can be normally only
 * one instance of this interface implementation loaded on the server at
 * any given time. The instance is responsible for managing all virtual
 * hosts and assigning correct component to each of the virtual hosts or
 * non-local domains.
 *
 * Created: 22 Nov 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface VHostManagerIfc {

	/**
	 * This method checks whether given domain is server by this server instance.
	 * That is if this domain is local to this server installation. It doesn't
	 * check however whether the domain is disabled or enabled. It only checks
	 * if tthe list of local domains contains this virtual host.
	 *
	 * @param domain is a String with domain name to check.
	 * @return a boolean value indicating whether given domain is local or not.
	 */
	boolean isLocalDomain(String domain);

	boolean isLocalDomainOrComponent(String domain);

	/**
	 * This method checks whether anonymous login is enabled for a given domain.
	 * That is it checks whether this domains is local and anonymousEnabled
	 * parameter for this domain is set to true.
	 * @param domain is a String with domain name to check.
	 * @return a boolean value indicating whether given domain is enabled for
	 * anonymous logins or not.
	 */
	boolean isAnonymousEnabled(String domain);

	/**
	 * The method returns an array with server components which can process
	 * packets for a given local domain. If the domain is not local
	 * <code>null</code> is returned. The given domain may also consist of:
	 * component name <code>ServerComponent.getName()</code> plus any local
	 * domain but only if the component returns <code>true</code> from method
	 * call: <code>VHostListener.handlesNameSubdomains()</code>
	 *
	 * @param domain is a <code>String</code> with a domain name to check. It may
	 * by just a local domain or string created with component name and localdomain.
	 * @return an array with ServerComponents which can handle packets for a
	 * given domain or <code>null</code> if no component found for a given domain.
	 */
	ServerComponent[] getComponentsForLocalDomain(String domain);

	/**
	 * The method returns an array of server components which can process packets
	 * sent to non-local domain. Most commonly there is only one such component:
	 * server-2-server connections manager. It is possible however there might
	 * be more such components. All of them will get the packet for processing.
	 * @param domain is a <code>String</code> with a domain to check. At the moment
	 * this parameter is ignored. In the future it will be possible to assign a
	 * specific component for any non-local domain.
	 * @return an array with ServerComponents which can handle packets to
	 * non-local domains.
	 */
	ServerComponent[] getComponentsForNonLocalDomain(String domain);

	VHostItem getVHostItem(String domain);

}
