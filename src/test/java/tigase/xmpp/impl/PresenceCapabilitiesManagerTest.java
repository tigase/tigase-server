/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2019 "Tigase, Inc." <office@tigase.com>
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
 */

package tigase.xmpp.impl;

import org.junit.Test;
import tigase.disco.ServiceIdentity;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.xml.Element;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class PresenceCapabilitiesManagerTest {

	@Test
	public void generateVerificationStringSimpleExample() {
		String[] features = new String[]{"http://jabber.org/protocol/caps", "http://jabber.org/protocol/disco#info",
										 "http://jabber.org/protocol/disco#items", "http://jabber.org/protocol/muc"};

		final ServiceIdentity exodus = new ServiceIdentity("client", "pc", "Exodus 0.9.1");
		String[] identities = new String[]{exodus.getAsCapsString()};

		final String s = PresenceCapabilitiesManager.generateVerificationString(identities, features, null);
		assertEquals("QgayPKawpkPSDYmwT/WM94uAlu0=", s);

	}

	@Test
	public void generateVerificationStringComplexExample() {
		String[] features = new String[]{"http://jabber.org/protocol/caps", "http://jabber.org/protocol/disco#info",
										 "http://jabber.org/protocol/disco#items", "http://jabber.org/protocol/muc"};

		final ServiceIdentity psi_en = new ServiceIdentity("client", "pc", "Psi 0.11", "en");
		final ServiceIdentity psi_el = new ServiceIdentity("client", "pc", "Ψ 0.11", "el");
		String[] identities = new String[]{psi_en.getAsCapsString(), psi_el.getAsCapsString()};

		final Element form = DataForm.createDataForm(Command.DataType.result);
		DataForm.addHiddenField(form, DataForm.FORM_TYPE, "urn:xmpp:dataforms:softwareinfo");
		DataForm.addFieldMultiValue(form, "ip_version", Arrays.asList("ipv4", "ipv6"));
		DataForm.addFieldMultiValue(form, "os", Arrays.asList("Mac"));
		DataForm.addFieldMultiValue(form, "os_version", Arrays.asList("10.5.1"));
		DataForm.addFieldMultiValue(form, "software", Arrays.asList("Psi"));
		DataForm.addFieldMultiValue(form, "software_version", Arrays.asList("0.11"));

		System.out.println(form.toStringPretty());

		final String s = PresenceCapabilitiesManager.generateVerificationString(identities, features, form);
		System.out.println(s);
		assertEquals("q07IKJEyjvHSyhy//CH0CxmKi8w=", s);

	}
}