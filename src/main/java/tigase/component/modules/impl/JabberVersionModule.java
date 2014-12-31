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

import tigase.component.Context;
import tigase.component.exceptions.ComponentException;
import tigase.component.modules.AbstractModule;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.server.Packet;
import tigase.xml.Element;

/**
 * Class description
 * 
 * 
 */
public class JabberVersionModule<CTX extends Context> extends AbstractModule<CTX> {

	private static final Criteria CRIT = ElementCriteria.nameType("iq", "get").add(
			ElementCriteria.name("query", "jabber:iq:version"));

	public final static String ID = "jabber:iq:version";

	public JabberVersionModule() {
	}

	@Override
	public String[] getFeatures() {
		return new String[] { "jabber:iq:version" };
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
	}

	@Override
	public void process(Packet packet) throws ComponentException {
		Element query = new Element("query", new String[] { "xmlns" }, new String[] { "jabber:iq:version" });

		query.addChild(new Element("name", context.getDiscoDescription()));
		query.addChild(new Element("version", context.getComponentVersion()));
		query.addChild(new Element("os", System.getProperty("os.name") + "-" + System.getProperty("os.arch") + "-"
				+ System.getProperty("os.version") + ", " + System.getProperty("java.vm.name") + "-"
				+ System.getProperty("java.version") + " " + System.getProperty("java.vm.vendor")));

		write(packet.okResult(query, 0));
	}

}
