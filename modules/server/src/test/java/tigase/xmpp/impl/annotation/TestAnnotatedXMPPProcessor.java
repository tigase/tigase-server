/*
 * TestAnnotatedXMPPProcessor.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
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
package tigase.xmpp.impl.annotation;

import tigase.xmpp.StanzaType;
import static tigase.xmpp.impl.annotation.TestAnnotatedXMPPProcessor.*;

/**
 *
 * @author andrzej
 */
@Id(ID)
@Handles({
	@Handle(path={ "iq", "query" }, xmlns=XMLNS1),
	@Handle(pathStr=IQ_QUERY_PATH, xmlns=XMLNS2)
})
@DiscoFeatures({
	XMLNS1,
	XMLNS2
})
@StreamFeatures({
	@StreamFeature(elem="bind", xmlns="urn:ietf:params:xml:ns:xmpp-bind")
})
@HandleStanzaTypes({
	StanzaType.get
})
class TestAnnotatedXMPPProcessor extends AnnotatedXMPPProcessor {
	
	protected static final String ID = "test-123";
	protected static final String XMLNS1 = "tigase:test1";
	protected static final String XMLNS2 = "tigase:test2";
	protected static final String IQ_QUERY_PATH = "iq/query";
	
}
