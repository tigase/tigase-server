package tigase.kernel.module2;

import tigase.kernel.Registrar;
import tigase.kernel.core.Kernel;

public class Module2Registrar implements Registrar {

	@Override
	public void register(Kernel kernel) {
		kernel.registerBean("service").asClass(Module2Service.class).exec();
	}

	@Override
	public void start(Kernel krnl) {
		System.out.println("Initializing Module2");
		// krnl.getParent().registerBean("Module2Service").asInstance(krnl.getInstance("service")).exec();
		krnl.ln("service", krnl.getParent(), "Module2Service");
	}

}
