package tigase.kernel.module1;

import tigase.kernel.Registrar;
import tigase.kernel.core.Kernel;

public class Module1Registrar implements Registrar {

	@Override
	public void register(Kernel kernel) {
		kernel.registerBean("service").asClass(Module1Service.class).exec();
		kernel.registerBean("util").asClass(Bean11.class).exec();
	}

	@Override
	public void start(Kernel krnl) {
		System.out.println("Initializing Module1");
		// krnl.getParent().registerBean("Module1Service").asInstance(krnl.getInstance("service")).exec();
		krnl.ln("service", krnl.getParent(), "Module1Service");
	}

}
