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
package tigase.cluster;

import tigase.cluster.api.ClusteredComponentIfc;
import tigase.eventbus.EventListener;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.selector.ClusterModeRequired;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.server.ServiceChecker;
import tigase.server.bosh.BoshConnectionManager;
import tigase.server.xmppclient.SeeOtherHostIfc;
import tigase.util.common.TimerTask;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.jid.JID;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author andrzej
 */
@Bean(name = "bosh", parent = Kernel.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.ConnectionManagersMode})
@ClusterModeRequired(active = true)
public class BoshConnectionClustered
		extends BoshConnectionManager
		implements ClusteredComponentIfc {

	private static final Logger log = Logger.getLogger(BoshConnectionClustered.class.getName());

	private EventListener<ClusterConnectionManager.ClusterInitializedEvent> clusterEventHandler = null;

	public BoshConnectionClustered() {
		delayPortListening = true;
	}

	@Override
	public void onNodeDisconnected(JID jid) {
		super.onNodeDisconnected(jid);

		List<JID> connectedNodes = getNodesConnectedWithLocal();
		if (see_other_host_strategy != null) {
			see_other_host_strategy.setNodes(connectedNodes);
		}

		final String hostname = jid.getDomain();

		doForAllServices(new ServiceChecker<XMPPIOService<Object>>() {
			@Override
			public void check(XMPPIOService<Object> service) {
				JID dataReceiver = service.getDataReceiver();

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Checking service for dataReceiver: {0}", dataReceiver);
				}
				if ((dataReceiver != null) && dataReceiver.getDomain().equals(hostname)) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Stopping service because corresponding cluster node stopped.");
					}
					service.stop();
				}
			}
		});
	}

	@Override
	public String getDiscoDescription() {
		return super.getDiscoDescription() + " clustered";
	}

	@Override
	public SeeOtherHostIfc getSeeOtherHostInstance(String see_other_host_class) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Configuring see_other_host clustered strategy for: " + see_other_host_class);
		}
		if (see_other_host_class == null) {
			see_other_host_class = SeeOtherHostIfc.CM_SEE_OTHER_HOST_CLASS_PROP_DEF_VAL_CLUSTER;
		}
		see_other_host_strategy = super.getSeeOtherHostInstance(see_other_host_class);
		List<JID> connectedNodes = getNodesConnectedWithLocal();
		if (see_other_host_strategy != null) {
			see_other_host_strategy.setNodes(connectedNodes);
		}

		return see_other_host_strategy;
	}

	@Override
	public void start() {
		super.start();

		if (clusterEventHandler == null) {
			clusterEventHandler = (ClusterConnectionManager.ClusterInitializedEvent event) -> {
				BoshConnectionClustered.this.connectWaitingTasks();
				log.log(Level.INFO, "Starting listening on ports of component: {0}",
						BoshConnectionClustered.this.getName());
				eventBus.removeListener(clusterEventHandler);
			};
		}

		eventBus.addListener(ClusterConnectionManager.ClusterInitializedEvent.class, clusterEventHandler);

		if (delayPortListening) {
			addTimerTask(new TimerTask() {
				@Override
				public void run() {
					log.log(Level.FINE,
							"Cluster synchronization timed-out, starting pending connections for " + getName());
					BoshConnectionClustered.this.connectWaitingTasks();
				}
			}, connectionDelay * 30);
		}
	}

	@Override
	public void stop() {
		super.stop();
		eventBus.removeListener(clusterEventHandler);
		clusterEventHandler = null;
	}

	@Override
	protected void onNodeConnected(JID jid) {
		super.onNodeConnected(jid);

		List<JID> connectedNodes = getNodesConnectedWithLocal();
		if (see_other_host_strategy != null) {
			see_other_host_strategy.setNodes(connectedNodes);
		}
	}

}
