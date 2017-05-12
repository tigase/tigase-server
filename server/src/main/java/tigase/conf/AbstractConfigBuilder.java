/*
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.conf;

import tigase.kernel.beans.config.AbstractBeanConfigurator;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created by andrzej on 11.05.2017.
 */
public class AbstractConfigBuilder<T extends Map, S extends AbstractConfigBuilder> {

	protected final T map;

	protected AbstractConfigBuilder(T map) {
		this.map = map;
	}

	public AbstractBeanConfigurator.BeanDefinition.Builder bean() {
		return new AbstractBeanConfigurator.BeanDefinition.Builder(map);
	}

	public void property(String key, Object value) {
		map.put(key, value);
	}

	public T build() {
		return map;
	}

	public S with(AbstractBeanConfigurator.BeanDefinition... beans) {
		for (AbstractBeanConfigurator.BeanDefinition bean : beans) {
			map.put(bean.getBeanName(), bean);
		}
		return (S) this;
	}

	public S with(String property, Optional optional) {
		if (optional.isPresent()) {
			with(property, optional.get());
		}
		return (S) this;
	}

	public S with(String property, Object value) {
		if (value instanceof Optional) {
			return with(property, (Optional) value);
		}
		map.put(property, value);
		return (S) this;
	}

	public S withBean(Consumer<AbstractBeanConfigurator.BeanDefinition.Builder>... builders) {
		for (Consumer<AbstractBeanConfigurator.BeanDefinition.Builder> builder : builders) {
			AbstractBeanConfigurator.BeanDefinition.Builder defBuilder = new AbstractBeanConfigurator.BeanDefinition.Builder();
			builder.accept(defBuilder);
			with(defBuilder.build());
		}
		return (S) this;
	}
}