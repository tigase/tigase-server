/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.xmpp.impl;

import java.util.EnumSet;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.Roster.SubscriptionType;

/**
 * 
 * 
 * @author <a href="mailto:bmalkow@tigase.org">Bartosz Małkowski</a>
 * @version $Rev$
 */
public class PubSub extends XMPPProcessor implements XMPPProcessorIfc {

	private static final String XMLNS = "http://jabber.org/protocol/pubsub";

	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] { "var" }, new String[] { XMLNS }),
			new Element("feature", new String[] { "var" }, new String[] { XMLNS + "#owner" }),
			new Element("feature", new String[] { "var" }, new String[] { XMLNS + "#publish" }),
			new Element("identity", new String[] { "category", "type" }, new String[] { "pubsub", "pep" }),

	};

	private static final String[] ELEMENTS = { "pubsub" };

	private static final EnumSet<SubscriptionType> SUBSCRITION_TYPES = EnumSet.of(SubscriptionType.both, SubscriptionType.from);

	private static final String[] XMLNSS = { XMLNS };

	public static Element createNotification(final Element publish, final String fromJID, final String toJID) {
		Element message = new Element("message");
		message.setAttribute("from", fromJID);
		message.setAttribute("to", toJID);

		String nodeName = publish.getAttribute("node");

		Element event = new Element("event", new String[] { "xmlns" }, new String[] { "http://jabber.org/protocol/pubsub#event" });
		message.addChild(event);

		Element items = new Element("items", new String[] { "node" }, new String[] { nodeName });
		event.addChild(items);

		for (Element si : publish.getChildren()) {
			if (!"item".equals(si.getName())) {
				continue;
			}
			items.addChild(si);
		}

		return message;
	}

	private final Logger log = Logger.getLogger(this.getClass().getName());

	public PubSub() {
	}

	@Override
	public String id() {
		return XMLNS;
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
			throws XMPPException {
		final Element element = packet.getElement();
		final Element pubSub = element.getChild("pubsub", "http://jabber.org/protocol/pubsub");
		final Element publish = pubSub.getChild("publish");
		final String nodeName = publish.getAttribute("node");

		if ("http://jabber.org/protocol/mood".equals(nodeName) && "iq".equals(packet.getElemName()) && "set".equals(element.getAttribute("type"))) {
			final String fromJid = session.getJID();

			String[] buddies = Roster.getBuddies(session, SUBSCRITION_TYPES);
			results.add(packet.okResult((Element) null, 0));
			for (String buddy : buddies) {
				Element notification = createNotification(publish, fromJid, buddy);
				results.add(new Packet(notification));
			}
			for (String buddy : session.getAllResourcesJIDs()) {
				Element notification = createNotification(publish, fromJid, buddy);
				results.add(new Packet(notification));
			}
		} else {
			results.add(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, null, true));
		}

	}

	public Element[] supDiscoFeatures(XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	public String[] supElements() {
		return ELEMENTS;
	}

	public String[] supNamespaces() {
		return XMLNSS;
	}

}
