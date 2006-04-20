/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;
import static tigase.server.MessageRouterConfig.*;
import tigase.util.JID;

/**
 * Class MessageRouter
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageRouter extends AbstractMessageReceiver {
	//  implements XMPPService {

  private static final Logger log =
    Logger.getLogger("tigase.server.MessageRouter");

	private String defHostName = null;
	private Set<String> localAddresses = new ConcurrentSkipListSet<String>();

  private ComponentRegistrator config = null;

  private Map<String, ServerComponent> components =
    new ConcurrentSkipListMap<String, ServerComponent>();
  private Map<String, ComponentRegistrator> registrators =
    new ConcurrentSkipListMap<String, ComponentRegistrator>();
  private Map<String, MessageReceiver> receivers =
    new ConcurrentSkipListMap<String, MessageReceiver>();

	public void processCommand(final Packet packet, final Queue<Packet> r) {
		Queue<Packet> results = new LinkedList<Packet>();
		for (ServerComponent comp: components.values()) {
			if (comp != this) {
				comp.processCommand(packet, results);
			} // end of if (comp != this)
		} // end of for ()
		for (Packet res: results) {
			processPacket(res);
		} // end of for ()
	}

  public void processPacket(Packet packet) {
		log.finer("Processing packet: " + packet.getElemName()
			+ ", type: " + packet.getType());
		log.finest("Processing packet: " + packet.getStringData()
			+ ", to: " + packet.getTo()
			+ ", from: " + packet.getFrom());

		if (packet.isCommand() && localAddresses.contains(packet.getTo())) {
			processCommand(packet, null);
			return;
		} // end of if (packet.isCommand() && localAddresses.contains(packet.getTo()))

		String host = JID.getNodeHost(packet.getTo());
		String ip = null;
		try {
			ip = InetAddress.getByName(host).getHostAddress();
		} catch (UnknownHostException e) {
			ip = host;
		} // end of try-catch
		String nick = JID.getNodeNick(packet.getTo());
		// Let's try to find message receiver quick way
		// In case if packet is handled internally:
		MessageReceiver first = receivers.get(nick);
		if (first != null) {
			// Well, I found something. Now we need to make sure it is
			// indeed to this receiver and it is not just accidental
			// nick name match, so we are checking routing hosts.
			Set<String> routings = first.getRoutings();
			if (routings != null) {
				log.finest(first.getName() + ": Looking for host: " + host +
					" or ip: " + ip + " in " + routings.toString());
				if (routings.contains(host) || routings.contains(ip)) {
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
					" or ip: " + ip + " in " + routings.toString());
				if (routings.contains(host) || routings.contains(ip)) {
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

		// 		int idx = Arrays.binarySearch(localAddresses, host);
		// 		if (idx >= 0) {
		// 			MessageReceiver mr = receivers.get(nick);
		// 			if (mr != null) {
		// 				mr.addPacket(packet);
		// 			} // end of if (mr != null)
		// 		} // end of if (idx >= 0)
		// 		else {
		// 			log.info("This packet is not to local server: " +
		// 				packet.getStringData());
		// 		} // end of else
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
				registr.addComponent(component);
      } // end of if (reg != component)
    } // end of for ()
    components.put(component.getName(), component);
  }

  public Map<String, Object> getDefaults() {
    Map<String, Object> defs = super.getDefaults();
    MessageRouterConfig.getDefaults(defs);
    return defs;
  }

//   public void run() {
//     while (! stopped) {
//       try {
// 				synchronized(this) { this.wait(); }
//       } catch (InterruptedException e) { } // end of try-catch
// 			Packet packet = null;
// 			while ((packet = inQueue.poll()) != null) {
// 				processPacket(packet);
// 			} // end of while ((packet = inQueue.poll()) != null)
//     } // end of while (! stopped)
//   }

  private boolean inProperties = false;
  public void setProperties(Map<String, Object> props) {

    if (inProperties) {
      return;
    } // end of if (inProperties)
    else {
      inProperties = true;
    } // end of if (inProperties) else

    try {
      super.setProperties(props);
			String[] localAddresses = (String[])props.get(LOCAL_ADDRESSES_PROP_KEY);
			this.localAddresses.clear();
			if (localAddresses != null && localAddresses.length > 0) {
				defHostName = localAddresses[0];
				for (String host: localAddresses) {
					this.localAddresses.add(host);
				} // end of for ()
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
				try {
					if (cr == null) {
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
				MessageReceiver mr = tmp_rec.remove(name);
				try {
					if (mr == null) {
						mr = conf.getMsgRcvInstance(name);
						mr.setParent(this);
						mr.setName(name);
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
			for (MessageReceiver mr: receivers.values()) {
				mr.start();
			} // end of for (MessageReceiver mr: receivers)
    } finally {
      inProperties = false;
    } // end of try-finally
  }

	public String getDefHostName() {
		return defHostName;
	}

}
