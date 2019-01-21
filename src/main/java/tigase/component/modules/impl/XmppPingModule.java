/**
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

import tigase.component.exceptions.ComponentException;
import tigase.component.modules.AbstractModule;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.kernel.beans.Bean;
import tigase.server.Packet;
import tigase.xml.Element;

@Bean(name = XmppPingModule.ID, active = true)
public class XmppPingModule
		extends AbstractModule {

	public final static String ID = "urn:xmpp:ping";
	private static final Criteria CRIT = ElementCriteria.nameType("iq", "get")
			.add(ElementCriteria.name("ping", "urn:xmpp:ping"));

	public XmppPingModule() {
	}

	@Override
	public String[] getFeatures() {
		return new String[]{"urn:xmpp:ping"};
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
	}

	@Override
	public void process(Packet iq) throws ComponentException {
		Packet reposnse = iq.okResult((Element) null, 0);
		write(reposnse);
	}

}
