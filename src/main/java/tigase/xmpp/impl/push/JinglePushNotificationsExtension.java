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
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;

import java.util.Objects;
import java.util.Optional;

@Bean(name = "jingle", parent = PushNotifications.class, active = true)
public class JinglePushNotificationsExtension implements PushNotificationsExtension, PushNotificationsFilter, PushNotificationsAware {

	private static final Element[] FEATURES = {
			new Element("feature", new String[]{"var"}, new String[]{"tigase:push:jingle:0"})};

	@Inject
	private EncryptedPushNotificationExtension encryptedPushNotificationExtension;

	@ConfigField(desc = "Always enable", alias = "push-jingle-always-enabled")
	private boolean alwaysEnabled = false;

	@Override
	public Element[] getDiscoFeatures() {
		return FEATURES;
	}

	@Override
	public void processEnableElement(Element enableEl, Element settingsEl) {
		Element jingle = enableEl.getChild("jingle", "tigase:push:jingle:0");
		if (jingle != null) {
			settingsEl.addChild(jingle);
		}
	}

	@Override
	public void prepareNotificationPayload(Element pushServiceSettings, Packet packet, long msgCount,
										   Element notification) {
		if (packet == null || packet.getElemName() != Message.ELEM_NAME) {
			return;
		}
		Element actionEl = packet.getElement().findChild(el -> el.getXMLNS() == "urn:xmpp:jingle-message:0");
		if (actionEl == null) {
			return;
		}
		String id = actionEl.getAttributeStaticStr("id");
		if (id == null) {
			return;
		}

		notification.withElement("jingle", "tigase:push:jingle:0", jingle -> {
			jingle.addAttribute("action", actionEl.getName());
			jingle.addAttribute("sid", id);
			Optional.ofNullable(actionEl.mapChildren(el -> Objects.equals(el.getName(), "description") && Objects.equals(el.getXMLNS(), "urn:xmpp:jingle:apps:rtp:1"),
								 el -> el.getAttributeStaticStr("media")))
					.ifPresent(mediaTypes -> mediaTypes.forEach(media -> jingle.withElement("media", null, media)));
			Optional.ofNullable(actionEl.mapChildren(el -> Objects.equals(el.getName(), "feature"), Element::getXMLNS))
					.ifPresent(features -> features.forEach(
							feature -> jingle.withElement("feature", el -> el.setXMLNS(feature))));
		});
	}

	@Override
	public boolean shouldSendNotification(Packet packet, BareJID userJid, XMPPResourceConnection session)
			throws XMPPException {
		if (packet.getElemName() != Message.ELEM_NAME) {
			return false;
		}
		Element actionEl = packet.getElement().findChild(el -> el.getXMLNS() == "urn:xmpp:jingle-message:0");
		if (actionEl == null || actionEl.getAttributeStaticStr("id") == null) {
			return false;
		}
		switch (actionEl.getName()) {
			case "retract":
				return false;
			case "propose":
				return packet.getStanzaFrom() != null && !packet.getStanzaFrom().getBareJID().equals(userJid);
			default:
				return packet.getStanzaFrom() != null && session != null && session.isUserId(packet.getStanzaFrom().getBareJID());
		}
	}

	@Override
	public boolean isSendingNotificationAllowed(BareJID userJid, XMPPResourceConnection session,
												Element pushServiceSettings, Packet packet) {
		Element actionEl = packet.getElement().findChild(el -> el.getXMLNS() == "urn:xmpp:jingle-message:0");
		if (actionEl == null) {
			return true;
		}

		if (alwaysEnabled) {
			return true;
		}
		
		Element jingle = pushServiceSettings.getChild("jingle", "tigase:push:jingle:0");
		if (jingle == null) {
			return false;
		}

		return true;
	}
	
}
