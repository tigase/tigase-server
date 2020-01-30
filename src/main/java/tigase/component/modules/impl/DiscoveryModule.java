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
package tigase.component.modules.impl;

import tigase.annotations.TigaseDeprecated;
import tigase.component.exceptions.ComponentException;
import tigase.component.exceptions.RepositoryException;
import tigase.component.modules.AbstractModule;
import tigase.component.modules.Module;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.criteria.Or;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.BasicComponent;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;
import tigase.xmpp.rsm.RSM;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Bean(name = DiscoveryModule.ID, active = true)
public class DiscoveryModule
		extends AbstractModule {

	public final static String DISCO_INFO_XMLNS = "http://jabber.org/protocol/disco#info";

	public final static String DISCO_ITEMS_XMLNS = "http://jabber.org/protocol/disco#items";

	private static final String[] FEATURES = { DISCO_INFO_XMLNS, DISCO_ITEMS_XMLNS };

	public final static String ID = "disco";

	@Inject(nullAllowed = true)
	private AdHocCommandModule adHocCommandModule;

	@Inject(bean = "service")
	private BasicComponent component;

	private Criteria criteria;

	@Inject(type = Module.class)
	private List<Module> modules;

	public DiscoveryModule() {
		this.criteria = ElementCriteria.nameType("iq", "get")
				.add(new Or(ElementCriteria.name("query", DISCO_INFO_XMLNS),
							ElementCriteria.name("query", DISCO_ITEMS_XMLNS)));
	}

	public AdHocCommandModule getAdHocCommandModule() {
		return adHocCommandModule;
	}

	public void setAdHocCommandModule(AdHocCommandModule adHocCommandModule) {
		this.adHocCommandModule = adHocCommandModule;
	}

	public Set<String> getAvailableFeatures(BareJID serviceJID, String node, BareJID senderJID) {
		return getAvailableFeatures();
	}

	public Set<String> getAvailableFeatures() {
		final HashSet<String> features = new HashSet<String>();
		for (Module m : modules) {
			String[] fs = m.getFeatures();
			if (fs != null) {
				for (String string : fs) {
					features.add(string);
				}
			}
		}
		return features;
	}

	@Override
	public String[] getFeatures() {
		return FEATURES;
	}

	@Override
	public Criteria getModuleCriteria() {
		return criteria;
	}

	public List<Module> getModules() {
		return modules;
	}

	public void setModules(List<Module> modules) {
		this.modules = modules;
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
			} else if (q.getXMLNS().equals(DISCO_ITEMS_XMLNS) && node != null &&
					node.equals(AdHocCommandModule.XMLNS)) {
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

	protected void processAdHocCommandItems(Packet packet, JID jid, String node, JID senderJID)
			throws ComponentException, RepositoryException {
		if (adHocCommandModule == null) {
			throw new ComponentException(Authorization.ITEM_NOT_FOUND);
		}

		Element resultQuery = new Element("query", new String[]{Packet.XMLNS_ATT}, new String[]{DISCO_ITEMS_XMLNS});
		Packet result = packet.okResult(resultQuery, 0);

		List<Element> items = adHocCommandModule.getScriptItems(node, packet.getStanzaTo(), packet.getStanzaFrom());

		resultQuery.addChildren(items);

		write(result);
	}

	protected void processDiscoInfo(Packet packet, JID jid, String node, JID senderJID)
			throws ComponentException, RepositoryException {
		Packet resultIq = prepareDiscoInfoResponse(packet, jid, node, senderJID);
		write(resultIq);
	}

	@Deprecated
	@TigaseDeprecated(removeIn = "9.0.0", since = "8.1.0", note = "Deprecating method with type-o")
	protected Packet prepareDiscoInfoReponse(Packet packet, JID jid, String node, JID senderJID) {
		return prepareDiscoInfoResponse(packet, jid, node, senderJID);
	}

	protected Packet prepareDiscoInfoResponse(Packet packet, JID jid, String node, JID senderJID) {
		Element resultQuery = new Element("query", new String[]{"xmlns"}, new String[]{DISCO_INFO_XMLNS});
		Packet resultIq = packet.okResult(resultQuery, 0);

		resultQuery.addChild(new Element("identity", new String[]{"category", "type", "name"},
										 new String[]{component.getDiscoCategory(), component.getDiscoCategoryType(),
													  component.getDiscoDescription()}));
		for (String f : getAvailableFeatures(jid.getBareJID(), node, senderJID.getBareJID())) {
			resultQuery.addChild(new Element("feature", new String[]{"var"}, new String[]{f}));
		}
		Element form = component.getDiscoExtensionsForm(jid.getDomain());
		if (form != null) {
			resultQuery.addChild(form);
		}
		return resultIq;
	}

	protected List<Element> prepareDiscoItems(JID jid, String node, JID senderJID, RSM rsm) throws ComponentException, RepositoryException {
		return Collections.emptyList();
	}

	protected void processDiscoItems(Packet packet, JID jid, String node, JID senderJID)
			throws ComponentException, RepositoryException {
		Element resultQuery = new Element("query", new String[]{Packet.XMLNS_ATT}, new String[]{DISCO_ITEMS_XMLNS});
		
		Element rsmEl = packet.getElement().getChildStaticStr(Iq.QUERY_NAME, "http://jabber.org/protocol/disco#items").getChildStaticStr("set", RSM.XMLNS);
		RSM rsm = null;
		if (rsmEl != null) {
			rsm = RSM.parseRootElement(packet.getElement().getChild("query"));
		}

		List<Element> results = prepareDiscoItems(jid, node, senderJID, rsm);

		if (node != null) {
			resultQuery.setAttribute("node", node);
		}

		resultQuery.addChildren(results);

		if (rsm != null && !results.isEmpty()) {
			resultQuery.addChild(rsm.toElement());
		}

		write(packet.okResult(resultQuery, 0));
	}

}
