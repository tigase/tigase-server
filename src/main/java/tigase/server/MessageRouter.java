/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;
import tigase.xml.Element;
import tigase.util.JID;
import tigase.xmpp.Authorization;
// import tigase.disco.XMPPService;
// import tigase.disco.ServiceEntity;
// import tigase.disco.ServiceIdentity;

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
public class MessageRouter extends AbstractMessageReceiver {
	//	implements XMPPService {

  private static final Logger log =
    Logger.getLogger("tigase.server.MessageRouter");

	private String defHostName = null;
	private static Set<String> localAddresses =	new CopyOnWriteArraySet<String>();

  private ComponentRegistrator config = null;

  private Map<String, ServerComponent> components =
    new ConcurrentSkipListMap<String, ServerComponent>();
  private Map<String, ComponentRegistrator> registrators =
    new ConcurrentSkipListMap<String, ComponentRegistrator>();
  private Map<String, MessageReceiver> receivers =
    new ConcurrentSkipListMap<String, MessageReceiver>();

	public void processPacket(final Packet packet, final Queue<Packet> r) {
		String to = packet.getTo();
		Queue<Packet> results = new LinkedList<Packet>();
		for (ServerComponent comp: components.values()) {
			if (comp != this) {
				comp.processPacket(packet, results);
			} // end of if (comp != this)
		} // end of for ()
		for (Packet res: results) {
			processPacket(res);
		} // end of for ()

		if (!to.startsWith(getName())) return;

		if (packet.getPermissions() != Permissions.ADMIN) {
			Packet res = Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
				"You are not authorized for this action.", true);
			processPacket(res);
			return;
		}

		log.finest("Command received: " + packet.getStringData());
		switch (packet.getCommand()) {
		case OTHER:
			if (packet.getStrCommand() != null) {
				if (packet.getStrCommand().startsWith("controll/")) {
					String[] spl = packet.getStrCommand().split("/");
					String cmd = spl[1];
					if (cmd.equals("stop")) {
						Packet result = packet.commandResult("result");
						processPacket(result);
						new Timer("Stopping...", true).schedule(new TimerTask() {
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

	private boolean isToLocalComponent(String jid) {
		for (String name: components.keySet()) {
			for (String hostname: localAddresses) {
				if (jid.equals(name + "." + hostname)) {
					return true;
				}
			}
		} // end of for ()
		return false;
	}

	public static boolean isLocalDomain(String domain) {
		return localAddresses.contains(domain);
	}

	public void processPacket(Packet packet) {

		if (packet.getTo() == null) {
			log.warning("Packet with TO attribute set to NULL: "
				+	packet.getStringData());
			return;
		} // end of if (packet.getTo() == null)

		log.finer("Processing packet: " + packet.getElemName()
			+ ", type: " + packet.getType());
		log.finest("Processing packet: " + packet.getStringData()
			+ ", to: " + packet.getTo()
			+ ", from: " + packet.getFrom());

		if (localAddresses.contains(packet.getTo())
			|| isToLocalComponent(packet.getTo())) {
			log.finest("This packet is addressed to server itself.");
			processPacket(packet, null);
			return;
		}

		String host = JID.getNodeHost(packet.getTo());
		String id =  JID.getNodeID(packet.getTo());
		String nick = JID.getNodeNick(packet.getTo());
		// Let's try to find message receiver quick way
		// In case if packet is handled internally:
		MessageReceiver first = null;
		if (nick != null) {
			first = receivers.get(nick);
		} // end of if (nick != null)
		if (first != null) {
			// Well, I found something. Now we need to make sure it is
			// indeed to this receiver and it is not just accidental
			// nick name match, so we are checking routing hosts.
			Set<String> routings = first.getRoutings();
			if (routings != null) {
				log.finest(first.getName() + ": Looking for host: " + host
					+ " in " + routings.toString());
				if (routings.contains(host)) {
					log.finest("Found receiver: " + first.getName());
					first.addPacket(packet);
					return;
				} // end of if (routings.contains())
			} // end of if (routings != null)
			else {
				log.severe("Routings are null for: " + first.getName());
			} // end of if (routings != null) else
		} // end of if (mr != null)
		// This packet is not processed localy, so let's find receiver
		// which will send it to correct destination:
		MessageReceiver s2s = null;
		for (MessageReceiver mr: receivers.values()) {
			Set<String> routings = mr.getRoutings();
			if (routings != null) {
				log.finest(mr.getName() + ": Looking for host: " + host +
					" in " + routings.toString());
				if (routings.contains(host) || routings.contains(id)) {
					log.finest("Found receiver: " + mr.getName());
					mr.addPacket(packet);
					return;
				} // end of if (routings.contains())
				if (routings.contains("*")) {
					// I found s2s receiver, remember it for later....
					s2s = mr;
				} // end of if (routings.contains())
			} // end of if (routings != null)
			else {
				log.severe("Routings are null for: " + mr.getName());
			} // end of if (routings != null) else
		} // end of for (MessageReceiver mr: receivers.values())
		// It is not for any local host, so maybe it is for some
		// remote server, let's try sending it through s2s service:
		if (s2s != null) {
			s2s.addPacket(packet);
		} // end of if (s2s != null)
  }

  public void setConfig(ComponentRegistrator config) {
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
				log.finer("Adding: " + component.getName() + " component to "
					+ registr.getName() + " registrator.");
				registr.addComponent(component);
      } // end of if (reg != component)
    } // end of for ()
    components.put(component.getName(), component);
  }

  public Map<String, Object> getDefaults(Map<String, Object> params) {
    Map<String, Object> defs = super.getDefaults(params);
    MessageRouterConfig.getDefaults(defs, params, getName());
    return defs;
  }

  private boolean inProperties = false;
  public void setProperties(Map<String, Object> props) {

    if (inProperties) {
      return;
    } else {
      inProperties = true;
    } // end of if (inProperties) else

    try {
      super.setProperties(props);
			String[] localAddresses = (String[])props.get(LOCAL_ADDRESSES_PROP_KEY);
			this.localAddresses.clear();
			if (localAddresses != null && localAddresses.length > 0) {
				defHostName = localAddresses[0];
				Collections.addAll(this.localAddresses, localAddresses);
			} else {
				defHostName = "localhost";
			} // end of else
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
				log.finer("Loading and registering message receiver: " + name);
				MessageReceiver mr = tmp_rec.remove(name);
				String cls_name =
					(String)props.get(MSG_RECEIVERS_PROP_KEY + name + ".class");
				try {
					if (mr == null || !mr.getClass().getName().equals(cls_name)) {
						if (mr != null) {
							mr.release();
						}
						mr = conf.getMsgRcvInstance(name);
						mr.setParent(this);
						mr.setName(name);
						mr.start();
					} // end of if (cr == null)
					addRouter(mr);
				} // end of try
				catch (Exception e) {
					e.printStackTrace();
				} // end of try-catch
      } // end of for (String name: reg_names)
			for (MessageReceiver mr: tmp_rec.values()) {
				mr.release();
			} // end of for ()
			tmp_rec.clear();
    } finally {
      inProperties = false;
    } // end of try-finally
  }

	public String getDefHostName() {
		return defHostName;
	}

// 	public Element getDiscoInfo(String node, String jid) {
// 		if (jid != null && localAddresses.contains(jid)) {
// 			Element query = serviceEntity.getDiscoInfo(null);
// 			log.finest("Returing disco-info: " + query.toString());
// 			return query;
// 		}
// 		return null;
// 	}

// 	public List<Element> getDiscoItems(String node, String jid) {
// 		return null;
// 	}

}
