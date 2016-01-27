package tigase.eventbus;

import org.junit.Assert;
import org.junit.Test;

import tigase.xml.Element;
import tigase.xmpp.JID;

public class SerializerTest {

	@Test
	public void testDeserialize() {
		Serializer serializer = new Serializer();

		Event1 eo = new Event1();
		eo.setJid(JID.jidInstanceNS("a@b.c/d"));
		eo.setTransientField("123");
		eo.setV1("message");
		eo.setV2(9898);
		eo.setElementField(new Element("x", "v", new String[] { "a" }, new String[] { "b" }));
		eo.setStrArrField(new String[] { "ala", "m,a", "kota" });

		Element ex = serializer.serialize(eo);

		Event1 ed = serializer.deserialize(ex);

		Assert.assertNotNull(ed);
		Assert.assertNotSame(eo, ed);
		Assert.assertEquals(JID.jidInstanceNS("a@b.c/d"), ed.getJid());
		Assert.assertNull(ed.getTransientField());
		Assert.assertEquals("message", ed.getV1());
		Assert.assertEquals(9898, ed.getV2());
		Assert.assertEquals(new Element("x", "v", new String[] { "a" }, new String[] { "b" }), ed.getElementField());
		Assert.assertArrayEquals(new String[] { "ala", "m,a", "kota" }, ed.getStrArrField());
	}

	@Test
	public void testSerialize() {
		Serializer serializer = new Serializer();

		Event1 eo = new Event1();
		eo.setJid(JID.jidInstanceNS("a@b.c/d"));
		eo.setTransientField("123");
		eo.setV1("message");
		eo.setV2(9898);
		eo.setElementField(new Element("x", "v", new String[] { "a" }, new String[] { "b" }));
		eo.setStrArrField(new String[] { "ala", "m,a", "kota" });

		Element ex = serializer.serialize(eo);

		Assert.assertEquals(5, ex.getChildren().size());
		Assert.assertEquals("a@b.c/d", ex.getCData(new String[] { "tigase.eventbus.Event1", "jid" }));
		Assert.assertNull(ex.getCData(new String[] { "tigase.eventbus.Event1", "transientField" }));
		Assert.assertEquals("message", ex.getCData(new String[] { "tigase.eventbus.Event1", "v1" }));
		Assert.assertEquals("9898", ex.getCData(new String[] { "tigase.eventbus.Event1", "v2" }));
		Assert.assertEquals("v", ex.getCData(new String[] { "tigase.eventbus.Event1", "elementField", "x" }));
		Assert.assertNotEquals("ala,m,a,kota",ex.getCData(new String[] { "tigase.eventbus.Event1", "strArrField" }));
	}

}