/*
 * ClusterElementTest.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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



/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.cluster.api;

//~--- non-JDK imports --------------------------------------------------------

import junit.framework.TestCase;

import org.junit.Test;

import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;

/**
 *
 * @author andrzej
 */
public class ClusterElementTest
				extends TestCase {
	/**
	 * Method description
	 *
	 */
	@Test
	@SuppressWarnings("deprecation")
	public void testGetMethodName() {
		SimpleParser parser       = new SimpleParser();
		DomBuilderHandler handler = new DomBuilderHandler();
		char[] data               =
			"<cluster to=\"sess-man@blue\" type=\"set\" id=\"cl-6627\" xmlns=\"tigase:cluster\" from=\"sess-man@green\"><control><visited-nodes><node-id>sess-man@green</node-id></visited-nodes><method-call name=\"packet-forward-sm-cmd\"/><first-node>sess-man@green</first-node></control><data><presence to=\"test2@test\" xmlns=\"jabber:client\" from=\"test1@test/test\"><status/><priority>5</priority></presence></data></cluster>".toCharArray();

		parser.parse(handler, data, 0, data.length);

		Element elem = handler.getParsedElements().poll();

		assertEquals(
				"packet-forward-sm-cmd",
				elem.findChild("/cluster/control/method-call").getAttributeStaticStr("name"));

//  assertEquals("cluster/control/method-call".split("/"),
//               ClusterElement.CLUSTER_METHOD_PATH);
		ClusterElement clElem = new ClusterElement(elem);

		assertEquals("packet-forward-sm-cmd", clElem.getMethodName());
	}
}


//~ Formatted in Tigase Code Convention on 13/02/20
