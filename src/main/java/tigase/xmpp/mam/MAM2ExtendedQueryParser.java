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
package tigase.xmpp.mam;

import tigase.component.exceptions.ComponentException;
import tigase.kernel.beans.Inject;
import tigase.server.DataForm;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

public class MAM2ExtendedQueryParser<Query extends tigase.xmpp.mam.Query> extends MAM2QueryParser<Query> {

	protected static final String MAM2_EXTENDED_XMLNS = MAM2_XMLNS + "#extended";

	@Inject
	private MAMRepository mamRepository;

	public MAM2ExtendedQueryParser() {
		this(Stream.empty());
	}

	protected MAM2ExtendedQueryParser(Stream<String> additionalNamespaces) {
		super(Stream.concat(additionalNamespaces, Stream.of(MAM2_EXTENDED_XMLNS)));
	}

	@Override
	public Query parseQuery(Query query, Packet packet) throws ComponentException {
		Query result = super.parseQuery(query, packet);
		Element queryEl = packet.getElement().getChildStaticStr("query");
		if (query instanceof ExtendedQuery) {
			ExtendedQuery extQuery = (ExtendedQuery) result;
			extQuery.setBeforeId(DataForm.getFieldValue(queryEl, "before-id"));
			extQuery.setAfterId(DataForm.getFieldValue(queryEl, "after-id"));
			extQuery.setIds(Optional.ofNullable(DataForm.getFieldValues(queryEl, "ids"))
								  .map(Arrays::asList)
								  .orElseGet(Collections::emptyList));
		}
		return result;
	}

	@Override
	public Element prepareForm(Element elem, String xmlns, Packet packet) {
		Element form = super.prepareForm(elem, xmlns, packet);
		Element x = form.getChild("x", "jabber:x:data");
		if (x != null && xmlns == MAM2_XMLNS) {
			JID from = packet.getStanzaFrom();
			if (from != null) {
				if (mamRepository.newQuery(from.getBareJID()) instanceof ExtendedQuery) {
					addField(x, "after-id", "text-single", "After item with ID");
					addField(x, "before-id", "text-single", "Before item with ID");
					addField(x, "ids", "text-multi", "Item IDs");
				}
			}
		}
		return form;
	}
}
