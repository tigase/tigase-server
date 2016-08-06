/*
 * DefaultTypesConverter.java
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

package tigase.kernel;

import tigase.kernel.beans.Bean;
import tigase.osgi.ModulesManagerImpl;
import tigase.util.Base64;
import tigase.util.TigaseStringprepException;
import tigase.xml.XMLUtils;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Bean(name = "defaultTypesConverter")
public class DefaultTypesConverter implements TypesConverter {

	private static final String[] decoded = {","};
	private static final String[] encoded = {"\\,"};
	private static final String[] decoded_1 = {","};
	private static final String[] encoded_1 = {"\\,"};

	private final static String regex = "(?<!\\\\)" + Pattern.quote(",");

	private static String escape(String input) {
		if (input != null) {
			return XMLUtils.translateAll(input, decoded, encoded);
		} else {
			return null;
		}
	}

	private static String unescape(String input) {
		if (input != null) {
			return XMLUtils.translateAll(input, encoded_1, decoded_1);
		} else {
			return null;
		}
	}

	/**
	 * Converts value to expected type.
	 *
	 * @param value        value to be converted.
	 * @param expectedType class of expected type.
	 * @param <T>          expected type.
	 * @return converted value.
	 */
	public <T> T convert(final Object value, final Class<T> expectedType) {
		return convert(value, expectedType, null);
	}

	public <T> T convert(final Object value, final Type type) {
		if (type instanceof Class)
			return convert(value, (Class<T>) type);
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			if (pt.getRawType() instanceof Class)
				return convert(value, (Class<T>) pt.getRawType(), pt);
		}

		throw new RuntimeException("Cannot convert to " + type);
	}

	public <T> T convert(final Object value, final Class<T> expectedType, Type genericType) {
		try {
			if (value == null)
				return null;

			final Class<?> currentType = value.getClass();

			if (expectedType.isAssignableFrom(currentType) && genericType == null) {
				return expectedType.cast(value);
			}

			if (expectedType.equals(Class.class)) {
				try {
					return expectedType.cast(ModulesManagerImpl.getInstance().forName(value.toString().trim()));
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Cannot convert to " + expectedType, e);
				}
			} else if (expectedType.equals(File.class)) {
				return expectedType.cast(new File(value.toString().trim()));
			} else if (expectedType.equals(Level.class)) {
				return expectedType.cast(Level.parse(value.toString().trim()));
			} else if (expectedType.isEnum()) {
				final Class<? extends Enum> enumType = (Class<? extends Enum>) expectedType;
				final Enum<?> theOneAndOnly = Enum.valueOf(enumType, value.toString().trim());
				return expectedType.cast(theOneAndOnly);
			} else if (expectedType.equals(JID.class)) {
				return expectedType.cast(JID.jidInstance(value.toString().trim()));
			} else if (expectedType.equals(BareJID.class)) {
				return expectedType.cast(BareJID.bareJIDInstance(value.toString().trim()));
			} else if (expectedType.equals(String.class)) {
				return expectedType.cast(String.valueOf(value.toString().trim()));
			} else if (expectedType.equals(Long.class)) {
				return expectedType.cast(Long.valueOf(value.toString().trim()));
			} else if (expectedType.equals(Integer.class)) {
				return expectedType.cast(Integer.valueOf(value.toString().trim()));
			} else if (expectedType.equals(Boolean.class)) {
				String val = value.toString().trim();
				boolean b = (val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equalsIgnoreCase("on")
						|| val.equals("1"));
				return expectedType.cast(Boolean.valueOf(b));
			} else if (expectedType.equals(Float.class)) {
				return expectedType.cast(Float.valueOf(value.toString().trim()));
			} else if (expectedType.equals(Double.class)) {
				return expectedType.cast(Double.valueOf(value.toString().trim()));
			} else if (expectedType.equals(char.class)) {
				String v = value.toString().trim();
				if (v.length() == 1)
					return (T) Character.valueOf(v.charAt(0));
				else
					throw new RuntimeException("Cannot convert '" + v + "' to char.");
			} else if (expectedType.equals(int.class)) {
				return (T) Integer.valueOf(value.toString().trim());
			} else if (expectedType.equals(byte.class)) {
				return (T) Byte.valueOf(value.toString().trim());
			} else if (expectedType.equals(long.class)) {
				return (T) Long.valueOf(value.toString().trim());
			} else if (expectedType.equals(double.class)) {
				return (T) Double.valueOf(value.toString().trim());
			} else if (expectedType.equals(short.class)) {
				return (T) Short.valueOf(value.toString().trim());
			} else if (expectedType.equals(float.class)) {
				return (T) Float.valueOf(value.toString().trim());
			} else if (expectedType.equals(boolean.class)) {
				String val = value.toString().trim();
				boolean b = (val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equalsIgnoreCase("on")
						|| val.equals("1"));
				return (T) Boolean.valueOf(b);
			} else if (expectedType.equals(byte[].class) && value.toString().startsWith("string:")) {
				return (T) value.toString().substring(7).getBytes();
			} else if (expectedType.equals(byte[].class) && value.toString().startsWith("base64:")) {
				return (T) Base64.decode(value.toString().substring(7));
			} else if (expectedType.equals(char[].class) && value.toString().startsWith("string:")) {
				return (T) value.toString().substring(7).toCharArray();
			} else if (expectedType.equals(char[].class) && value.toString().startsWith("base64:")) {
				return (T) (new String(Base64.decode(value.toString().substring(7)))).toCharArray();
			} else if (expectedType.isArray()) {
				if (value instanceof Collection) {
					Collection col = (Collection) value;
					Object result = Array.newInstance(expectedType.getComponentType(), col.size());
					Iterator it = col.iterator();
					int i = 0;
					while (it.hasNext()) {
						Object v = it.next();
						Array.set(result, i, (v instanceof String) ? convert(unescape((String) v), expectedType.getComponentType()) : v);
						i++;
					}
					return (T) result;
				} else {
					String[] a_str = value.toString().split(regex);
					Object result = Array.newInstance(expectedType.getComponentType(), a_str.length);
					for (int i = 0; i < a_str.length; i++) {
						Array.set(result, i, convert(unescape(a_str[i]), expectedType.getComponentType()));
					}
					return (T) result;
				}
			} else if (EnumSet.class.isAssignableFrom(expectedType) && genericType instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) genericType;
				Type[] actualTypes = pt.getActualTypeArguments();
				if (actualTypes[0] instanceof Class) {
					String[] a_str = value.toString().split(regex);
					HashSet<Enum> result = new HashSet<>();
					for (int i = 0; i < a_str.length; i++) {
						result.add((Enum) convert(unescape(a_str[i]), (Class<?>) actualTypes[0]));
					}

					return (T) EnumSet.copyOf(result);
				}
			} else if (Pattern.class.isAssignableFrom(expectedType)) {
				return (T) Pattern.compile(value.toString());
			} else if (Collection.class.isAssignableFrom(expectedType) && genericType != null) {
				int mod = expectedType.getModifiers();
				if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod) && genericType instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) genericType;
					Type[] actualTypes = pt.getActualTypeArguments();
					if (actualTypes[0] instanceof Class) {
						if (value instanceof Collection) {
							try {
								Collection result = (Collection) expectedType.newInstance();
								for (Object c : ((Collection) value)) {
									result.add(c);
								}
								return (T) result;
							} catch (InstantiationException | IllegalAccessException ex) {
								throw new RuntimeException("Unsupported conversion to " + expectedType, ex);
							}
						} else {
							String[] a_str = value.toString().split(regex);
							try {
								Collection result = (Collection) expectedType.newInstance();
								for (int i = 0; i < a_str.length; i++) {
									result.add(convert(unescape(a_str[i]), (Class<?>) actualTypes[0]));
								}
								return (T) result;
							} catch (InstantiationException | IllegalAccessException ex) {
								throw new RuntimeException("Unsupported conversion to " + expectedType, ex);
							}
						}
					}
				}
			} else if (Map.class.isAssignableFrom(expectedType) && genericType instanceof ParameterizedType && value instanceof Map) {
				// this is additional support for convertion to type of Map, however value needs to be instance of Map
				// Added mainly for BeanConfigurators to be able to configure Map fields
				int mod = expectedType.getModifiers();
				if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {
					ParameterizedType pt = (ParameterizedType) genericType;
					Type[] actualTypes = pt.getActualTypeArguments();
					try {
						Map result = (Map) expectedType.newInstance();
						for (Map.Entry<String, String> e : ((Map<String, String>) value).entrySet()) {
							Object k = convert(unescape(e.getKey()), actualTypes[0]);
							Object v = convert(unescape(e.getValue()), actualTypes[1]);
							result.put(k, v);
						}
						return (T) result;
					} catch (InstantiationException | IllegalAccessException ex) {
						throw new RuntimeException("Unsupported conversion to " + expectedType, ex);
					}
				}
			} else {
				// here we try to assign instances of class passed as paramter if possible
				try {
					Class<?> cls = ModulesManagerImpl.getInstance().forName(value.toString());
					if (expectedType.isAssignableFrom(cls)) {
						return (T) cls.newInstance();
					}
				} catch (ClassNotFoundException ex) {
					// ignoring this
				} catch (InstantiationException | IllegalAccessException ex) {
					throw new RuntimeException("Could not instantiate instance of " + value.toString());
				}
			}

			{
				throw new RuntimeException("Unsupported conversion to " + expectedType);
			}
		} catch (TigaseStringprepException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Converts object to String.
	 *
	 * @param value object to convert.
	 * @return text representation of value.
	 */
	public String toString(final Object value) {
		if (value == null)
			return null;
		if (value.getClass().isEnum()) {
			return ((Enum) value).name();
		} else if (value instanceof Collection) {
			StringBuilder sb = new StringBuilder();
			Iterator it = ((Collection) value).iterator();
			while (it.hasNext()) {
				sb.append(escape(toString(it.next())));
				if (it.hasNext())
					sb.append(',');
			}
			return sb.toString();
		} else if (value.getClass().isArray()) {
			StringBuilder sb = new StringBuilder();
			final int l = Array.getLength(value);
			for (int i = 0; i < l; i++) {
				Object o = Array.get(value, i);
				sb.append(escape(toString(o)));
				if (i + 1 < l)
					sb.append(',');

			}
			return sb.toString();
		} else
			return value.toString();
	}

}
