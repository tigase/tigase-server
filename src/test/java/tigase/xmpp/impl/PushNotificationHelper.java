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
package tigase.xmpp.impl;

import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

/**
 * Created by andrzej on 03.01.2017.
 */
public class PushNotificationHelper {

	public static Element createNotification(long messageCount, JID lastMessageSender, String lastMessageBody) {
		return new Element("notification",
						   new Element[]{createNotificationForm(messageCount, lastMessageSender, lastMessageBody)},
						   new String[]{"xmlns"}, new String[]{"urn:xmpp:push:0"});
	}

	public static Element createNotificationForm(long messageCount, JID lastMessageSender, String lastMessageBody) {
		Element[] fields = new Element[]{
				new Element("field", new Element[]{new Element("value", "urn:xmpp:push:summary")}, new String[]{"var"},
							new String[]{"FORM_TYPE"}),
				new Element("field", new Element[]{new Element("value", String.valueOf(messageCount))},
							new String[]{"var"}, new String[]{"message-count"}),
				new Element("field", new Element[]{new Element("value", lastMessageSender.toString())},
							new String[]{"var"}, new String[]{"last-message-sender"}),
				new Element("field", new Element[]{new Element("value", lastMessageBody)}, new String[]{"var"},
							new String[]{"last-message-body"})};
		return new Element("x", fields, new String[]{"xmlns"}, new String[]{"jabber:x:data"});
	}

	public static Packet createPushNotification(JID serviceJid, JID userJid, String node, Element notification) {
		Element iq = new Element("iq", new Element[]{new Element("pubsub", new Element[]{
				new Element("publish", new Element[]{new Element("item", new Element[]{notification}, null, null)},
							new String[]{"node"}, new String[]{node})}, new String[]{"xmlns"},
																 new String[]{"http://jabber.org/protocol/pubsub"})},
								 new String[]{"type"}, new String[]{"set"});
		return Packet.packetInstance(iq, JID.jidInstanceNS(userJid.getDomain()), serviceJid);
	}

}
