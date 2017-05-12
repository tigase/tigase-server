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
package tigase.util.setup;

import tigase.cluster.ClusterConnectionManager;
import tigase.cluster.ClusterController;
import tigase.disteventbus.component.EventBusComponent;
import tigase.kernel.beans.Bean;
import tigase.server.MessageRouter;
import tigase.server.ServerComponent;
import tigase.server.xmppsession.SessionManager;
import tigase.stats.StatisticsCollector;
import tigase.vhosts.VHostManager;

import java.util.Arrays;
import java.util.List;

/**
 * Created by andrzej on 30.03.2017.
 */
public class BeanDefinition {

	public static final List<Class<? extends ServerComponent>> CORE_COMPONENTS = Arrays.asList(SessionManager.class,
																							   VHostManager.class,
																							   MessageRouter.class,
																							   ClusterConnectionManager.class,
																							   ClusterController.class,
																							   EventBusComponent.class,
																							   EventBusComponent.class,
																							   StatisticsCollector.class);

	private final String name;
	private final Class<?> clazz;
	private final boolean active;

	public BeanDefinition(Class<?> cls) {
		Bean bean = cls.getAnnotation(Bean.class);
		name = bean.name();
		clazz = cls;
		active = bean.active();
	}

	public String getName() {
		return name;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isCoreComponent() {
		return CORE_COMPONENTS.contains(clazz) ||
				CORE_COMPONENTS.stream().filter(cmp -> cmp.isAssignableFrom(clazz)).findAny().isPresent();
	}
}
