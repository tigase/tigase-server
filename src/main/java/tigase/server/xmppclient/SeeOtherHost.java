/**
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
package tigase.server.xmppclient;

import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.eventbus.events.ShutdownEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ClusterModeRequired;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostManagerIfc;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

/**
 * Default and basic implementation of SeeOtherHost returning same host as the initial one
 *
 * @author Wojtek
 */
@Bean(name = "seeOtherHost", parent = ClientConnectionManager.class, active = true)
@ClusterModeRequired(active = false)
public class SeeOtherHost
		implements SeeOtherHostIfc, Initializable {

	public static final String REDIRECTION_ENABLED = "see-other-host-redirect-enabled";
	private static final Logger log = Logger.getLogger(SeeOtherHost.class.getName());
	@ConfigField(desc = "Default host to redirect to")
	protected List<BareJID> defaultHost = null;
	@Inject
	protected EventBus eventBus;
	@Inject
	protected VHostManagerIfc vHostManager = null;
	@ConfigField(desc = "Active phases", alias = "phases")
	private ArrayList<Phase> active = new ArrayList<Phase>(Arrays.asList(Phase.OPEN));
	private Set<String> shutdownNodes = new CopyOnWriteArraySet<String>();

	@Override
	public BareJID findHostForJID(BareJID jid, BareJID host) {
		if (defaultHost != null && !defaultHost.isEmpty()) {
			return defaultHost.get(0);
		} else {
			return host;
		}
	}

	@Override
	public void initialize() {
		List<VHostItem.DataType> types = new ArrayList<VHostItem.DataType>();
		types.add(new VHostItem.DataType(REDIRECTION_ENABLED, "see-other-host redirection enabled", Boolean.class,
										 Boolean.TRUE));
		VHostItem.registerData(types);
	}

	public void setDefaultHost(List<BareJID> defaultHost) {
		if (defaultHost != null) {
			Collections.sort(defaultHost);
		}
		this.defaultHost = defaultHost;
	}

	@Override
	public void setNodes(List<JID> nodes) {
		// log.log(Level.CONFIG, "Action invalid for current implementation.");
		synchronized (this) {
			List<String> toRemove = new ArrayList<String>();
			Iterator<String> it = shutdownNodes.iterator();
			while (it.hasNext()) {
				String shutdownNode = it.next();
				boolean found = false;
				for (JID node : nodes) {
					found |= shutdownNode.equals(node.getDomain());
				}
				// remove node from nodes during shutdown if nodes was disconnected 
				// as it probably was shutdown and should reconnect after restart
				if (!found) {
					toRemove.add(shutdownNode);
				}
			}
			shutdownNodes.removeAll(toRemove);
		}
	}

	@Override
	public boolean isEnabled(VHostItem vHost, Phase ph) {
		return (boolean) vHost.getData(REDIRECTION_ENABLED) && active.contains(ph);
	}

	@Override
	public void start() {
		eventBus.registerAll(this);
	}

	@Override
	public void stop() {
		eventBus.unregisterAll(this);
	}

	protected boolean isNodeShutdown(BareJID jid) {
		return jid != null && shutdownNodes.contains(jid.getDomain());
	}

	@HandleEvent
	protected void nodeShutdown(ShutdownEvent event) {
		synchronized (this) {
			shutdownNodes.add(event.getNode());
		}
	}

}
