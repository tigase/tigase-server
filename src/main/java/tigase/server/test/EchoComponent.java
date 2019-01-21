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
package tigase.server.test;

import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Sep 30, 2010 1:07:13 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class EchoComponent
		extends AbstractMessageReceiver {

	private static final Logger log = Logger.getLogger(EchoComponent.class.getName());

	@Override
	public void processPacket(Packet packet) {
		log.log(Level.FINEST, "Received: {0}", packet);

		Packet result = packet.swapStanzaFromTo();

		addOutPacket(result);
		log.log(Level.FINEST, "Sent back: {0}", result);
	}
}
