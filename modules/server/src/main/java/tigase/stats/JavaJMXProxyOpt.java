/*
 * JavaJMXProxyOpt.java
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

//~--- non-JDK imports --------------------------------------------------------

import tigase.util.DataTypes;

import javax.management.*;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- JDK imports ------------------------------------------------------------

/**
 * @author Artur Hefczyc Created Jun 3, 2011
 */
public class JavaJMXProxyOpt
				implements NotificationListener {
	private static final Logger log = Logger.getLogger(JavaJMXProxyOpt.class.getName());

	//~--- fields ---------------------------------------------------------------

	private int                             cpuNo              = 0;
	private long                            delay              = -1;
	private Map<String, LinkedList<Object>> history            = null;
	private String                          hostname           = null;
	private String                          id                 = null;
	private long                            interval           = -1;
	private JMXConnector                    jmxc               = null;
	private JMXServiceURL                   jmxUrl             = null;
	private Date                            lastDisconnectTime = null;
	private String                          password           = null;
	private int                             port               = -1;
	private MBeanServerConnection           server             = null;
	private String                          sysDetails         = "No data yet...";
	private StatisticsProviderMBean         tigBean            = null;
	private StatisticsUpdater               updater            = null;
	private String                          urlPath            = null;
	private String                          userName           = null;
	private Set<String>                     metrics            =
			new LinkedHashSet<String>();
	private boolean                         loadHistory        = false;
	private List<JMXProxyListenerOpt>       listeners =
			new LinkedList<JMXProxyListenerOpt>();
	private boolean                         initialized        = false;

	//~--- constructors ---------------------------------------------------------

	public JavaJMXProxyOpt(String id, String hostname, int port, String userName,
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
		System.out.println("Created: " + id + ":" + hostname + ":" + port);
	}

	//~--- methods --------------------------------------------------------------

	public void addJMXProxyListener(JMXProxyListenerOpt listener) {
		listeners.add(listener);

		String[] dataIds = listener.getDataIds();

		if ((dataIds != null) && (dataIds.length > 0)) {
			for (String did : dataIds) {
				metrics.add(did);
			}
		}
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
			System.out.println("Connected: " + id + ":" + hostname + ":" + port);
			try {
				server = jmxc.getMBeanServerConnection();

				ObjectName obn = new ObjectName(StatisticsCollector.STATISTICS_MBEAN_NAME);

				tigBean = MBeanServerInvocationHandler.newProxyInstance(server, obn,
						StatisticsProviderMBean.class, false);
				if (history == null) {
					if (loadHistory) {
						String[] metrics_arr = metrics.toArray(new String[metrics.size()]);

						history = tigBean.getStatsHistory(metrics_arr);
						System.out.println(hostname + " loaded history, size: " + (((history !=
								null) && (history.get(metrics_arr[0]) != null))
								? history.get(metrics_arr[0]).size()
								: "null"));
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
				} else {
					System.out.println(hostname + " history already loaded, skipping.");
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
			server             = null;
			tigBean            = null;
			lastDisconnectTime = new Date();
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

	public void start() {
		if (updater == null) {
			updater = new StatisticsUpdater();
			System.out.println("Started: " + id + ":" + hostname + ":" + port);
		}
	}

	public void update() {
		if (tigBean != null) {

			// This doesn't ever change so it is enough to query it once
			if (cpuNo == 0) {
				cpuNo = tigBean.getCPUsNumber();
			}

			Map<String, Object> curMetrics = tigBean.getCurStats(metrics.toArray(
					new String[metrics.size()]));

			if (null == history) {
				return;
			}
			for (Map.Entry<String, Object> e : curMetrics.entrySet()) {
				LinkedList<Object> list = history.get(e.getKey());

				if (list != null) {
					list.add(e.getValue());
					if (list.size() > 1) {
						list.removeFirst();
					}
				}
			}
			sysDetails  = tigBean.getSystemDetails();
			initialized = true;
		}
	}

	//~--- get methods ----------------------------------------------------------

	public Map<String, String> getAllStats(int level) {
		if (tigBean != null) {
			return tigBean.getAllStats(level);
		}

		return null;
	}

	public List<String> getComponentsNames() {
		if (tigBean != null) {
			return tigBean.getComponentsNames();
		}

		return null;
	}

	public Map<String, String> getComponentStats(String compName, int level) {
		if (tigBean != null) {
			return tigBean.getComponentStats(compName, level);
		}

		return null;
	}

	public String getHostname() {
		return hostname;
	}

	public String getId() {
		return id;
	}

	public Object getMetricData(String key) {
		if (null == history) {
			return null;
		}
		LinkedList<Object> h = history.get(key);

		if (h != null && h.size() > 0) {
			return h.getLast();
		}

		return null;
	}

	public Object[] getMetricHistory(String key) {
		if (null == history) {
			return null;
		}
		List<Object> result = history.get(key);

		if (result != null) {
			switch (DataTypes.decodeTypeIdFromName(key)) {
			case 'I' :
				return result.toArray(new Integer[result.size()]);

			case 'L' :
				return result.toArray(new Long[result.size()]);

			case 'F' :
				return result.toArray(new Float[result.size()]);

			case 'D' :
				return result.toArray(new Double[result.size()]);

			default :
				return result.toArray(new String[result.size()]);
			}
		}

		return null;
	}

	public String getSystemDetails() {
		return sysDetails;
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

			// updateTimer.scheduleAtFixedRate(new TimerTask() {
			updateTimer.schedule(new TimerTask() {
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

						String disconnected = "";

						if (lastDisconnectTime != null) {
							long disconnectedInterval = (System.currentTimeMillis() - lastDisconnectTime
									.getTime()) / (1000 * 60);

							disconnected = ", disconnected: " + lastDisconnectTime + ", " +
									disconnectedInterval + " minutes ago.";
						}
						log.log(Level.WARNING, "{0}, {1}, retrying in {2} seconds{3}", new Object[] {
								cause.getMessage(),
								hostname, interval / 1000, disconnected });

						// log.log(Level.FINEST, e.getMessage(), e);
					} catch (Exception e) {
						log.log(Level.WARNING, "Problem retrieving statistics: ", e);
					}
				}
			}, delay, interval);
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/09/21
