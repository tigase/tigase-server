/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */

package tigase.server.xmppserver;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.vhosts.VHostManagerIfc;
import tigase.xmpp.jid.JID;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Bean(name = "KnownDomainsListProvider", parent = Kernel.class, active = true, exportable = true)
public class KnownDomainsListProvider {

	private static final Logger log = Logger.getLogger(KnownDomainsListProvider.class.getCanonicalName());

	@Inject
	private VHostManagerIfc vHostManagerIfc;

	protected Set<String> authenticatedRemoteDomains = new CopyOnWriteArraySet<>();


	private static final String[] emptyArray = new String[0];

	public void addRemoteDomain(String remoteHost) {
		authenticatedRemoteDomains.add(remoteHost);
	}


	public Set<String> getAuthenticatedRemoteDomains() {
		return Collections.unmodifiableSet(authenticatedRemoteDomains);
	}

	public Set<String> getAllLocalDomains() {
		return vHostManagerIfc.getAllVHosts().stream().map(JID::toString).collect(Collectors.toSet());
	}

}
