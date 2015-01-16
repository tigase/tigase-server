package tigase.kernel;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DependencyManagerTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		final DependencyManager dm = new DependencyManager();

		dm.registerBeanClass("bean1", Bean1.class);
		dm.registerBeanClass("bean2", Bean2.class);
		dm.registerBeanClass("bean3", Bean3.class);
		dm.registerBeanClass("bean4", Bean4.class);
		dm.registerBeanClass("bean4_1", Bean4.class);


	}

}
