/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
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

import java.util.List;
import java.util.ArrayList;

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
	protected List<E> components = new ArrayList<E>();

	/**
	 * Creates a new <code>AbstractComponentRegistrator</code> instance.
	 *
	 */
	public AbstractComponentRegistrator() {}


	public abstract boolean isCorrectType(ServerComponent component);

	@SuppressWarnings("unchecked")
	public boolean addComponent(ServerComponent component) {
		if (isCorrectType(component)) {
			boolean result = components.add((E)component);
			if (result) {
				componentAdded((E)component);
			} // end of if (result)
			return result;
		} else {
			return false;
		}
	}

	public abstract void componentAdded(E component);

  /**
   *
   * @return tigase.server.ServerComponent
   */
  @SuppressWarnings("unchecked")
	public boolean deleteComponent(ServerComponent component) {
		boolean result = components.remove(component);
		if (result) {
			componentRemoved((E)component);
		} // end of if (result)
		return result;
	}

	public void release() {}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public abstract void componentRemoved(E component);

} // AbstractComponentRegistrator
