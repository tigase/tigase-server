package tigase.kernel;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class BeanConfig {

	private final String beanName;

	private final Class<?> clazz;

	private final Map<Field, Dependency> fieldDependencies = new HashMap<Field, Dependency>();

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

	public String getBeanName() {
		return beanName;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public Map<Field, Dependency> getFieldDependencies() {
		return fieldDependencies;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((beanName == null) ? 0 : beanName.hashCode());
		return result;
	}

}
