package tigase.kernel.core;

import tigase.kernel.Registrar;

public class RegistrarKernel extends Kernel {

	private Class<? extends Registrar> registrarClass;

	
	public void setRegistrar(Registrar registrar) {
		registrar.register(this);
		registrar.start(this);

		for (BeanConfig rbc : getDependencyManager().getBeanConfigs(Registrar.class)) {
			Registrar r = getInstance(rbc.getBeanName());
		}
	}

}
