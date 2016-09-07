/*
 * ExternalServiceDiscoveryModule.java
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
package tigase.server.extdisco;

import tigase.component.PacketWriter;
import tigase.component.exceptions.ComponentException;
import tigase.component.modules.Module;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.db.TigaseDBException;
import tigase.db.comp.ComponentRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 06.09.2016.
 */
@Bean(name = "extDiscoModule", parent = ExternalServiceDiscoveryComponent.class)
public class ExternalServiceDiscoveryModule implements Module {

	private static final Logger log = Logger.getLogger(ExternalServiceDiscoveryModule.class.getCanonicalName());

	private static final String XMLNS = "urn:xmpp:extdisco:2";

	private static final Criteria CRITERIA = ElementCriteria.name(Iq.ELEM_NAME).add(ElementCriteria.name("services", XMLNS));

	@Inject
	private PacketWriter packetWriter;

	@Inject
	private ComponentRepository<ExtServiceDiscoItem> repo;

	@Override
	public String[] getFeatures() {
		return null;
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRITERIA;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		if (packet.getType() == StanzaType.error) {
			log.log(Level.FINEST, "Received packet of type 'error', dropping packet = {0}", packet);
			return;
		}
		if (packet.getType() != StanzaType.get) {
			throw new ComponentException(Authorization.BAD_REQUEST, "Invalid packet type");
		}

		String type = packet.getElement().getChild("services", XMLNS).getAttributeStaticStr("type");

		List<ExtServiceDiscoItem> services = getServices(type);

		Element servicesEl = new Element("services");
		servicesEl.setXMLNS(XMLNS);

		services.forEach(service -> {
			Element item = service.toElement();
			item.removeAttribute("key");
			servicesEl.addChild(item);
		});

		packetWriter.write(packet.okResult(servicesEl, 0));
	}

	protected List<ExtServiceDiscoItem> getServices(String type) {
		try {
			List<ExtServiceDiscoItem> items = new ArrayList<>(repo.allItems());

			if (type != null) {
				items.removeIf((item) -> !type.equals(item.getType()));
			}

			return items;
		} catch (TigaseDBException ex) {
			throw new RuntimeException("Exception reading items from repository", ex);
		}
	}
}
