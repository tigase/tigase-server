/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server;

import java.util.LinkedHashMap;
import java.util.Map;
import tigase.conf.ConfigurationException;

/**
 * This is an archetype of a special types of classes which collect some data
 * from Tigase components or provide these data to components. They normally
 * do not process normall packets and are usually accessed by admins via ad-hoc
 * commands. Good examples of such components are <code>StatisticsCollector</code>
 * or <code>Configurator</code>.<br>
 * Extensions of these class can process packets addresses to the component via
 * <code>processPacket(Packet packet, Queue&lt;Packet&gt; results)</code> method.
 * Alternatively scripting API can be used via ad-hoc commands.<br>
 * The class does not have any queues buffering packets or separate threads for
 * packets processing. All packets are processed from <code>MessageRouter</code>
 * threads via <code>processPacket(Packet packet, Queue&lt;Packet&gt; results)</code>
 * method. Hence this is important that processing implemented in extensions to the
 * class does not take long time. In particular no DB processing is expected.
 *
 *
 * Created: Tue Nov 22 22:57:44 2005
 *
 * @param <E>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class AbstractComponentRegistrator<E extends ServerComponent>
		extends BasicComponent implements ComponentRegistrator {
	private long packetId = 0;

	/**
	 * A collection of server components which implement special interface, related to the
	 * functionality provided by extension of the class.
	 */
	protected Map<String, E> components = new LinkedHashMap<String, E>();

	/**
	 * Creates a new <code>AbstractComponentRegistrator</code> instance.
	 *
	 */
	public AbstractComponentRegistrator() {}

	/**
	 * Method provides a callback mechanism signaling that a new component implementing
	 * special interface has been added to the internal <code>components</code> collection.
	 *
	 *
	 * @param component is a reference to the component just added to the collection.
	 */
	public abstract void componentAdded(E component) throws ConfigurationException;

	/**
	 * Method provides a callback mechanism signaling that a component implementing
	 * special interface has been removed from the internal <code>components</code>
	 * collection.
	 *
	 *
	 * @param component is a reference to the component removed from the collection.
	 */
	public abstract void componentRemoved(E component);

	/**
	 * Method checks whether the component provides as method parameter is correct type
	 * that is implements special interface or extends special class. Result of the method
	 * determines whether the component can be added to the internal <code>components</code>
	 * collection.
	 *
	 *
	 * @param component is a reference to the component being checked.
	 *
	 * @return a <code>boolean</code> value of <code>true</code> if the component is of a
	 * correct type and <code>false</code> otherwise.
	 */
	public abstract boolean isCorrectType(ServerComponent component);

	/**
	 * Method checks whether the component is of a correct type, adds it to the internal
	 * <code>components</code> collection and calls <code>componentAdded(...)</code> callback.
	 *
	 *
	 * @param component a reference to the component which is being added to the intenal
	 * collection.
	 *
	 * @return a <code>boolean</code> value of <code>true</code> if the component
	 * has been added to the internal collection and <code>false</code> otherwise.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean addComponent(ServerComponent component) throws ConfigurationException {
		if (isCorrectType(component)) {
			components.put(component.getName(), (E) component);
			componentAdded((E) component);

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Method removes specified component from the internal <code>components</code>
	 * collection and calls <code>componentRemoved(...)</code> callback method.
	 *
	 * @param component is a reference to the component being removed.
	 * @return a <code>boolean</code> value of <code>true</code> if the component
	 * has been removed from the internal collection and <code>false</code> otherwise.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean deleteComponent(ServerComponent component) {
		if (isCorrectType(component)) {
			components.remove(component.getName());
			componentRemoved((E) component);
		}

		return true;
	}

	/**
	 * Method returns a component for a specified component name from internal
	 * <code>components</code> collection or <code>null</code> of there is no such
	 * component in the collection.
	 *
	 *
	 * @param name is a <code>String</code> value of the component name.
	 *
	 * @return a reference to the component found in the internal collection or
	 * <code>null</code> if no component has been found.
	 */
	public E getComponent(String name) {
		return components.get(name);
	}

	/**
	 * Method generates and returns an unique packet ID. The ID is unique within running
	 * Tigase instance. The method can be overwritten to change the generation of the
	 * packet ID.
	 *
	 *
	 * @param prefix is a <code>String</code> value of the ID profix or <code>null</code>
	 * if no prefix is necessary.
	 *
	 * @return a <code>String</code> instance of a new packet ID.
	 */
	public String newPacketId(String prefix) {
		StringBuilder sb = new StringBuilder(32);

		if (prefix != null) {
			sb.append(prefix).append("-");
		}

		sb.append(getName()).append(++packetId);

		return sb.toString();
	}

	@Override
	public void release() {}
}    // AbstractComponentRegistrator
