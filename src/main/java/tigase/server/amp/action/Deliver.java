/*
 * Deliver.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

import java.util.List;
import tigase.server.amp.ActionAbstract;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.JID;

/**
 * Created: May 1, 2010 11:28:40 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Deliver
				extends ActionAbstract {
	private static final String name = "deliver";

	//~--- methods --------------------------------------------------------------

	@Override
	public boolean execute(Packet packet, Element rule) {
		Packet result     = packet.copyElementOnly();
		if (packet.getAttributeStaticStr(FROM_CONN_ID) == null)
			result.setPacketFrom(packet.getPacketTo());	
		removeTigasePayload(result);	
		resultsHandler.addOutPacket(result);
		return true;
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getName() {
		return name;
	}
}
