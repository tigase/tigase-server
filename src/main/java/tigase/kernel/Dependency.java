package tigase.kernel;

import java.lang.reflect.Field;

public class Dependency {

	private String beanName;

	private Field field;

	private Class<?> type;

	public String getBeanName() {
		return beanName;
	}

	public Field getField() {
		return field;
	}

	public Class<?> getType() {
		return type;
	}

	public void setBeanName(String beanId) {
		this.beanName = beanId;
	}

	public void setField(Field field) {
		this.field = field;
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
