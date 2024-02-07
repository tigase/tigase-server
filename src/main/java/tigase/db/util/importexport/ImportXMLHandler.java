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
package tigase.db.util.importexport;

import tigase.xml.SimpleHandler;

import java.util.HashMap;
import java.util.logging.Logger;

public class ImportXMLHandler
		implements SimpleHandler {
	private static final Logger log = Logger.getLogger(ImportXMLHandler.class.getSimpleName());

	private final Importer importer;

	public ImportXMLHandler(Importer importer) {
		this.importer = importer;
	}

	@Override
	public void error(String s) {
		// ignore
		log.severe("error: " + s);
	}

	@Override
	public void startElement(StringBuilder name, StringBuilder[] att_names, StringBuilder[] att_values) {
		HashMap<String, String> attrs = new HashMap<>();
		if (att_names != null && att_values != null) {
			for (int i = 0; i < att_names.length; i++) {
				if (att_names[i] != null && att_values[i] != null) {
					attrs.put(att_names[i].toString(), att_values[i].toString());
				}
			}
		}
		importer.startElement(name.toString(), attrs);
	}

	@Override
	public void elementCData(StringBuilder stringBuilder) {
		importer.elementCData(stringBuilder.toString());
	}

	@Override
	public boolean endElement(StringBuilder stringBuilder) {
		return importer.endElement(stringBuilder.toString());
	}

	@Override
	public void otherXML(StringBuilder other) {
		log.fine("Other XML content: " + other);
	}

	private Object parserState;

	@Override
	public void saveParserState(Object o) {
		parserState = o;
	}

	@Override
	public Object restoreParserState() {
		return parserState;
	}
}
