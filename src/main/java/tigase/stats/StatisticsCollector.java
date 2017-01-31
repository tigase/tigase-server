/*
 * StatisticsCollector.java
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

import tigase.server.AbstractComponentRegistrator;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.ServerComponent;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

import tigase.conf.ConfigurationException;
import tigase.conf.ConfiguratorAbstract;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.sys.ShutdownHook;
import tigase.sys.TigaseRuntime;
import tigase.util.ElementUtils;
import tigase.xml.Element;
import tigase.xml.XMLUtils;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;

/**
 * Class StatisticsCollector
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StatisticsCollector
				extends AbstractComponentRegistrator<StatisticsContainer>
				implements ShutdownHook {
	public static final String ERRORS_STATISTICS_MBEAN_NAME =
			"tigase.stats:type=ErrorStatistics";
	public static final String STATISTICS_MBEAN_NAME =
			"tigase.stats:type=StatisticsProvider";

	public static final String STATS_ARCHIVIZERS = "--stats-archiv";

	public static final String STATS_ARCHIVIZERS_PROP_KEY = "stats-archiv";

	public static final String STATS_HISTORY = "--stats-history";

	public static final String STATS_HISTORY_SIZE_PROP_KEY = "stats-history-size";

	public static final int STATS_HISTORY_SIZE_PROP_VAL = 8640;

	public static final String STATS_UPDATE_INTERVAL_PROP_KEY = "stats-update-interval";

	public static final long    STATS_UPDATE_INTERVAL_PROP_VAL = 10l;
	private static final String STATS_XMLNS = "http://jabber.org/protocol/stats";
	private static final Logger log = Logger.getLogger(StatisticsCollector.class.getName());

	//~--- fields ---------------------------------------------------------------

	private int                                  historySize                 = 0;
	private TimerTask                            initializationCompletedTask = null;
	private ServiceEntity                        serviceEntity               = null;
	private StatisticsProvider                   sp                          = null;
	private ErrorsStatisticsProvider			 esp						 = null;
	private final Map<String, StatisticsArchivizerIfc> archivizers =
			new ConcurrentSkipListMap<>();
	private final ArchivizerRunner arch_runner = new ArchivizerRunner();

	// private ServiceEntity stats_modules = null;
	private Level statsLevel       = Level.INFO;
	private final Timer statsArchivTasks = new Timer("stats-archivizer-tasks", true);
	private long  updateInterval   = 10;

	//~--- methods --------------------------------------------------------------

	@Override
	public void componentAdded(StatisticsContainer component) {
		ServiceEntity item = serviceEntity.findNode(component.getName());

		if (item == null) {
			item = new ServiceEntity(getName(), component.getName(), "Component: " + component
					.getName());
			item.addFeatures(CMD_FEATURES);
			item.addIdentities(new ServiceIdentity("automation", "command-node",
					"Component: " + component.getName()));
			serviceEntity.addItems(item);
		}
	}

	@Override
	public void componentRemoved(StatisticsContainer component) {}

	@Override
	public void initializationCompleted() {
		if (isInitializationComplete()) {

			// Do we really need to do this again?
			return;
		}
		super.initializationCompleted();
		try {
			sp = new StatisticsProvider(this, historySize, updateInterval);

			String     objName = STATISTICS_MBEAN_NAME;
			ObjectName on      = new ObjectName(objName);

			ManagementFactory.getPlatformMBeanServer().registerMBean(sp, on);
			ConfiguratorAbstract.putMXBean(objName, sp);
			
			esp = new ErrorsStatisticsProvider();
			
			objName = ERRORS_STATISTICS_MBEAN_NAME;
			on      = new ObjectName(objName);

			ManagementFactory.getPlatformMBeanServer().registerMBean(esp, on);
			ConfiguratorAbstract.putMXBean(objName, esp);			
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Can not install Statistics MXBean: ", ex);
		}
		TigaseRuntime.getTigaseRuntime().addShutdownHook(this);
		if (initializationCompletedTask != null) {
			initializationCompletedTask.run();
		}
	}

	@Override
	public void processPacket(final Packet packet, final Queue<Packet> results) {
		if (!packet.isCommand() || (packet.getType() == StanzaType.result)) {
			return;
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0} command received: {1}", new Object[] { packet
					.getCommand().name(),
					packet });
		}

		Iq      iqc            = (Iq) packet;
		BareJID stanzaFromBare = iqc.getStanzaFrom().getBareJID();
		JID     stanzaFrom     = JID.jidInstance(stanzaFromBare);

		if (!isAdmin(stanzaFrom)) {
			Packet result = iqc.commandResult(Command.DataType.result);

			Command.addTextField(result, "Error",
					"You do not have enough permissions to manage this domain");
			results.offer(result);

			return;
		}
		switch (iqc.getCommand()) {
		case GETSTATS : {

			// Element statistics = new Element("statistics");
			Element iq = ElementUtils.createIqQuery(iqc.getStanzaTo(), iqc.getStanzaFrom(),
					StanzaType.result, iqc.getStanzaId(), STATS_XMLNS);
			Element        query = iq.getChild("query");
			StatisticsList stats = getAllStats();

			if (stats != null) {
				for (StatRecord record : stats) {
					Element item = new Element("stat");

					item.addAttribute("name", record.getComponent() + "/" + record
							.getDescription());
					item.addAttribute("value", record.getValue());
					query.addChild(item);
				}    // end of for ()
			}      // end of if (stats != null && stats.count() > 0)

			Packet result = Packet.packetInstance(iq, iqc.getStanzaTo(), iqc.getStanzaFrom());

			// Command.setData(result, statistics);
			results.offer(result);

			break;
		}

		case OTHER : {
			if (iqc.getStrCommand() == null) {
				return;
			}

			String nick = iqc.getTo().getLocalpart();

			if (!getName().equals(nick)) {
				return;
			}

			Command.Action action = Command.getAction(iqc);

			if (action == Command.Action.cancel) {
				Packet result = iqc.commandResult(null);

				results.offer(result);

				return;
			}

			String tmp_val = Command.getFieldValue(iqc, "Stats level");

			// copying default value of stats level to local variable to not override default value
			Level statsLevel = this.statsLevel;
			if (tmp_val != null) {
				statsLevel = Level.parse(tmp_val);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "statsLevel parsed to: {0}", statsLevel.getName());
				}
			}

			StatisticsList list = new StatisticsList(statsLevel);

			if (iqc.getStrCommand().equals("stats")) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Getting all stats for level: {0}", statsLevel.getName());
				}
				getAllStats(list);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "All stats for level loaded: {0}", statsLevel.getName());
				}
			} else {
				String[] spl = iqc.getStrCommand().split("/");

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Getting stats for component: {0}, level: {1}",
							new Object[] { spl[1],
							statsLevel.getName() });
				}
				getComponentStats(spl[1], list);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Stats loaded for component: {0}, level: {1}",
							new Object[] { spl[1],
							statsLevel.getName() });
				}
			}

			Packet result = iqc.commandResult(Command.DataType.form);

			for (StatRecord rec : list) {
				Command.addFieldValue(result, XMLUtils.escape(rec.getComponent() + "/" + rec
						.getDescription()), XMLUtils.escape(rec.getValue()));
			}

			Command.addFieldValue(result, "Stats level", statsLevel.getName(), "Stats level",
					new String[] { Level.INFO.getName(),
					Level.FINE.getName(), Level.FINER.getName(), Level.FINEST.getName() },
							new String[] { Level.INFO.getName(),
					Level.FINE.getName(), Level.FINER.getName(), Level.FINEST.getName() });
			results.offer(result);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Returning stats result: {0}", result);
			}

			break;
		}

		default :
			break;
		}    // end of switch (packet.getCommand())
	}

	@Override
	public void release() {
		super.release();
		sp.stop();
		statsArchivTasks.cancel();

		for (String stat_arch_key : archivizers.keySet()) {
			StatisticsArchivizerIfc stat_arch = archivizers.remove(stat_arch_key);

			if (stat_arch != null) {
				stat_arch.release();
			}
		}
	}

	@Override
	public String shutdown() {
		StatisticsList allStats = getAllStats();
		StringBuilder  sb       = new StringBuilder(4096);

		for (StatRecord statRecord : allStats) {
			sb.append(statRecord.toString()).append('\n');
		}

		return sb.toString();
	}

	//~--- get methods ----------------------------------------------------------

	public StatisticsList getAllStats() {
		StatisticsList list = new StatisticsList(Level.ALL);

		getAllStats(list);

		return list;
	}

	public void getAllStats(StatisticsList list) {
		for (StatisticsContainer comp : components.values()) {
			getComponentStats(comp.getName(), list);
		}

		int  totalQueuesWait     = 0;
		long totalQueuesOverflow = 0;

		for (StatisticsContainer comp : components.values()) {
			totalQueuesWait     += list.getValue(comp.getName(), "Total queues wait", 0);
			totalQueuesOverflow += list.getValue(comp.getName(), "Total queues overflow", 0L);
		}
		list.add("total", "Total queues wait", totalQueuesWait, Level.INFO);
		list.add("total", "Total queues overflow", totalQueuesOverflow, Level.INFO);
	}

	public List<String> getComponentsNames() {
		return new ArrayList<String>(components.keySet());
	}

	public void getComponentStats(String name, StatisticsList list) {
		StatisticsContainer stats = components.get(name);

		if (stats != null) {
			stats.getStatistics(list);
		}
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs             = super.getDefaults(params);
		String              statsArchivizers = (String) params.get(STATS_ARCHIVIZERS);

		if ((statsArchivizers != null) &&!statsArchivizers.isEmpty()) {
			String[] archivs = statsArchivizers.split(",");

			defs.put(STATS_ARCHIVIZERS_PROP_KEY, archivs);
		}

		int    hSize         = historySize;
		long   updateInt     = updateInterval;
		String stats_history = (String) params.get(STATS_HISTORY);

		if (stats_history != null) {
			String[] st_pars = stats_history.split(",");

			try {
				hSize = Integer.parseInt(st_pars[0]);
			} catch (NumberFormatException ex) {
				log.log(Level.CONFIG, "Invalid statistics history size settings: {0}",
						st_pars[0]);
			}
			if (st_pars.length > 1) {
				try {
					updateInt = Long.parseLong(st_pars[1]);
				} catch (NumberFormatException ex) {
					log.log(Level.CONFIG, "Invalid statistics update interval: {0}", st_pars[1]);
				}
			}
		}
		defs.put(STATS_HISTORY_SIZE_PROP_KEY, hSize);
		defs.put(STATS_UPDATE_INTERVAL_PROP_KEY, updateInt);

		return defs;
	}

	@Override
	public List<Element> getDiscoFeatures(JID from) {
		return null;
	}

	@Override
	public Element getDiscoInfo(String node, JID jid, JID from) {
		if ((jid != null) && getName().equals(jid.getLocalpart()) && isAdmin(from)) {
			return serviceEntity.getDiscoInfo(node);
		}

		return null;
	}

	@Override
	public List<Element> getDiscoItems(String node, JID jid, JID from) {
		if (isAdmin(from)) {
			if (getName().equals(jid.getLocalpart()) || getComponentId().equals(jid)) {
				List<Element> items = serviceEntity.getDiscoItems(node, jid.toString());

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Processing discoItems for node: {0}, result: {1}",
							new Object[] { node, (items == null)
							? null
							: items.toString() });
				}

				return items;
			} else {
				if (node == null) {
					Element item = serviceEntity.getDiscoItem(null, BareJID.toString(getName(), jid
							.toString()));

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Processing discoItems, result: {0}", ((item == null)
								? null
								: item.toString()));
					}

					return Arrays.asList(item);
				} else {
					return null;
				}
			}
		}

		return null;
	}

	@Override
	public String getName() {
		return super.getName();
	}

	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof StatisticsContainer;
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setName(String name) {
		super.setName(name);
		serviceEntity = new ServiceEntity(name, "stats", "Server statistics");
		serviceEntity.addIdentities(new ServiceIdentity("component", "stats",
				"Server statistics"), new ServiceIdentity("automation", "command-node",
				"All statistics"), new ServiceIdentity("automation", "command-list",
				"Statistics retrieving commands"));
		serviceEntity.addFeatures(DEF_FEATURES);
		serviceEntity.addFeatures(CMD_FEATURES);
	}

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		super.setProperties(props);

		String[] archivs = (String[]) props.get(STATS_ARCHIVIZERS_PROP_KEY);

		if (archivs != null) {
			initStatsArchivizers(archivs, props);
		}
		if (props.get(STATS_HISTORY_SIZE_PROP_KEY) != null) {
			historySize = (Integer) props.get(STATS_HISTORY_SIZE_PROP_KEY);
		}
		if (props.get(STATS_UPDATE_INTERVAL_PROP_KEY) != null) {
			updateInterval = (Long) props.get(STATS_UPDATE_INTERVAL_PROP_KEY);
		}
	}

	//~--- methods --------------------------------------------------------------

	protected void statsUpdated() {
		synchronized (arch_runner) {
			arch_runner.notifyAll();
		}
		esp.update(sp);
	}

	private void initStatsArchivizers(final String[] archivs, final Map<String,
			Object> props) {
		for (String stat_arch_key : archivizers.keySet()) {
			StatisticsArchivizerIfc stat_arch = archivizers.remove(stat_arch_key);

			if (stat_arch != null) {
				stat_arch.release();
			}
		}
		initializationCompletedTask = new TimerTask() {
			@Override
			public void run() {
				for (String arch_prop : archivs) {
					try {
						String[]                      arch_prop_a = arch_prop.split(":");
						String                        arch_class  = arch_prop_a[0];
						String                        arch_name   = arch_prop_a[1];
						final StatisticsArchivizerIfc stat_arch = (StatisticsArchivizerIfc) Class
								.forName(arch_class).newInstance();

						stat_arch.init(getArchivizerConf(arch_name, props));

						long freq = -1;

						if (arch_prop_a.length > 2) {
							try {
								freq = Long.parseLong(arch_prop_a[2]);
							} catch (Exception e) {
								freq = -1;
							}
						}

						// Some archivizers run in regular intervals of time
						// some others run each time statistics collection has completed.
						if (freq > 0) {
							statsArchivTasks.schedule(new TimerTask() {
								@Override
								public void run() {
									stat_arch.execute(sp);
								}
							}, freq * 1000, freq * 1000);
						} else {
							archivizers.put(arch_name, stat_arch);
						}
						log.log(Level.CONFIG, "Loaded statistics archivizer: {0} for class: {1}",
								new Object[] { arch_name,
								arch_class });
					} catch (Exception e) {
						log.log(Level.SEVERE, "Can't initialize statistics archivizer: " + arch_prop,
								e);
					}
				}
			}
		};
	}

	//~--- get methods ----------------------------------------------------------

	private Map<String, Object> getArchivizerConf(String name, Map<String, Object> props) {
		Map<String, Object> result    = new LinkedHashMap<String, Object>(4);
		String              key_start = STATS_ARCHIVIZERS_PROP_KEY + "/" + name + "/";

		for (Map.Entry<String, Object> entry : props.entrySet()) {
			if (entry.getKey().startsWith(key_start)) {
				String key = entry.getKey().substring(key_start.length());

				log.log(Level.CONFIG, "Found {0} property: {1} = {2}", new Object[] { name, key,
						entry.getValue() });
				result.put(key, entry.getValue());
			}
		}

		return result;
	}

	//~--- inner classes --------------------------------------------------------

	private class ArchivizerRunner
					extends Thread {
		private boolean stopped = false;

		//~--- constructors -------------------------------------------------------

		private ArchivizerRunner() {
			super("stats-archivizer");
			setDaemon(true);
			start();
		}

		//~--- methods ------------------------------------------------------------

		@Override
		public void run() {
			while (!stopped) {
				try {
					synchronized (this) {
						this.wait();
					}
					for (Map.Entry<String, StatisticsArchivizerIfc> archiv_entry : archivizers
							.entrySet()) {
						archiv_entry.getValue().execute(sp);
					}
				} catch (InterruptedException ex) {

					// Ignore...
				}
			}
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/11/29
