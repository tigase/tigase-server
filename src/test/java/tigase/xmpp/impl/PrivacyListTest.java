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

package tigase.xmpp.impl;

import org.junit.Ignore;
import org.junit.Test;
import tigase.xml.Element;

import java.util.Collections;

public class PrivacyListTest {

	@Test
	@Ignore
	public void testToString() {
		Element list = new Element("list");
		list.setAttribute("name", "special");
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"jid", "juliet@example.com", "allow", "6"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"group", "my_group", "allow", "7"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"jid", "mercutio@example.org", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		list.addChild(new Element("item", new String[]{"type", "value", "action", "order"},
								  new String[]{"subscription", "both", "allow", "42"}));
		final PrivacyList privacyList = PrivacyList.create(Collections.EMPTY_MAP, list);

//		System.out.println(privacyList.toString());
	}
}