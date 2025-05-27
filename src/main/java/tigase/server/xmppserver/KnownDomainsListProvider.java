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
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.vhosts.VHostManagerIfc;
import tigase.xmpp.jid.JID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Bean(name = "KnownDomainsListProvider", parent = Kernel.class, active = true, exportable = true)
public class KnownDomainsListProvider {

	private static final Logger log = Logger.getLogger(KnownDomainsListProvider.class.getCanonicalName());

	@ConfigField(desc = "Static list of domains that are to be provided instead of dynamic ones")
	private HashSet<String> staticDomainsSet = new HashSet<>();

	@ConfigField(desc = "Use only static list of domains for both local and remote domains")
	private boolean useOnlyStaticDomainsList = false;

	@Inject
	private VHostManagerIfc vHostManagerIfc;

	protected Set<String> authenticatedRemoteDomains = new CopyOnWriteArraySet<>();


	public void addRemoteDomain(String remoteHost) {
		if (staticDomainsSet.isEmpty()) {
			authenticatedRemoteDomains.add(remoteHost);
		}
	}


	public Set<String> getAuthenticatedRemoteDomains() {
		if (!staticDomainsSet.isEmpty()) {
			return Collections.unmodifiableSet(staticDomainsSet);
		}
		return Collections.unmodifiableSet(authenticatedRemoteDomains);
	}

	public Set<String> getAllLocalDomains() {
		if (useOnlyStaticDomainsList) {
			return Collections.unmodifiableSet(staticDomainsSet);
		}
		return vHostManagerIfc.getAllVHosts().stream().map(JID::toString).collect(Collectors.toSet());
	}

}
