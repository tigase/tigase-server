package tigase.kernel;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class KernelTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Kernel krnl = new Kernel();
		krnl.registerBeanClass("bean1", Bean1.class);
		krnl.registerBeanClass("bean2", Bean2.class);
		krnl.registerBeanClass("bean3", Bean3.class);
		krnl.registerBeanClass("bean4", Bean4.class);
		krnl.registerBeanClass("bean4_1", Bean4.class);

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

		assertTrue(b1 instanceof Bean1);
		assertTrue(b2 instanceof Bean2);
		assertTrue(b3 instanceof Bean3);
		assertTrue(b4 instanceof Bean4);
		assertTrue(b41 instanceof Bean4);

		assertEquals(b1.getBean2(), b2);
		assertEquals(b1.getBean3(), b3);
		assertEquals(b2.getBean3(), b3);
		assertEquals(b2.getBean4(), b41);
		assertEquals(b3.getBean4(), b4);
		assertEquals(b3.getBean41(), b41);

		assertNotNull(b1.getSs());
		assertEquals(3, b1.getSs().length);

		krnl.unregister("bean4_1");
		assertNull(krnl.getInstance("bean4_1"));
		assertNull(b3.getBean41());
		assertNull(b2.getBean4());

	}
}
