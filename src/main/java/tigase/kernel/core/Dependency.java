package tigase.kernel.core;

import java.lang.reflect.Field;

public class Dependency {

	private BeanConfig beanConfig;

	private String beanName;

	private Field field;

	private boolean nullAllowed;

	private Class<?> type;

	public Dependency(BeanConfig beanConfig) {
		this.beanConfig = beanConfig;
	}

	public BeanConfig getBeanConfig() {
		return beanConfig;
	}

	public String getBeanName() {
		return beanName;
	}

	public Field getField() {
		return field;
	}

	public Class<?> getType() {
		return type;
	}

	public boolean isNullAllowed() {
		return nullAllowed;
	}

	public void setBeanName(String beanId) {
		this.beanName = beanId;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public void setNullAllowed(boolean nullAllowed) {
		this.nullAllowed = nullAllowed;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	@Override
	public String toString() {
		if (beanName != null)
			return "bean:" + beanName;
		else
			return "type:" + type.getName();
	}

}
