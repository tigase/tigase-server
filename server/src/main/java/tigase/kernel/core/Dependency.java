/*
 * Dependency.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package tigase.kernel.core;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Class describing dependency.
 */
public class Dependency {

	private BeanConfig beanConfig;

	private String beanName;

	private Field field;

	private boolean nullAllowed;

	private Class<?> type;

	private Type genericType;

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

	public Type getGenericType() {
		return genericType;
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

	public void setGenericType(Type genericType) { this.genericType = genericType; }

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
