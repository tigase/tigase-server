/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.ObjectName;
import tigase.conf.Configurator;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;
import tigase.server.AbstractComponentRegistrator;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.ServerComponent;
import tigase.sys.ShutdownHook;
import tigase.sys.TigaseRuntime;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.StanzaType;
import tigase.util.ElementUtils;
import tigase.util.JIDUtils;

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
	implements XMPPService, ShutdownHook {

  private static final Logger log =
		Logger.getLogger("tigase.stats.StatisticsCollector");

  private static final String STATS_XMLNS = "http://jabber.org/protocol/stats";

	private ServiceEntity serviceEntity = null;
	//private ServiceEntity stats_modules = null;
	private Level statsLevel = Level.INFO;

	@Override
	public void setName(String name) {
		super.setName(name);
		serviceEntity = new ServiceEntity(name, "stats", "Server statistics");
		serviceEntity.addIdentities(
			new ServiceIdentity("component", "stats",	"Server statistics"),
			new ServiceIdentity("automation", "command-node",	"All statistics"),
			new ServiceIdentity("automation", "command-list",
				"Statistics retrieving commands"));
		serviceEntity.addFeatures(DEF_FEATURES);
		serviceEntity.addFeatures(CMD_FEATURES);
		try {
			StatisticsProvider sp = new StatisticsProvider(this);
			String objName = "tigase.stats:type=StatisticsProvider";
			ObjectName on = new ObjectName(objName);
			ManagementFactory.getPlatformMBeanServer().registerMBean(sp, on);
			Configurator.putMXBean(objName, sp);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Can not install Statistics MXBean: ", ex);
		}
		TigaseRuntime.getTigaseRuntime().addShutdownHook(this);
	}

	@Override
	public String getName() {
		return super.getName();
	}

	@Override
	public void componentAdded(StatisticsContainer component) {
		ServiceEntity item = serviceEntity.findNode(component.getName());
		if (item == null) {
			item = new ServiceEntity(getName(), component.getName(),
				"Component: " + component.getName());
			item.addFeatures(CMD_FEATURES);
			item.addIdentities(new ServiceIdentity("automation", "command-node",
						"Component: " + component.getName()));
			serviceEntity.addItems(item);
		}
	}

	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof StatisticsContainer;
	}

	@Override
	public void componentRemoved(StatisticsContainer component) {}

	public List<StatRecord> getAllStats() {
		return getAllStats(Level.ALL.intValue());
	}

	public List<StatRecord> getAllStats(int level) {
		List<StatRecord> result = new ArrayList<StatRecord>();
		for (StatisticsContainer comp : components.values()) {
			result.addAll(getComponentStats(comp.getName(), level));
		}
		return result;
	}

	public List<StatRecord> getComponentStats(String name, int level) {
		List<StatRecord> result = null;
		StatisticsContainer stats = components.get(name);
		if (stats != null) {
			result = stats.getStatistics();
			if (result != null) {
				for (Iterator<StatRecord> it = result.iterator(); it.hasNext();) {
					StatRecord statRecord = it.next();
					if (statRecord.getLevel().intValue() < level) {
						it.remove();
					}
				}
			}
		}
		return result;
	}

	public List<String> getComponentsNames() {
		return new ArrayList<String>(components.keySet());
	}

	@Override
	public void processPacket(final Packet packet, final Queue<Packet> results) {

		if (!packet.isCommand()
			|| (packet.getType() != null && packet.getType() == StanzaType.result)) {
			return;
		}

		switch (packet.getCommand()) {
		case GETSTATS: {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Command received: " + packet.getStringData());
			}
			//			Element statistics = new Element("statistics");
			Element iq =
				ElementUtils.createIqQuery(packet.getElemTo(), packet.getElemFrom(),
					StanzaType.result, packet.getElemId(), STATS_XMLNS);
			Element query = iq.getChild("query");
			List<StatRecord> stats = getAllStats();
			if (stats != null && stats.size() > 0) {
				for (StatRecord record: stats) {
					Element item = new Element("stat");
					item.addAttribute("name", record.getComponent() + "/"
						+ record.getDescription());
					item.addAttribute("units", record.getUnit());
					item.addAttribute("value", record.getValue());
					query.addChild(item);
				} // end of for ()
			} // end of if (stats != null && stats.count() > 0)
			Packet result = new Packet(iq);
			//			Command.setData(result, statistics);
			results.offer(result);
			break;
		}
		case OTHER: {
			if (packet.getStrCommand() == null) return;
			String nick = JIDUtils.getNodeNick(packet.getTo());
			if (nick == null || !getName().equals(nick)) return;
			Command.Action action = Command.getAction(packet);
			if (action == Command.Action.cancel) {
				Packet result = packet.commandResult(null);
				results.offer(result);
				return;
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Command received: " + packet.getStringData());
			}
			String tmp_val = Command.getFieldValue(packet, "Stats level");
			if (tmp_val != null) {
				statsLevel = Level.parse(tmp_val);
			}
			List<StatRecord> stats = null;
			if (packet.getStrCommand().equals("stats")) {
				stats = getAllStats(statsLevel.intValue());
			} else {
				String[] spl = packet.getStrCommand().split("/");
				stats = getComponentStats(spl[1], statsLevel.intValue());
			}
			if (stats != null && stats.size() > 0) {
				Packet result = packet.commandResult(Command.DataType.form);
				for (StatRecord rec: stats) {
					if (rec.getType() == StatisticType.LIST) {
						Command.addFieldMultiValue(result,
										XMLUtils.escape(rec.getComponent() + "/" + rec.
										getDescription()),
										rec.getListValue());
					} else {
						Command.addFieldValue(result,
										XMLUtils.escape(rec.getComponent() + "/" + rec.
										getDescription()),
										XMLUtils.escape(rec.getValue()));
					}
				}
				Command.addFieldValue(result, "Stats level", statsLevel.getName(),
					"Stats level",
					new String[] {Level.INFO.getName(), Level.FINE.getName(),
												Level.FINER.getName(), Level.FINEST.getName()},
					new String[] {Level.INFO.getName(), Level.FINE.getName(),
												Level.FINER.getName(), Level.FINEST.getName()});
				results.offer(result);
			}
			break;
		}
		default:
			break;
		} // end of switch (packet.getCommand())
	}

	@Override
	public Element getDiscoInfo(String node, String jid) {
		if (jid != null && getName().equals(JIDUtils.getNodeNick(jid))) {
			return serviceEntity.getDiscoInfo(node);
		}
		return null;
	}

	@Override
	public 	List<Element> getDiscoFeatures() { return null; }

	@Override
	public List<Element> getDiscoItems(String node, String jid) {
		if (getName().equals(JIDUtils.getNodeNick(jid)) ||
						getComponentId().equals(jid)) {
			List<Element> items = serviceEntity.getDiscoItems(node, jid);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Processing discoItems for node: " + node + ", result: "
					+ (items == null ? null : items.toString()));
			}
			return items;
		} else {
			if (node == null) {
				Element item = serviceEntity.getDiscoItem(null,
								JIDUtils.getNodeID(getName(), jid));
    			if (log.isLoggable(Level.FINEST)) {
        			log.finest("Processing discoItems, result: " +
            					(item == null ? null : item.toString()));
                }
				return Arrays.asList(item);
			} else {
				return null;
			}
		}
	}

	@Override
	public String shutdown() {
		List<StatRecord> allStats = getAllStats();
		StringBuilder sb = new StringBuilder();
		for (StatRecord statRecord : allStats) {
			sb.append(statRecord.toString()).append('\n');
		}
		return sb.toString();
	}

}
