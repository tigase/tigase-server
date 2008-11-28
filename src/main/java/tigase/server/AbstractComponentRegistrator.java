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

import java.util.Map;
import java.util.LinkedHashMap;
import tigase.util.JIDUtils;
import tigase.util.DNSResolver;

/**
 * Describe class AbstractComponentRegistrator here.
 *
 *
 * Created: Tue Nov 22 22:57:44 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class AbstractComponentRegistrator<E extends ServerComponent>
	implements ComponentRegistrator {

	private String name = null;
	private String componentId = null;
	protected Map<String, E> components = new LinkedHashMap<String, E>();

	/**
	 * Creates a new <code>AbstractComponentRegistrator</code> instance.
	 *
	 */
	public AbstractComponentRegistrator() {}

	public abstract boolean isCorrectType(ServerComponent component);

	@SuppressWarnings("unchecked")
	public boolean addComponent(ServerComponent component) {
		if (isCorrectType(component)) {
			components.put(component.getName(), (E)component);
			componentAdded((E)component);
			return true;
		} else {
			return false;
		}
	}

	public E getComponent(String name) {
		return components.get(name);
	}

	public abstract void componentAdded(E component);

  /**
   *
   * @return tigase.server.ServerComponent
   */
  @SuppressWarnings("unchecked")
	public boolean deleteComponent(ServerComponent component) {
		components.remove(component.getName());
		componentRemoved((E)component);
		return true;
	}

	public void release() {}

	public void setName(String name) {
		this.name = name;
		this.componentId = JIDUtils.getNodeID(name, DNSResolver.getDefaultHostname());
	}

	public String getName() {
		return name;
	}

	public String getComponentId() {
		return componentId;
	}

	@Override
	public void initializationCompleted() {}

	public abstract void componentRemoved(E component);

} // AbstractComponentRegistrator
