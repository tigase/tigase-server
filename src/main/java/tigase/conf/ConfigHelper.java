/*
* ConfigHelper.java
*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
*/
package tigase.conf;

import tigase.kernel.beans.config.AbstractBeanConfigurator;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by andrzej on 08.11.2016.
 */
public class ConfigHelper {

	public static Map<String, Object> merge(Map<String, Object>... props) {
		switch (props.length) {
			case 0:
				return null;
			case 1:
				return props[0];
			default:
				final Map<String, Object> mergedProps = new HashMap<>();
				for (Map<String, Object> config : props) {
					mergeConfigs(mergedProps, config);
				}
				return mergedProps;
		}
	}

	private static void mergeConfigs(Map<String, Object> result, Map<String, Object> input) {
		input.forEach((key, value) -> {
			Object currVal = result.get(key);
			if (currVal == null) {
				result.put(key, value);
			} else {
				if (currVal instanceof AbstractBeanConfigurator.BeanDefinition) {
					if (value instanceof AbstractBeanConfigurator.BeanDefinition) {
						result.put(key, value);
					} else if (value instanceof Map) {
						mergeConfigs((Map<String, Object>) currVal, (Map<String, Object>) value);
					}
				} else if (currVal instanceof Map && value instanceof Map) {
					mergeConfigs((Map<String, Object>) currVal, (Map<String, Object>) value);
				} else  {
					result.put(key, value);
				}
			}
		});
	}

}
