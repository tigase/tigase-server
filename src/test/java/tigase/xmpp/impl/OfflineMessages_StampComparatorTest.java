/*
 * OfflineMessages_StampComparatorTest.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

package tigase.xmpp.impl;

import org.junit.Test;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;

import static org.junit.Assert.assertTrue;

/**
 * Created by andrzej on 12.01.2017.
 */
public class OfflineMessages_StampComparatorTest {

	@Test
	public void test_jabberXDelay() throws TigaseStringprepException {
		OfflineMessages.StampComparator comparator = new OfflineMessages.StampComparator();

		Packet p1 = createJabberXDelayPacket("20020910T23:08:25");
		Packet p2 = createJabberXDelayPacket("20020910T22:08:25");
		assertTrue(comparator.compare(p1, p2) > 0);
		assertTrue(comparator.compare(p2, p1) < 0);

		p1 = Packet.packetInstance(new Element("message"));
		assertTrue(comparator.compare(p1, p2) < 0);
		assertTrue(comparator.compare(p2, p1) > 0);
	}

	@Test
	public void test_urnDelay() throws TigaseStringprepException {
		OfflineMessages.StampComparator comparator = new OfflineMessages.StampComparator();

		Packet p1 = createUrnXmppDelayPacket("2002-09-10T23:08:25Z");
		Packet p2 = createUrnXmppDelayPacket("2002-09-10T22:08:25Z");
		assertTrue(comparator.compare(p1, p2) > 0);
		assertTrue(comparator.compare(p2, p1) < 0);

		p1 = Packet.packetInstance(new Element("message"));
		assertTrue(comparator.compare(p1, p2) < 0);
		assertTrue(comparator.compare(p2, p1) > 0);
	}

	@Test
	public void test_mixedDelay() throws TigaseStringprepException {
		OfflineMessages.StampComparator comparator = new OfflineMessages.StampComparator();

		Packet p1 = createJabberXDelayPacket("20020910T23:08:25");
		Packet p2 = createUrnXmppDelayPacket("2002-09-10T22:08:25Z");
		assertTrue(comparator.compare(p1, p2) > 0);
		assertTrue(comparator.compare(p2, p1) < 0);

		p1 = createUrnXmppDelayPacket("2002-09-10T23:08:25Z");
		p2 = createJabberXDelayPacket("20020910T22:08:25");
		assertTrue(comparator.compare(p1, p2) > 0);
		assertTrue(comparator.compare(p2, p1) < 0);
	}

	private Packet createJabberXDelayPacket(String timestamp) throws TigaseStringprepException {
		Element elem = new Element("message", new Element[]{
				new Element("x", new String[]{"xmlns", "stamp"}, new String[]{"jabber:x:delay", timestamp})},
								   null, null);
		return Packet.packetInstance(elem);
	}

	private Packet createUrnXmppDelayPacket(String timestamp) throws TigaseStringprepException {
		Element elem = new Element("message", new Element[]{
				new Element("delay", new String[]{"xmlns", "stamp"}, new String[]{"urn:xmpp:delay", timestamp})},
								   null, null);
		return Packet.packetInstance(elem);
	}
}
