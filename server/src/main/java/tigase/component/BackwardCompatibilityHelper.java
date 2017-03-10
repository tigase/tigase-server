/*
 * BackwardCompatibilityHelper.java
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
 */
package tigase.component;

import tigase.conf.Configurable;
import tigase.db.AuthRepository;
import tigase.db.UserRepository;
import tigase.kernel.core.Kernel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by andrzej on 12.08.2016.
 */
public class BackwardCompatibilityHelper {

	public static Map<String, Object> getDefConfigParams(Kernel kernel, String configType, String dbUri, Map<String, Object> params) {
		Map<String, Object> initProperties = new HashMap<>();
		initProperties.put("config-type", configType);
		for (Map.Entry<String, Object> e : params.entrySet()) {
			if (e.getKey().startsWith("-")) {
				initProperties.put(e.getKey(), e.getValue());
			}
		}

		// Injecting default DB URI for backward compatibility
		initProperties.put(Configurable.USER_REPO_URL_PROP_KEY, dbUri);
		initProperties.put(Configurable.GEN_USER_DB_URI, dbUri);
		UserRepository userRepo = kernel.getInstance(UserRepository.class);
		initProperties.put(Configurable.SHARED_USER_REPO_PROP_KEY, userRepo);
		AuthRepository authRepo = kernel.getInstance(AuthRepository.class);
		initProperties.put(Configurable.SHARED_AUTH_REPO_PROP_KEY, authRepo);

		return initProperties;
	}

	public static Map<String, Object> fillProps(Map<String, Object> beanProperties) {
		Map<String, Object> result = new HashMap<>();

		for (Map.Entry<String, Object> e : beanProperties.entrySet()) {
			String key = e.getKey();
			Object value = e.getValue();
			if (value instanceof Collection) {
				value = convertToArray((Collection) value);
				if (value != null) {
					result.put(key, value);
				}
			} if (value instanceof Map) {
				String prefix = key;
				for (Map.Entry<String, Object> e1 : ((Map<String, Object>) value).entrySet()) {
					result.put(key + "/" + e1.getKey(), e1.getValue());
				}
			} else {
				result.put(key, value);
			}
		}

		return result;
	}

	public static Object convertToArray(Collection collection) {
		Iterator iter = collection.iterator();
		if (!iter.hasNext())
			return null;

		Class objCls = iter.next().getClass();
		if (objCls == Integer.class) {
			return convertToIntArray(collection);
		} else if (objCls == Long.class) {
			return convertToLongArray(collection);
		} else if (objCls == Double.class) {
			return convertToDoubleArray(collection);
		} else if (objCls == Float.class) {
			return convertToFloatArray(collection);
		} else if (objCls == Boolean.class) {
			return convertToBoolArray(collection);
		} else if (objCls == String.class) {
			return convertToStringArray(collection);
		}
		return null;
	}

	public static Object convertToIntArray(Collection col) {
		int[] arr = new int[col.size()];
		int pos = 0;
		Iterator iter = col.iterator();
		while (iter.hasNext()) {
			Number v = (Number) iter.next();
			arr[pos++] = v.intValue();
		}
		return arr;
	}

	public static Object convertToLongArray(Collection col) {
		long[] arr = new long[col.size()];
		int pos = 0;
		Iterator iter = col.iterator();
		while (iter.hasNext()) {
			Number v = (Number) iter.next();
			arr[pos++] = v.longValue();
		}
		return arr;
	}

	public static Object convertToDoubleArray(Collection col) {
		double[] arr = new double[col.size()];
		int pos = 0;
		Iterator iter = col.iterator();
		while (iter.hasNext()) {
			Number v = (Number) iter.next();
			arr[pos++] = v.doubleValue();
		}
		return arr;
	}

	public static Object convertToFloatArray(Collection col) {
		float[] arr = new float[col.size()];
		int pos = 0;
		Iterator iter = col.iterator();
		while (iter.hasNext()) {
			Number v = (Number) iter.next();
			arr[pos++] = v.floatValue();
		}
		return arr;
	}

	public static Object convertToBoolArray(Collection col) {
		boolean[] arr = new boolean[col.size()];
		int pos = 0;
		Iterator iter = col.iterator();
		while (iter.hasNext()) {
			Boolean v = (Boolean) iter.next();
			arr[pos++] = v.booleanValue();
		}
		return arr;
	}

	public static Object convertToStringArray(Collection col) {
		String[] arr = new String[col.size()];
		int pos = 0;
		Iterator iter = col.iterator();
		while (iter.hasNext()) {
			String v = (String) iter.next();
			arr[pos++] = v;
		}
		return arr;
	}

}
