package tigase.kernel.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * This is internal configuration of each bean. It stores name of bean,
 * dependencies, state of bean etc.
 */
public class BeanConfig {

	/**
	 * State of bean.
	 */
	public enum State {
		/**
		 * Bean is initialized and ready to use.
		 */
		initialized,
		/**
		 * Instance of bean is created, but bean isn't initialized.
		 */
		instanceCreated,
		/**
		 * Bean class is registered, but instance of bean isn't created yet.
		 */
		registered
	}

	private final String beanName;

	private final Class<?> clazz;

	private boolean exportable;

	private BeanConfig factory;

	private final Map<Field, Dependency> fieldDependencies = new HashMap<Field, Dependency>();

	private Kernel kernel;

	private State state;

	BeanConfig(String id, Class<?> clazz) {
		super();
		this.beanName = id;
		this.clazz = clazz;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BeanConfig other = (BeanConfig) obj;
		if (beanName == null) {
			if (other.beanName != null)
				return false;
		} else if (!beanName.equals(other.beanName))
			return false;
		return true;
	}

	/**
	 * Returns name of bean.
	 * 
	 * @return name of bean.
	 */
	public String getBeanName() {
		return beanName;
	}

	/**
	 * Returns class of bean.
	 * 
	 * @return class of bean.
	 */
	public Class<?> getClazz() {
		return clazz;
	}

	/**
	 * Return factory of bean.
	 * 
	 * @return factory of bean. It may return <code>null</code> if default
	 *         factory is used.
	 */
	public BeanConfig getFactory() {
		return factory;
	}

	/**
	 * Returns map of dependencies. Note that Kernel has field-based-dependency
	 * model, it means that each dependency must be related to field in class.
	 * 
	 * @return map of dependencies.
	 */
	public Map<Field, Dependency> getFieldDependencies() {
		return fieldDependencies;
	}

	/**
	 * Returns {@link Kernel} managing this bean.
	 * 
	 * @return {@link Kernel}.
	 */
	public Kernel getKernel() {
		return kernel;
	}

	/**
	 * Returns state of bean.
	 * 
	 * @return state of bean.
	 */
	public State getState() {
		return state;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((beanName == null) ? 0 : beanName.hashCode());
		return result;
	}

	/**
	 * Checks if bean may be visible in child Kernels.
	 * 
	 * @return <code>true</code> if beans will be visible in child Kernel (other
	 *         Kernels deployed as beans to current Kernel).
	 */
	public boolean isExportable() {
		return exportable;
	}

	void setExportable(boolean value) {
		this.exportable = value;
	}

	void setFactory(final BeanConfig bfc) {
		this.factory = bfc;
	}

	void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	void setState(State state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return beanName + ":" + clazz.getName();
	}

}
