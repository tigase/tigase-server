/*
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
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;

import java.util.Optional;

@Bean(name = "meet", parent = PushNotifications.class, active = true)
public class MeetPushNotificationExtension implements PushNotificationsExtension, PushNotificationsFilter {

	@ConfigField(desc = "Always enable", alias = "push-meet-always-enabled")
	private boolean alwaysEnabled = false;

	@Override
	public void processEnableElement(Element enableEl, Element settingsEl) {
		Element meet = enableEl.getChild("meet", "tigase:push:meet:0");
		if (meet != null) {
			settingsEl.addChild(meet);
		}
	}

	@Override
	public void prepareNotificationPayload(Element pushServiceSettings, Packet packet, long msgCount,
										   Element notification) {
		if (packet == null || packet.getElemName() != Message.ELEM_NAME) {
			return;
		}
		Element actionEl = packet.getElement().findChild(el -> el.getXMLNS() == "tigase:meet:0");
		if (actionEl == null) {
			return;
		}
		String id = actionEl.getAttributeStaticStr("id");
		if (id == null) {
			return;
		}
		String jid = actionEl.getAttributeStaticStr("jid");
		if (jid == null) {
			return;
		}

		notification.withElement("meet", "tigase:push:meet:0", meet -> {
			meet.addAttribute("id", id);
			meet.addAttribute("jid", jid);
			Optional.ofNullable(actionEl.mapChildren(el -> el.getName() == "media",
								 el -> el.getAttributeStaticStr("type")))
					.ifPresent(mediaTypes -> mediaTypes.forEach(media -> meet.withElement("media", null, media)));
		});
	}

	@Override
	public boolean shouldSendNotification(Packet packet, BareJID userJid, XMPPResourceConnection session)
			throws XMPPException {
		if (packet.getElemName() != Message.ELEM_NAME) {
			return false;
		}
		Element actionEl = packet.getElement().findChild(el -> el.getXMLNS() == "tigase:meet:0");
		if (actionEl == null || actionEl.getAttributeStaticStr("id") == null) {
			return false;
		}
		switch (actionEl.getName()) {
			case "propose":
				return packet.getStanzaFrom() != null && !packet.getStanzaFrom().getBareJID().equals(userJid);
			default:
				return false;
		}
	}

	@Override
	public boolean isSendingNotificationAllowed(BareJID userJid, XMPPResourceConnection session,
												Element pushServiceSettings, Packet packet) {
		Element actionEl = packet.getElement().findChild(el -> el.getXMLNS() == "tigase:meet:0");
		if (actionEl == null) {
			return true;
		}

		if (alwaysEnabled) {
			return true;
		}

		Element meet = pushServiceSettings.getChild("meet", "tigase:push:meet:0");
		if (meet == null) {
			return false;
		}

		return true;
	}
}
