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

import tigase.conf.ConfigurationException;

/**
 * Interface ComponentRegistrator
 *
 * Collects information about all ServerComponents connected to MessageRouter
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ComponentRegistrator extends ServerComponent {

  /**
   *
   * @param component
   * @return a <code>boolean</code> value indicating whehether component has
   * been successfuly added or not.
   */
	boolean addComponent(ServerComponent component) throws ConfigurationException;

  /**
   *
	 * @param component 
	 * @return a <code>boolean</code> value indicating whehether component has
   * been successfuly removed or not.
   */
	boolean deleteComponent(ServerComponent component);

}
