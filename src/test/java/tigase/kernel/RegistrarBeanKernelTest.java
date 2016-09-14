package tigase.kernel;

import org.junit.Assert;
import org.junit.Test;
import tigase.component.PropertiesBeanConfigurator;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.core.DependencyGrapher;
import tigase.kernel.core.Kernel;
import tigase.kernel.core.RegistrarKernel;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by andrzej on 05.03.2016.
 */
public class RegistrarBeanKernelTest {

	public RegistrarBeanKernelTest() {
//		Logger logger = Logger.getLogger("tigase.kernel");
//
//		// create a ConsoleHandler
//		Handler handler = new ConsoleHandler();
//		handler.setLevel(Level.ALL);
//		logger.addHandler(handler);
//		logger.setLevel(Level.ALL);
//
//		if (logger.isLoggable(Level.CONFIG))
//			logger.config("Logger successfully initialized");
	}

	@Test
	public void test01() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.setName("root");
		krnl.registerBean(PropertiesBeanConfigurator.class).exec();
		krnl.registerBean(RegistrarBeanImpl.class).exec();

		//krnl.startSubKernels();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());

		Assert.assertNotNull(krnl.getInstance("RegistrarBean"));

		Kernel rb1k = krnl.getInstance("RegistrarBean#KERNEL");
		Assert.assertNotEquals(null, rb1k.getInstance(Bean1.class));
		Assert.assertEquals(krnl.getInstance("RegistrarBean"), rb1k.getInstance(RegistrarBeanImpl.class));
		Assert.assertNotNull(rb1k.getInstance("service"));

		krnl.setBeanActive("RegistrarBean", false);
		krnl.gc();

		dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());

		Assert.assertTrue(krnl.isBeanClassRegistered("RegistrarBean"));
		boolean exception = false;
		try {
			Assert.assertNull(krnl.getInstance("RegistrarBean#KERNEL"));
		} catch (KernelException ex) {
			// unknow bean - this is what we expect
			exception = true;
		}
		Assert.assertTrue(exception);
	}

	@Bean(name="RegistrarBean")
	public static class RegistrarBeanImpl implements RegistrarBean {


		public RegistrarBeanImpl() {

		}

		@Override
		public void register(Kernel kernel) {
			kernel.registerBean(Bean1.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
			kernel.unregister("bean1");
		}
	}
}
