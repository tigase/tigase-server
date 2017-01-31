package tigase.kernel;

import org.junit.Assert;
import org.junit.Test;
import tigase.kernel.core.DependencyGrapher;
import tigase.kernel.core.Kernel;
import tigase.kernel.core.RegistrarKernel;
import tigase.kernel.module1.Module1Registrar;
import tigase.kernel.module1.Module1Service;
import tigase.kernel.module2.Module2Registrar;
import tigase.kernel.module2.Module2Service;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegistrarKernelTest {

	public RegistrarKernelTest() {
		Logger logger = Logger.getLogger("tigase.kernel");

		// create a ConsoleHandler
		Handler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);

		if (logger.isLoggable(Level.CONFIG))
			logger.config("Logger successfully initialized");
	}

	@Test
	public void test01() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.setName("root");
		krnl.setRegistrar(new TestRegistrar());

		krnl.startSubKernels();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());

		Kernel m1k = krnl.getInstance("module1#KERNEL");
		Kernel m2k = krnl.getInstance("module2#KERNEL");

		Assert.assertEquals(krnl.getInstance("bean1"), m1k.getInstance(Module1Service.class).getBean1());
		Assert.assertEquals(krnl.getInstance("bean1"), m2k.getInstance(Module2Service.class).getBean1());

		Assert.assertEquals((Object)krnl.getInstance("Module1Service"),(Object) m1k.getInstance("service"));
		Assert.assertEquals((Object)krnl.getInstance("Module2Service"),(Object) m2k.getInstance("service"));

	}

	public static class TestRegistrar implements Registrar {

		@Override
		public void register(Kernel krnl) {
			krnl.registerBean(Bean1.class).exportable().exec();
			krnl.registerBean("bean2").asClass(Bean2.class).exec();
			krnl.registerBean("bean3").asClass(Bean3.class).exec();
			krnl.registerBean("bean4").asClass(Bean4.class).exec();
			krnl.registerBean("bean4_1").asClass(Bean4.class).exec();
			krnl.registerBean("bean5").asClass(Bean5.class).withFactory(Bean5Factory.class).exec();

			krnl.registerBean("module1").asClass(Module1Registrar.class).exec();
			krnl.registerBean("module2").asClass(Module2Registrar.class).exec();
		}

		@Override
		public void start(Kernel krnl) {
			System.out.println(((Bean1) krnl.getInstance("bean1")).getBean2());
		}
	}
}
