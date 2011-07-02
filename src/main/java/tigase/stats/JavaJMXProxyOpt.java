/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2011 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 * $Rev: 2411 $
 * Last modified by $Author: kobit $
 * $Date: 2010-10-27 20:27:58 -0600 (Wed, 27 Oct 2010) $
 * 
 */
package tigase.stats;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import tigase.util.DataTypes;

/**
 * @author Artur Hefczyc Created Jun 3, 2011
 */
public class JavaJMXProxyOpt implements NotificationListener {

	private static final Logger log = Logger.getLogger(JavaJMXProxyOpt.class.getName());

	private String id = null;
	private String hostname = null;
	private int port = -1;
	private String userName = null;
	private String password = null;
	private String urlPath = null;

	private JMXServiceURL jmxUrl = null;
	private JMXConnector jmxc = null;

	private long delay = -1;
	private long interval = -1;

	private boolean loadHistory = false;
	private boolean initialized = false;

	private StatisticsProviderMBean tigBean = null;
	private MBeanServerConnection server = null;
	private StatisticsUpdater updater = null;

	private int cpuNo = 0;

	private List<JMXProxyListenerOpt> listeners = new LinkedList<JMXProxyListenerOpt>();
	private Set<String> metrics = new LinkedHashSet<String>();
	private Map<String, LinkedList<Object>> history = null;
	private String sysDetails = "No data yet...";

	public JavaJMXProxyOpt(String id, String hostname, int port, String userName,
			String password, long delay, long interval, boolean loadHistory) {
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

	public void addJMXProxyListener(JMXProxyListenerOpt listener) {
		listeners.add(listener);
		String[] dataIds = listener.getDataIds();
		if (dataIds != null && dataIds.length > 0) {
			for (String did : dataIds) {
				metrics.add(did);
			}
		}
	}

	public void connect() throws Exception {
		this.jmxUrl = new JMXServiceURL("rmi", "", 0, this.urlPath);

		String[] userCred = new String[] { userName, password };
		HashMap<String, Object> env = new HashMap<String, Object>();

		env.put(JMXConnector.CREDENTIALS, userCred);
		jmxc = JMXConnectorFactory.newJMXConnector(jmxUrl, env);
		jmxc.addConnectionNotificationListener(this, null, null);
		jmxc.connect();
	}

	public Map<String, String> getAllStats(int level) {
		if (tigBean != null) {
			return tigBean.getAllStats(level);
		}

		return null;
	}

	public Map<String, String> getComponentStats(String compName, int level) {
		if (tigBean != null) {
			return tigBean.getComponentStats(compName, level);
		}

		return null;
	}

	public List<String> getComponentsNames() {
		if (tigBean != null) {
			return tigBean.getComponentsNames();
		}

		return null;
	}

	public String getId() {
		return id;
	}

	public void start() {
		if (updater == null) {
			updater = new StatisticsUpdater();
			System.out.println("Started: " + hostname);
		}
	}

	@Override
	public void handleNotification(Notification notification, Object handback) {
		if (notification.getType().equals(JMXConnectionNotification.OPENED)) {
			System.out.println("Connected: " + hostname);

			try {
				server = jmxc.getMBeanServerConnection();

				ObjectName obn = new ObjectName(StatisticsCollector.STATISTICS_MBEAN_NAME);

				tigBean =
						MBeanServerInvocationHandler.newProxyInstance(server, obn,
								StatisticsProviderMBean.class, false);

				if (loadHistory) {
					String[] metrics_arr = metrics.toArray(new String[metrics.size()]);
					history = tigBean.getStatsHistory(metrics_arr);
					System.out.println(hostname
							+ " loaded history, size: "
							+ (history != null && history.get(metrics_arr[0]) != null ? history.get(
									metrics_arr[0]).size() : "null"));
				} else {
					System.out.println(hostname + " loading history switched off.");
				}
				if (history == null) {
					history = new LinkedHashMap<String, LinkedList<Object>>();
					for (String m : metrics) {
						LinkedList<Object> list = new LinkedList<Object>();
						history.put(m, list);
					}
				}

				for (JMXProxyListenerOpt jMXProxyListener : listeners) {
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

			for (JMXProxyListenerOpt jMXProxyListener : listeners) {
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

	public boolean isConnected() {
		return tigBean != null;
	}

	public boolean isInitialized() {
		return isConnected() && initialized;
	}

	public void update() {
		if (tigBean != null) {

			// This doesn't ever change so it is enough to query it once
			if (cpuNo == 0) {
				cpuNo = tigBean.getCPUsNumber();
			}

			Map<String, Object> curMetrics =
					tigBean.getCurStats(metrics.toArray(new String[metrics.size()]));
			for (Map.Entry<String, Object> e : curMetrics.entrySet()) {
				LinkedList<Object> list = history.get(e.getKey());
				if (list != null) {
					list.add(e.getValue());
					if (list.size() > 1) {
						list.removeFirst();
					}
				}
			}
			sysDetails = tigBean.getSystemDetails();
			initialized = true;

		}
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
					} catch (IOException e) {
						Throwable cause = e;

						while (cause.getCause() != null) {
							cause = cause.getCause();
						}

						log.log(Level.WARNING, "{0}, {1}, retrying in {2} seconds.", new Object[] {
								cause.getMessage(), hostname, interval / 1000 });
						//log.log(Level.FINEST, e.getMessage(), e);
					} catch (Exception e) {
						log.log(Level.WARNING, "Problem retrieving statistics: ", e);
					}
				}
			}, delay, interval);
		}
	}

	/**
	 * @param string
	 * @return
	 */
	public Object[] getMetricHistory(String key) {
		List<Object> result = history.get(key);
		if (result != null) {
			switch (DataTypes.decodeTypeIdFromName(key)) {
				case 'I':
					return result.toArray(new Integer[result.size()]);
				case 'L':
					return result.toArray(new Long[result.size()]);
				case 'F':
					return result.toArray(new Float[result.size()]);
				case 'D':
					return result.toArray(new Double[result.size()]);
				default:
					return result.toArray(new String[result.size()]);
			}
		}
		return null;
	}

	public String getHostname() {
		return hostname;
	}

	/**
	 * @param string
	 * @return
	 */
	public Object getMetricData(String key) {
		LinkedList<Object> h = history.get(key);
		if (h != null) {
			return h.getLast();
		}
		return null;
	}

	/**
	 * @return
	 */
	public String getSystemDetails() {
		return sysDetails;
	}

}
