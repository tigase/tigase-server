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
package tigase.xmpp;

import java.util.Queue;
import java.util.Map;
import tigase.server.Packet;
import tigase.db.NonAuthUserRepository;

/**
 * Describe interface XMPPProcessorIfc here.
 *
 *
 * Created: Wed Feb 8 13:47:56 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface XMPPProcessorIfc extends XMPPImplIfc {

	/**
	 * @param packet   packet is which being processed. This parameter may never
	 *                 be null. Even though this is not immutable object it
	 *                 mustn't be altered. None of it's fields or attributes can
	 *                 be changed during processing.
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data. It allows
	 *                 for storing information in a permanent storage or in memory
	 *                 only during the live of the online session. This parameter
	 *                 can be null if there is no online user session at the time
	 *                 of the packet processing.
	 * @param repo     this is a user data storage which is normally used when the
	 *                 user session (parameter above) is null. This is repository
	 *                 allows for a very restricted access only. It allows for
	 *                 storing some user private data (doesn't allow overwriting
	 *                 existing data) like messages for offline users and it also
	 *                 allows for reading user public data like VCard.
	 * @param results  this a collection with packets which have been generated as
	 *                 input packet processing results. Regardless a response to a
	 *                 user request is sent or the packet is forwarded to it's
	 *                 destination it is always required that a copy of the input
	 *                 packet is created and stored in the results queue.
	 * @param settings this map keeps plugin specific settings loaded from the
	 *                 Tigase server configuration. In most cases it is unused,
	 *                 however if the plugin needs to access an external database
	 *                 that this is a way to pass database connection string to
	 *                 the plugin.
	 *
	 * @throws XMPPException
	 */
	void process( Packet packet, XMPPResourceConnection session,
								NonAuthUserRepository repo, Queue<Packet> results,
								Map<String, Object> settings ) throws XMPPException;
} // XMPPProcessorIfc
