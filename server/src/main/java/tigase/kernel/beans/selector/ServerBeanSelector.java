/*
 * ServerBeanSelector.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */package tigase.kernel.beans.selector;

import tigase.kernel.beans.BeanSelector;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.Kernel;

import java.util.Arrays;
import java.util.List;

/**
 * Created by andrzej on 26.04.2017.
 */
public class ServerBeanSelector implements BeanSelector {

	@Override
	public boolean shouldRegister(Class clazz, Kernel kernel) {
		return checkClusterMode(clazz, kernel) && checkConfigType(clazz, kernel);
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

	private static boolean checkClusterMode(Class clazz, Kernel kernel) {
		ClusterModeRequired clusterModeRequired = (ClusterModeRequired) clazz.getAnnotation(ClusterModeRequired.class);
		return clusterModeRequired == null || clusterModeRequired.active() == getClusterMode(kernel);
	}


	public static ConfigTypeEnum getConfigType(Kernel kernel) {
		while (kernel.getParent() != null) {
			kernel = kernel.getParent();
		}

		String type = "default";
		if (kernel.isBeanClassRegistered(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)) {
			type = (String) kernel.getInstance(AbstractBeanConfigurator.class).getProperties().getOrDefault("config-type", "default");
		}
		switch (type) {
			case "--gen-config-def":
			case "--gen-config-default":
				type = "default";
			default:
				break;
		}
		return ConfigTypeEnum.valueForId(type);
	}

	private static boolean getClusterMode(Kernel kernel) {
		return Boolean.valueOf(System.getProperty("cluster-mode", "false"));
	}
}
