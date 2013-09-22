/*
 * DataTypes.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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



package tigase.util;

//~--- JDK imports ------------------------------------------------------------

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created: May 28, 2009 7:39:07 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DataTypes {
	/** Field description */
	public static final Map<String, Character> typesMap = new LinkedHashMap<String,
																													Character>();

	//~--- static initializers --------------------------------------------------

	static {
		typesMap.put(String.class.getName(), 'S');
		typesMap.put(Long.class.getName(), 'L');
		typesMap.put(Integer.class.getName(), 'I');
		typesMap.put(Boolean.class.getName(), 'B');
		typesMap.put(Float.class.getName(), 'F');
		typesMap.put(Double.class.getName(), 'D');
		typesMap.put(String[].class.getName(), 's');
		typesMap.put(Long[].class.getName(), 'l');
		typesMap.put(Integer[].class.getName(), 'i');
		typesMap.put(Boolean[].class.getName(), 'b');
		typesMap.put(Float[].class.getName(), 'f');
		typesMap.put(Double[].class.getName(), 'd');
		typesMap.put(long[].class.getName(), 'l');
		typesMap.put(int[].class.getName(), 'i');
		typesMap.put(boolean[].class.getName(), 'b');
		typesMap.put(float[].class.getName(), 'f');
		typesMap.put(double[].class.getName(), 'd');
	}

	//~--- methods --------------------------------------------------------------

	// public static char[] sizeChars = {'k', 'K', 'm', 'M', 'g', 'G', 't', 'T'};

	/**
	 * Method description
	 *
	 *
	 * @param num
	 * @param cls
	 * @param def
	 * @param <T>
	 *
	 * 
	 */
	public static <T extends Number> T parseNum(String num, Class<T> cls, T def) {
		if (num == null) {
			return def;
		}

		T result        = def;
		String toParse  = num;
		long multiplier = 1;

		try {
			switch (num.charAt(num.length() - 1)) {
			case 'k' :
			case 'K' :
				multiplier = 1024;
				toParse    = num.substring(0, num.length() - 1);

				break;

			case 'm' :
			case 'M' :
				multiplier = 1024 * 1024;
				toParse    = num.substring(0, num.length() - 1);

				break;

			case 'g' :
			case 'G' :
				multiplier = 1024 * 1024 * 1024;
				toParse    = num.substring(0, num.length() - 1);

				break;
			}
			result = cls.cast(Long.valueOf(Long.parseLong(toParse) * multiplier));
		} catch (Exception e) {
			return def;
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param size
	 * @param def
	 *
	 * 
	 */
	public static int parseSizeInt(String size, int def) {
		return parseNum(size, Integer.class, def);
	}

	/**
	 * Method description
	 *
	 *
	 * @param val
	 *
	 * 
	 */
	public static boolean parseBool(final String val) {
		return (val != null) &&
					 (val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") ||
						val.equalsIgnoreCase("on") || val.equals("1"));
	}

	/**
	 * Method description
	 *
	 *
	 * @param typeId
	 * @param value
	 *
	 * 
	 *
	 * @throws IllegalArgumentException
	 */
	public static Object decodeValueType(char typeId, String value)
					throws IllegalArgumentException {
		Object result = value.trim();

		try {
			switch (typeId) {
			case 'L' :

				// Long value
				result = Long.decode(value.trim());

				break;

			case 'I' :

				// Integer value
				result = Integer.decode(value.trim());

				break;

			case 'B' :

				// Boolean value
				result = parseBool(value.trim());

				break;

			case 'F' :

				// Float value
				result = Float.parseFloat(value.trim());

				break;

			case 'D' :

				// Double value
				result = Double.parseDouble(value.trim());

				break;

			case 's' :

				// Comma separated, Strings array
				String[] s_str      = value.split(",");
				String[] trimed_str = new String[s_str.length];
				int si              = 0;

				for (String s : s_str) {
					trimed_str[si++] = s.trim();
				}
				result = trimed_str;

				break;

			case 'l' :

				// Comma separated, long array
				String[] longs_str = value.split(",");
				long[] longs       = new long[longs_str.length];
				int l              = 0;

				for (String s : longs_str) {
					longs[l++] = Long.parseLong(s.trim());
				}
				result = longs;

				break;

			case 'i' :

				// Comma separated, int array
				String[] ints_str = value.split(",");
				int[] ints        = new int[ints_str.length];
				int i             = 0;

				for (String s : ints_str) {
					ints[i++] = Integer.parseInt(s.trim());
				}
				result = ints;

				break;

			case 'b' :

				// Comma separated, boolean array
				String[] bools_str = value.split(",");
				boolean[] bools    = new boolean[bools_str.length];
				int b              = 0;

				for (String s : bools_str) {
					bools[b++] = parseBool(s.trim());
				}
				result = bools;

				break;

			case 'f' :

				// Comma separated, float array
				String[] float_str = value.split(",");
				float[] floats     = new float[float_str.length];
				int f              = 0;

				for (String s : float_str) {
					floats[f++] = Float.parseFloat(s.trim());
				}
				result = floats;

				break;

			case 'd' :

				// Comma separated, double array
				String[] doubles_str = value.split(",");
				double[] doubles     = new double[doubles_str.length];
				int d                = 0;

				for (String s : doubles_str) {
					doubles[d++] = Double.parseDouble(s.trim());
				}
				result = doubles;

				break;

			default :

				// Do nothing, default to String
				break;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param value
	 *
	 * 
	 */
	public static String valueToString(Object value) {
		char t = getTypeId(value);
		String varr;

		if (value == null) {
			varr = "<null>";
		} else {
			varr = value.toString();
		}
		switch (t) {
		case 'l' :
			varr = Arrays.toString((long[]) value);

			break;

		case 'i' :
			varr = Arrays.toString((int[]) value);

			break;

		case 'b' :
			varr = Arrays.toString((boolean[]) value);

			break;

		case 'f' :
			varr = Arrays.toString((float[]) value);

			break;

		case 'd' :
			varr = Arrays.toString((double[]) value);

			break;

		default :
			if ((value != null) && value.getClass().isArray()) {
				varr = Arrays.toString((Object[]) value);
			}
		}
		if ((value != null) && value.getClass().isArray()) {
			varr = varr.substring(1, varr.length() - 1);
		}

		return varr;
	}

	/**
	 * Method description
	 *
	 *
	 * @param name
	 *
	 * 
	 */
	public static char decodeTypeIdFromName(String name) {
		char result = 'S';

		if (name.endsWith("]")) {
			result = name.charAt(name.length() - 2);
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param name
	 *
	 * 
	 */
	public static String stripNameFromTypeId(String name) {
		if (name.endsWith("]")) {
			return name.substring(0, name.length() - 3);
		} else {
			return name;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param name
	 * @param value
	 *
	 * 
	 */
	public static String encodeTypeIdInName(String name, Object value) {
		char t = DataTypes.getTypeId(value);

		return name + "[" + t + "]";
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param instance
	 *
	 * 
	 */
	public static char getTypeId(Object instance) {
		Character result;

		if (instance == null) {
			result = 'S';
		} else {
			result = typesMap.get(instance.getClass().getName());
		}
		if (result == null) {
			result = 'S';
		}

		return result.charValue();
	}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 * @param def
	 *
	 * 
	 */
	public static boolean getProperty(String key, Boolean def) {
		String val = System.getProperty(key, (def != null)
						? def.toString()
						: null);

		return parseBool(val);
	}
}


//~ Formatted in Tigase Code Convention on 13/03/04
