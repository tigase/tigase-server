
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
*
* $Rev$
* Last modified by $Author$
* $Date$
 */
package tigase.server.test;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Sep 30, 2010 1:07:13 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class EchoComponent extends AbstractMessageReceiver {
	private static final Logger log = Logger.getLogger(EchoComponent.class.getName());

	//~--- methods --------------------------------------------------------------

	@Override
	public void processPacket(Packet packet) {
		log.log(Level.FINEST, "Received: {0}", packet);

		Packet result = packet.swapStanzaFromTo();

		addOutPacket(result);
		log.log(Level.FINEST, "Sent back: {0}", result);
	}
}
