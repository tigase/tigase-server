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
package tigase.kernel.beans.selector;

import tigase.kernel.beans.BeanSelector;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.Kernel;

import java.util.Arrays;
import java.util.List;

/**
 * Created by andrzej on 26.04.2017.
 */
public class ServerBeanSelector
		implements BeanSelector {

	private static boolean checkClusterMode(Class clazz, Kernel kernel) {
		ClusterModeRequired clusterModeRequired = (ClusterModeRequired) clazz.getAnnotation(ClusterModeRequired.class);
		return clusterModeRequired == null || clusterModeRequired.active() == getClusterMode(kernel);
	}

	private static boolean checkConfigType(Class clazz, Kernel kernel) {
		ConfigType configType = (ConfigType) clazz.getAnnotation(ConfigType.class);
		if (configType == null) {
			return true;
		}
		List<ConfigTypeEnum> supportedTypes = Arrays.asList(configType.value());

		ConfigTypeEnum activeConfigType = getConfigType(kernel);

		return supportedTypes.contains(activeConfigType);
	}

	public static boolean getClusterMode(Kernel kernel) {
		Object val = getProperty(kernel, "cluster-mode", false);
		if (val instanceof Boolean) {
			return (Boolean) val;
		} else {
			return Boolean.valueOf((String) val);
		}

	}

	public static ConfigTypeEnum getConfigType(Kernel kernel) {
		while (kernel.getParent() != null) {
			kernel = kernel.getParent();
		}

		String type = getProperty(kernel, "config-type", "default");
		switch (type) {
			case "--gen-config-def":
			case "--gen-config-default":
				type = "default";
			default:
				break;
		}
		return ConfigTypeEnum.valueForId(type);
	}

	protected static <T> T getProperty(Kernel kernel, String name, T defValue) {
		if (kernel.isBeanClassRegistered(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)) {
			return (T) kernel.getInstance(AbstractBeanConfigurator.class).getProperties().getOrDefault(name, defValue);
		}
		return defValue;
	}

	@Override
	public boolean shouldRegister(Class clazz, Kernel kernel) {
		return checkClusterMode(clazz, kernel) && checkConfigType(clazz, kernel);
	}
}
