/*
 * JavaJMXProxy.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



package tigase.stats;

//~--- JDK imports ------------------------------------------------------------

import javax.management.*;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Aug 24, 2009 12:35:28 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 *
 * @deprecated {@link tigase.stats.JavaJMXProxyOpt} - optimised JMXProxy class should be used. This
 * class will be removed in the next version after 7.1.0
 */
@Deprecated
public class JavaJMXProxy
				implements StatisticsProviderMBean, NotificationListener {
	private static final Logger log = Logger.getLogger(JavaJMXProxy.class.getName());

	//~--- fields ---------------------------------------------------------------

	private float   clCompressionRatio   = 0;
	private int     clIOQueueSize        = 0;
	private long    clNetworkBytes       = 0;
	private float   clNetworkBytesPerSec = 0;
	private float   clPacketsPerSec      = 0;
	private float[] clpacks_history      = null;
	private int     clQueueSize          = 0;
	private int     clusterCacheSize     = 0;
	private long    clusterPacketsNumber = 0;
	private int     connectionsNumber    = 0;
	private int[]   conns_history        = null;
	private float[] cpu_history          = null;

	// Cache section...
	private int                     cpuNo                    = 0;
	private float                   cpuUsage                 = 0;
	private long                    delay                    = -1;
	private long[]                  direct_history           = null;
	private long                    directUsed               = 0;
	private float[]                 heap_history             = null;
	private float                   heapUsage                = 0;
	private String                  hostname                 = null;
	private String                  id                       = null;
	private long                    interval                 = -1;
	private long                    iqAuthNumber             = 0;
	private long                    iqOtherNumber            = 0;
	private float                   iqOtherPerSec            = 0;
	private JMXConnector            jmxc                     = null;
	private JMXServiceURL           jmxUrl                   = null;
	private long                    lastCacheUpdate          = 0;
	private long                    messagesNumber           = 0;
	private float                   messagesPerSec           = 0;
	private float                   nonHeapUsage             = 0;
	private String                  password                 = null;
	private int                     port                     = -1;
	private long                    presencesNumber          = 0;
	private float                   presencesPerSec          = 0;
	private long                    processCPUTime           = 0;
	private long                    queueOverflow            = 0;
	private int                     queueSize                = 0;
	private MBeanServerConnection   server                   = null;
	private int                     serverConnections        = 0;
	private int[]                   serverConnectionsHistory = null;
	private List<JMXProxyListener>  listeners = new LinkedList<JMXProxyListener>();
	private long                    smPacketsNumber          = 0;
	private float                   smPacketsPerSec          = 0;
	private float[]                 smpacks_history          = null;
	private int                     smQueueSize              = 0;
	private String                  sysDetails               = "No details loaded yet";
	private StatisticsProviderMBean tigBean                  = null;
	private StatisticsUpdater       updater                  = null;
	private long                    uptime                   = 0;
	private String                  urlPath                  = null;
	private String                  userName                 = null;
	private boolean                 loadHistory              = false;
	private boolean                 initialized              = false;

	//~--- constructors ---------------------------------------------------------

	public JavaJMXProxy(String id, String hostname, int port, String userName,
			String password, long delay, long interval, boolean loadHistory) {
		this.id          = id;
		this.hostname    = hostname;
		this.port        = port;
		this.userName    = userName;
		this.password    = password;
		this.delay       = delay;
		this.interval    = interval;
		this.urlPath     = "/jndi/rmi://" + this.hostname + ":" + this.port + "/jmxrmi";
		this.loadHistory = loadHistory;
		System.out.println("Created: " + hostname);
	}

	//~--- methods --------------------------------------------------------------

	public void addJMXProxyListener(JMXProxyListener listener) {
		listeners.add(listener);
	}

	public void connect() throws Exception {
		this.jmxUrl = new JMXServiceURL("rmi", "", 0, this.urlPath);

		String[]                userCred = new String[] { userName, password };
		HashMap<String, Object> env      = new HashMap<String, Object>();

		env.put(JMXConnector.CREDENTIALS, userCred);
		jmxc = JMXConnectorFactory.newJMXConnector(jmxUrl, env);
		jmxc.addConnectionNotificationListener(this, null, null);
		jmxc.connect();
	}

	@Override
	public void handleNotification(Notification notification, Object handback) {
		if (notification.getType().equals(JMXConnectionNotification.OPENED)) {
			System.out.println("Connected: " + hostname);
			try {
				server = jmxc.getMBeanServerConnection();

				ObjectName obn = new ObjectName(StatisticsCollector.STATISTICS_MBEAN_NAME);

				tigBean = MBeanServerInvocationHandler.newProxyInstance(server, obn,
						StatisticsProviderMBean.class, false);
				if (loadHistory) {
					cpu_history = tigBean.getCPUUsageHistory();
					System.out.println(hostname + " loaded cpu_history, size: " + cpu_history
							.length);
					heap_history = tigBean.getHeapUsageHistory();
					System.out.println(hostname + " loaded heap_history, size: " + heap_history
							.length);
					smpacks_history = tigBean.getSMPacketsPerSecHistory();
					System.out.println(hostname + " loaded smpacks_history, size: " +
							smpacks_history.length);
					clpacks_history = tigBean.getCLPacketsPerSecHistory();
					System.out.println(hostname + " loaded clpacks_history, size: " +
							clpacks_history.length);
					conns_history = tigBean.getConnectionsNumberHistory();
					System.out.println(hostname + " loaded conns_history, size: " + conns_history
							.length);
					serverConnectionsHistory = tigBean.getServerConnectionsHistory();
					System.out.println(hostname + " loaded server_conns_history, size: " +
							serverConnectionsHistory.length);
				} else {
					System.out.println(hostname + " loading history switched off.");
				}
				for (JMXProxyListener jMXProxyListener : listeners) {
					jMXProxyListener.connected(id, this);
				}
				start();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}
		if (notification.getType().equals(JMXConnectionNotification.CLOSED)) {
			server  = null;
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
		System.out.println("Unsupported JMX notification: {notification.getType()}");
	}

	public void start() {
		if (updater == null) {
			updater = new StatisticsUpdater();
			System.out.println("Started: " + hostname);
		}
	}

	public void update() {
		if (tigBean != null) {

			// This doesn't ever change so it is enough to query it once
			if (cpuNo == 0) {
				cpuNo = tigBean.getCPUsNumber();
			}
			uptime               = tigBean.getUptime();
			processCPUTime       = tigBean.getProcesCPUTime();
			connectionsNumber    = tigBean.getConnectionsNumber();
			serverConnections    = tigBean.getServerConnections();
			clusterCacheSize     = tigBean.getClusterCacheSize();
			queueSize            = tigBean.getQueueSize();
			smQueueSize          = tigBean.getSMQueueSize();
			clQueueSize          = tigBean.getCLQueueSize();
			clIOQueueSize        = tigBean.getCLIOQueueSize();
			queueOverflow        = tigBean.getQueueOverflow();
			smPacketsNumber      = tigBean.getSMPacketsNumber();
			clusterPacketsNumber = tigBean.getClusterPackets();
			messagesNumber       = tigBean.getMessagesNumber();
			presencesNumber      = tigBean.getPresencesNumber();
			smPacketsPerSec      = tigBean.getSMPacketsNumberPerSec();
			clPacketsPerSec      = tigBean.getClusterPacketsPerSec();
			messagesPerSec       = tigBean.getMessagesNumberPerSec();
			presencesPerSec      = tigBean.getPresencesNumberPerSec();

			// iqOtherNumber = tigBean.getIQOtherNumber();
			// iqOtherPerSec = tigBean.getIQOtherNumberPerSec();
			// iqAuthNumber = tigBean.getIQAuthNumber();
			cpuUsage             = tigBean.getCPUUsage();
			heapUsage            = tigBean.getHeapMemUsage();
			nonHeapUsage         = tigBean.getNonHeapMemUsage();
			direct_history       = tigBean.getDirectMemUsedHistory();
			directUsed           = tigBean.getDirectMemUsed();
			sysDetails           = tigBean.getSystemDetails();
			clCompressionRatio   = tigBean.getClusterCompressionRatio();
			clNetworkBytes       = tigBean.getClusterNetworkBytes();
			clNetworkBytesPerSec = tigBean.getClusterNetworkBytesPerSecond();
			lastCacheUpdate      = System.currentTimeMillis();
			initialized          = true;
		}
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public Map<String, String> getAllStats(int level) {
		if (tigBean != null) {
			return tigBean.getAllStats(level);
		}

		return null;
	}

	@Override
	public int getCLIOQueueSize() {
		return clIOQueueSize;
	}

	@Override
	public float[] getCLPacketsPerSecHistory() {
		return clpacks_history;
	}

	@Override
	public int getCLQueueSize() {
		return clQueueSize;
	}

	@Override
	public int getClusterCacheSize() {
		return clusterCacheSize;
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
	public long getClusterPackets() {
		return clusterPacketsNumber;
	}

	@Override
	public float getClusterPacketsPerSec() {
		return clPacketsPerSec;
	}

	@Override
	public List<String> getComponentsNames() {
		if (tigBean != null) {
			return tigBean.getComponentsNames();
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
	public int getConnectionsNumber() {
		return connectionsNumber;
	}

	@Override
	public int[] getConnectionsNumberHistory() {
		return conns_history;
	}

	@Override
	public int getCPUsNumber() {
		return cpuNo;
	}

	@Override
	public float getCPUUsage() {
		return cpuUsage;
	}

	@Override
	public float[] getCPUUsageHistory() {
		return cpu_history;
	}

	@Override
	public Map<String, Object> getCurStats(String[] statsKeys) {
		return null;
	}

	@Override
	public long getDirectMemUsed() {
		return directUsed;
	}

	@Override
	public long[] getDirectMemUsedHistory() {
		return direct_history;
	}

	@Override
	public float getHeapMemUsage() {
		return heapUsage;
	}

	@Override
	public float[] getHeapUsageHistory() {
		return heap_history;
	}

	public String getId() {
		return id;
	}

	@Override
	public long getIQAuthNumber() {
		return iqAuthNumber;
	}

	@Override
	public long getIQOtherNumber() {
		return iqOtherNumber;
	}

	@Override
	public float getIQOtherNumberPerSec() {
		return iqOtherPerSec;
	}

	public long getLastCacheUpdate() {
		return lastCacheUpdate;
	}

	@Override
	public long getMessagesNumber() {
		return messagesNumber;
	}

	@Override
	public float getMessagesNumberPerSec() {
		return messagesPerSec;
	}

	@Override
	public String getName() {
		if (tigBean != null) {
			return tigBean.getName();
		}

		return null;
	}

	@Override
	public float getNonHeapMemUsage() {
		return nonHeapUsage;
	}

	@Override
	public long getPresencesNumber() {
		return presencesNumber;
	}

	@Override
	public float getPresencesNumberPerSec() {
		return presencesPerSec;
	}

	@Override
	public long getProcesCPUTime() {
		return processCPUTime;
	}

	@Override
	public long getQueueOverflow() {
		return queueOverflow;
	}

	@Override
	public int getQueueSize() {
		return queueSize;
	}

	@Override
	public int getServerConnections() {
		return serverConnections;
	}

	@Override
	public int[] getServerConnectionsHistory() {
		return serverConnectionsHistory;
	}

	@Override
	public long getSMPacketsNumber() {
		return smPacketsNumber;
	}

	@Override
	public float getSMPacketsNumberPerSec() {
		return smPacketsPerSec;
	}

	@Override
	public float[] getSMPacketsPerSecHistory() {
		return smpacks_history;
	}

	@Override
	public int getSMQueueSize() {
		return smQueueSize;
	}

	@Override
	public Map<String, LinkedList<Object>> getStatsHistory(String[] statsKeys) {
		return null;
	}

	@Override
	public String getSystemDetails() {
		return sysDetails;
	}

	@Override
	public long getUptime() {
		return uptime;
	}

	public boolean isConnected() {
		return tigBean != null;
	}

	public boolean isInitialized() {
		return isConnected() && initialized;
	}

	//~--- inner classes --------------------------------------------------------

	private class StatisticsUpdater {
		private Timer updateTimer = null;

		//~--- constructors -------------------------------------------------------

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
					} catch (IOException e) {
						Throwable cause = e;

						while (cause.getCause() != null) {
							cause = cause.getCause();
						}
						log.log(Level.WARNING, "{0}, retrying in {1} seconds.", new Object[] { cause
								.getMessage(),
								interval / 1000 });
						log.log(Level.FINEST, e.getMessage(), e);
					} catch (Exception e) {
						log.log(Level.WARNING, "Problem retrieving statistics: ", e);
					}
				}
			}, delay, interval);
		}
	}
}
