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
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;

@Bean(name = "priority", parent = PushNotifications.class, active = true)
public class PriorityExtension implements PushNotificationsExtension {

	private static final String XMLNS = "tigase:push:priority:0";

	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[]{"var"}, new String[]{XMLNS}) };

	@Override
	public Element[] getDiscoFeatures() {
		return DISCO_FEATURES;
	}

	@Override
	public boolean shouldSendNotification(Packet packet, BareJID userJid, XMPPResourceConnection session)
			throws XMPPException {
		return false;
	}

	@Override
	public void processEnableElement(Element enableEl, Element settingsEl) {
		if (enableEl.getChild("priority", XMLNS) != null) {
			settingsEl.addAttribute("priority", "true");
		}
	}

	@Override
	public void prepareNotificationPayload(Element pushServiceSettings, Packet packet, long msgCount,
										   Element notification) {
		if (packet != null && Boolean.parseBoolean(pushServiceSettings.getAttributeStaticStr("priority"))) {
			Element element = new Element("priority", "high");
			element.setXMLNS(XMLNS);
			notification.addChild(element);
		}
	}
}
