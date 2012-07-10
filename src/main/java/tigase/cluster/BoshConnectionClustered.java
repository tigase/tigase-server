/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.cluster;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.cluster.api.ClusterControllerIfc;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.server.ServiceChecker;
import tigase.server.bosh.BoshConnectionManager;
import tigase.server.xmppclient.SeeOtherHostIfc;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPIOService;

/**
 *
 * @author andrzej
 */
public class BoshConnectionClustered extends BoshConnectionManager implements
		ClusteredComponentIfc {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(BoshConnectionClustered.class
			.getName());
        
	@SuppressWarnings("serial")
	private List<BareJID> connectedNodes = new CopyOnWriteArrayList<BareJID>() {
		{
			add(getDefHostName());
		}
	};
        
	/**
	 * Method description
	 * 
	 * 
	 * @param node
	 */
	@Override
	public void nodeConnected(String node) {
		BareJID nodeJID = BareJID.bareJIDInstanceNS(null, node);

		// connectedNodes must be synchronized here. If it is executed concurrently,
		// then most likely only one connected node will endup in the collection
		synchronized (connectedNodes) {
			if (!connectedNodes.contains(nodeJID)) {
				connectedNodes.add(nodeJID);

				// ugly workaround to sort CopyOnWriteArrayList
				BareJID[] arr_list = connectedNodes.toArray(new BareJID[connectedNodes.size()]);
				Arrays.sort(arr_list);
				connectedNodes = new CopyOnWriteArrayList<BareJID>(arr_list);

				see_other_host_strategy.setNodes(connectedNodes);
			}
		}
	}

	/**
	 * 
	 * @param node
	 */
	@Override
	public void nodeDisconnected(String node) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Disconnected nodes: {0}", node);
		}

		BareJID nodeJID = BareJID.bareJIDInstanceNS(null, node);

		// if (connectedNodes.contains(nodeJID)) {
		connectedNodes.remove(nodeJID);
		// }

		final String hostname = node;

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
	public SeeOtherHostIfc getSeeOtherHostInstance(String see_other_host_class) {
		if ( log.isLoggable( Level.FINEST ) ){
			log.finest( "Configuring see_other_host clustered strategy for: " + see_other_host_class );
		}

		if (see_other_host_class == null) {
			see_other_host_class = SeeOtherHostIfc.CM_SEE_OTHER_HOST_CLASS_PROP_DEF_VAL_CLUSTER;
		}

		see_other_host_strategy = super.getSeeOtherHostInstance(see_other_host_class);

		if (see_other_host_strategy != null) {
			see_other_host_strategy.setNodes(connectedNodes);
		}

		return see_other_host_strategy;
	}

	// ~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param cl_controller
	 */
        @Override
        public void setClusterController(ClusterControllerIfc cl_controller) {
        }
}
