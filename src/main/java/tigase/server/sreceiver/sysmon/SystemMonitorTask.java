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

package tigase.server.sreceiver.sysmon;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.server.sreceiver.PropertyItem;
import tigase.server.sreceiver.RepoRosterTask;
import tigase.util.ClassUtil;
import tigase.xmpp.StanzaType;

import static tigase.server.sreceiver.PropertyConstants.*;
import static tigase.server.sreceiver.sysmon.ResourceMonitorIfc.*;

/**
 * Created: Dec 6, 2008 8:12:41 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SystemMonitorTask extends RepoRosterTask {

  private static Logger log =
					Logger.getLogger("tigase.server.sreceiver.sysmon.SystemMonitorTask");

	private static final String TASK_TYPE = "System Monitor";
	private static final String TASK_HELP =
		"This is a system monitor task." +
		" It monitors system resources usage and sends notifications" +
		" to subscribed users. It allos responds to your messages with" +
		" a simple reply message. This is to ensure the monitor works.";
	private static final String MONITORS_CLASSES_PROP_KEY =
					"Monitor implementations";
	private static final String WARNING_TRESHOLD_PROP_KEY =
					"Warning treshold";
	//private long interval = 10*SECOND;

	private String[] all_monitors = null;
	private String[] selected_monitors = null;
	private Map<String, ResourceMonitorIfc> monitors =
					new LinkedHashMap<String, ResourceMonitorIfc>();
	private double warning_treshold = 0.9;

	private enum command { help, state; };

	private Timer tasks = null;

	public SystemMonitorTask() {
		try {
			Set<ResourceMonitorIfc> mons =
							ClassUtil.getImplementations(ResourceMonitorIfc.class);
			all_monitors = new String[mons.size()];
			int idx = 0;
			for (ResourceMonitorIfc monitor : mons) {
				all_monitors[idx++] = monitor.getClass().getName();
			}
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Can't load resource monitors implementations", ex);
			all_monitors = new String[2];
			all_monitors[0] = "tigase.server.sreceiver.sysmon.CPUMonitor";
			all_monitors[1] = "tigase.server.sreceiver.sysmon.MemMonitor";
		}
	}

	protected void sendPacketsOut(Queue<Packet> input) {
		Queue<Packet> results = new LinkedList<Packet>();
		for (Packet packet : input) {
			if (packet.getElemName() == "message" || packet.getElemTo() == null ||
							packet.getElemTo().isEmpty()) {
				super.processMessage(packet, results);
			} else {
				results.add(packet);
			}
		}
		for (Packet packet : results) {
			addOutPacket(packet);
		}
	}

	protected void sendPacketOut(Packet input) {
		Queue<Packet> results = new LinkedList<Packet>();
		if (input.getElemName() == "message" || input.getElemTo() == null ||
						input.getElemTo().isEmpty()) {
			super.processMessage(input, results);
		} else {
			results.add(input);
		}
		for (Packet packet : results) {
			addOutPacket(packet);
		}
	}

	private void monitor10Secs() {
		Queue<Packet> results = new LinkedList<Packet>();
		for (ResourceMonitorIfc monitor : monitors.values()) {
			monitor.check10Secs(results);
		}
		sendPacketsOut(results);
	}
	
	private void monitor1Min() {
		Queue<Packet> results = new LinkedList<Packet>();
		for (ResourceMonitorIfc monitor : monitors.values()) {
			monitor.check1Min(results);
		}
		sendPacketsOut(results);
	}
	
	private void monitor1Hour() {
		Queue<Packet> results = new LinkedList<Packet>();
		for (ResourceMonitorIfc monitor : monitors.values()) {
			monitor.check1Hour(results);
		}
		sendPacketsOut(results);
	}
	
	private void monitor1Day() {
		Queue<Packet> results = new LinkedList<Packet>();
		for (ResourceMonitorIfc monitor : monitors.values()) {
			monitor.check1Day(results);
		}
		sendPacketsOut(results);
	}

	@Override
	public void init(Queue<Packet> results) {
		super.init(results);
		tasks = new Timer("SystemMonitorTask", true);
		tasks.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				monitor10Secs();
			}
		}, INTERVAL_10SECS, INTERVAL_10SECS);
		tasks.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				monitor1Min();
			}
		}, INTERVAL_1MIN, INTERVAL_1MIN);
		tasks.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				monitor1Hour();
			}
		}, INTERVAL_1HOUR, INTERVAL_1HOUR);
		tasks.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				monitor1Day();
			}
		}, INTERVAL_1DAY, INTERVAL_1DAY);
	}

	@Override
	public void destroy(Queue<Packet> results) {
		tasks.cancel();
		tasks = null;
		super.destroy(results);
	}

	public String getType() {
		return TASK_TYPE;
	}

	public String getHelp() {
		return TASK_HELP;
	}

	private String commandsHelp() {
		return "Available commands are:\n" +
						"//help - display this help info\n" +
						"//state - displays current state from all monitors\n";
	}

	private boolean isPostCommand(Packet packet) {
		String body = packet.getElemCData("/message/body");
		if (body != null) {
			for (command comm: command.values()) {
				if (body.startsWith("//" + comm.toString())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void setParams(Map<String, Object> map)	{
		super.setParams(map);
		String treshold = (String) map.get(WARNING_TRESHOLD_PROP_KEY);
		try {
			double tresh = Double.parseDouble(treshold);
			warning_treshold = tresh;
		} catch (Exception e) {
			log.warning("Incorrect warning treshold, using default" + treshold);
		}
		String[] mons = null;
		try {
			mons = (String[]) map.get(MONITORS_CLASSES_PROP_KEY);
		} catch (Exception e) {
			log.warning("Incorrect monitors list: " + 
							map.get(MONITORS_CLASSES_PROP_KEY));
			mons = all_monitors;
		}
		if (mons != null) {
			selected_monitors = mons;
			monitors.clear();
			for (String string : mons) {
				try {
					ResourceMonitorIfc resMon =
									(ResourceMonitorIfc) Class.forName(string).newInstance();
					String monJid = getJID() + "/" + resMon.getClass().getSimpleName();
					resMon.init(monJid, warning_treshold, this);
					monitors.put(monJid, resMon);
					log.config("Loaded resource monitor: " + monJid);
				} catch (Exception ex) {
					log.log(Level.SEVERE,
									"Can't instantiate resource monitor: " + string, ex);
				}
			}
		}
	}

	public Map<String, PropertyItem> getParams() {
		Map<String, PropertyItem> props = super.getParams();
		props.put(MONITORS_CLASSES_PROP_KEY,
						new PropertyItem(MONITORS_CLASSES_PROP_KEY,
						MONITORS_CLASSES_PROP_KEY, selected_monitors, all_monitors,
						"List of system monitors available for use"));
		props.put(WARNING_TRESHOLD_PROP_KEY,
						new PropertyItem(WARNING_TRESHOLD_PROP_KEY,
						WARNING_TRESHOLD_PROP_KEY, warning_treshold));
//	log.fine("selected_monitors: " + Arrays.toString(selected_monitors) +
//						", all_monitors: " + Arrays.toString(all_monitors));
		return props;
	}

	@Override
	public Map<String, PropertyItem> getDefaultParams() {
		Map<String, PropertyItem> defs = super.getDefaultParams();
		defs.put(DESCRIPTION_PROP_KEY, new PropertyItem(DESCRIPTION_PROP_KEY,
						DESCRIPTION_DISPL_NAME, "System Monitor Task"));
		defs.put(MESSAGE_TYPE_PROP_KEY,
						new PropertyItem(MESSAGE_TYPE_PROP_KEY,
						MESSAGE_TYPE_DISPL_NAME, MessageType.NORMAL));
		defs.put(ONLINE_ONLY_PROP_KEY,
						new PropertyItem(ONLINE_ONLY_PROP_KEY,
						ONLINE_ONLY_DISPL_NAME, false));
		defs.put(REPLACE_SENDER_PROP_KEY,
						new PropertyItem(REPLACE_SENDER_PROP_KEY,
						REPLACE_SENDER_DISPL_NAME, SenderAddress.LEAVE));
		defs.put(SUBSCR_RESTRICTIONS_PROP_KEY,
						new PropertyItem(SUBSCR_RESTRICTIONS_PROP_KEY,
						SUBSCR_RESTRICTIONS_DISPL_NAME, SubscrRestrictions.MODERATED));
		defs.put(MONITORS_CLASSES_PROP_KEY,
						new PropertyItem(MONITORS_CLASSES_PROP_KEY,
						MONITORS_CLASSES_PROP_KEY, all_monitors, all_monitors,
						"List of system monitors available for use"));
		defs.put(WARNING_TRESHOLD_PROP_KEY,
						new PropertyItem(WARNING_TRESHOLD_PROP_KEY,
						WARNING_TRESHOLD_PROP_KEY, warning_treshold));
		return defs;
	}

	private void runCommand(Packet packet, Queue<Packet> results) {
		String body = packet.getElemCData("/message/body");
		String[] body_split = body.split(" |\n|\r");
		command comm = command.valueOf(body_split[0].substring(2));
		switch (comm) {
		case help:
			results.offer(Packet.getMessage(packet.getElemFrom(),
					packet.getElemTo(), StanzaType.chat, commandsHelp(),
					"Commands description", null));
			break;
			case state:
				StringBuilder sb = new StringBuilder("\n");
				for (ResourceMonitorIfc resmon : monitors.values()) {
					sb.append(resmon.getClass().getSimpleName() + ":\n");
					sb.append(resmon.getState() + "\n");
				}
				results.offer(Packet.getMessage(packet.getElemFrom(),
								packet.getElemTo(), StanzaType.chat, sb.toString(),
								"Monitors State", null));
			break;
		}
	}

	@Override
	protected void processMessage(Packet packet, Queue<Packet> results) {
		if (isPostCommand(packet)) {
			runCommand(packet, results);
		} else {
			String body = packet.getElemCData("/message/body");
			results.offer(Packet.getMessage(packet.getElemFrom(),
					packet.getElemTo(), StanzaType.normal,
					"This is response to your message: [" + body + "]",
					"Response", null));
		}
	}

}
