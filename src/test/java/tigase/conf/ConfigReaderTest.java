package tigase.conf;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by andrzej on 05.06.2016.
 */
public class ConfigReaderTest {

	@Test
	public void test1() throws Exception {

		// equals is not working when type is different - ie. bool true vs string "true"
		Map<String, Object> root = new HashMap<>();
		root.put("test123", "2313");
		root.put("tes22", "223");
		root.put("x-gy+=x",123);
		List list = new ArrayList();
		list.add(1);
		list.add(3);
		root.put("some-list", list);
		list = new ArrayList();
		list.add("1");
		list.add("3");
		root.put("some-list-2", list);
		Map<String, Object> map = new HashMap<>();
		map.put("ala-ma-kota", true);
		map.put("test", "false");
		root.put("some-map", map);

		Map<String, Object> embeddedMap = new HashMap<>();
		embeddedMap.put("test", 123);
		embeddedMap.put("other", true);

		map = new HashMap<>();
		map.put("simple", "text");
		map.put("integer", 1);
		map.put("long", 2L);
		map.put("double", (double) 2.0);
		list = new ArrayList();

		list.add(1);
		list.add(embeddedMap);
		list.add("3");

		map.put("embedded-list", list);
		map.put("embedded-map", embeddedMap);
		root.put("another-map", map);

		Map<String, Object> parsed = null;

		File f = File.createTempFile("xx3232", "ccxx");
		try {
			new ConfigWriter().write(f, root);
			displayFile(f);
			parsed = new ConfigReader().read(f);
		} finally {
			f.delete();
		}

		f = File.createTempFile("xx3232", "ccxx");
		try {
			new ConfigWriter().write(f, root);
			displayFile(f);
		} finally {
			f.delete();
		}

		assertMapEquals(root, parsed, "/");

		assertEquals(root, parsed);
	}

	@Test
	public void test2() throws Exception {

		// equals is not working when type is different - ie. bool true vs string "true"
		Map<String, Object> props = new HashMap<>();
		props.put("--cluster-mode",true);
		props.put("dataSource/repo-uri", "jdbc:postgresql://127.0.0.1/tigase?user=test&password=test&autoCreateUser=true");
		props.put("sess-man/commands/ala-ma-kota", "DOMAIN");
		props.put("c2s/incoming-filters", Arrays.asList("tigase.server.filters.PacketCounter", "tigase.server.filters.PacketCounter"));
		Map<String, Object> root = ConfigWriter.buildTree(props);

		Map<String, Object> parsed = null;

		File f = File.createTempFile("xx3232", "ccxx");
		try {
			new ConfigWriter().write(f, root);
			displayFile(f);
			parsed = new ConfigReader().read(f);
		} finally {
			f.delete();
		}

		assertEquals(root, parsed);
		assertEquals(props, ConfigReader.flatTree(parsed));
	}

	private void displayFile(File f) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String line = null;
		System.out.println("file content: " + f.getAbsolutePath());
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
		}
	}

	private void assertMapEquals(Map<String,Object> expected, Map actual, String prefix) {
		for (Map.Entry e : expected.entrySet()) {
			Object value = actual.get(e.getKey());
			System.out.println("checking key = " + prefix + e.getKey());
			if (value instanceof Map) {
				assertMapEquals((Map<String, Object>) e.getValue(), (Map) value, prefix + e.getKey() + "/");
			} else if(value instanceof List) {
				assertListEquals((List) e.getValue(), (List) value);
			} else {
				assertEquals(e.getValue(), value);
			}
		}
	}

	private void assertListEquals(List expected, List actual) {
		assertEquals(expected.size(), actual.size());

		for (int i=0; i<expected.size(); i++) {
			assertEquals(expected.get(i), actual.get(i));
		}
	}
}
