/*
 * MAMQueryParser.java
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
package tigase.xmpp.mam;

import tigase.component.exceptions.ComponentException;
import tigase.kernel.beans.Bean;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.util.TimestampHelper;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;

import java.text.ParseException;

/**
 * Implementation of parser for XEP-0313: Message Archive Management
 *
 * Created by andrzej on 19.07.2016.
 */
@Bean(name = "mamQueryParser")
public class MAMQueryParser implements QueryParser<Query> {

	protected static final String MAM_XMLNS = "urn:xmpp:mam:1";

	private final TimestampHelper timestampHelper = new TimestampHelper();

	@Override
	public Query parseQuery(Query query, Packet packet) throws ComponentException {
		Element queryEl = packet.getElement().getChildStaticStr("query", MAM_XMLNS);

		query.setQuestionerJID(packet.getStanzaFrom());
		query.setComponentJID(packet.getStanzaTo());

		query.setId(queryEl.getAttributeStaticStr("queryid"));

		if (queryEl.getChild("x", "jabber:x:data") == null)
			return query;

		if (!MAM_XMLNS.equals(DataForm.getFieldValue(queryEl, "FORM_TYPE"))) {
			throw new ComponentException(Authorization.BAD_REQUEST, "Invalid form type");
		}

		String start = DataForm.getFieldValue(queryEl, "start");
		try {
			query.setStart(timestampHelper.parseTimestamp(start));
		} catch (ParseException ex) {
			throw new ComponentException(Authorization.BAD_REQUEST, "Invalid value in 'start' field", ex);
		}

		String end = DataForm.getFieldValue(queryEl, "end");
		try {
			query.setEnd(timestampHelper.parseTimestamp(end));
		} catch (ParseException ex) {
			throw new ComponentException(Authorization.BAD_REQUEST, "Invalid value in 'end' field", ex);
		}

		String with = DataForm.getFieldValue(queryEl, "with");
		if (with != null && !with.isEmpty()) {
			try {
				query.setWith(JID.jidInstance(with));
			} catch (TigaseStringprepException ex) {
				throw new ComponentException(Authorization.BAD_REQUEST, "Invalid value in 'with' field", ex);
			}
		}

		query.getRsm().fromElement(queryEl);

		return query;
	}

	@Override
	public Element prepareForm(Element elem) {
		Element x = DataForm.addDataForm(elem, Command.DataType.form);
		DataForm.addHiddenField(elem, "FORM_TYPE", MAM_XMLNS);

		addField(x, "with", "jid-single", "With");
		addField(x, "start", "jid-single", "Start");
		addField(x, "end", "jid-single", "End");

		return elem;
	}

	protected void addField(Element x, String var, String type, String label) {
		Element field = new Element("field", new String[] { "type", "var" }, new String[] { type, var });
		if (label != null) {
			field.setAttribute("label", label);
		}
		x.addChild(field);
	}
}
