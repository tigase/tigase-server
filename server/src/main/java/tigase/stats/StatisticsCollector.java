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

import tigase.conf.ConfiguratorAbstract;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.*;
import tigase.sys.ShutdownHook;
import tigase.sys.TigaseRuntime;
import tigase.util.ElementUtils;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class StatisticsCollector
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = "stats", parent = Kernel.class, active = true)
public class StatisticsCollector
				extends AbstractComponentRegistrator<StatisticsContainer>
				implements ShutdownHook, RegistrarBean {
	public static final String ERRORS_STATISTICS_MBEAN_NAME =
			"tigase.stats:type=ErrorStatistics";
	public static final String STATISTICS_MBEAN_NAME =
			"tigase.stats:type=StatisticsProvider";

	public static final String STATS_HISTORY_SIZE_PROP_KEY = "stats-history-size";

	public static final int STATS_HISTORY_SIZE_PROP_VAL = 8640;

	public static final String STATS_UPDATE_INTERVAL_PROP_KEY = "stats-update-interval";

	public static final String STATS_HIGH_MEMORY_LEVEL_KEY = "stats-high-memory-level";

	/** Field description */
	private static final String STATS_XMLNS = "http://jabber.org/protocol/stats";
	private static final Logger log = Logger.getLogger(StatisticsCollector.class.getName());

	//~--- fields ---------------------------------------------------------------

	@ConfigField(desc = "History size", alias = STATS_HISTORY_SIZE_PROP_KEY)
	private int                                  historySize                 = 0;
	private TimerTask                            initializationCompletedTask = null;
	private ServiceEntity                        serviceEntity               = null;
	private StatisticsProvider                   sp                          = null;
	private ErrorsStatisticsProvider			 esp						 = null;
	@Inject
	private StatisticsArchivizerIfc[] archivizers = new StatisticsArchivizerIfc[0];
	private Map<StatisticsArchivizerIfc, TimerTask> archiverTasks = new ConcurrentHashMap<>();
	private final ArchivizerRunner arch_runner = new ArchivizerRunner();

	// private ServiceEntity stats_modules = null;
	private Level statsLevel       = Level.INFO;
	private final Timer statsArchivTasks = new Timer("stats-archivizer-tasks", true);
	private final Timer everyX = new Timer("stats-timer", true);
	@ConfigField(desc = "Update interval", alias = STATS_UPDATE_INTERVAL_PROP_KEY)
	private long  updateInterval   = 10;
	@ConfigField(desc = "High memory level", alias = STATS_HIGH_MEMORY_LEVEL_KEY)
	private int   highMemoryLevel  = 95;
	
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
			sp = new StatisticsProvider(this, historySize, updateInterval, highMemoryLevel);

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
		everyX.schedule(new TimerTask() {
			@Override
			public void run() {
				everySecond();
			}
		}, 1000,1000);
		everyX.schedule(new TimerTask() {
			@Override
			public void run() {
				everyMinute();
			}
		}, 1000 * 60,1000 * 60);
		everyX.schedule(new TimerTask() {
			@Override
			public void run() {
				everyHour();
			}
		}, 1000 * 60 * 60,1000 * 60 * 60);
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

		getStatistics(list);

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
	public void register(Kernel kernel) {
		
	}

	@Override
	public void unregister(Kernel kernel) {

	}

	//~--- methods --------------------------------------------------------------

	protected void statsUpdated() {
		synchronized (arch_runner) {
			arch_runner.notifyAll();
		}
		esp.update(sp);
	}

	public void setArchivizers(StatisticsArchivizerIfc[] archivizers) {
		if (archivizers == null) {
			archivizers = new StatisticsArchivizerIfc[0];
		}
		List<StatisticsArchivizerIfc> newArchivizers = Arrays.asList(archivizers);
		Arrays.stream(this.archivizers).filter(it -> !newArchivizers.contains(it)).forEach(it -> {
			TimerTask tt = this.archiverTasks.get(it);
			if (tt != null) {
				tt.cancel();
			}
		});
		List<StatisticsArchivizerIfc> oldArchivizers = Arrays.asList(this.archivizers);
		this.archivizers = archivizers;
		Arrays.stream(this.archivizers)
				.filter(it -> !oldArchivizers.contains(it))
				.filter(it -> it.getFrequency() > 0)
				.forEach(it -> {
					TimerTask tt = new TimerTask() {
						@Override
						public void run() {
							it.execute(sp);
						}
					};
					statsArchivTasks.schedule(tt, it.getFrequency() * 1000, it.getFrequency() * 1000);
					this.archiverTasks.put(it, tt);
		});
	}

	//~--- get methods ----------------------------------------------------------
	
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
					Arrays.stream(archivizers)
							.filter(archiv -> archiv.getFrequency() <= 0)
							.forEach(archiv -> archiv.execute(sp));
				} catch (InterruptedException ex) {

					// Ignore...
				}
			}
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/11/29
