/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 */
package tigase.component.modules;

import tigase.component.exceptions.ComponentException;
import tigase.criteria.Criteria;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;

/**
 * Basic interface to implement component module. Single module should implement
 * fragment of component logic. Is easy to add or remove in component (Server)
 * configuration.
 * 
 * @author bmalkow
 * 
 */
public interface Module {

	/**
	 * Returns XMPP features offered by module. Features will be returned by
	 * Service Discovery.
	 * 
	 * @return array of features or <code>null</code>.
	 */
	String[] getFeatures();

	/**
	 * Returns critera used by Component to select module to handle incoming
	 * stanza.
	 * 
	 * @return criteria of selecting module.
	 */
	Criteria getModuleCriteria();

	/**
	 * Process incoming stanza.
	 * 
	 * @param packet
	 *            received {@link Packet stanza}.
	 * @throws ComponentException
	 *             if stanza can't be processed correctly. ComponentException is
	 *             converted to error stanza and returned to stanza sender.
	 * @throws TigaseStringprepException
	 *             if there was an error during stringprep processing.
	 */
	void process(final Packet packet) throws ComponentException, TigaseStringprepException;

}
