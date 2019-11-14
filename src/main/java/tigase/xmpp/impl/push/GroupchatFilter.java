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
package tigase.xmpp.impl.push;

import tigase.kernel.beans.Bean;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;

import java.util.List;

import static tigase.xmpp.impl.push.GroupchatFilter.ID;

@Bean(name = ID, parent = PushNotifications.class, active = true)
public class GroupchatFilter
		implements PushNotificationsFilter {

	public static final String XMLNS = "tigase:push:muc:0";
	public static final String ID = "groupchat-filter";
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[]{"var"}, new String[]{XMLNS}) };

	@Override
	public Element[] getDiscoFeatures() {
		return DISCO_FEATURES;
	}

	@Override
	public void processEnableElement(Element enableEl, Element settingsEl) {
		Element allowNotMentionedEl = enableEl.getChild("muc", XMLNS);
		if (allowNotMentionedEl != null) {
			settingsEl.addChild(allowNotMentionedEl);
		}
	}

	@Override
	public boolean isSendingNotificationAllowed(BareJID userJid, XMPPResourceConnection session,
												Element pushServiceSettings, Packet packet) {
		if (packet.getType() != StanzaType.groupchat) {
			return true;
		}

		Element mucEl = pushServiceSettings.getChild("muc", XMLNS);
		if (mucEl == null) {
			return true;
		}

		List<Element> rooms = mucEl.getChildren();
		if (rooms == null) {
			return false;
		}

		for (Element room : rooms) {
			String jidStr = room.getAttributeStaticStr("jid");
			if (jidStr == null) {
				continue;
			}

			if (!packet.getStanzaFrom().getBareJID().toString().equals(jidStr)) {
				continue;
			}

			String when = room.getAttributeStaticStr("when");
			if (when == null) {
				return false;
			}

			switch (when) {
				case "always":
					return true;
				case "mentioned":
					String nick = room.getAttributeStaticStr("nick");
					if (nick == null) {
						return false;
					}
					return isMentioned(packet, nick);
				default:
					return false;
			}
		}

		return false;
	}

	protected boolean isMentioned(Packet packet, String nick) {
		String body = packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH);
		if (body == null) {
			return false;
		}
		return body.contains(nick);
	}
}
