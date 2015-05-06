package tigase.kernel.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class BeanConfig {

	public enum State {
		initialized,
		instanceCreated,
		registered;
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
	};

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

	public String getBeanName() {
		return beanName;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public BeanConfig getFactory() {
		return factory;
	}

	public Map<Field, Dependency> getFieldDependencies() {
		return fieldDependencies;
	}

	public Kernel getKernel() {
		return kernel;
	}

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
