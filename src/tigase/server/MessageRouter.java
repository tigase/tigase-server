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

	private List<ServerComponent> components =
		new ArrayList<ServerComponent>();
	private List<ComponentRegistrator> registrators =
		new ArrayList<ComponentRegistrator>();
	private List<MessageReceiver> receivers = new ArrayList<MessageReceiver>();

	public MessageRouter() {
		super(null);
		components.add(this);
	}

	public Queue<Packet> processPacket(Packet packet) {
		return null;
	}

	public void addRegistrator(ComponentRegistrator registr) {
		log.info("Adding registrator: " + registr.getClass().getSimpleName());
		registrators.add(registr);
		addComponent(registr);
		for (ServerComponent comp : components) {
			if (comp != registr) {
				registr.addComponent(comp);
			} // end of if (comp != registr)
		} // end of for (ServerComponent comp : components)
	}

	public void addRouter(MessageReceiver receiver) {
		log.info("Adding receiver: " + receiver.getClass().getSimpleName());
		addComponent(receiver);
		receivers.add(receiver);
	}

	public void addComponent(ServerComponent component) {
		log.info("Adding component: " + component.getClass().getSimpleName());
		for (ComponentRegistrator registr : registrators) {
			if (registr != component) {
				registr.addComponent(component);
			} // end of if (reg != component)
		} // end of for ()
		components.add(component);
	}

}

