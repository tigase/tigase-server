package tigase.kernel;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

public class TypesConverterTest {

	@Test
	public void testConvert() throws Exception {
		Assert.assertEquals(Integer.valueOf(123), TypesConverter.convert("123", Integer.class));
		Assert.assertEquals(Integer.valueOf(123), TypesConverter.convert(Integer.valueOf(123), Integer.class));
		Assert.assertEquals("123", TypesConverter.convert(Integer.valueOf(123), String.class));

		Integer x1 = new Integer(1);
		Integer x2 = TypesConverter.convert(x1, Integer.class);
		Assert.assertTrue(x1 == x2);

		Assert.assertEquals(XT.a1, TypesConverter.convert("a1", XT.class));

		Assert.assertEquals(JID.jidInstanceNS("a@.b.c/d"), TypesConverter.convert("a@.b.c/d", JID.class));
		Assert.assertEquals(BareJID.bareJIDInstanceNS("a@.b.c"), TypesConverter.convert("a@.b.c", BareJID.class));

		Assert.assertEquals("test", TypesConverter.convert("test", String.class));
		Assert.assertEquals(Long.valueOf(123), TypesConverter.convert("123", Long.class));
		Assert.assertEquals(1234l, (long) TypesConverter.convert("1234", long.class));

		Assert.assertEquals(Integer.valueOf(123), TypesConverter.convert("123", Integer.class));

		Assert.assertEquals(Boolean.FALSE, TypesConverter.convert("anything", Boolean.class));
		Assert.assertEquals(Boolean.TRUE, TypesConverter.convert("yes", Boolean.class));
		Assert.assertEquals(Boolean.TRUE, TypesConverter.convert("true", Boolean.class));
		Assert.assertEquals(Boolean.TRUE, TypesConverter.convert("on", Boolean.class));
		Assert.assertEquals(Boolean.TRUE, TypesConverter.convert("1", Boolean.class));
		Assert.assertTrue(TypesConverter.convert("1", boolean.class));

		Assert.assertEquals(Float.valueOf(123.1f), TypesConverter.convert("123.1", Float.class));
		Assert.assertEquals(Float.valueOf(123.1f), TypesConverter.convert("123.1", float.class));

		Assert.assertEquals(Double.valueOf(123.1d), TypesConverter.convert("123.1", Double.class));
		Assert.assertEquals(Double.valueOf(123.1d), TypesConverter.convert("123.1", double.class));

		assertArrayEquals(
				new JID[] { JID.jidInstanceNS("1@b.c/a"), JID.jidInstanceNS("2@b.c/a"), JID.jidInstanceNS("3@b.c/a") },
				TypesConverter.convert("1@b.c/a,2@b.c/a,3@b.c/a", JID[].class));

		assertArrayEquals(new BareJID[] { BareJID.bareJIDInstanceNS("1@b.c"), BareJID.bareJIDInstanceNS("2@b.c"),
				BareJID.bareJIDInstanceNS("3@b.c") }, TypesConverter.convert("1@b.c,2@b.c,3@b.c", BareJID[].class));

		assertArrayEquals(new String[] { "1", "2", "3" }, TypesConverter.convert("1,2,3", String[].class));
		assertArrayEquals(new XT[] { XT.a1, XT.a1, XT.c3 }, TypesConverter.convert("a1,a1,c3", XT[].class));
		assertArrayEquals(new Integer[] { 1, 2, 3, 1 }, TypesConverter.convert("1,2,3,1", Integer[].class));

		assertArrayEquals(new int[] { 1, 2, 3, 1 }, TypesConverter.convert("1,2,3,1", int[].class));
		assertArrayEquals(new long[] { 1, 2, 3, 1 }, TypesConverter.convert("1,2,3,1", long[].class));

		assertArrayEquals(new byte[] { 1, 2, 3, 4 }, TypesConverter.convert("1,2,3,4", byte[].class));
		assertArrayEquals(new byte[] { 48, 49, 50, 52 }, TypesConverter.convert("string:0124", byte[].class));
		assertArrayEquals(new byte[] { 48, 49, 50, 53 }, TypesConverter.convert("base64:MDEyNQ==", byte[].class));

		assertArrayEquals(new char[] { 49, 50, 51, 52 }, TypesConverter.convert("1,2,3,4", char[].class));
		assertArrayEquals(new char[] { 48, 49, 50, 52 }, TypesConverter.convert("string:0124", char[].class));
		assertArrayEquals(new char[] { 48, 49, 50, 53 }, TypesConverter.convert("base64:MDEyNQ==", char[].class));

		Assert.assertEquals(Level.CONFIG, TypesConverter.convert("CONFIG", Level.class));
		Assert.assertEquals(Level.ALL, TypesConverter.convert("ALL", Level.class));

		Assert.assertEquals(new File("/dupa.txt"), TypesConverter.convert("/dupa.txt", File.class));
		Assert.assertEquals(new File("/dupa.txt"),
				TypesConverter.convert(TypesConverter.toString(new File("/dupa.txt")), File.class));
	}

	@Test
	public void testToString() throws Exception {
		Assert.assertEquals("a1", TypesConverter.toString(XT.a1));
		Assert.assertEquals("a1,a1,b2", TypesConverter.toString(new XT[] { XT.a1, XT.a1, XT.b2 }));
		Assert.assertEquals("a1,a1,b2", TypesConverter.toString(new String[] { "a1", "a1", "b2" }));
		Assert.assertEquals("true,true,false,true", TypesConverter.toString(new boolean[] { true, true, false, true }));
		Assert.assertEquals("1,2,3,4", TypesConverter.toString(new char[] { 49, 50, 51, 52 }));
		Assert.assertEquals("1,2,3", TypesConverter.toString(new byte[] { 1, 2, 3 }));
		Assert.assertEquals("1@b.c/a,2@b.c/a,3@b.c/a", TypesConverter.toString(
				new JID[] { JID.jidInstanceNS("1@b.c/a"), JID.jidInstanceNS("2@b.c/a"), JID.jidInstanceNS("3@b.c/a") }));
	}

	public enum XT {
		a1,
		b2,
		c3
	}
}