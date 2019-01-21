/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This is internal configuration of each bean. It stores name of bean, dependencies, state of bean etc.
 */
public class BeanConfig {

	public enum Source {
		hardcoded,
		annotation,
		configuration
	}
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
		registered,
		/**
		 * Bean class is registered, but it CANNOT be used!!! Should be treated as not registered at all.
		 */
		inactive,
	}
	private final String beanName;
	private final Class<?> clazz;
	private final Map<Field, Dependency> fieldDependencies = new HashMap<Field, Dependency>();
	private String beanInstanceName = null;
	private boolean exportable;
	private BeanConfig factory;
	private Kernel kernel;
	private boolean pinned = true;
	private Set<BeanConfig> registeredBeans = new HashSet<>();
	private Set<BeanConfig> registeredBy = new HashSet<>();
	private Source source = Source.hardcoded;
	private State state;

	BeanConfig(String id, Class<?> clazz) {
		super();
		this.beanName = id;
		this.clazz = clazz;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		BeanConfig other = (BeanConfig) obj;
		if (beanName == null) {
			if (other.beanName != null) {
				return false;
			}
		} else if (!beanName.equals(other.beanName)) {
			return false;
		}
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
	 * @return factory of bean. It may return <code>null</code> if default factory is used.
	 */
	public BeanConfig getFactory() {
		return factory;
	}

	void setFactory(final BeanConfig bfc) {
		this.factory = bfc;
	}

	/**
	 * Returns map of dependencies. Note that Kernel has field-based-dependency model, it means that each dependency
	 * must be related to field in class.
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

	void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	/**
	 * Returns state of bean.
	 *
	 * @return state of bean.
	 */
	public State getState() {
		return state;
	}

	void setState(State state) {
		this.state = state;
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
	 * @return <code>true</code> if beans will be visible in child Kernel (other Kernels deployed as beans to current
	 * Kernel).
	 */
	public boolean isExportable() {
		return exportable;
	}

	void setExportable(boolean value) {
		this.exportable = value;
	}

	/**
	 * Returns information if bean in pinned. If bean is pinned it will not be unloaded even if no other bean uses it.
	 * @return
	 */
	public boolean isPinned() {
		return pinned;
	}

	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}

	/**
	 * Returns information about source of the bean registration (annotation, code, config, etc.)
	 * @return
	 */
	public Source getSource() {
		return source;
	}

	void setSource(Source source) {
		this.source = source;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("BeanConfig{");
		sb.append("beanName='").append(beanName).append('\'');
		sb.append(", clazz=").append(clazz);
		sb.append(", exportable=").append(exportable);
		sb.append(", pinned=").append(pinned);
		sb.append(", factory=").append(factory);
		sb.append(", kernel=").append(kernel.getName());
		sb.append(", source=").append(source);
		sb.append(", state=").append(state);
		sb.append('}');
		return sb.toString();
	}

	/**
	 * List of beans registered by registration of this bean - related to <code>Bean::parent</code>.
	 * @return
	 */
	public Set<BeanConfig> getRegisteredBeans() {
		return registeredBeans;
	}
	
	public void addRegisteredBean(BeanConfig beanConfig) {
		registeredBeans.add(beanConfig);
	}

	public void removeRegisteredBean(BeanConfig beanConfig) {
		registeredBeans.remove(beanConfig);
	}

	public void addRegisteredBy(BeanConfig beanConfig) {
		registeredBy.add(beanConfig);
	}

	public boolean removeRegisteredBy(BeanConfig beanConfig) {
		registeredBy.remove(beanConfig);
		return registeredBy.isEmpty();
	}

	/**
	 * Set of beans which caused registration of this bean - related to <code>Bean::parent</code>.
	 * @return
	 */
	public Set<BeanConfig> getRegisteredBy() {
		return registeredBy;
	}

	protected String getBeanInstanceName() {
		return beanInstanceName == null ? getBeanName() : beanInstanceName;
	}

	protected void setBeanInstanceName(String beanInstanceName) {
		this.beanInstanceName = beanInstanceName;
	}
}
