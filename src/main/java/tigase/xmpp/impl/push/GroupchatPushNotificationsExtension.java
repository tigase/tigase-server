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
import tigase.server.Message;
import tigase.server.Packet;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.MessageDeliveryProviderIfc;
import tigase.xmpp.jid.BareJID;

@Bean(name = "groupchat", parent = PushNotifications.class, active = true)
public class GroupchatPushNotificationsExtension implements PushNotificationsExtension {

	@Inject
	protected MessageDeliveryProviderIfc message;

	@Override
	public boolean shouldSendNotification(Packet packet, BareJID userJid, XMPPResourceConnection session)
			throws XMPPException {
		if (packet.getElemName() != Message.ELEM_NAME || packet.getType() != StanzaType.groupchat) {
			return false;
		}

		if (session == null || !this.message.hasConnectionForMessageDelivery(session)) {
			return false;
		}
		if (packet.getStanzaTo() == null) {
			return false;
		}

		return session.isUserId(packet.getStanzaTo().getBareJID()) && packet.getStanzaTo().getResource() == null &&
				packet.getElement().findChild(el -> el.getName() == "mix" && el.getXMLNS() == "urn:xmpp:mix:core:1") ==
						null;
	}
}
