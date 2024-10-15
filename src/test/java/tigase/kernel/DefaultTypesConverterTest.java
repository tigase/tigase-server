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
package tigase.kernel;

import org.junit.Assert;
import org.junit.Test;
import tigase.conf.ConfigReader;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

import static org.junit.Assert.*;

public class DefaultTypesConverterTest {

	public enum XT {
		a1,
		b2,
		c3
	}

	private HashMap<String, EnumSet<XT>> mapEnumSetField;
	private Collection<Integer> collectionIntField;
	private List<Integer> listIntField;
	private ArrayList<Integer> arrayListIntField;
	private Set<Integer> setIntField;

	@Test
	public void testDurationConvert() throws Exception {
		TypesConverter converter = new DefaultTypesConverter();

		Assert.assertEquals(Duration.ofDays(1), converter.convert("P1D", Duration.class));
	}

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
										BareJID.bareJIDInstanceNS("3@b.c")},
						  converter.convert("1@b.c,2@b.c,3@b.c", BareJID[].class));

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

		HashMap<String, String> values = new HashMap<>();
		values.put("t1", "a1,b2");
		values.put("t2", "b2,c3");
		mapEnumSetField = converter.convert(values, HashMap.class,
											this.getClass().getDeclaredField("mapEnumSetField").getGenericType());
		assertEquals(EnumSet.of(XT.a1, XT.b2), mapEnumSetField.get("t1"));
		assertEquals(EnumSet.of(XT.b2, XT.c3), mapEnumSetField.get("t2"));

		values = new HashMap<>();
		values.put("t1", "A1,B2");
		values.put("t2", "B2,C3");
		mapEnumSetField = converter.convert(values, HashMap.class,
											this.getClass().getDeclaredField("mapEnumSetField").getGenericType());
		assertEquals(EnumSet.of(XT.a1, XT.b2), mapEnumSetField.get("t1"));
		assertEquals(EnumSet.of(XT.b2, XT.c3), mapEnumSetField.get("t2"));

		values = new HashMap<>();
		values.put("t1", "A1,b2");
		values.put("t2", "b2,C3");
		mapEnumSetField = converter.convert(values, HashMap.class,
											this.getClass().getDeclaredField("mapEnumSetField").getGenericType());
		assertEquals(EnumSet.of(XT.a1, XT.b2), mapEnumSetField.get("t1"));
		assertEquals(EnumSet.of(XT.b2, XT.c3), mapEnumSetField.get("t2"));

		assertEquals(System.getProperty("java.home"),
					 converter.convert(new ConfigReader.PropertyVariable("java.home", null), String.class));

		ConfigReader.CompositeVariable compositeVariable = new ConfigReader.CompositeVariable();
		compositeVariable.add("Java: ");
		compositeVariable.add('+', new ConfigReader.PropertyVariable("java.vendor", null));
		compositeVariable.add('+', " ");
		compositeVariable.add('+', new ConfigReader.PropertyVariable("java.version", null));
		assertEquals("Java: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"),
					 converter.convert(compositeVariable, String.class));

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

		try {
			HashMap<String, String> test = new HashMap<>();
			test.put("test-domain.com", "true");
			assertFalse("Invalid conversion of Map<> to String, should throw!", converter.convert(test, String.class) != null);
		} catch (RuntimeException ex) {
			assertTrue(true);
		}
		try {
			HashMap<String, String> test = new HashMap<>();
			test.put("test-domain.com", "true");
			assertFalse("Invalid conversion of Map<> to BareJID, should throw!", converter.convert(test, BareJID.class) != null);
		} catch (RuntimeException ex) {
			assertTrue(true);
		}
		try {
			HashMap<String, String> test = new HashMap<>();
			test.put("test-domain.com", "true");
			assertFalse("Invalid conversion of Map<> to JID, should throw!", converter.convert(test, JID.class) != null);
		} catch (RuntimeException ex) {
			assertTrue(true);
		}

		try {
			List<String> test = new ArrayList<>();
			test.add("test-domain.com");
			assertFalse("Invalid conversion of List<> to String, should throw!", converter.convert(test, String.class) != null);
		} catch (RuntimeException ex) {
			assertTrue(true);
		}
		try {
			List<String> test = new ArrayList<>();
			test.add("test-domain.com");
			assertFalse("Invalid conversion of List<> to BareJID, should throw!", converter.convert(test, BareJID.class) != null);
		} catch (RuntimeException ex) {
			assertTrue(true);
		}
		try {
			List<String> test = new ArrayList<>();
			test.add("test-domain.com");
			assertFalse("Invalid conversion of List<> to JID, should throw!", converter.convert(test, JID.class) != null);
		} catch (RuntimeException ex) {
			assertTrue(true);
		}

	}

	@Test
	public void testCollections() throws NoSuchFieldException {
		TypesConverter converter = new DefaultTypesConverter();

		// -----

		ParameterizedType pt = (ParameterizedType) this.getClass().getDeclaredField("collectionIntField").getGenericType();
		collectionIntField = converter.convert("1,2,3", (Class<Collection>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, collectionIntField.toArray(Integer[]::new));

		collectionIntField = converter.convert(new int[]{1, 2, 3}, (Class<Collection>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, collectionIntField.toArray(Integer[]::new));

		collectionIntField = converter.convert(new String[]{"1", "2", "3"}, (Class<Collection>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, collectionIntField.toArray(Integer[]::new));

		collectionIntField = converter.convert(Arrays.asList(1,2,3), (Class<Collection>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, collectionIntField.toArray(Integer[]::new));

		try {
			collectionIntField.add(4);
			assertFalse(true);
		} catch (UnsupportedOperationException ex) {
			// ok
		}

		// -----

		pt = (ParameterizedType) this.getClass().getDeclaredField("listIntField").getGenericType();
		listIntField = converter.convert("1,2,3" , (Class<List>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, listIntField.toArray(Integer[]::new));

		listIntField = converter.convert(new int[]{1, 2, 3}, (Class<List>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, listIntField.toArray(Integer[]::new));

		listIntField = converter.convert(new String[]{"1", "2", "3"}, (Class<List>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, listIntField.toArray(Integer[]::new));

		listIntField = converter.convert(Arrays.asList(1,2,3) , (Class<List>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, listIntField.toArray(Integer[]::new));

		try {
			listIntField.add(4);
			assertFalse(true);
		} catch (UnsupportedOperationException ex) {
			// ok
		}

		// -----

		pt = (ParameterizedType) this.getClass().getDeclaredField("setIntField").getGenericType();
		setIntField = converter.convert("1,2,3", (Class<Set>) pt.getRawType(), pt);
		assertEquals(listIntField.size(), setIntField.stream().filter(i -> listIntField.contains(i)).count());

		setIntField = converter.convert(new int[]{1, 2, 3}, (Class<Set>) pt.getRawType(), pt);
		assertEquals(listIntField.size(), setIntField.stream().filter(i -> listIntField.contains(i)).count());

		setIntField = converter.convert(new String[]{"1", "2", "3"}, (Class<Set>) pt.getRawType(), pt);
		assertEquals(listIntField.size(), setIntField.stream().filter(i -> listIntField.contains(i)).count());

		setIntField = converter.convert(Arrays.asList(1,2,3), (Class<Set>) pt.getRawType(), pt);
		assertEquals(listIntField.size(), setIntField.stream().filter(i -> listIntField.contains(i)).count());

		try {
			setIntField.add(4);
			assertFalse(true);
		} catch (UnsupportedOperationException ex) {
			// ok
		}
		// -----

		pt = (ParameterizedType) this.getClass().getDeclaredField("arrayListIntField").getGenericType();
		arrayListIntField = converter.convert("1,2,3" , (Class<ArrayList>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, arrayListIntField.toArray(Integer[]::new));

		arrayListIntField = converter.convert(new int[]{1, 2, 3}, (Class<ArrayList>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, arrayListIntField.toArray(Integer[]::new));

		arrayListIntField = converter.convert(new String[]{"1", "2", "3"}, (Class<ArrayList>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, arrayListIntField.toArray(Integer[]::new));

		arrayListIntField = converter.convert(Arrays.asList(1,2,3), (Class<ArrayList>) pt.getRawType(), pt);
		assertArrayEquals(new Integer[]{1, 2, 3}, arrayListIntField.toArray(Integer[]::new));

		try {
			arrayListIntField.add(4);
		} catch (UnsupportedOperationException ex) {
			assertFalse(true);
		}

	}

	@Test
	public void testParcelable() {
		TypesConverter converter = new DefaultTypesConverter();

		ParcelableObject o1 = new ParcelableObject();
		o1.setIp("1.2.3.4");
		o1.setTimestamp(123456);
		o1.setJid(JID.jidInstanceNS("ala@ma.kota"));
		o1.setDescription("blah bla, blah; blah... :-)");

		String encoded = converter.toString(o1);
		assertEquals("1.2.3.4,ala@ma.kota,123456,blah bla\\, blah; blah... :-)", encoded);

		ParcelableObject o2 = converter.convert(encoded, ParcelableObject.class);

		assertEquals(o1.getIp(), o2.getIp());
		assertEquals(o1.getTimestamp(), o2.getTimestamp());
		assertEquals(o1.getJid(), o2.getJid());
		assertEquals(o1.getDescription(), o2.getDescription());
		assertEquals(o1, o2);

	}

	@Test
	public void testToString() {
		TypesConverter converter = new DefaultTypesConverter();

		Assert.assertEquals("a1", converter.toString(XT.a1));
		Assert.assertEquals("a1,a1,b2", converter.toString(new XT[]{XT.a1, XT.a1, XT.b2}));
		Assert.assertEquals("a1,a1,b2", converter.toString(new String[]{"a1", "a1", "b2"}));
		Assert.assertEquals("true,true,false,true", converter.toString(new boolean[]{true, true, false, true}));
		Assert.assertEquals("1,2,3,4", converter.toString(new char[]{49, 50, 51, 52}));
		Assert.assertEquals("1,2,3", converter.toString(new byte[]{1, 2, 3}));
		Assert.assertEquals("1@b.c/a,2@b.c/a,3@b.c/a", converter.toString(
				new JID[]{JID.jidInstanceNS("1@b.c/a"), JID.jidInstanceNS("2@b.c/a"), JID.jidInstanceNS("3@b.c/a")}));
		Assert.assertEquals(System.getProperty("java.home"),
							converter.toString(new ConfigReader.PropertyVariable("java.home", null)));

		ConfigReader.CompositeVariable compositeVariable = new ConfigReader.CompositeVariable();
		compositeVariable.add("Java: ");
		compositeVariable.add('+', new ConfigReader.PropertyVariable("java.vendor", null));
		compositeVariable.add('+', " ");
		compositeVariable.add('+', new ConfigReader.PropertyVariable("java.version", null));
		assertEquals("Java: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"),
					 converter.toString(compositeVariable));

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

	static class ParcelableObject
			implements TypesConverter.Parcelable {

		private String description;
		private String ip;
		private JID jid;
		private long timestamp;

		@Override
		public String[] encodeToStrings() {
			return new String[]{ip, jid.toString(), Long.toString(timestamp), description};
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			ParcelableObject that = (ParcelableObject) o;

			if (timestamp != that.timestamp) {
				return false;
			}
			if (!description.equals(that.description)) {
				return false;
			}
			if (!ip.equals(that.ip)) {
				return false;
			}
			return jid.equals(that.jid);
		}

		@Override
		public void fillFromString(String[] encoded) {
			this.ip = encoded[0];
			this.jid = JID.jidInstanceNS(encoded[1]);
			this.timestamp = Long.valueOf(encoded[2]);
			this.description = encoded[3];
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public JID getJid() {
			return jid;
		}

		public void setJid(JID jid) {
			this.jid = jid;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		@Override
		public int hashCode() {
			int result = description.hashCode();
			result = 31 * result + ip.hashCode();
			result = 31 * result + jid.hashCode();
			result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
			return result;
		}
	}
}