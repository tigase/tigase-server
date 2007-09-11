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
package tigase.server.bosh;

import java.io.IOException;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.xml.Element;
import tigase.net.IOService;
import tigase.server.Command;
import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.util.DNSResolver;
import tigase.util.RoutingsContainer;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;

import static tigase.server.bosh.Constants.*;
import static tigase.server.MessageRouterConfig.DEF_SM_NAME;

/**
 * Describe class BoshConnectionManager here.
 *
 *
 * Created: Sat Jun  2 12:24:29 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BoshConnectionManager extends ConnectionManager<BoshIOService>
	implements BoshSessionTaskHandler {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.bosh.BoshConnectionManager");

	private static final String ROUTINGS_PROP_KEY = "routings";
	private static final String ROUTING_MODE_PROP_KEY = "multi-mode";
	private static final boolean ROUTING_MODE_PROP_VAL = true;
	private static final String ROUTING_ENTRY_PROP_KEY = ".+";
	private static final String ROUTING_ENTRY_PROP_VAL = DEF_SM_NAME + "@localhost";

	private static final int DEF_PORT_NO = 5280;
	private int[] PORTS = {DEF_PORT_NO};
	private static final String HOSTNAMES_PROP_KEY = "hostnames";
	private String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};

	private RoutingsContainer routings = null;
	private Set<String> hostnames = new TreeSet<String>();
	private long max_wait = MAX_WAIT_DEF_PROP_VAL;
	private long min_polling = MIN_POLLING_PROP_VAL;
	private long max_inactivity = MAX_INACTIVITY_PROP_VAL;
	private int concurrent_requests = CONCURRENT_REQUESTS_PROP_VAL;
	private int hold_requests = HOLD_REQUESTS_PROP_VAL;
	private long max_pause = MAX_PAUSE_PROP_VAL;
	private Map<UUID, BoshSession> sessions =
		new LinkedHashMap<UUID, BoshSession>();

	public void processPacket(Packet packet) {
		log.finer("Processing packet: " + packet.getElemName()
			+ ", type: " + packet.getType());
		log.finest("Processing packet: " + packet.getStringData());
		UUID sid = UUID.fromString(JIDUtils.getNodeResource(packet.getTo()));
		BoshSession session = sessions.get(sid);
		if (session != null) {
			if (packet.isCommand() && packet.getCommand() != Command.OTHER) {
				processCommand(packet, session);
			} else {
				Queue<Packet> out_results = new LinkedList<Packet>();
				session.processPacket(packet, out_results);
				addOutPackets(out_results, session);
			}
		} else {
			log.warning("Session does not exist for packet: " + packet.toString());
		}
	}

	private void processCommand(Packet packet, BoshSession session) {
		XMPPIOService serv = getXMPPIOService(packet);
		switch (packet.getCommand()) {
		case GETFEATURES:
			if (packet.getType() == StanzaType.result) {
				Element elem_features = new Element("stream:features");
				elem_features.addChildren(Command.getData(packet));
				session.processPacket(new Packet(elem_features), null);
			} // end of if (packet.getType() == StanzaType.get)
			break;
		default:
			break;
		} // end of switch (pc.getCommand())
	}

	public Queue<Packet> processSocketData(BoshIOService serv) {
		Packet p = null;
		while ((p = serv.getReceivedPackets().poll()) != null) {
			log.finer("Processing packet: " + p.getElemName()
				+ ", type: " + p.getType());
			log.finest("Processing socket data: " + p.getStringData());
			String sid_str = p.getAttribute(SID_ATTR);
			UUID sid = null;
			try {
				Queue<Packet> out_results = new LinkedList<Packet>();
				BoshSession bs = null;
				if (sid_str == null) {
					bs = new BoshSession(getDefHostName(), this);
					sid = bs.getSid();
					sessions.put(sid, bs);
					bs.init(p, serv, max_wait, min_polling, max_inactivity,
						concurrent_requests, hold_requests, max_pause, out_results);
				} else {
					sid = UUID.fromString(sid_str);
					bs = sessions.get(sid);
					if (bs != null) {
						bs.processSocketPacket(p, serv, out_results);
					} else {
						log.warning("There is no session with given SID. Ignoring for now...");
					}
				}
				addOutPackets(out_results, bs);
			} catch (Exception e) {
				log.log(Level.WARNING,
					"Problem processing socket data for sid =  " + sid,	e);
			}
			//addOutPackets(out_results);
		} // end of while ()
		return null;
	}

	private void addOutPackets(Queue<Packet> out_results, BoshSession bs) {
		for (Packet res: out_results) {
			res.setFrom(getFromAddress(bs.getSid().toString()));
			res.setTo(routings.computeRouting(bs.getDomain()));
			addOutPacket(res);
		}
		out_results.clear();
	}

	private String getFromAddress(String id) {
		return JIDUtils.getJID(getName(), getDefHostName(), id);
	}

	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		if (params.get(GEN_VIRT_HOSTS) != null) {
			HOSTNAMES_PROP_VAL = ((String)params.get(GEN_VIRT_HOSTS)).split(",");
		} else {
			HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
		}
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_MODE_PROP_KEY,
			ROUTING_MODE_PROP_VAL);
		// If the server is configured as connection manager only node then
		// route packets to SM on remote host where is default routing
		// for external component.
		// Otherwise default routing is to SM on localhost
		if (params.get("config-type").equals(GEN_CONFIG_CS)
			&& params.get(GEN_EXT_COMP) != null) {
			String[] comp_params = ((String)params.get(GEN_EXT_COMP)).split(",");
			props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_ENTRY_PROP_KEY,
				DEF_SM_NAME + "@" + comp_params[1]);
		} else {
			props.put(ROUTINGS_PROP_KEY + "/" + ROUTING_ENTRY_PROP_KEY,
				DEF_SM_NAME + "@" + HOSTNAMES_PROP_VAL[0]);
		}
		props.put(MAX_WAIT_DEF_PROP_KEY, MAX_WAIT_DEF_PROP_VAL);
		props.put(MIN_POLLING_PROP_KEY, MIN_POLLING_PROP_VAL);
		props.put(MAX_INACTIVITY_PROP_KEY, MAX_INACTIVITY_PROP_VAL);
		props.put(CONCURRENT_REQUESTS_PROP_KEY, CONCURRENT_REQUESTS_PROP_VAL);
		props.put(HOLD_REQUESTS_PROP_KEY, HOLD_REQUESTS_PROP_VAL);
		props.put(MAX_PAUSE_PROP_KEY, MAX_PAUSE_PROP_VAL);
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		boolean routing_mode =
			(Boolean)props.get(ROUTINGS_PROP_KEY + "/" + ROUTING_MODE_PROP_KEY);
		routings = new RoutingsContainer(routing_mode);
		int idx = (ROUTINGS_PROP_KEY + "/").length();
		for (Map.Entry<String, Object> entry: props.entrySet()) {
			if (entry.getKey().startsWith(ROUTINGS_PROP_KEY + "/")
				&& !entry.getKey().equals(ROUTINGS_PROP_KEY + "/" +
					ROUTING_MODE_PROP_KEY)) {
				routings.addRouting(entry.getKey().substring(idx),
					(String)entry.getValue());
			} // end of if (entry.getKey().startsWith(ROUTINGS_PROP_KEY + "/"))
		} // end of for ()
		String[] hnames = (String[])props.get(HOSTNAMES_PROP_KEY);
		clearRoutings();
		hostnames.clear();
		for (String host: hnames) {
			addRouting(getName() + "@" + host);
			hostnames.add(host);
		} // end of for ()
		max_wait = (Long)props.get(MAX_WAIT_DEF_PROP_KEY);
		min_polling  = (Long)props.get(MIN_POLLING_PROP_KEY);
		max_inactivity = (Long)props.get(MAX_INACTIVITY_PROP_KEY);
		concurrent_requests = (Integer)props.get(CONCURRENT_REQUESTS_PROP_KEY);
		hold_requests = (Integer)props.get(HOLD_REQUESTS_PROP_KEY);
		max_pause = (Long)props.get(MAX_PAUSE_PROP_KEY);
	}

	protected int[] getDefPlainPorts() {
		return PORTS;
	}

	public void serviceStopped(BoshIOService service) {
		super.serviceStopped(service);
		UUID sid = service.getSid();
		if (sid != null) {
			BoshSession bs = sessions.get(sid);
			if (bs != null) {
				bs.disconnected(service);
			}
		}
	}

	public void serviceStarted(BoshIOService service) {
		super.serviceStarted(service);
	}

	/**
	 * Method <code>getMaxInactiveTime</code> returns max keep-alive time
	 * for inactive connection. we shoulnd not really close external component
	 * connection at all, so let's say something like: 1000 days...
	 *
	 * @return a <code>long</code> value
	 */
	protected long getMaxInactiveTime() {
		return 10*MINUTE;
	}

	public void xmppStreamClosed(BoshIOService serv) {
		log.finer("Stream closed.");
	}

	public String xmppStreamOpened(BoshIOService serv,
		Map<String, String> attribs) {
		return null;
	}

	protected BoshIOService getXMPPIOServiceInstance() {
		return new BoshIOService();
	}

	private Timer boshTasks = new Timer("BoshTasks");

	public TimerTask scheduleTask(BoshSession bs, long delay) {
		BoshTask bt = new BoshTask(bs);
		boshTasks.schedule(bt, delay);
		return bt;
	}

	public void cancelTask(TimerTask tt) {
		tt.cancel();
	}

	private class BoshTask extends TimerTask {

		private BoshSession bs = null;

		public BoshTask(BoshSession bs) {
			this.bs = bs;
		}

		public void run() {
			Queue<Packet> out_results = new LinkedList<Packet>();
			if (bs.task(out_results, this)) {
				sessions.remove(bs.getSid());
			}
			addOutPackets(out_results, bs);
		}

	}

}
