/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

package tigase.server.test;

import java.util.logging.Logger;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;

/**
 * A test component used to demonstrate API and for running different kinds of
 * tests on the Tigase server - generate local traffic for performance and load tests.
 * Created: Nov 28, 2009 9:22:36 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TestComponent extends AbstractMessageReceiver {

	private static final Logger log =
			Logger.getLogger(TestComponent.class.getName());

	@Override
	public void processPacket(Packet packet) {
		if (packet != null) {
			log.finest("My packet: " + packet.toString());
		}
	}

}
