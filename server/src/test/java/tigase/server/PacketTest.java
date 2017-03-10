/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
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
package tigase.server;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

import tigase.xml.Element;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Wojciech Kapcia
 */
public class PacketTest {

	Packet packetInstance;

	public PacketTest() {
	}

	@Before
	public void setUp() {

		JID from = JID.jidInstanceNS( "server" );
		JID to = JID.jidInstanceNS( "user" );

		Element property = new Element( "property", new String[] { "name", "value", "xmlns" },
																		new String[] { "some name", "some value", "namespace" } );
		Element iq = Command.createIqCommand( from, to, StanzaType.get, "myId", "node", Command.DataType.submit );
		iq.findChild( new String[] { "iq", "command", "x" } ).addChild( property );
		packetInstance = Iq.packetInstance( iq, from, to );
		packetInstance.getStanzaFrom().getDomain();

	}


	@Test
	public void testOkResult_String_int() {

		String includeXML = null;
		int depth = 5;
		Packet result = packetInstance.okResult( includeXML, depth );
		Element property = result.getElement().findChild( new String[] { "iq", "command", "x", "property" } );

		assertEquals( "some name", property.getAttributeStaticStr( "name" ) );
		assertEquals( "some value", property.getAttributeStaticStr( "value" ) );
		assertEquals( "namespace", property.getAttributeStaticStr( "xmlns" ) );
		assertEquals( null, property.getAttributeStaticStr( "unknown" ) );

		depth = 0;
		result = packetInstance.okResult( includeXML, depth );
		property = result.getElement().findChild( new String[] { "iq", "command", "x", "property" } );

		assertEquals( null, property );

		depth = 1;
		result = packetInstance.okResult( includeXML, depth );
		property = result.getElement().findChild( new String[] { "iq", "command", "x" } );
		assertEquals( null, property );

		property = result.getElement().findChild( new String[] { "iq", "command" } );
		assertEquals( "command", property.getName() );
		assertEquals( "http://jabber.org/protocol/commands", property.getAttributeStaticStr( "xmlns" ) );
		assertEquals( "node", property.getAttributeStaticStr( "node" ) );
		assertEquals( null, property.getAttributeStaticStr( "unknown" ) );

	}

	@Test
	public void testOkResult_Element_int() {
		Element includeXML = null;
		int depth = 5;
		Packet result = packetInstance.okResult( includeXML, depth );
		Element property = result.getElement().findChild( new String[] { "iq", "command", "x", "property" } );

		assertEquals( "some name", property.getAttributeStaticStr( "name" ) );
		assertEquals( "some value", property.getAttributeStaticStr( "value" ) );
		assertEquals( "namespace", property.getAttributeStaticStr( "xmlns" ) );
		assertEquals( null, property.getAttributeStaticStr( "unknown" ) );

		depth = 0;
		result = packetInstance.okResult( includeXML, depth );
		property = result.getElement().findChild( new String[] { "iq", "command", "x", "property" } );

		assertEquals( null, property );

		depth = 1;
		result = packetInstance.okResult( includeXML, depth );
		property = result.getElement().findChild( new String[] { "iq", "command", "x" } );
		assertEquals( null, property );

		property = result.getElement().findChild( new String[] { "iq", "command" } );
		assertEquals( "command", property.getName() );
		assertEquals( "http://jabber.org/protocol/commands", property.getAttributeStaticStr( "xmlns" ) );
		assertEquals( "node", property.getAttributeStaticStr( "node" ) );
		assertEquals( null, property.getAttributeStaticStr( "unknown" ) );

	}

}
