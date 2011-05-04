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

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

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

//~--- classes ----------------------------------------------------------------

/**
 * Created: Aug 24, 2009 12:35:28 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JavaJMXProxy implements StatisticsProviderMBean, NotificationListener {
	private static final Logger log = Logger.getLogger(JavaJMXProxy.class.getName());

	//~--- fields ---------------------------------------------------------------

	private float clCompressionRatio = 0;
	private int clIOQueueSize = 0;
	private long clNetworkBytes = 0;
	private float clNetworkBytesPerSec = 0;
	private float clPacketsPerSec = 0;
	private int clQueueSize = 0;
	private float[] clpacks_history = null;
	private int clusterCacheSize = 0;
	private long clusterPacketsNumber = 0;
	private int connectionsNumber = 0;
	private int[] conns_history = null;

	// Cache section...
	private int cpuNo = 0;
	private float cpuUsage = 0;
	private float[] cpu_history = null;
	private long delay = -1;
	private float heapUsage = 0;
	private float[] heap_history = null;
	private String hostname = null;
	private String id = null;
	private long interval = -1;
	private long iqAuthNumber = 0;
	private long iqOtherNumber = 0;
	private float iqOtherPerSec = 0;
	private JMXServiceURL jmxUrl = null;
	private JMXConnector jmxc = null;
	private long lastCacheUpdate = 0;
	private long messagesNumber = 0;
	private float messagesPerSec = 0;
	private float nonHeapUsage = 0;
	private String password = null;
	private int port = -1;
	private long presencesNumber = 0;
	private float presencesPerSec = 0;
	private long processCPUTime = 0;
	private long queueOverflow = 0;
	private int queueSize = 0;
	private MBeanServerConnection server = null;
	private int serverConnections = 0;
	private int[] serverConnectionsHistory = null;
	private List<JMXProxyListener> listeners = new LinkedList<JMXProxyListener>();
	private long smPacketsNumber = 0;
	private float smPacketsPerSec = 0;
	private int smQueueSize = 0;
	private float[] smpacks_history = null;
	private String sysDetails = "No details loaded yet";
	private StatisticsProviderMBean tigBean = null;
	private StatisticsUpdater updater = null;
	private long uptime = 0;
	private String urlPath = null;
	private String userName = null;
	private boolean loadHistory = false;
	private boolean initialized = false;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param id
	 * @param hostname
	 * @param port
	 * @param userName
	 * @param password
	 * @param delay
	 * @param interval
	 * @param loadHistory
	 */
	public JavaJMXProxy(String id, String hostname, int port, String userName, String password,
			long delay, long interval, boolean loadHistory) {
		this.id = id;
		this.hostname = hostname;
		this.port = port;
		this.userName = userName;
		this.password = password;
		this.delay = delay;
		this.interval = interval;
		this.urlPath = "/jndi/rmi://" + this.hostname + ":" + this.port + "/jmxrmi";
		this.loadHistory = loadHistory;
		System.out.println("Created: " + hostname);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param listener
	 */
	public void addJMXProxyListener(JMXProxyListener listener) {
		listeners.add(listener);
	}

	/**
	 * Method description
	 *
	 *
	 * @throws Exception
	 */
	public void connect() throws Exception {
		this.jmxUrl = new JMXServiceURL("rmi", "", 0, this.urlPath);

		String[] userCred = new String[] { userName, password };
		HashMap<String, Object> env = new HashMap<String, Object>();

		env.put(JMXConnector.CREDENTIALS, userCred);
		jmxc = JMXConnectorFactory.newJMXConnector(jmxUrl, env);
		jmxc.addConnectionNotificationListener(this, null, null);
		jmxc.connect();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param level
	 *
	 * @return
	 */
	@Override
	public Map<String, String> getAllStats(int level) {
		if (tigBean != null) {
			return tigBean.getAllStats(level);
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int getCLIOQueueSize() {
		return clIOQueueSize;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float[] getCLPacketsPerSecHistory() {
		return clpacks_history;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int getCLQueueSize() {
		return clQueueSize;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float getCPUUsage() {
		return cpuUsage;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float[] getCPUUsageHistory() {
		return cpu_history;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int getCPUsNumber() {
		return cpuNo;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int getClusterCacheSize() {
		return clusterCacheSize;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float getClusterCompressionRatio() {
		return clCompressionRatio;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public long getClusterNetworkBytes() {
		return clNetworkBytes;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float getClusterNetworkBytesPerSecond() {
		return clNetworkBytesPerSec;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public long getClusterPackets() {
		return clusterPacketsNumber;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float getClusterPacketsPerSec() {
		return clPacketsPerSec;
	}

	/**
	 * Method description
	 *
	 *
	 * @param compName
	 * @param level
	 *
	 * @return
	 */
	@Override
	public Map<String, String> getComponentStats(String compName, int level) {
		if (tigBean != null) {
			return tigBean.getComponentStats(compName, level);
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public List getComponentsNames() {
		if (tigBean != null) {
			return tigBean.getComponentsNames();
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int getConnectionsNumber() {
		return connectionsNumber;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int[] getConnectionsNumberHistory() {
		return conns_history;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float getHeapMemUsage() {
		return heapUsage;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float[] getHeapUsageHistory() {
		return heap_history;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public long getIQAuthNumber() {
		return iqAuthNumber;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public long getIQOtherNumber() {
		return iqOtherNumber;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float getIQOtherNumberPerSec() {
		return iqOtherPerSec;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public long getLastCacheUpdate() {
		return lastCacheUpdate;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public long getMessagesNumber() {
		return messagesNumber;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float getMessagesNumberPerSec() {
		return messagesPerSec;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getName() {
		if (tigBean != null) {
			return tigBean.getName();
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float getNonHeapMemUsage() {
		return nonHeapUsage;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public long getPresencesNumber() {
		return presencesNumber;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float getPresencesNumberPerSec() {
		return presencesPerSec;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public long getProcesCPUTime() {
		return processCPUTime;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public long getQueueOverflow() {
		return queueOverflow;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int getQueueSize() {
		return queueSize;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public long getSMPacketsNumber() {
		return smPacketsNumber;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float getSMPacketsNumberPerSec() {
		return smPacketsPerSec;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public float[] getSMPacketsPerSecHistory() {
		return smpacks_history;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int getSMQueueSize() {
		return smQueueSize;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int getServerConnections() {
		return serverConnections;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int[] getServerConnectionsHistory() {
		return serverConnectionsHistory;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getSystemDetails() {
		return sysDetails;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public long getUptime() {
		return uptime;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param notification
	 * @param handback
	 */
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
					System.out.println(hostname + " loaded cpu_history, size: " + cpu_history.length);
					heap_history = tigBean.getHeapUsageHistory();
					System.out.println(hostname + " loaded heap_history, size: " + heap_history.length);
					smpacks_history = tigBean.getSMPacketsPerSecHistory();
					System.out.println(hostname + " loaded smpacks_history, size: " + smpacks_history.length);
					clpacks_history = tigBean.getCLPacketsPerSecHistory();
					System.out.println(hostname + " loaded clpacks_history, size: " + clpacks_history.length);
					conns_history = tigBean.getConnectionsNumberHistory();
					System.out.println(hostname + " loaded conns_history, size: " + conns_history.length);
					serverConnectionsHistory = tigBean.getServerConnectionsHistory();
					System.out.println(hostname + " loaded server_conns_history, size: "
							+ serverConnectionsHistory.length);
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

		System.out.println("Unsupported JMX notification: {notification.getType()}");
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public boolean isConnected() {
		return tigBean != null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public boolean isInitialized() {
		return isConnected() && initialized;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	public void start() {
		if (updater == null) {
			updater = new StatisticsUpdater();
			System.out.println("Started: " + hostname);
		}
	}

	/**
	 * Method description
	 *
	 */
	public void update() {
		if (tigBean != null) {

			// This doesn't ever change so it is enough to query it once
			if (cpuNo == 0) {
				cpuNo = tigBean.getCPUsNumber();
			}

			uptime = tigBean.getUptime();
			processCPUTime = tigBean.getProcesCPUTime();
			connectionsNumber = tigBean.getConnectionsNumber();
			serverConnections = tigBean.getServerConnections();
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

			// iqOtherNumber = tigBean.getIQOtherNumber();
			// iqOtherPerSec = tigBean.getIQOtherNumberPerSec();
			// iqAuthNumber = tigBean.getIQAuthNumber();
			cpuUsage = tigBean.getCPUUsage();
			heapUsage = tigBean.getHeapMemUsage();
			nonHeapUsage = tigBean.getNonHeapMemUsage();
			sysDetails = tigBean.getSystemDetails();
			clCompressionRatio = tigBean.getClusterCompressionRatio();
			clNetworkBytes = tigBean.getClusterNetworkBytes();
			clNetworkBytesPerSec = tigBean.getClusterNetworkBytesPerSecond();
			lastCacheUpdate = System.currentTimeMillis();
			initialized = true;
		}
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

						log.log(Level.WARNING, "{0}, retrying in {1} seconds.",
								new Object[] { cause.getMessage(),
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


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
