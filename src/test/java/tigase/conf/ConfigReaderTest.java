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
}
