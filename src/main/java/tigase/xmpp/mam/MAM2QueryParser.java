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

import java.util.stream.Stream;

public class MAM2QueryParser<Query extends tigase.xmpp.mam.Query> extends MAMQueryParser<Query> {

	protected static final String MAM2_XMLNS = "urn:xmpp:mam:2";
	
	public MAM2QueryParser() {
		this(Stream.empty());
	}

	protected MAM2QueryParser(Stream<String> additionalNamespaces) {
		super(Stream.concat(additionalNamespaces, Stream.of(MAM2_XMLNS)));
	}

}
