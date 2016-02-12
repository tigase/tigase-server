/*
 * EventDiscoveryModule.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.eventbus.component;

import java.util.*;

import tigase.component.exceptions.ComponentException;
import tigase.component.exceptions.RepositoryException;
import tigase.component.modules.impl.DiscoveryModule;
import tigase.eventbus.EventName;
import tigase.eventbus.EventsRegistrar;
import tigase.eventbus.component.stores.Affiliation;
import tigase.eventbus.component.stores.AffiliationStore;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;

@Bean(name = DiscoveryModule.ID)
public class EventbusDiscoveryModule extends DiscoveryModule {

	@Inject
	private EventsRegistrar registrar;
	@Inject
	private AffiliationStore affiliationStore;

	private Map<String, Set<String>> prepareEventsTree() {
		Map<String, Set<String>> result = new HashMap<>();

		for (String e : registrar.getRegisteredEvents()) {
			EventName name = new EventName(e);

			Set<String> nodes;

			final String pck = name.getPackage() + ".*";
			if (result.containsKey(pck)) {
				nodes = result.get(pck);
			} else {
				nodes = new HashSet<>();
				result.put(pck, nodes);
			}

			nodes.add(e);
		}

		return result;
	}

	@Override
	protected void processDiscoInfo(Packet packet, JID jid, String node, JID senderJID)
			throws ComponentException, RepositoryException {
		final Affiliation affiliation = affiliationStore.getAffiliation(packet.getStanzaFrom());
		if (node == null || !affiliation.isSubscribe()) {
			super.processDiscoInfo(packet, jid, node, senderJID);
		} else {
			final Map<String, Set<String>> tree = prepareEventsTree();

			Element resultQuery = new Element("query", new String[] { "xmlns", "node" },
					new String[] { "http://jabber.org/protocol/disco#info", node });
			Packet resultIq = packet.okResult(resultQuery, 0);

			if (tree.containsKey(node)) {
				resultQuery.addChild(
						new Element("identity", new String[] { "category", "type" }, new String[] { "pubsub", "collection" }));
			} else if (registrar.isRegistered(node)) {
				resultQuery.addChild(
						new Element("identity", new String[] { "category", "type" }, new String[] { "pubsub", "leaf" }));
			} else {
				try {
					write(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "Unknown event", true));
					return;
				} catch (PacketErrorTypeException e) {
					throw new RuntimeException(e);
				}
			}
			resultQuery.addChild(
					new Element("feature", new String[] { "var" }, new String[] { "http://jabber.org/protocol/pubsub" }));

			write(resultIq);
		}

	}

	@Override
	protected void processDiscoItems(Packet packet, JID jid, String node, JID senderJID)
			throws ComponentException, RepositoryException {
		final Affiliation affiliation = affiliationStore.getAffiliation(packet.getStanzaFrom());
		if (!affiliation.isSubscribe()) {
			write(packet.okResult(new Element("query", new String[] { Packet.XMLNS_ATT }, new String[] { DISCO_ITEMS_XMLNS }),
					0));
			return;
		}

		final Map<String, Set<String>> tree = prepareEventsTree();

		if (node == null) {
			List<Element> items = new ArrayList<Element>();
			Element resultQuery = new Element("query", new String[] { Packet.XMLNS_ATT }, new String[] { DISCO_ITEMS_XMLNS });
			Packet result = packet.okResult(resultQuery, 0);

			for (String n : tree.keySet()) {
				items.add(new Element("item", new String[] { "jid", "node", "name" }, new String[] { jid.toString(), n, n }));
			}

			// for (String eventName : registrar.getRegisteredEvents()) {
			// String description = registrar.getDescription(eventName);
			// items.add(new Element("item", new String[] { "jid", "node",
			// "name" }, new String[] { jid.toString(), eventName,
			// description == null || description.isEmpty() ? eventName :
			// description }));
			// }

			resultQuery.addChildren(items);
			write(result);

		} else if (tree.containsKey(node)) {
			List<Element> items = new ArrayList<Element>();
			Element resultQuery = new Element("query", new String[] { Packet.XMLNS_ATT }, new String[] { DISCO_ITEMS_XMLNS });
			Packet result = packet.okResult(resultQuery, 0);

			for (String eventName : tree.get(node)) {
				String description = registrar.getDescription(eventName);
				items.add(new Element("item", new String[] { "jid", "node", "name" }, new String[] { jid.toString(), eventName,
						description == null || description.isEmpty() ? eventName : description }));
			}
			resultQuery.addChildren(items);
			write(result);
		} else {
			Element resultQuery = new Element("query", new String[] { Packet.XMLNS_ATT }, new String[] { DISCO_ITEMS_XMLNS });
			write(packet.okResult(resultQuery, 0));
		}
	}
}
