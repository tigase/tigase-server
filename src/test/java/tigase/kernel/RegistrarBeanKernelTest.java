package tigase.kernel;

import org.junit.Assert;
import org.junit.Test;
import tigase.component.PropertiesBeanConfigurator;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
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

	@Test
	public void test02() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.setName("root");
		krnl.registerBean(PropertiesBeanConfigurator.class).exec();
		krnl.registerBean(DummyBean.class).exec();
		krnl.registerBean(RegistrarBeanImplWithLink.class).exec();

		//krnl.startSubKernels();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());

		Assert.assertNotNull(krnl.getInstance("RegistrarBean"));

		Kernel rb1k = krnl.getInstance("RegistrarBean#KERNEL");
		Assert.assertNotEquals(null, rb1k.getInstance(Bean1.class));
		Assert.assertEquals(krnl.getInstance("RegistrarBean"), rb1k.getInstance(RegistrarBeanImplWithLink.class));
		Assert.assertNotNull(rb1k.getInstance("service"));
		Assert.assertNotNull(rb1k.getInstance("dummy"));
		Assert.assertNotNull(rb1k.getInstance("DummyBeanUser"));
		Assert.assertNotNull(((DummyBeanUser) rb1k.getInstance("DummyBeanUser")).dummyBean);

		krnl.unregister("DummyBean");

		try {
			// maybe it should still be there but in unresolved state?
			Assert.assertNull(rb1k.getDependencyManager().getBeanConfig("dummy"));
			rb1k.getInstance("dummy");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			// wrong cause of KernelException (caused by NPE), should be thrown due to fact
			// that "dummy" is DelegatedBeanConfig pointing to removed BeanConfig!
			Assert.assertTrue(ex.getMessage().contains("Unknown bean"));
		}

		Assert.assertNull(((DummyBeanUser) rb1k.getInstance("DummyBeanUser")).dummyBean);

		dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());
	}

	@Test
	public void test03() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.setName("root");
		krnl.registerBean(PropertiesBeanConfigurator.class).exec();
		krnl.registerBean(DummyBean2.class).exec();
		krnl.registerBean(RegistrarBeanImplWithLink2.class).exec();

		//krnl.startSubKernels();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());

		Assert.assertNotNull(krnl.getInstance("RegistrarBean"));

		Kernel rb1k = krnl.getInstance("RegistrarBean#KERNEL");
		Assert.assertNotEquals(null, rb1k.getInstance(Bean1.class));
		Assert.assertEquals(krnl.getInstance("RegistrarBean"), rb1k.getInstance(RegistrarBeanImplWithLink2.class));
		Assert.assertNotNull(rb1k.getInstance("service"));
		Assert.assertNotNull(rb1k.getInstance(DummyBean2.class));
		Assert.assertNotNull(rb1k.getInstance("DummyBean2User"));
		Assert.assertNotNull(((DummyBean2User) rb1k.getInstance("DummyBean2User")).dummyBean);

		krnl.unregister("DummyBean2");

		try {
			rb1k.getInstance("DummyBean2");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			Assert.assertTrue(ex.getMessage().contains("Unknown bean"));
		}

		Assert.assertNull(((DummyBean2User) rb1k.getInstance("DummyBean2User")).dummyBean);

		dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());
	}


	@Test
	public void test04() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.setName("root");
		krnl.registerBean(PropertiesBeanConfigurator.class).exec();
		krnl.registerBean(DummyBean3.class).exec();
		krnl.registerBean(RegistrarBeanImplWithLink3.class).exec();

		//krnl.startSubKernels();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());

		Assert.assertNotNull(krnl.getInstance("RegistrarBean"));

		Kernel rb1k = krnl.getInstance("RegistrarBean#KERNEL");
		Assert.assertNotEquals(null, rb1k.getInstance(Bean1.class));
		Assert.assertEquals(krnl.getInstance("RegistrarBean"), rb1k.getInstance(RegistrarBeanImplWithLink3.class));
		Assert.assertNotNull(rb1k.getInstance("service"));
		Assert.assertNotNull(rb1k.getInstance("dummy"));
		Assert.assertNotNull(rb1k.getInstance("DummyBean3User"));
		Assert.assertNotNull(rb1k.getInstance("DummyBean34User"));
		Assert.assertNotNull(((DummyBean3User) rb1k.getInstance("DummyBean3User")).dummyBean);
		Assert.assertNotNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean3User);

		krnl.unregister("DummyBean3");

		try {
			// maybe it should still be there but in unresolved state?
			Assert.assertNull(rb1k.getDependencyManager().getBeanConfig("dummy"));
			rb1k.getInstance("dummy");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			// wrong cause of KernelException (caused by NPE), should be thrown due to fact
			// that "dummy" is DelegatedBeanConfig pointing to removed BeanConfig!
			Assert.assertTrue(ex.getMessage().contains("Unknown bean"));
		}

		try {
			rb1k.getInstance("DummyBean3User");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			Assert.assertTrue(true);
		}

		Assert.assertNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean3User);

		krnl.registerBean(DummyBean3.class).exec();
		krnl.ln("DummyBean3", rb1k, "dummy");

		Assert.assertNotNull(((DummyBean3User) rb1k.getInstance("DummyBean3User")).dummyBean);
		Assert.assertNotNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean3User);

		dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());
	}

	@Test
	public void test05() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.registerBean(DefaultTypesConverter.class).exportable().exec();
		krnl.setName("root");
		krnl.registerBean(PropertiesBeanConfigurator.class).exec();
		krnl.registerBean(DummyBean4.class).exec();
		krnl.registerBean(RegistrarBeanImplWithLink4.class).exec();

		//krnl.startSubKernels();

		DependencyGrapher dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());

		Assert.assertNotNull(krnl.getInstance("RegistrarBean"));

		Kernel rb1k = krnl.getInstance("RegistrarBean#KERNEL");
		Assert.assertNotEquals(null, rb1k.getInstance(Bean1.class));
		Assert.assertEquals(krnl.getInstance("RegistrarBean"), rb1k.getInstance(RegistrarBeanImplWithLink4.class));
		Assert.assertNotNull(rb1k.getInstance("service"));
		Assert.assertNotNull(rb1k.getInstance(DummyBean4.class));
		Assert.assertNotNull(rb1k.getInstance("DummyBean4User"));
		Assert.assertNotNull(rb1k.getInstance("DummyBean34User"));
		Assert.assertNotNull(((DummyBean4User) rb1k.getInstance("DummyBean4User")).dummyBean);
		Assert.assertNotNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean4User);

		krnl.unregister("DummyBean4");

		try {
			rb1k.getInstance("DummyBean4");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			Assert.assertTrue(ex.getMessage().contains("Unknown bean"));
		}

		try {
			rb1k.getInstance("DummyBean4User");
			Assert.assertTrue(false);
		} catch (KernelException ex) {
			Assert.assertTrue(true);
		}

		Assert.assertNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean4User);

		krnl.registerBean(DummyBean4.class).exec();

		Assert.assertNotNull(((DummyBean34User) rb1k.getInstance("DummyBean34User")).dummyBean4User);
		Assert.assertNotNull(((DummyBean4User) rb1k.getInstance("DummyBean4User")).dummyBean);


		dg = new DependencyGrapher(krnl);
		System.out.println(dg.getDependencyGraph());
	}

	@Bean(name="DummyBean")
	public static class DummyBean {

		public DummyBean() {}

	}

	@Bean(name="DummyBeanUser")
	public static class DummyBeanUser {

		@Inject(nullAllowed = true)
		public DummyBean dummyBean;

		public DummyBeanUser() {}

	}

	@Bean(name="DummyBean2", exportable = true)
	public static class DummyBean2 {

		public DummyBean2() {}

	}

	@Bean(name="DummyBean2User")
	public static class DummyBean2User {

		@Inject(nullAllowed = true)
		public DummyBean2 dummyBean;

		public DummyBean2User() {}

	}

	@Bean(name="DummyBean3")
	public static class DummyBean3 {

		public DummyBean3() {}

	}

	@Bean(name="DummyBean3User")
	public static class DummyBean3User {

		@Inject
		public DummyBean3 dummyBean;

		public DummyBean3User() {}

	}

	@Bean(name="DummyBean4", exportable = true)
	public static class DummyBean4 {

		public DummyBean4() {}

	}

	@Bean(name="DummyBean4User")
	public static class DummyBean4User {

		@Inject
		public DummyBean4 dummyBean;

		public DummyBean4User() {}

	}

	@Bean(name="DummyBean34User")
	public static class DummyBean34User {

		@Inject(nullAllowed = true)
		public DummyBean3User dummyBean3User;

		@Inject(nullAllowed = true)
		public DummyBean4User dummyBean4User;

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

	@Bean(name="RegistrarBean")
	public static class RegistrarBeanImplWithLink implements RegistrarBean {


		public RegistrarBeanImplWithLink() {

		}

		@Override
		public void register(Kernel kernel) {
			kernel.getParent().ln("DummyBean", kernel, "dummy");
			kernel.registerBean(DummyBeanUser.class).exec();
			kernel.registerBean(Bean1.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
			kernel.unregister("bean1");
		}
	}


	@Bean(name="RegistrarBean")
	public static class RegistrarBeanImplWithLink2 implements RegistrarBean {


		public RegistrarBeanImplWithLink2() {

		}

		@Override
		public void register(Kernel kernel) {
			kernel.registerBean(DummyBean2User.class).exec();
			kernel.registerBean(Bean1.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
			kernel.unregister("bean1");
		}
	}

	@Bean(name="RegistrarBean")
	public static class RegistrarBeanImplWithLink3 implements RegistrarBean {


		public RegistrarBeanImplWithLink3() {

		}

		@Override
		public void register(Kernel kernel) {
			kernel.getParent().ln("DummyBean3", kernel, "dummy");
			kernel.registerBean(DummyBean3User.class).exec();
			kernel.registerBean(DummyBean34User.class).exec();
			kernel.registerBean(Bean1.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
			kernel.unregister("bean1");
		}
	}

	@Bean(name="RegistrarBean")
	public static class RegistrarBeanImplWithLink4 implements RegistrarBean {


		public RegistrarBeanImplWithLink4() {

		}

		@Override
		public void register(Kernel kernel) {
			kernel.registerBean(DummyBean4User.class).exec();
			kernel.registerBean(DummyBean34User.class).exec();
			kernel.registerBean(Bean1.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
			kernel.unregister("bean1");
		}
	}

}
