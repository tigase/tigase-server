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
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;

import java.util.List;

import static tigase.xmpp.impl.push.MutedFilter.ID;

@Bean(name = ID, parent = PushNotifications.class, active = true)
public class MutedFilter implements PushNotificationsFilter {

	public static final String XMLNS = "tigase:push:filter:muted:0";
	public static final String ID = "muted-filter";
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[]{"var"}, new String[]{XMLNS}) };

	@Override
	public Element[] getDiscoFeatures() {
		return DISCO_FEATURES;
	}

	@Override
	public void processEnableElement(Element enableEl, Element settingsEl) {
		Element el = enableEl.getChild("muted", XMLNS);
		if (el != null) {
			settingsEl.addChild(el);
		}
	}

	@Override
	public boolean isSendingNotificationAllowed(BareJID userJid, XMPPResourceConnection session,
												Element pushServiceSettings, Packet packet) {
		Element mutedEl = pushServiceSettings.getChild("muted", XMLNS);
		if (mutedEl == null) {
			return true;
		}
		List<BareJID> list = mutedEl.mapChildren(child -> child.getAttributeStaticStr("jid") != null,
												 child -> BareJID.bareJIDInstanceNS(
														 child.getAttributeStaticStr("jid")));
		if (list == null) {
			return true;
		}

		return !list.contains(packet.getStanzaFrom().getBareJID());
	}
}
