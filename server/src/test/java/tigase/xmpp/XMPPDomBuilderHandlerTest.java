/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.xmpp;

import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;
import tigase.xml.Element;

/**
 *
 * @author andrzej
 */
public class XMPPDomBuilderHandlerTest {
	
	@Test
	public void testPrefixesAndNamespacesHandling() {
		XMPPIOService ioserv = new XMPPIOService() {
			@Override
			protected void xmppStreamOpened(Map attribs) {
			}
			
		};
		boolean error = false;
		XMPPDomBuilderHandler<Element> handler = new XMPPDomBuilderHandler<>(ioserv);
		handler.setElementsLimit(10);
		handler.startElement(new StringBuilder("stream:stream"), new StringBuilder[] { new StringBuilder("xmlns"), new StringBuilder("xmlns:stream"), new StringBuilder("xmlns:db") }, 
				new StringBuilder[] { new StringBuilder("jabber:server"), new StringBuilder("http://etherx.jabber.org/streams"), new StringBuilder("jabber:server:dialback") });
		
		handler.startElement(new StringBuilder("db:result"), new StringBuilder[] { new StringBuilder("to") }, new StringBuilder[] { new StringBuilder("example.com") });
		handler.elementCData(new StringBuilder("CAESBxCXyf6RqCoaEGPHnXDLTIeKBNx9ZJ1SmzM="));
		error = !handler.endElement(new StringBuilder("db:result"));
		
		assertFalse(handler.parseError() || error);
		assertEquals("result", handler.getParsedElements().peek().getName());
		assertEquals("jabber:server:dialback", handler.getParsedElements().peek().getXMLNS());
		
		handler.getParsedElements().clear();
		handler.startElement(new StringBuilder("test:message"), null, null);
		error = !handler.endElement(new StringBuilder("test:message"));
		assertFalse(handler.parseError() || error);

		handler.getParsedElements().clear();
		handler.startElement(new StringBuilder("message"), null, null);
		error = !handler.endElement(new StringBuilder("message"));
		assertFalse(handler.parseError() || error);

		handler.getParsedElements().clear();
		handler.startElement(new StringBuilder("test:message"), null, null);
		error = !handler.endElement(new StringBuilder("message"));
		assertTrue(handler.parseError() || error);

		handler.getParsedElements().clear();
		handler.startElement(new StringBuilder("message"), null, null);
		error = !handler.endElement(new StringBuilder("test:message"));
		assertTrue(handler.parseError() || error);

		handler.getParsedElements().clear();
		handler.startElement(new StringBuilder("message"), null, null);
		error = !handler.endElement(new StringBuilder("db:message"));
		assertTrue(handler.parseError() || error);

	}
	
}
