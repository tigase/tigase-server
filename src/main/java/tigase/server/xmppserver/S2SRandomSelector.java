/*
 * S2SRandomSelector.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



package tigase.server.xmppserver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Logger;
import java.util.Random;
import java.util.Set;

/**
 * Created: Jun 26, 2010 9:40:04 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class S2SRandomSelector
				implements S2SConnectionSelector {
	private static final Logger log = Logger.getLogger(S2SRandomSelector.class.getName());

	//~--- fields ---------------------------------------------------------------

	private Random rand = new Random();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param outgoing
	 *
	 *
	 *
	 * @return a value of S2SConnection
	 */
	@Override
	public S2SConnection selectConnection(Packet packet, Set<S2SConnection> outgoing) {
		int size = outgoing.size();

		if (size == 0) {
			return null;
		}

		int           pos    = rand.nextInt(size);
		S2SConnection result = null;
		int           i      = -1;

		for (S2SConnection s2SConnection : outgoing) {
			if (++i == pos) {
				result = s2SConnection;

				break;
			}
		}

		return result;
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
