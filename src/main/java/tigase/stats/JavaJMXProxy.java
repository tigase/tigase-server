/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

package tigase.stats;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * Created: Aug 24, 2009 12:35:28 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JavaJMXProxy implements StatisticsProviderMBean, NotificationListener {

	private static final Logger log =
    Logger.getLogger(JavaJMXProxy.class.getName());

	private List<JMXProxyListener> listeners = new LinkedList<JMXProxyListener>();
	private StatisticsUpdater updater = null;
	private long lastCacheUpdate = 0;

	private String id = null;
	private String hostname = null;
	private int port = -1;
	private String userName = null;
	private String password = null;
	private long interval = -1;
	private long delay = -1;

	private String urlPath = null;
	private JMXServiceURL jmxUrl = null;
	private JMXConnector jmxc = null;
	private MBeanServerConnection server = null;
	private StatisticsProviderMBean tigBean = null;

	// Cache section...
	private int cpuNo = 0;
	private long uptime = 0;
	private long processCPUTime = 0;
	private int connectionsNumber = 0;
	private int clusterCacheSize = 0;
  private int queueSize = 0;
	private int smQueueSize = 0;
	private int clQueueSize = 0;
	private int clIOQueueSize = 0;
	private long queueOverflow = 0;
	private long smPacketsNumber = 0;
	private long clusterPacketsNumber = 0;
	private long messagesNumber = 0;
	private long presencesNumber = 0;
	private float smPacketsPerSec = 0;
	private float clPacketsPerSec = 0;
	private float messagesPerSec = 0;
	private float presencesPerSec = 0;
	private long iqOtherNumber = 0;
	private float iqOtherPerSec = 0;
	private long iqAuthNumber = 0;
	private float cpuUsage = 0;
	private float heapUsage = 0;
	private String sysDetails = "No details loaded yet";
	private float clCompressionRatio = 0;
	private long clNetworkBytes = 0;
	private float clNetworkBytesPerSec = 0;

	public JavaJMXProxy(String id, String hostname, int port,
			String userName, String password,	long delay, long interval) {
		this.id = id;
		this.hostname = hostname;
		this.port = port;
		this.userName = userName;
		this.password = password;
		this.delay = delay;
		this.interval = interval;
		this.urlPath = "/jndi/rmi://" + this.hostname + ":" + this.port + "/jmxrmi";
		System.out.println("Created: " + hostname);
	}

	public void addJMXProxyListener(JMXProxyListener listener) {
		listeners.add(listener);
	}

	public void start() {
		this.updater = new StatisticsUpdater();
		System.out.println("Started: " + hostname);
	}

	public boolean isConnected() {
		return tigBean != null;
	}

	public void connect() throws Exception {
		this.jmxUrl = new JMXServiceURL("rmi", "", 0, this.urlPath);
		String[] userCred = new String[] {userName, password};
		HashMap<String, Object> env = new HashMap<String, Object>();
		env.put(JMXConnector.CREDENTIALS, userCred);
		jmxc = JMXConnectorFactory.newJMXConnector(jmxUrl, env);
		jmxc.addConnectionNotificationListener(this, null, null);
		jmxc.connect();
	}

	public void update() {
		if (tigBean != null) {
			// This doesn't ever change so it is enough to query it once
			if (cpuNo == 0) {
				cpuNo = tigBean.getCPUsNumber();
			}
			uptime = tigBean.getUptime();
			processCPUTime = tigBean.getProcesCPUTime();
			connectionsNumber = tigBean.getConnectionsNumber();
			clusterCacheSize = tigBean.getClusterCacheSize();
			queueSize = tigBean.getQueueSize();
			smQueueSize = tigBean.getSMQueueSize();
			clQueueSize = tigBean.getCLQueueSize();
			clIOQueueSize = tigBean.getCLIOQueueSize();
			queueOverflow = tigBean.getQueueOverflow();
			smPacketsNumber = tigBean.getSMPacketsNumber();
			clusterPacketsNumber = tigBean.getClusterPackets();
			messagesNumber = tigBean.getMessagesNumber();
			presencesNumber = tigBean.getPresencesNumber();
			smPacketsPerSec = tigBean.getSMPacketsNumberPerSec();
			clPacketsPerSec = tigBean.getClusterPacketsPerSec();
			messagesPerSec = tigBean.getMessagesNumberPerSec();
			presencesPerSec = tigBean.getPresencesNumberPerSec();
			//iqOtherNumber = tigBean.getIQOtherNumber();
			//iqOtherPerSec = tigBean.getIQOtherNumberPerSec();
			//iqAuthNumber = tigBean.getIQAuthNumber();
			cpuUsage = tigBean.getCPUUsage();
			heapUsage = tigBean.getHeapMemUsage();
			sysDetails = tigBean.getSystemDetails();
			clCompressionRatio = tigBean.getClusterCompressionRatio();
			clNetworkBytes = tigBean.getClusterNetworkBytes();
			clNetworkBytesPerSec = tigBean.getClusterNetworkBytesPerSecond();
			lastCacheUpdate = System.currentTimeMillis();
		}
	}

	public long getLastCacheUpdate() {
		return lastCacheUpdate;
	}

	@Override
	public List getComponentsNames() {
		if (tigBean != null) {
			return tigBean.getComponentsNames();
		}
		return null;
	}

	@Override
	public String getName() {
		if (tigBean != null) {
			return tigBean.getName();
		}
		return null;
	}

	@Override
	public Map<String, String> getAllStats(int level) {
		if (tigBean != null) {
			return tigBean.getAllStats(level);
		}
		return null;
	}

	@Override
	public Map<String, String> getComponentStats(String compName, int level) {
		if (tigBean != null) {
		return tigBean.getComponentStats(compName, level);
		}
		return null;
	}

	@Override
	public int getCPUsNumber() {
		return cpuNo;
	}

	@Override
	public long getUptime() {
		return uptime;
	}

	@Override
	public long getProcesCPUTime() {
		return processCPUTime;
	}

	@Override
	public int getConnectionsNumber() {
		return connectionsNumber;
	}

	@Override
	public int getClusterCacheSize() {
		return clusterCacheSize;
	}

	@Override
	public int getQueueSize() {
		return queueSize;
	}

	@Override
	public int getSMQueueSize() {
		return smQueueSize;
	}

	@Override
	public int getCLQueueSize() {
		return clQueueSize;
	}

	@Override
	public int getCLIOQueueSize() {
		return clIOQueueSize;
	}

	@Override
	public long getQueueOverflow() {
		return queueOverflow;
	}

	@Override
	public long getSMPacketsNumber() {
		return smPacketsNumber;
	}

	@Override
	public long getClusterPackets() {
		return clusterPacketsNumber;
	}

	@Override
	public long getMessagesNumber() {
		return messagesNumber;
	}

	@Override
	public long getPresencesNumber() {
		return presencesNumber;
	}

	@Override
	public float getSMPacketsNumberPerSec() {
		return smPacketsPerSec;
	}

	@Override
	public float getClusterPacketsPerSec() {
		return clPacketsPerSec;
	}

	@Override
	public float getMessagesNumberPerSec() {
		return messagesPerSec;
	}

	@Override
	public float getPresencesNumberPerSec() {
		return presencesPerSec;
	}

	@Override
	public long getIQOtherNumber() {
		return iqOtherNumber;
	}

	@Override
	public float getIQOtherNumberPerSec() {
		return iqOtherPerSec;
	}

	@Override
	public long getIQAuthNumber() {
		return iqAuthNumber;
	}

	@Override
	public float getCPUUsage() {
		return cpuUsage;
	}

	@Override
	public float getHeapMemUsage() {
		return heapUsage;
	}

	@Override
	public String getSystemDetails() {
		return sysDetails;
	}

	@Override
	public float getClusterCompressionRatio() {
		return clCompressionRatio;
	}

	@Override
	public long getClusterNetworkBytes() {
		return clNetworkBytes;
	}

	@Override
	public float getClusterNetworkBytesPerSecond() {
		return clNetworkBytesPerSec;
	}

	@Override
	public void handleNotification(Notification notification, Object handback) {
		if (notification.getType().equals(JMXConnectionNotification.OPENED)) {
			System.out.println("Connected: " + hostname);
			try {
				server = jmxc.getMBeanServerConnection();
				ObjectName obn = new ObjectName(StatisticsCollector.STATISTICS_MBEAN_NAME);
				tigBean = (StatisticsProviderMBean)MBeanServerInvocationHandler.newProxyInstance(
						server, obn, StatisticsProviderMBean.class, false);
				for (JMXProxyListener jMXProxyListener : listeners) {
					jMXProxyListener.connected(id);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
    }
		if (notification.getType().equals(JMXConnectionNotification.CLOSED)) {
  		server = null;
  		tigBean = null;
			for (JMXProxyListener jMXProxyListener : listeners) {
	  	  jMXProxyListener.disconnected(id);
			}
			return;
    }
		if (notification.getType().equals(JMXConnectionNotification.FAILED)) {
      System.out.println("Reconnection to {hostName} failed...");
			return;
		}
    System.out.println("Unsupported JMX connection notification: {notification.getType()}");
	}

	private class StatisticsUpdater {

		private Timer updateTimer = null;

		private StatisticsUpdater() {
			updateTimer = new Timer("stats-updater", true);
			updateTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						if (server == null) {
							connect();
						}
						if (server != null) {
							update();
						}
					} catch (Exception e) {
						log.log(Level.WARNING, "Problem retrieving statistics: ", e);
					}
				}
			}, delay, interval);

		}
		
	}

}
