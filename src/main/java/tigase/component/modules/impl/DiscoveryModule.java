/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package tigase.component.modules.impl;

import java.util.List;

import tigase.component.Context;
import tigase.component.exceptions.ComponentException;
import tigase.component.exceptions.RepositoryException;
import tigase.component.modules.AbstractModule;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.criteria.Or;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;

public class DiscoveryModule<CTX extends Context> extends AbstractModule<CTX> {

	public final static String DISCO_INFO_XMLNS = "http://jabber.org/protocol/disco#info";

	public final static String DISCO_ITEMS_XMLNS = "http://jabber.org/protocol/disco#items";

	public final static String ID = "disco";

	private Criteria criteria;

	public DiscoveryModule() {
		this.criteria = ElementCriteria.nameType("iq", "get").add(
				new Or(ElementCriteria.name("query", DISCO_INFO_XMLNS), ElementCriteria.name("query", DISCO_ITEMS_XMLNS)));
	}

	@Override
	public String[] getFeatures() {
		return null;
	}

	@Override
	public Criteria getModuleCriteria() {
		return criteria;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		final Element q = packet.getElement().getChild("query");
		final JID senderJID = packet.getStanzaFrom();
		final JID jid = packet.getStanzaTo();
		final String node = q.getAttributeStaticStr("node");

		try {
			if (q.getXMLNS().equals(DISCO_INFO_XMLNS)) {
				processDiscoInfo(packet, jid, node, senderJID);
			} else if (q.getXMLNS().equals(DISCO_ITEMS_XMLNS) && node != null && node.equals(AdHocCommandModule.XMLNS)) {
				processAdHocCommandItems(packet, jid, node, senderJID);
			} else if (q.getXMLNS().equals(DISCO_ITEMS_XMLNS)) {
				processDiscoItems(packet, jid, node, senderJID);
			} else {
				throw new ComponentException(Authorization.BAD_REQUEST);
			}
		} catch (ComponentException e) {
			throw e;
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}

	protected void processAdHocCommandItems(Packet packet, JID jid, String node, JID senderJID) throws ComponentException,
			RepositoryException {
		AdHocCommandModule<?> module = context.getModuleProvider().getModule(AdHocCommandModule.ID);
		if (module == null)
			throw new ComponentException(Authorization.ITEM_NOT_FOUND);

		Element resultQuery = new Element("query", new String[] { Packet.XMLNS_ATT }, new String[] { DISCO_ITEMS_XMLNS });
		Packet result = packet.okResult(resultQuery, 0);

		List<Element> items = module.getScriptItems(node, packet.getStanzaTo(), packet.getStanzaFrom());

		resultQuery.addChildren(items);

		write(result);
	}

	protected void processDiscoInfo(Packet packet, JID jid, String node, JID senderJID) throws ComponentException,
			RepositoryException {
		Element resultQuery = new Element("query", new String[] { "xmlns" }, new String[] { DISCO_INFO_XMLNS });
		Packet resultIq = packet.okResult(resultQuery, 0);

		resultQuery.addChild(new Element("identity", new String[] { "category", "type", "name" }, new String[] {
				context.getDiscoCategory(), context.getDiscoCategoryType(), context.getDiscoDescription() }));
		for (String f : context.getModuleProvider().getAvailableFeatures()) {
			resultQuery.addChild(new Element("feature", new String[] { "var" }, new String[] { f }));
		}

		write(resultIq);
	}

	protected void processDiscoItems(Packet packet, JID jid, String node, JID senderJID) throws ComponentException,
			RepositoryException {
		Element resultQuery = new Element("query", new String[] { Packet.XMLNS_ATT }, new String[] { DISCO_ITEMS_XMLNS });
		write(packet.okResult(resultQuery, 0));
	}

}
