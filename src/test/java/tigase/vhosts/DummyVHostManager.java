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

package tigase.vhosts;

import tigase.server.ServerComponent;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Dummy {@code VHostManagerIfc} implementation
 */
public class DummyVHostManager
		implements VHostManagerIfc {

	private final static Logger LOG = Logger.getLogger(DummyVHostManager.class.getName());
	private final Map<String, VHostItem> items = new ConcurrentHashMap<>();

	public DummyVHostManager() {
	}

	@Override
	public void addComponentDomain(String domain) {

	}

	public void addVhost(String vhost) {

		try {
			VHostItem item = new VHostItemImpl(vhost);
			items.put(vhost, item);
		} catch (TigaseStringprepException e) {
			LOG.log(Level.WARNING, "Adding VHost failed", e);
		}
	}

	@Override
	public List<JID> getAllVHosts() {
		return items.values().stream().map(VHostItem::getVhost).collect(Collectors.toList());
	}

	@Override
	public ServerComponent[] getComponentsForLocalDomain(String domain) {
		return new ServerComponent[0];
	}

	@Override
	public ServerComponent[] getComponentsForNonLocalDomain(String domain) {
		return new ServerComponent[0];
	}

	@Override
	public BareJID getDefVHostItem() {
		return items.values()
				.stream()
				.map(VHostItem::getVhost)
				.map(JID::toString)
				.map(BareJID::bareJIDInstanceNS)
				.findFirst()
				.orElse(BareJID.bareJIDInstanceNS("not@available"));
	}

	@Override
	public VHostItem getVHostItem(String domain) {
		return items.get(domain);
	}

	@Override
	public VHostItem getVHostItemDomainOrComponent(String domain) {
		return items.get(domain);
	}

	@Override
	public boolean isAnonymousEnabled(String domain) {
		return false;
	}

	@Override
	public boolean isLocalDomain(String domain) {
		return items.containsKey(domain);
	}

	@Override
	public boolean isLocalDomainOrComponent(String domain) {
		return items.containsKey(domain);
	}

	@Override
	public void removeComponentDomain(String domain) {

	}
}
