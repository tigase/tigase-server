/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.server;

import tigase.conf.ConfigurationException;

/**
 * Interface ComponentRegistrator
 * <br>
 * Collects information about all ServerComponents connected to MessageRouter
 * <br>
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ComponentRegistrator
		extends ServerComponent {

	/**
	 * @param component
	 *
	 * @return a <code>boolean</code> value indicating whehether component has been successfuly added or not.
	 */
	boolean addComponent(ServerComponent component) throws ConfigurationException;

	/**
	 * @param component
	 *
	 * @return a <code>boolean</code> value indicating whehether component has been successfuly removed or not.
	 */
	boolean deleteComponent(ServerComponent component);

}
