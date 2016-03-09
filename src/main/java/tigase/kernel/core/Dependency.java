package tigase.kernel.core;

import java.lang.reflect.Field;

/**
 * Class describing dependency.
 */
public class Dependency {

	private BeanConfig beanConfig;

	private String beanName;

	private Field field;

	private boolean nullAllowed;

	private Class<?> type;

	private Class<?> subType;

	/**
	 * Creates instance of class.
	 * 
	 * @param beanConfig
	 *            definition of bean.
	 */
	public Dependency(BeanConfig beanConfig) {
		this.beanConfig = beanConfig;
	}

	/**
	 * Returns definition of bean.
	 * 
	 * @return definition of bean.
	 */
	public BeanConfig getBeanConfig() {
		return beanConfig;
	}

	/**
	 * Returns name of dependent bean.
	 * 
	 * @return name of dependent bean, or <code>null</code> if name is not
	 *         specified.
	 * 
	 */
	public String getBeanName() {
		return beanName;
	}

	/**
	 * Returns field to be filled by dependency.
	 * 
	 * @return field.
	 */
	public Field getField() {
		return field;
	}

	public Class<?> getSubType() {
		return subType;
	}

	/**
	 * Returns type of wanted bean.
	 * 
	 * @return type of bean.
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * Checks if empty value may be injected.
	 * 
	 * @return <code>true</code> if dependency is optional.
	 */
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

	public void setSubType(Class<?> subType) {
		this.subType = subType;
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
