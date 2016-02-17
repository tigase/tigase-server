/*
 * KernelTest.java
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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyGrapher;
import tigase.kernel.core.Kernel;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class KernelTest {

	public KernelTest() {
		Logger logger = Logger.getLogger("tigase.kernel");

		// create a ConsoleHandler
		Handler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);

		if (logger.isLoggable(Level.CONFIG))
			logger.config("Logger successfully initialized");
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Kernel krnl = new Kernel();

		Bean4 bean4_1_o = new Bean4();
		krnl.registerBean(Bean1.class).exec();
		krnl.registerBean("bean2").asClass(Bean2.class).exec();
		krnl.registerBean("bean3").asClass(Bean3.class).exec();
		krnl.registerBean("bean4").asClass(Bean4.class).exec();
		krnl.registerBean("bean4_1").asInstance(bean4_1_o).exec();
		krnl.registerBean("bean5").asClass(Bean5.class).withFactory(Bean5Factory.class).exec();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());

		Bean1 b1 = krnl.getInstance("bean1");
		Bean2 b2 = krnl.getInstance("bean2");
		Bean3 b3 = krnl.getInstance("bean3");
		Bean4 b4 = krnl.getInstance("bean4");
		Bean4 b41 = krnl.getInstance("bean4_1");

		assertNotNull(b1);
		assertNotNull(b2);
		assertNotNull(b3);
		assertNotNull(b4);
		assertNotNull(b41);

		assertEquals(b41, bean4_1_o);

		assertTrue(b1 instanceof Bean1);
		assertTrue(b2 instanceof Bean2);
		assertTrue(b3 instanceof Bean3);
		assertTrue(b4 instanceof Bean4);
		assertTrue(b41 instanceof Bean4);

		assertEquals(b2, b1.getBean2());
		assertEquals(b3, b1.getBean3());
		assertEquals(b3, b2.getBean3());
		assertEquals(b41, b2.getBean4());
		assertEquals(b4, b3.getBean4());
		assertEquals(b41, b3.getBean41());

		assertNotNull(b1.getTableOfSpecial());
		assertEquals(3, b1.getTableOfSpecial().length);

		assertEquals(3, b1.getCollectionOfSpecial().size());
		assertTrue(b1.getCollectionOfSpecial().contains(b3));
		assertTrue(b1.getCollectionOfSpecial().contains(b4));
		assertTrue(b1.getCollectionOfSpecial().contains(b41));

		krnl.unregister("bean4_1");
		try {
			assertNull(krnl.getInstance("bean4_1"));
			Assert.fail();
		} catch (KernelException e) {
			assertEquals("Unknown bean 'bean4_1'.", e.getMessage());
		}
		assertNull(b3.getBean41());
		assertNull(b2.getBean4());

		assertNotNull(b1.getBean2());
		assertNotNull(b1.getBean3());

		assertEquals(2, b1.getTableOfSpecial().length);

		assertEquals(2, b1.getCollectionOfSpecial().size());
		assertTrue(b1.getCollectionOfSpecial().contains(b3));
		assertTrue(b1.getCollectionOfSpecial().contains(b4));

		krnl.registerBean("bean6").asClass(Bean6.class).exec();

		assertEquals(3, b1.getTableOfSpecial().length);
		assertEquals(3, b1.getCollectionOfSpecial().size());

		System.out.println(dg.getDependencyGraph());

	}

	@Test
	public void test2() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Kernel krnl = new Kernel();
		krnl.registerBean("bean7").asClass(Bean7.class).exec();

		krnl.registerBean("beanX").asClass(Bean4.class).exec();
		krnl.registerBean("beanX").asClass(Bean5.class).withFactory(Bean5Factory.class).exec();

		assertEquals(Bean5.class, krnl.getInstance(Bean7.class).getObj().getClass());
		assertEquals(Bean5.class, krnl.getInstance("beanX").getClass());

		krnl.registerBean("beanX").asClass(Bean6.class).exec();

		assertEquals(Bean6.class, krnl.getInstance("beanX").getClass());
		assertEquals(Bean6.class, krnl.getInstance(Bean7.class).getObj().getClass());
	}

	@Test
	public void test3() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Kernel krnl = new Kernel();

		krnl.registerBean(Bean1.class).exec();
		krnl.registerBean("bean4").asClass(Bean4.class).exec();
		// krnl.registerBean("bean41").asClass(Bean4.class).exec();

		Bean1 b1 = krnl.getInstance("bean1");

		assertNotNull(b1);

		assertNotNull(b1.getTableOfSpecial());
		assertEquals(1, b1.getTableOfSpecial().length);

		assertEquals(1, b1.getCollectionOfSpecial().size());
	}

	@Test
	public void test4() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Kernel krnl = new Kernel();

		krnl.registerBean(Bean1.class).exec();

		Bean1 b1 = krnl.getInstance("bean1");

		assertNotNull(b1);

		assertNull(b1.getTableOfSpecial());

		assertEquals(0, b1.getCollectionOfSpecial().size());
	}

	@Test
	public void test5() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Kernel krnl = new Kernel();

		krnl.registerBean(Bean8.class).exec();

		try {
			krnl.getInstance("bean8");
			fail("This bean shouldn't be initialized because of empty dependency.");
		} catch (KernelException e) {
			Assert.assertEquals("tigase.kernel.KernelException: Can't inject <null> to field tigase.kernel.Bean8.bean6", e.getMessage());
		}
	}

	@Test
	public void testBeanConfiguration() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Kernel krnl = new Kernel();


		krnl.registerBean(CustomTypesConverter.class).exec();
		krnl.registerBean("bean5").asClass(Bean5.class).exec();
		krnl.registerBean(DefaultTypesConverter.class).exec();
		krnl.registerBean(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME).asClass(TestBeanConfigurationProvider.class).exec();
		krnl.registerBean("bean6").asClass(Bean6.class).exec();
		krnl.registerBean(Bean8.class).exec();


		Bean5 b5 = krnl.getInstance(Bean5.class);
		Bean6 b6 = krnl.getInstance(Bean6.class);
		Bean8 b8 = krnl.getInstance(Bean8.class);

		assertEquals("yytestxx", b6.getTestValue());
		assertEquals(9987l, b5.getValue().longValue());
		assertEquals("EXAMPLE", b8.getSample());

		((TestBeanConfigurationProvider) krnl.getInstance(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)).restoreDefaults("bean5");
		assertEquals("yytestxx", b6.getTestValue());
		assertEquals(15l, b5.getValue().longValue());

		((TestBeanConfigurationProvider) krnl.getInstance(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)).restoreDefaults("bean6");
		assertNull(b6.getTestValue());
		assertEquals(15l, b5.getValue().longValue());
	}

	@Test
	public void testCascadeKernels() throws Exception {
		Kernel krnlParent = new Kernel("Parent");
		krnlParent.registerBean("bean1").asClass(Bean1.class).exec();
		krnlParent.registerBean("bean40").asClass(Bean4.class).exportable().exec();
		krnlParent.registerBean("bean41").asClass(Bean4.class).exec();
		krnlParent.registerBean("bean42").asClass(Bean4.class).exportable().exec();
		krnlParent.registerBean("bean5").asClass(Bean5.class).exportable().exec();
		final Bean5 b5parent = new Bean5();
		final Bean5 b51parent = new Bean5();
		krnlParent.registerBean("bean5").asInstance(b5parent).exportable().exec();
		krnlParent.registerBean("bean51").asInstance(b51parent).exportable().exec();

		Kernel krnlChild1 = new Kernel("Child01");
		krnlChild1.registerBean("bean40").asClass(Bean4.class).exportable().exec();

		final Bean5 b5ch1 = new Bean5();
		krnlChild1.registerBean("bean5").asInstance(b5ch1).exportable().exec();

		Kernel krnlChild2 = new Kernel("Child02");
		krnlChild2.registerBean("bean1").asClass(Bean1.class).exec();
		krnlChild2.registerBean("bean43").asClass(Bean4.class).exec();

		krnlParent.registerBean(krnlChild1.getName()).asInstance(krnlChild1).exec();
		krnlParent.registerBean(krnlChild2.getName()).asInstance(krnlChild2).exec();

		Bean1 bean1 = krnlChild2.getInstance(Bean1.class);

		assertEquals(3, bean1.getCollectionOfSpecial().size());

		assertTrue("Bean should be located in parent", krnlChild2.isBeanClassRegistered("bean40"));
		assertNotNull("Bean should be get from parent!", krnlChild2.getInstance("bean40"));
		assertEquals((Object) krnlParent.getInstance("bean40"),
				krnlChild2.getInstance("bean40"));

		assertTrue(bean1.getCollectionOfSpecial().contains(krnlParent.getInstance("bean40")));
		assertTrue(bean1.getCollectionOfSpecial().contains(krnlParent.getInstance("bean42")));

		assertEquals(3, bean1.getTableOfSpecial().length);

		DependencyGrapher dg = new DependencyGrapher(krnlParent);
		System.out.println(dg.getDependencyGraph());

		assertEquals(b5ch1, krnlChild1.getInstance("bean5"));
		assertEquals(b51parent, krnlChild1.getInstance("bean51"));

		assertNotNull(krnlChild1.getInstance(Bean5.class));

		try {
			krnlChild1.getInstance(Bean1.class);
			Assert.fail();
		} catch (KernelException e) {
			assertEquals("Can't find bean implementing class tigase.kernel.Bean1", e.getMessage());
		}
		try {
			krnlChild1.getInstance(Bean3.class);
			Assert.fail();
		} catch (KernelException e) {
			assertEquals("Can't find bean implementing class tigase.kernel.Bean3", e.getMessage());
		}

		try {
			krnlChild1.getInstance("zzz");
			Assert.fail();
		} catch (KernelException e) {
			assertEquals("Unknown bean 'zzz'.", e.getMessage());
		}
	}

	@Test
	public void testGc() throws Exception {
		Kernel krnl = new Kernel();
		krnl.registerBean(DefaultTypesConverter.class).exec();

		krnl.registerBean(Bean10.class).setPinned(false).exec();
		krnl.registerBean("a_1").asClass(Bean6.class).setPinned(false).exec();

		krnl.registerBean(Bean1.class).setPinned(false).exec();
		krnl.registerBean("bean2").asClass(Bean2.class).setPinned(false).setActive(true).exec();
		krnl.registerBean("bean3").asClass(Bean3.class).setPinned(false).exec();
		krnl.registerBean("bean4").asClass(Bean4.class).setPinned(false).exec();
		krnl.registerBean("bean5").asClass(Bean5.class).setPinned(false).withFactory(Bean5Factory.class).exec();

		Assert.assertEquals(1, krnl.getDependencyManager().getBeanConfigs().stream().filter(bc -> bc.getState() == BeanConfig.State.initialized).count());

		krnl.getInstance(Bean1.class);
		krnl.getInstance(Bean10.class);

		Assert.assertEquals(7, krnl.getDependencyManager().getBeanConfigs().stream().filter(bc -> bc.getState() == BeanConfig.State.initialized).count());

		krnl.gc();

		Assert.assertEquals(1, krnl.getDependencyManager().getBeanConfigs().stream().filter(bc -> bc.getState() == BeanConfig.State.initialized).count());

		krnl.getInstance(Bean5.class);

		Assert.assertEquals(8, krnl.getDependencyManager().getBeanConfigs().stream().filter(bc -> bc.getState() == BeanConfig.State.initialized).count());

		krnl.gc();

		Assert.assertEquals(1, krnl.getDependencyManager().getBeanConfigs().stream().filter(bc -> bc.getState() == BeanConfig.State.initialized).count());

		krnl.getDependencyManager().getBeanConfig("bean10").setPinned(true);

		krnl.getInstance(Bean1.class);
		krnl.getInstance(Bean10.class);

		Assert.assertEquals(7, krnl.getDependencyManager().getBeanConfigs().stream().filter(bc -> bc.getState() == BeanConfig.State.initialized).count());

		krnl.gc();

		Assert.assertEquals(5, krnl.getDependencyManager().getBeanConfigs().stream().filter(bc -> bc.getState() == BeanConfig.State.initialized).count());
	}

	@Test
	public void testInactiveBean() throws Exception {
		Kernel krnl = new Kernel();
		krnl.registerBean(DefaultTypesConverter.class).exec();

		krnl.registerBean(Bean10.class).exec();
		krnl.registerBean("a_1").asClass(Bean6.class).exec();
		krnl.registerBean("a_2").asClass(Bean6.class).exec();
		krnl.registerBean("a_3").asClass(Bean6.class).setActive(false).exec();

		krnl.registerBean(Bean1.class).exec();
		krnl.registerBean("bean2").asClass(Bean2.class).setActive(false).exec();
		krnl.registerBean("bean3").asClass(Bean3.class).exec();
		krnl.registerBean("bean4").asClass(Bean4.class).exec();
		krnl.registerBean("bean5").asClass(Bean5.class).withFactory(Bean5Factory.class).exec();


		Bean1 b1 = krnl.getInstance(Bean1.class);
		Bean10 b10 = krnl.getInstance("bean10");

		assertNull(b1.getBean2());
		assertEquals(4, b10.getTableOfSpecial().length);

		krnl.setBeanActive("a_3", true);
		krnl.setBeanActive("bean2", true);

		assertNotNull(b1.getBean2());
		assertEquals(5, b10.getTableOfSpecial().length);

		krnl.setBeanActive("bean2", false);
		krnl.setBeanActive("a_1", false);

		assertNull(b1.getBean2());
		assertEquals(4, b10.getTableOfSpecial().length);

		krnl.setBeanActive("a_1", false);
		assertNull(b1.getBean2());
		assertEquals(4, b10.getTableOfSpecial().length);
	}

	@Bean(name = "customTypesConverter")
	public static class CustomTypesConverter extends DefaultTypesConverter {

		@Override
		public <T> T convert(Object value, Class<T> expectedType) {
			return super.convert(value.toString().substring(3), expectedType);
		}

		@Override
		public String toString(Object value) {
			return "___" + super.toString(value);
		}
	}

	public static class TestBeanConfigurationProvider extends AbstractBeanConfigurator {

		@Override
		public Map<String, Object> getConfiguration(BeanConfig beanConfig) {
			if (beanConfig.getBeanName().equals("bean5")) {
				HashMap<String, Object> result = new HashMap<String, Object>();
				result.put("value", Long.valueOf(9987));
				return result;
			} else if (beanConfig.getBeanName().equals("bean6")) {
				HashMap<String, Object> result = new HashMap<String, Object>();
				result.put("testValue", "yytestxx");
				return result;
			} else if (beanConfig.getBeanName().equals("bean8")) {
				HashMap<String, Object> result = new HashMap<String, Object>();
				result.put("sample", "___EXAMPLE");
				return result;
			} else
				return null;
		}
	}
}
