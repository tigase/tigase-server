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

package tigase.server;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import tigase.conf.ConfiguratorAbstract;
import tigase.xml.Element;
import tigase.util.UpdatesChecker;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.PacketErrorTypeException;
import tigase.disco.XMPPService;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;

import tigase.stats.StatisticsList;
import tigase.sys.TigaseRuntime;
import static tigase.server.MessageRouterConfig.*;

/**
 * Class MessageRouter
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageRouter extends AbstractMessageReceiver 
		implements MessageRouterIfc {
	//	implements XMPPService {

//	public static final String INFO_XMLNS =	"http://jabber.org/protocol/disco#info";
//	public static final String ITEMS_XMLNS = "http://jabber.org/protocol/disco#items";

  private static final Logger log =
    Logger.getLogger(MessageRouter.class.getName());

	//private static final long startupTime = System.currentTimeMillis();

//	private Set<String> localAddresses =	new CopyOnWriteArraySet<String>();
	private String disco_name = DISCO_NAME_PROP_VAL;
	private boolean disco_show_version = DISCO_SHOW_VERSION_PROP_VAL;

  private ConfiguratorAbstract config = null;
	private ServiceEntity serviceEntity = null;
	private UpdatesChecker updates_checker = null;

	private Map<String, XMPPService> xmppServices =
		new ConcurrentHashMap<String, XMPPService>();
  private Map<String, ServerComponent> components =
    new ConcurrentHashMap<String, ServerComponent>();
  private Map<String, ServerComponent> components_byId =
    new ConcurrentHashMap<String, ServerComponent>();
  private Map<String, ComponentRegistrator> registrators =
    new ConcurrentHashMap<String, ComponentRegistrator>();
  private Map<String, MessageReceiver> receivers =
    new ConcurrentHashMap<String, MessageReceiver>();

	public void processPacketMR(final Packet packet, final Queue<Packet> results) {
		if (packet.getPermissions() != Permissions.ADMIN) {
			try {
				Packet res = Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You are not authorized for this action.", true);
				results.offer(res);
				//processPacket(res);
			} catch (PacketErrorTypeException e) {
				log.warning("Packet processing exception: " + e);
			}
			return;
		}

		if (log.isLoggable(Level.FINEST)) {
			log.finest("Command received: " + packet.getStringData());
		}
		switch (packet.getCommand()) {
		case OTHER:
			if (packet.getStrCommand() != null) {
				if (packet.getStrCommand().startsWith("controll/")) {
					String[] spl = packet.getStrCommand().split("/");
					String cmd = spl[1];
					if (cmd.equals("stop")) {
						Packet result = packet.commandResult(Command.DataType.result);
						results.offer(result);
						//processPacket(result);
						new Timer("Stopping...", true).schedule(new TimerTask() {
							@Override
							public void run() {
								System.exit(0);
							}
						}, 2000);
					}
				}
			}
			break;
		default:
			break;
		}
	}

	@Override
	protected Integer getMaxQueueSize(int def) {
		return def*10;
	}

	private ServerComponent[] getServerComponentsForRegex(String id) {
		LinkedHashSet<ServerComponent> comps = new LinkedHashSet<ServerComponent>();
		for (MessageReceiver mr: receivers.values()) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Checking routings for: " + mr.getName());
			}
			if (mr.isInRegexRoutings(id)) {
				comps.add(mr);
			}
		}
		if (comps.size() > 0) {
			return comps.toArray(new ServerComponent[comps.size()]);
		} else {
			return null;
		}
	}

	private ServerComponent getLocalComponent(String jid, String host, String nick) {
		ServerComponent comp = components_byId.get(jid);
		if (comp != null) {
			return comp;
		}

		if (nick != null) {
			comp = components.get(nick);
			if (comp != null && 
							(isLocalDomain(host) || host.equals(getDefHostName()))) {
				return comp;
			}
		}
		int idx = host.indexOf('.');
		if (idx > 0) {
			String cmpName = host.substring(0, idx);
			String basename = host.substring(idx + 1);
			comp = components.get(cmpName);
			if (comp != null &&
							(isLocalDomain(basename) || basename.equals(getDefHostName()))) {
				return comp;
			}
		}
		return null;
	}

// 	private String isToLocalComponent(String jid) {
// 		String nick = JIDUtils.getNodeNick(jid);
// 		if (nick == null) {
// 			return null;
// 		}
// 		String host = JIDUtils.getNodeHost(jid);
// 		if (isLocalDomain(host) && components.get(nick) != null) {
// 			return nick;
// 		}
// 		return null;
// 	}

//	private boolean isLocalDomain(String domain) {
//		return localAddresses.contains(domain);
//	}

	@Override
	public void processPacket(Packet packet) {

		if (packet.getTo() == null) {
			log.warning("Packet with TO attribute set to NULL: "
				+	packet.getStringData());
			return;
		} // end of if (packet.getTo() == null)

		// Intentionally comparing to static, final String
		if (packet.getTo() == NULL_ROUTING) {
			log.info("NULL routing, it is normal if server doesn't know how to"
				+ " process packet: " + packet.toStringSecure());
			try {
				Packet error =
					Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet,
						"Feature not supported yet.", true);
				addOutPacketNB(error);
			} catch (PacketErrorTypeException e) {
				log.warning("Packet processing exception: " + e);
			}
			return;
		}

// 		if (log.isLoggable(Level.FINER)) {
// 			log.finer("Processing packet: " + packet.getElemName()
// 				+ ", type: " + packet.getType());
// 		}
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Processing packet: " + packet.toStringSecure());
		}

		// Detect inifinite loop if from == to
		// Maybe it is not needed anymore...
		// There is a need to process packets with the same from and to address
		// let't try to relax restriction and block all packets with error type
		// 2008-06-16
		if ((packet.getType() == StanzaType.error &&
						packet.getFrom() != null &&
						packet.getFrom().equals(packet.getTo())) ||
						(packet.getFrom() == NULL_ROUTING &&
						packet.getElemFrom() != null &&
						packet.getElemFrom().equals(packet.getTo()))) {
			log.warning("Possible infinite loop, dropping packet: "
				+ packet.toStringSecure());
			return;
		}

		ServerComponent comp = packet.getElemTo() == null ? null
      : getLocalComponent(packet.getElemTo(), packet.getElemToHost(),
			packet.getElemToNick());
		if (packet.isServiceDisco() && packet.getType() != null &&
						packet.getType() == StanzaType.get &&
						packet.getElemFrom() != null &&
						((comp != null && !(comp instanceof DisableDisco)) ||
						isLocalDomain(packet.getElemTo()))) {
			Queue<Packet> results = new ArrayDeque<Packet>();
			processDiscoQuery(packet, results);
			if (results.size() > 0) {
				for (Packet res: results) {
					// No more recurrential calls!!
					addOutPacketNB(res);
				} // end of for ()
			}
			return;
		}
//		String id =  packet.getToId();
		comp = getLocalComponent(packet.getToId(), packet.getToHost(),
						packet.getToNick());
		if (comp != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Packet will be processed by: " + comp.getComponentId());
			}
			Queue<Packet> results = new ArrayDeque<Packet>();
			if (comp == this) {
				processPacketMR(packet, results);
			} else {
				comp.processPacket(packet, results);
			}
			if (results.size() > 0) {
				for (Packet res: results) {
					// No more recurrential calls!!
					addOutPacketNB(res);
					//					processPacket(res);
				} // end of for ()
			}
			return;
		}

		// Let's try to find message receiver quick way
		// In case if packet is handled internally:
//		String nick = JIDUtils.getNodeNick(packet.getTo());
		String host = packet.getToHost();
//		MessageReceiver first = null;
		// Below code probably never get's executed anyway.
		// All components included in commented code below should
		// be picked up by code above.
//		if (nick != null) {
//			first = receivers.get(nick);
//		} // end of if (nick != null)
//		if (first != null && host.equals(getDefHostName())) {
//			log.finest("Found receiver: " + first.getName());
//			first.addPacketNB(packet);
//			return;
//		} // end of if (mr != null)
		// This packet is not processed localy, so let's find receiver
		// which will send it to correct destination:

		ServerComponent[] comps = getComponentsForLocalDomain(host);
		if (comps == null) {
			comps = getServerComponentsForRegex(packet.getToId());
		}
		if (comps == null && !isLocalDomain(host)) {
			comps = getComponentsForNonLocalDomain(host);
		}
		if (comps != null) {
			Queue<Packet> results = new ArrayDeque<Packet>();
			for (ServerComponent serverComponent : comps) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Packet will be processed by: " +
									serverComponent.getComponentId());
				}
				serverComponent.processPacket(packet, results);
				if (results.size() > 0) {
					for (Packet res : results) {
						// No more recurrential calls!!
						addOutPacketNB(res);
					//					processPacket(res);
					} // end of for ()
				}
			}
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("There is no component for the packet, sending it back");
			}
			try {
				addOutPacketNB(
					Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
						"There is no service found to process your request.", true));
			} catch (PacketErrorTypeException e) {
				// This packet is to local domain, we don't want to send it out
				// drop packet :-(
				log.warning("Can't process packet to local domain, dropping..."
					+ packet.toStringSecure());
			}
		}

//		MessageReceiver s2s = null;
//		for (MessageReceiver mr: receivers.values()) {
//			Set<String> routings = mr.getRoutings();
//			if (routings != null) {
//				log.finest(mr.getName() + ": Looking for host: " + host +
//					" in " + routings.toString());
//				if (routings.contains(host) || routings.contains(id)) {
//					log.finest("Found receiver: " + mr.getName());
//					mr.addPacketNB(packet);
//					return;
//				} // end of if (routings.contains())
//				// Resolve wildchars routings....
//				if (mr.isInRegexRoutings(id)) {
//					log.finest("Found receiver: " + mr.getName());
//					mr.addPacketNB(packet);
//					return;
//				}
//				if (routings.contains("*")) {
//					// I found s2s receiver, remember it for later....
//					s2s = mr;
//				} // end of if (routings.contains())
//			} // end of if (routings != null)
//			else {
//				log.severe("Routings are null for: " + mr.getName());
//			} // end of if (routings != null) else
//		} // end of for (MessageReceiver mr: receivers.values())
//		// It is not for any local host, so maybe it is for some
//		// remote server, let's try sending it through s2s service:
//		if (localAddresses.contains(host) || comp != null) {
//			try {
//				addOutPacketNB(
//					Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet,
//						"Your request can not be processed.", true));
//			} catch (PacketErrorTypeException e) {
//				// This packet is to local domain, we don't want to send it out
//				// drop packet :-(
//				log.warning("Can't process packet to local domain, dropping..."
//					+ packet.toString());
//			}
//			return;
//		}
//		if (s2s != null) {
//			s2s.addPacketNB(packet);
//		} // end of if (s2s != null)
  }

	private ServerComponent[] getComponentsForLocalDomain(String domain) {
		return vHostManager.getComponentsForLocalDomain(domain);
	}

	private ServerComponent[] getComponentsForNonLocalDomain(String domain) {
		return vHostManager.getComponentsForNonLocalDomain(domain);
	}

	@Override
  public void setConfig(ConfiguratorAbstract config) {
    components.put(getName(), this);
    this.config = config;
    addRegistrator(config);
  }

  public void addRegistrator(ComponentRegistrator registr) {
    log.info("Adding registrator: " + registr.getClass().getSimpleName());
    registrators.put(registr.getName(), registr);
    addComponent(registr);
    for (ServerComponent comp : components.values()) {
      // 			if (comp != registr) {
      registr.addComponent(comp);
      // 			} // end of if (comp != registr)
    } // end of for (ServerComponent comp : components)
  }

  public void addRouter(MessageReceiver receiver) {
    log.info("Adding receiver: " + receiver.getClass().getSimpleName());
    addComponent(receiver);
    receivers.put(receiver.getName(), receiver);
  }

  public void addComponent(ServerComponent component) {
    log.info("Adding component: " + component.getClass().getSimpleName());
    for (ComponentRegistrator registr : registrators.values()) {
      if (registr != component) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Adding: " + component.getName() + " component to "
					+ registr.getName() + " registrator.");
			}
				registr.addComponent(component);
      } // end of if (reg != component)
    } // end of for ()
    components.put(component.getName(), component);
    components_byId.put(component.getComponentId(), component);
		if (component instanceof XMPPService) {
			xmppServices.put(component.getName(), (XMPPService)component);
		}
  }

	@Override
  public Map<String, Object> getDefaults(Map<String, Object> params) {
    Map<String, Object> defs = super.getDefaults(params);
    MessageRouterConfig.getDefaults(defs, params, getName());
    return defs;
  }

  private boolean inProperties = false;
	@Override
  public void setProperties(Map<String, Object> props) {

    if (inProperties) {
      return;
    } else {
      inProperties = true;
    } // end of if (inProperties) else

		disco_name = (String)props.get(DISCO_NAME_PROP_KEY);
		disco_show_version = (Boolean)props.get(DISCO_SHOW_VERSION_PROP_KEY);

		serviceEntity = new ServiceEntity("Tigase", "server", "Session manager");
		serviceEntity.addIdentities(new ServiceIdentity[] {
				new ServiceIdentity("server", "im", disco_name +
					(disco_show_version ?
						(" ver. " + tigase.server.XMPPServer.getImplementationVersion())
						: ""))});
		serviceEntity.addFeatures(XMPPService.DEF_FEATURES);

    try {
      super.setProperties(props);
//			String[] localAddresses = (String[])props.get(LOCAL_ADDRESSES_PROP_KEY);
//			this.localAddresses.clear();
//			if (localAddresses != null && localAddresses.length > 0) {
//				Collections.addAll(this.localAddresses, localAddresses);
//				this.localAddresses.add(getDefHostName());
//			}
      Map<String, ComponentRegistrator> tmp_reg = registrators;
      Map<String, MessageReceiver> tmp_rec = receivers;
      components = new TreeMap<String, ServerComponent>();
      registrators = new TreeMap<String, ComponentRegistrator>();
      receivers = new TreeMap<String, MessageReceiver>();
      setConfig(config);

      MessageRouterConfig conf = new MessageRouterConfig(props);
      String[] reg_names = conf.getRegistrNames();
      for (String name: reg_names) {
				ComponentRegistrator cr = tmp_reg.remove(name);
				String cls_name =
					(String)props.get(REGISTRATOR_PROP_KEY + name + ".class");
				try {
					if (cr == null || !cr.getClass().getName().equals(cls_name)) {
						if (cr != null) {
							cr.release();
						}
						cr = conf.getRegistrInstance(name);
						cr.setName(name);
					} // end of if (cr == null)
					addRegistrator(cr);
				} // end of try
				catch (Exception e) {
					e.printStackTrace();
				} // end of try-catch
      } // end of for (String name: reg_names)
      for (ComponentRegistrator cr: tmp_reg.values()) {
				cr.release();
      } // end of for ()
      tmp_reg.clear();

      String[] msgrcv_names = conf.getMsgRcvNames();
      for (String name: msgrcv_names) {
				if (log.isLoggable(Level.FINER)) {
					log.finer("Loading and registering message receiver: " + name);
				}
				ServerComponent mr = tmp_rec.remove(name);
				String cls_name =
					(String)props.get(MSG_RECEIVERS_PROP_KEY + name + ".class");
				try {
					if (mr == null || !mr.getClass().getName().equals(cls_name)) {
						if (mr != null) {
							mr.release();
						}
						mr = conf.getMsgRcvInstance(name);
						mr.setName(name);
						if (mr instanceof MessageReceiver) {
							((MessageReceiver)mr).setParent(this);
							((MessageReceiver)mr).start();
						}
					} // end of if (cr == null)
					if (mr instanceof MessageReceiver) {
						addRouter((MessageReceiver)mr);
					} else {
						addComponent(mr);
					}
				} // end of try
				catch (Exception e) {
					e.printStackTrace();
				} // end of try-catch
      } // end of for (String name: reg_names)
			for (MessageReceiver mr: tmp_rec.values()) {
				mr.release();
			} // end of for ()
			tmp_rec.clear();
			if ((Boolean)props.get(UPDATES_CHECKING_PROP_KEY)) {
				installUpdatesChecker((Long)props.get(UPDATES_CHECKING_INTERVAL_PROP_KEY));
			} else {
				stopUpdatesChecker();
			}
    } finally {
      inProperties = false;
    } // end of try-finally
		for (ServerComponent comp : components.values()) {
			log.info("Initialization completed notification to: " + comp.getName());
			comp.initializationCompleted();
		}
//		log.info("Initialization completed notification to: " + config.getName());
//		config.initializationCompleted();
  }

	private void stopUpdatesChecker() {
		if (updates_checker != null) {
			updates_checker.interrupt();
			updates_checker = null;
		}
	}

	private void installUpdatesChecker(long interval) {
		stopUpdatesChecker();
		updates_checker = new UpdatesChecker(interval, this,
			"This is automated message generated by updates checking module.\n"
			+ " You can disable this function changing configuration option: "
			+ "'/" + getName() + "/" + UPDATES_CHECKING_PROP_KEY + "' or adjust"
			+ " updates checking interval time changing option: "
			+ "'/" + getName() + "/" + UPDATES_CHECKING_INTERVAL_PROP_KEY + "' which"
			+ " now set to " + interval + " days.");
		updates_checker.start();
	}

	private void processDiscoQuery(final Packet packet,
			final Queue<Packet> results) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Processing disco query by: " + packet.toStringSecure());
		}
		String jid = packet.getElemTo();
		String nick = packet.getElemToNick();
		String from = packet.getElemFrom();
		String node = packet.getAttribute("/iq/query", "node");
		Element query = packet.getElement().getChild("query").clone();

		if (packet.isXMLNS("/iq/query", INFO_XMLNS)) {
			if (isLocalDomain(jid) && node == null) {
				query = getDiscoInfo(node, jid, from);
				for (XMPPService comp : xmppServices.values()) {
					List<Element> features = comp.getDiscoFeatures(from);
					if (features != null) {
						query.addChildren(features);
					}
				}
			} else {
				for (XMPPService comp : xmppServices.values()) {
					//						if (jid.startsWith(comp.getName() + ".")) {
					Element resp = comp.getDiscoInfo(node, jid, from);
					if (resp != null) {
						query = resp;
						break;
					}
					//						}
					}
			}
		}

		if (packet.isXMLNS("/iq/query", ITEMS_XMLNS)) {
			boolean localDomain = isLocalDomain(jid);
			if (localDomain) {
				for (XMPPService comp : xmppServices.values()) {
					//	if (localDomain || (nick != null && comp.getName().equals(nick))) {
					List<Element> items = comp.getDiscoItems(node, jid, from);
					if (log.isLoggable(Level.FINEST)) {
						log.finest("DiscoItems processed by: " + comp.getComponentId() +
								", items: " + (items == null ? null : items.toString()));
					}
					if (items != null && items.size() > 0) {
						query.addChildren(items);
					}
				} // end of for ()
			} else {
				ServerComponent comp = getLocalComponent(packet.getElemTo(),
						packet.getElemToHost(), packet.getElemToNick());
				if (comp != null && comp instanceof XMPPService) {
					List<Element> items = ((XMPPService)comp).getDiscoItems(node, jid, from);
					if (log.isLoggable(Level.FINEST)) {
						log.finest("DiscoItems processed by: " + comp.getComponentId() +
								", items: " + (items == null ? null : items.toString()));
					}
					if (items != null && items.size() > 0) {
						query.addChildren(items);
					}
				}
			}
		}
		results.offer(packet.okResult(query, 0));
	}

	@Override
	public int processingThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public Element getDiscoInfo(String node, String jid, String from) {
		Element query = serviceEntity.getDiscoInfo(null);
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Returing disco-info: " + query.toString());
		}
		return query;
	}

//	public List<Element> getDiscoItems(String node, String jid) {
//		return null;
//	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
    list.add(getName(), "Local hostname", getDefHostName(),
						Level.INFO);
		TigaseRuntime runtime = TigaseRuntime.getTigaseRuntime();
		list.add(getName(), "Uptime", runtime.getUptimeString(), Level.INFO);

		NumberFormat format = NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(4);
		list.add(getName(), "Load average",
						format.format(runtime.getLoadAverage()), Level.FINE);
		list.add(getName(), "CPUs no", runtime.getCPUsNumber(), Level.FINEST);

		list.add(getName(), "Threads count", runtime.getThreadsNumber(), Level.FINEST);
		float cpuUsage = runtime.getCPUUsage();
		format = NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(1);
//		if (format instanceof DecimalFormat) {
//			DecimalFormat decf = (DecimalFormat)format;
//			decf.applyPattern(decf.toPattern()+"%");
//		}
		list.add(getName(), "CPU usage", format.format(cpuUsage) + "%", Level.INFO);
		MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		MemoryUsage nonHeap =
						ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();

		format = NumberFormat.getIntegerInstance();
		if (format instanceof DecimalFormat) {
			DecimalFormat decf = (DecimalFormat)format;
			decf.applyPattern(decf.toPattern()+" KB");
		}
		list.add(getName(), "Max Heap mem", format.format(heap.getMax()/1024), Level.INFO);
		list.add(getName(), "Used Heap", format.format(heap.getUsed()/1024), Level.INFO);
		list.add(getName(), "Free Heap",
						format.format((heap.getMax() - heap.getUsed())/1024), Level.FINE);
		list.add(getName(), "Max NonHeap mem",
						format.format(nonHeap.getMax()/1024), Level.FINE);
		list.add(getName(), "Used NonHeap",
						format.format(nonHeap.getUsed()/1024), Level.FINE);
		list.add(getName(), "Free NonHeap",
						format.format((nonHeap.getMax() - nonHeap.getUsed()) / 1024),
						Level.FINE);
	}

}
