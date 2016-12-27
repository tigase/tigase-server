package tigase.conf;

import org.junit.Test;
import tigase.kernel.beans.config.AbstractBeanConfigurator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
		root.put("env-1", new ConfigReader.EnvironmentVariable("PATH"));
		root.put("prop-1", new ConfigReader.PropertyVariable("java.vendor", null));
		root.put("prop-2", new ConfigReader.PropertyVariable("java.version", "-1"));

		ConfigReader.CompositeVariable compositeVariable = new ConfigReader.CompositeVariable();
		compositeVariable.add("Java: ");
		compositeVariable.add('+', new ConfigReader.PropertyVariable("java.vendor", null));
		compositeVariable.add('+', " ");
		compositeVariable.add('+', new ConfigReader.PropertyVariable("java.version", null));
		root.put("comp-prop1", compositeVariable);

		compositeVariable = new ConfigReader.CompositeVariable();
		compositeVariable.add(5);
		compositeVariable.add('-', 2);
		compositeVariable.add('*', 60);
		compositeVariable.add('*', 1000);
		root.put("comp-prop2", compositeVariable);

		List list = new ArrayList();
		list.add(new ConfigReader.EnvironmentVariable("USER"));
		root.put("env-list", list);
		list = new ArrayList();
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
		root.put("for-null", null);

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

		AbstractBeanConfigurator.BeanDefinition b1 = new AbstractBeanConfigurator.BeanDefinition();
		b1.setBeanName("s2s");
		b1.setClazzName(tigase.server.xmppserver.S2SConnectionManager.class.getCanonicalName());

		b1.put("list", new ArrayList(list));
		b1.put("map", new HashMap(embeddedMap));
		b1.put("some", true);
		b1.put("other", 123);
		b1.put("ala", "kot");

		AbstractBeanConfigurator.BeanDefinition conns = new AbstractBeanConfigurator.BeanDefinition();
		conns.setBeanName("connections");
		AbstractBeanConfigurator.BeanDefinition port = new AbstractBeanConfigurator.BeanDefinition();
		port.setBeanName("5269");
		conns.put(port.getBeanName(), port);
		b1.put(conns.getBeanName(), conns);

		root.put(b1.getBeanName(), b1);

		AbstractBeanConfigurator.BeanDefinition b2 = new AbstractBeanConfigurator.BeanDefinition();
		b2.setBeanName("c2s");
		b2.setClazzName(tigase.server.xmppclient.ClientConnectionManager.class.getCanonicalName());
		b2.setActive(true);
		b2.setExportable(true);

		root.put(b2.getBeanName(), b2);

		AbstractBeanConfigurator.BeanDefinition b3 = new AbstractBeanConfigurator.BeanDefinition();
		b3.setBeanName("upload");

		root.put(b3.getBeanName(), b3);

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
		props.put("http/active", "true");
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
			if (e.getValue() == null) {
				assertNull(value);
				continue;
			}
			assertEquals(e.getValue().getClass(), value.getClass());
			if (value instanceof AbstractBeanConfigurator.BeanDefinition) {
				AbstractBeanConfigurator.BeanDefinition av = (AbstractBeanConfigurator.BeanDefinition) value;
				AbstractBeanConfigurator.BeanDefinition ev = (AbstractBeanConfigurator.BeanDefinition) e.getValue();
				assertEquals(ev.getClazzName(), av.getClazzName());
				assertEquals(ev.getBeanName(), av.getBeanName());
				assertEquals(ev.isActive(), av.isActive());
				assertEquals(ev.isExportable(), av.isExportable());
			}
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
