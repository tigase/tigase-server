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
package tigase.server.rtbl;

import tigase.cluster.ClusterConnectionManager;
import tigase.component.AbstractKernelBasedComponent;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.component.modules.impl.DiscoveryModule;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;

import java.util.List;

@Bean(name = "rtbl-component", parent = Kernel.class, active = true, exportable = true)
@ConfigType({ConfigTypeEnum.DefaultMode})
public class RTBLComponent extends AbstractKernelBasedComponent {

	@Inject
	private RTBLRepository repository;
	@Inject(nullAllowed = true)
	private RTBLSubscribeModule subscribeModule;
	@Inject(nullAllowed = true)
	private RTBLFetchModule fetchModule;

	@Override
	public boolean isDiscoNonAdmin() {
		return false;
	}

	@Override
	protected void registerModules(Kernel kernel) {
		kernel.registerBean(AdHocCommandModule.class).exec();
		kernel.registerBean(DiscoveryModule.class).exec();
	}

	@Override
	public void start() {
		super.start();
		eventBus.registerAll(this);
	}

	@Override
	public void stop() {
		super.stop();
		eventBus.unregisterAll(this);
	}

	@Override
	public boolean isSubdomain() {
		return true;
	}

	@HandleEvent
	public void serverInitialized(ClusterConnectionManager.ClusterInitializedEvent event) {
		if (subscribeModule != null && fetchModule != null) {
			List<RTBL> blocklists = repository.getBlockLists();

			for (RTBL rtbl : blocklists) {
				subscribeModule.subscribe(rtbl.getJID(), rtbl.getNode());
				fetchModule.fetch(rtbl.getJID(), rtbl.getNode());
			}
		} else {
			log.warning("failed to initialize RTBLComponent - missing subscribeModule: " + subscribeModule + ", fetchModule: " +  fetchModule);
		}

	}
}
