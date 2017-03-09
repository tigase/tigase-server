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
package tigase.server.sreceiver;

import java.util.Map;
import tigase.server.Command;
import tigase.xml.XMLUtils;
import tigase.xml.Element;
import tigase.server.Packet;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

import static tigase.server.sreceiver.PropertyConstants.*;

/**
 * Describe class TaskCommons here.
 *
 *
 * Created: Mon May 21 08:31:25 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class TaskCommons {

	public static void propertyItems2Command(Map<String, PropertyItem> props,
		Packet result) {
		for (Map.Entry<String, PropertyItem> entry: props.entrySet()) {
			if (!entry.getKey().equals(USER_REPOSITORY_PROP_KEY)) {
				PropertyItem item = entry.getValue();
				if (item.isMultiValue()) {
					if (item.getPossible_values() != null) {
						Command.addFieldValue(result,
										XMLUtils.escape(item.getName()),
										(String[])item.getValue(),
										XMLUtils.escape(item.getDisplay_name()),
										item.getPossible_values(), item.getPossible_values());
					} else {
						Command.addFieldValue(result,
										XMLUtils.escape(item.getName()),
										XMLUtils.escape(item.toString()),
										"text-single", XMLUtils.escape(item.getDisplay_name()));
					} // end of if (item.getPossible_values() != null) else
				} else {
					if (item.getPossible_values() != null) {
						Command.addFieldValue(result,
										XMLUtils.escape(item.getName()),
										XMLUtils.escape(item.toString()),
										XMLUtils.escape(item.getDisplay_name()),
										item.getPossible_values(), item.getPossible_values());
					} else {
						Command.addFieldValue(result,
										XMLUtils.escape(item.getName()),
										XMLUtils.escape(item.toString()),
										"text-single", XMLUtils.escape(item.getDisplay_name()));
					} // end of if (item.getPossible_values() != null) else
				}
			} // end of if (!entry.getKey().equals(USER_REPOSITORY_PROP_KEY))
		} // end of for (Map.Entry entry: prop.entrySet())
	}

	public static Packet getPresence(JID from, JID to, StanzaType type,
		String nick, String status) {
		Element presence = new Element("presence",
			new String[] {"to", "from", "type"},
			new String[] {to.toString(), from.toString(), type.toString()});
		if (nick != null) {
			//<x xmlns="vcard-temp:x:update"><nickname>tus</nickname></x>
			//<nick xmlns="http://jabber.org/protocol/nick">tus</nick>
			presence.addChild(new Element("nick", nick,
					new String[] {"xmlns"},
					new String[] {"http://jabber.org/protocol/nick"}));
		}
		if (status != null) {
			presence.addChild(new Element("status", status));
		}
		return Packet.packetInstance(presence, from, to);
	}

	public static Packet getPresence(JID from, JID to, StanzaType type) {
		return getPresence(to, from, type, null, null);
	}

	public static Packet getMessage(JID from, JID to, StanzaType type,
		String body) {
		Element message = new Element("message",
			new Element[] {
				new Element("subject", "Automatic system message"),
				new Element("body", body)},
			new String[] {"to", "from", "type"},
			new String[] {to.toString(), from.toString(), type.toString()});
		return Packet.packetInstance(message, from, to);
	}

	public static boolean parseBool(final Object val) {
		return val != null &&
			(val.toString().equalsIgnoreCase("yes")
				|| val.toString().equalsIgnoreCase("true")
				|| val.toString().equalsIgnoreCase("on"));
	}

} // TaskCommons
