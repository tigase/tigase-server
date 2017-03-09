/*
 * TypesConverterTest.java
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
 */

package tigase.kernel;

import org.junit.Assert;
import org.junit.Test;
import tigase.conf.ConfigReader;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.logging.Level;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TypesConverterTest {

	private HashMap<String,EnumSet<XT>> mapEnumSetField;

	@Test
	public void testConvert() throws Exception {
		TypesConverter converter = new DefaultTypesConverter();

		Assert.assertEquals(Integer.valueOf(123), converter.convert("123", Integer.class));
		Assert.assertEquals(Integer.valueOf(123), converter.convert(Integer.valueOf(123), Integer.class));
		Assert.assertEquals("123", converter.convert(Integer.valueOf(123), String.class));

		Integer x1 = new Integer(1);
		Integer x2 = converter.convert(x1, Integer.class);
		Assert.assertTrue(x1 == x2);

		Assert.assertEquals(XT.a1, converter.convert("a1", XT.class));

		Assert.assertEquals(JID.jidInstanceNS("a@.b.c/d"), converter.convert("a@.b.c/d", JID.class));
		Assert.assertEquals(BareJID.bareJIDInstanceNS("a@.b.c"), converter.convert("a@.b.c", BareJID.class));

		Assert.assertEquals("test", converter.convert("test", String.class));
		Assert.assertEquals(Long.valueOf(123), converter.convert("123", Long.class));
		Assert.assertEquals(1234l, (long) converter.convert("1234", long.class));

		Assert.assertEquals(Integer.valueOf(123), converter.convert("123", Integer.class));

		Assert.assertEquals(Boolean.FALSE, converter.convert("anything", Boolean.class));
		Assert.assertEquals(Boolean.TRUE, converter.convert("yes", Boolean.class));
		Assert.assertEquals(Boolean.TRUE, converter.convert("true", Boolean.class));
		Assert.assertEquals(Boolean.TRUE, converter.convert("on", Boolean.class));
		Assert.assertEquals(Boolean.TRUE, converter.convert("1", Boolean.class));
		Assert.assertTrue(converter.convert("1", boolean.class));

		Assert.assertEquals(Float.valueOf(123.1f), converter.convert("123.1", Float.class));
		Assert.assertEquals(Float.valueOf(123.1f), converter.convert("123.1", float.class));

		Assert.assertEquals(Double.valueOf(123.1d), converter.convert("123.1", Double.class));
		Assert.assertEquals(Double.valueOf(123.1d), converter.convert("123.1", double.class));

		assertArrayEquals(
				new JID[]{JID.jidInstanceNS("1@b.c/a"), JID.jidInstanceNS("2@b.c/a"), JID.jidInstanceNS("3@b.c/a")},
				converter.convert("1@b.c/a,2@b.c/a,3@b.c/a", JID[].class));

		assertArrayEquals(new BareJID[]{BareJID.bareJIDInstanceNS("1@b.c"), BareJID.bareJIDInstanceNS("2@b.c"),
				BareJID.bareJIDInstanceNS("3@b.c")}, converter.convert("1@b.c,2@b.c,3@b.c", BareJID[].class));

		assertArrayEquals(new String[]{"1", "2", "3"}, converter.convert("1,2,3", String[].class));
		assertArrayEquals(new XT[]{XT.a1, XT.a1, XT.c3}, converter.convert("a1,a1,c3", XT[].class));
		assertArrayEquals(new Integer[]{1, 2, 3, 1}, converter.convert("1,2,3,1", Integer[].class));

		assertArrayEquals(new int[]{1, 2, 3, 1}, converter.convert("1,2,3,1", int[].class));
		assertArrayEquals(new long[]{1, 2, 3, 1}, converter.convert("1,2,3,1", long[].class));

		assertArrayEquals(new byte[]{1, 2, 3, 4}, converter.convert("1,2,3,4", byte[].class));
		assertArrayEquals(new byte[]{48, 49, 50, 52}, converter.convert("string:0124", byte[].class));
		assertArrayEquals(new byte[]{48, 49, 50, 53}, converter.convert("base64:MDEyNQ==", byte[].class));

		assertArrayEquals(new char[]{49, 50, 51, 52}, converter.convert("1,2,3,4", char[].class));
		assertArrayEquals(new char[]{48, 49, 50, 52}, converter.convert("string:0124", char[].class));
		assertArrayEquals(new char[]{48, 49, 50, 53}, converter.convert("base64:MDEyNQ==", char[].class));

		Assert.assertEquals(Level.CONFIG, converter.convert("CONFIG", Level.class));
		Assert.assertEquals(Level.ALL, converter.convert("ALL", Level.class));

		Assert.assertEquals(new File("/dupa.txt"), converter.convert("/dupa.txt", File.class));
		Assert.assertEquals(new File("/dupa.txt"),
				converter.convert(converter.toString(new File("/dupa.txt")), File.class));

		HashMap<String,String> values = new HashMap<>();
		values.put("t1", "a1,b2");
		values.put("t2", "b2,c3");
		mapEnumSetField = converter.convert(values, HashMap.class, this.getClass().getDeclaredField("mapEnumSetField").getGenericType());
		assertEquals(EnumSet.of(XT.a1, XT.b2), mapEnumSetField.get("t1"));
		assertEquals(EnumSet.of(XT.b2, XT.c3), mapEnumSetField.get("t2"));

		assertEquals(System.getProperty("java.home"), converter.convert(new ConfigReader.PropertyVariable("java.home", null), String.class));

		ConfigReader.CompositeVariable compositeVariable = new ConfigReader.CompositeVariable();
		compositeVariable.add("Java: ");
		compositeVariable.add('+', new ConfigReader.PropertyVariable("java.vendor", null));
		compositeVariable.add('+', " ");
		compositeVariable.add('+', new ConfigReader.PropertyVariable("java.version", null));
		assertEquals("Java: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"), converter.convert(compositeVariable, String.class));

		compositeVariable = new ConfigReader.CompositeVariable();
		compositeVariable.add(5);
		compositeVariable.add('-', 2);
		compositeVariable.add('*', 60);
		compositeVariable.add('*', 1000);
		assertEquals(new Integer(-119995), converter.convert(compositeVariable, Integer.class));

		compositeVariable = new ConfigReader.CompositeVariable();
		compositeVariable.add(5);
		compositeVariable.add('*', 60.0);
		compositeVariable.add('*', 1000);
		assertEquals(new Double(300000.0), converter.convert(compositeVariable, Double.class));
	}

	@Test
	public void testToString() throws Exception {
		TypesConverter converter = new DefaultTypesConverter();

		Assert.assertEquals("a1", converter.toString(XT.a1));
		Assert.assertEquals("a1,a1,b2", converter.toString(new XT[]{XT.a1, XT.a1, XT.b2}));
		Assert.assertEquals("a1,a1,b2", converter.toString(new String[]{"a1", "a1", "b2"}));
		Assert.assertEquals("true,true,false,true", converter.toString(new boolean[]{true, true, false, true}));
		Assert.assertEquals("1,2,3,4", converter.toString(new char[]{49, 50, 51, 52}));
		Assert.assertEquals("1,2,3", converter.toString(new byte[]{1, 2, 3}));
		Assert.assertEquals("1@b.c/a,2@b.c/a,3@b.c/a", converter.toString(
				new JID[]{JID.jidInstanceNS("1@b.c/a"), JID.jidInstanceNS("2@b.c/a"), JID.jidInstanceNS("3@b.c/a")}));
		Assert.assertEquals(System.getProperty("java.home"), converter.toString(new ConfigReader.PropertyVariable("java.home", null)));

		ConfigReader.CompositeVariable compositeVariable = new ConfigReader.CompositeVariable();
		compositeVariable.add("Java: ");
		compositeVariable.add('+', new ConfigReader.PropertyVariable("java.vendor", null));
		compositeVariable.add('+', " ");
		compositeVariable.add('+', new ConfigReader.PropertyVariable("java.version", null));
		assertEquals("Java: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"), converter.toString(compositeVariable));

		compositeVariable = new ConfigReader.CompositeVariable();
		compositeVariable.add(5);
		compositeVariable.add('-', 2);
		compositeVariable.add('*', 60);
		compositeVariable.add('*', 1000);
		assertEquals("-119995", converter.toString(compositeVariable));

		compositeVariable = new ConfigReader.CompositeVariable();
		compositeVariable.add(5);
		compositeVariable.add('*', 60.0);
		compositeVariable.add('*', 1000);
		assertEquals("300000.0", converter.toString(compositeVariable));
	}

	public enum XT {
		a1,
		b2,
		c3
	}
}