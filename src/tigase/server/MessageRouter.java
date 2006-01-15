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

import java.util.logging.Logger;
import java.util.Queue;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

/**
 * Class MessageRouter
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageRouter extends AbstractMessageReceiver
	implements XMPPService {

  private static final Logger log =
    Logger.getLogger("tigase.server.MessageRouter");

	private ComponentRegistrator config = null;

	private Map<String, ServerComponent> components =
		new TreeMap<String, ServerComponent>();
	private Map<String, ComponentRegistrator> registrators =
		new TreeMap<String, ComponentRegistrator>();
	private Map<String, MessageReceiver> receivers =
		new TreeMap<String, MessageReceiver>();

	public Queue<Packet> processPacket(Packet packet) {
		return null;
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

	private boolean inProperties = false;
	public void setProperties(Map<String, Object> props) {

		if (inProperties) {
			return;
		} // end of if (inProperties)
		else {
			inProperties = true;
		} // end of if (inProperties) else

		super.setProperties(props);
		Map<String, ComponentRegistrator> tmp_reg = registrators;
		Map<String, MessageReceiver> tmp_rec = receivers;
		components = new TreeMap<String, ServerComponent>();
		registrators = new TreeMap<String, ComponentRegistrator>();
		receivers = new TreeMap<String, MessageReceiver>();
		setConfig(config);

		MessageRouterConfig conf = new MessageRouterConfig(props);
		String[] reg_names = conf.getRegistrNames();
		for (String name: reg_names) {
			ComponentRegistrator cr = tmp_reg.get(name);
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

		String[] msgrcv_names = conf.getMsgRcvNames();
		for (String name: msgrcv_names) {
			MessageReceiver mr = tmp_rec.get(name);
			try {
				if (mr == null) {
					mr = conf.getMsgRcvInstance(name);
					mr.setName(name);
					mr.setParent(this);
				} // end of if (cr == null)
				addRouter(mr);
			} // end of try
			catch (Exception e) {
				e.printStackTrace();
			} // end of try-catch
		} // end of for (String name: reg_names)

		inProperties = false;
	}

}
