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
package tigase.eventbus.impl;

import org.junit.Assert;
import org.junit.Test;
import tigase.auth.BruteForceLockerBean;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

public class EventBusSerializerTest {

	@Test
	public void testDeserialize() {
		EventBusSerializer serializer = new EventBusSerializer();

		Event1 eo = new Event1();
		eo.setJid(JID.jidInstanceNS("a@b.c/d"));
		eo.setTransientField("123");
		eo.setV1("message");
		eo.setV2(9898);
		eo.setElementField(new Element("x", "v", new String[]{"a"}, new String[]{"b"}));
		eo.setStrArrField(new String[]{"ala", "m,a", "kota"});
		eo.setSetField(new HashSet<>(Arrays.asList("test123")));

		Element ex = serializer.serialize(eo);

		Event1 ed = serializer.deserialize(ex);

		assertNotNull(ed);
		Assert.assertNotSame(eo, ed);
		Assert.assertEquals(JID.jidInstanceNS("a@b.c/d"), ed.getJid());
		Assert.assertNull(ed.getTransientField());
		Assert.assertEquals("message", ed.getV1());
		Assert.assertEquals(9898, ed.getV2());
		Assert.assertEquals(new Element("x", "v", new String[]{"a"}, new String[]{"b"}), ed.getElementField());
		Assert.assertArrayEquals(new String[]{"ala", "m,a", "kota"}, ed.getStrArrField());
		Assert.assertTrue(eo.getSetField().contains("test123"));
	}

	@Test
	public void testSerialize() {
		EventBusSerializer serializer = new EventBusSerializer();

		Event1 eo = new Event1();
		eo.setJid(JID.jidInstanceNS("a@b.c/d"));
		eo.setTransientField("123");
		eo.setV1("message");
		eo.setV2(9898);
		eo.setElementField(new Element("x", "v", new String[]{"a"}, new String[]{"b"}));
		eo.setStrArrField(new String[]{"ala", "m,a", "kota"});

		Element ex = serializer.serialize(eo);

		Assert.assertEquals(5, ex.getChildren().size());
		Assert.assertEquals("event", ex.getName());
		Assert.assertEquals("tigase.eventbus.impl.Event1", ex.getAttributeStaticStr("class"));
		Assert.assertEquals("a@b.c/d", ex.getCData(new String[]{"event", "jid"}));
		Assert.assertNull(ex.getCData(new String[]{"event", "transientField"}));
		Assert.assertEquals("message", ex.getCData(new String[]{"event", "v1"}));
		Assert.assertEquals("9898", ex.getCData(new String[]{"event", "v2"}));
		Assert.assertEquals("v", ex.getCData(new String[]{"event", "elementField", "x"}));
		Assert.assertNotEquals("ala,m,a,kota", ex.getCData(new String[]{"event", "strArrField"}));
	}

	@Test
	public void testSerializeXmlValidity() throws TigaseStringprepException {
		EventBusSerializer serializer = new EventBusSerializer();

		BruteForceLockerBean.StatHolder statHolder = new BruteForceLockerBean.StatHolder();
		statHolder.addIP("192.168.0.1");
		statHolder.addIP("::1");
		statHolder.addJID(BareJID.bareJIDInstance("test@zeus"));
		BruteForceLockerBean.StatisticsEmitEvent eo = new BruteForceLockerBean.StatisticsEmitEvent("test", statHolder);
		String xmlString = serializer.serialize(eo).toString();

		SimpleParser parser = SingletonFactory.getParserInstance();
		TestDomBuilderHandler domHandler = new TestDomBuilderHandler();
		parser.parse(domHandler, xmlString.toCharArray(), 0, xmlString.length());
		assertFalse(domHandler.isError());
		
		Element ex = domHandler.getParsedElements().poll();

		BruteForceLockerBean.StatisticsEmitEvent eor = serializer.deserialize(ex);
		assertNotNull(eor);

		String xmlString2 = serializer.serialize(eor).toString();
		assertEquals(xmlString, xmlString2);
	}

	private static class TestDomBuilderHandler extends DomBuilderHandler {

		private boolean error = false;

		@Override
		public void error(String errorMessage) {
			super.error(errorMessage);
			this.error = true;
		}

		public boolean isError() {
			return error;
		}
	}

}