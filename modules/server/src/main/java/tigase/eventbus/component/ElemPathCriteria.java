/*
 * ElemPathCriteria.java
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
 */

package tigase.eventbus.component;

import tigase.criteria.Criteria;
import tigase.xml.Element;

public class ElemPathCriteria implements Criteria {

	private final String[] names;
	private final String[] xmlns;

	public ElemPathCriteria(String[] elemNames, String[] namespaces) {
		this.names = elemNames;
		this.xmlns = namespaces;
	}

	@Override
	public Criteria add(Criteria criteria) {
		throw new RuntimeException("UNSUPPORTED!");
	}

	@Override
	public boolean match(Element element) {

		boolean match = element.getName().equals(names[0]);
		if (match && xmlns[0] != null)
			match &= xmlns[0].equals(element.getXMLNS());

		Element child = element;
		int i = 1;
		for (; i < names.length; i++) {
			String n = names[i];
			String x = xmlns[i];

			child = child.getChildStaticStr(n, x);

			match &= child != null;

			if (!match)
				return match;

		}

		// TODO Auto-generated method stub
		return match;
	}
}
