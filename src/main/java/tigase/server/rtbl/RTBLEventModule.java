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
package tigase.server.rtbl;

import tigase.component.exceptions.ComponentException;
import tigase.component.modules.AbstractModule;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.criteria.Or;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.util.List;

@Bean(name = "rtblEventModule", parent = RTBLComponent.class, active = true)
public class RTBLEventModule
		extends AbstractModule {

	private static final String PUBSUB_XMLNS = "http://jabber.org/protocol/pubsub";
	private static final String PUBSUB_EVENT_XMLNS = PUBSUB_XMLNS + "#event";

	public Criteria criteria = ElementCriteria.name("message")
			.add(ElementCriteria.name("event", PUBSUB_EVENT_XMLNS).add(new Or(ElementCriteria.name("items"),ElementCriteria.name("purge"))));

	@Inject
	private RTBLRepository repository;

	@Override
	public Criteria getModuleCriteria() {
		return criteria;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		Element event = packet.getElemChild("event", PUBSUB_EVENT_XMLNS);
		JID from = packet.getStanzaFrom();
		if (from == null) {
			return;
		}
		Element items = event.getChild("items");
		if (items == null) {
			Element purge = event.getChild("purge");
			if (purge == null) {
				return;
			}
			String node = event.getAttributeStaticStr("node");
			if (node != null) {
				repository.purge(from.getBareJID(), node);
			}
		}
		String node = items.getAttributeStaticStr("node");
		if (node == null) {
			return;
		}
		List<Element> notifications = items.getChildren();
		if (notifications == null) {
			return;
		}

		for (Element notification : notifications) {
			String id = notification.getAttributeStaticStr("id");
			if (id == null) {
				continue;
			}
			switch (notification.getName()) {
				case "retract" -> repository.update(from.getBareJID(), node, RTBLRepository.Action.remove, id);
				case "item" -> repository.update(from.getBareJID(), node, RTBLRepository.Action.add, id);
				default -> {}
			}
		}
	}
}
