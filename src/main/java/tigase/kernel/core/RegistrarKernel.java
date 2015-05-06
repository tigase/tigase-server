package tigase.kernel.core;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import tigase.kernel.Registrar;

public class RegistrarKernel extends Kernel {

	private Class<? extends Registrar> registrarClass;

	@Override
	protected void initBean(BeanConfig beanConfig, Set<BeanConfig> createdBeansConfig, int deep) throws IllegalAccessException,
	IllegalArgumentException, InvocationTargetException, InstantiationException {
		super.initBean(beanConfig, createdBeansConfig, deep);

	}

	public void setRegistrar(Registrar registrar) {
		registrar.register(this);
		registrar.start(this);

		for (BeanConfig rbc : getDependencyManager().getBeanConfigs(Registrar.class)) {
			Registrar r = getInstance(rbc.getBeanName());
		}
	}

}
