/*
 * Notify.java
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



package tigase.server.amp.action;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.amp.ActionAbstract;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.PacketErrorTypeException;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Apr 27, 2010 5:36:03 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Notify
				extends ActionAbstract {
	private static final String name = "notify";

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param rule
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean execute(Packet packet, Element rule) {
		try {
			Packet result = prepareAmpPacket(packet, rule);

			resultsHandler.addOutPacket(result);
		} catch (PacketErrorTypeException ex) {

			// Ignore
		}

		return true;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getName() {
		return name;
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
