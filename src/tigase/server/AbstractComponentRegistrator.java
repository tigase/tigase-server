/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
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
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class AbstractComponentRegistrator
	implements ComponentRegistrator {

	private String name = null;
	private List<ServerComponent> components = new ArrayList<ServerComponent>();

	/**
	 * Creates a new <code>AbstractComponentRegistrator</code> instance.
	 *
	 */
	public AbstractComponentRegistrator() {}

	public boolean addComponent(ServerComponent component) {
		boolean result = components.add(component);
		if (result) {
			componentAdded(component);
		} // end of if (result)
		return result;
	}

	public abstract void componentAdded(ServerComponent component);

  /**
   *
   * @return tigase.server.ServerComponent
   */
	public boolean deleteComponent(ServerComponent component) {
		boolean result = components.remove(component);
		if (result) {
			componentRemoved(component);
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

	public abstract void componentRemoved(ServerComponent component);

} // AbstractComponentRegistrator
