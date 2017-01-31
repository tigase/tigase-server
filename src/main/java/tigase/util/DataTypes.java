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
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.xmpp.JID;

import java.lang.reflect.Array;

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

	private static final Logger log = Logger.getLogger(DataTypes.class.getName());
	//~--- static initializers --------------------------------------------------

	static {
		typesMap.put(String.class.getName(), 'S');
		typesMap.put(Long.class.getName(), 'L');
		typesMap.put(Integer.class.getName(), 'I');
		typesMap.put(Boolean.class.getName(), 'B');
		typesMap.put(Float.class.getName(), 'F');
		typesMap.put(Double.class.getName(), 'D');
		typesMap.put(JID.class.getName(), 'J');
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
		typesMap.put(JID[].class.getName(), 'j');
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
		Long multiplier = 1L;

		try {
			switch (num.charAt(num.length() - 1)) {
			case 'k' :
			case 'K' :
				multiplier = 1024L;
				toParse    = num.substring(0, num.length() - 1);

				break;

			case 'm' :
			case 'M' :
				multiplier = 1024L * 1024L;
				toParse    = num.substring(0, num.length() - 1);

				break;

			case 'g' :
			case 'G' :
				multiplier = 1024L * 1024L * 1024L;
				toParse    = num.substring(0, num.length() - 1);

				break;
			}

			if(cls.equals(Integer.class)) {
				result = cls.cast(Integer.valueOf(toParse) * multiplier.intValue());
			} else if(cls.equals(Long.class)) {
				result = cls.cast(Long.valueOf(toParse) * multiplier);
			} else if(cls.equals(Double.class)) {
				result = cls.cast(Double.valueOf(toParse) * multiplier.doubleValue());
			} else if(cls.equals(Float.class)) {
				result = cls.cast(Float.valueOf(toParse) * multiplier.floatValue());
			} else if(cls.equals(Byte.class)) {
				Integer res = Byte.valueOf(toParse) * multiplier.byteValue();
				result = cls.cast(res.byteValue());
			} else if(cls.equals(Short.class)) {
				Integer res = Short.valueOf(toParse) * multiplier.shortValue();
				result = cls.cast(res.shortValue());
			}
		} catch (Exception e) {
			log.log( Level.WARNING, "Error parsing value: {0} as {1}, using default: {2}", new Object[] {num,cls,def});
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
				
			case 'J' :
				result = JID.jidInstance(value);
				
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
						
			case 'j' :
				String[] jids_str = value.split(",");
				JID[] jids = new JID[jids_str.length];
				int j = 0;
				
				for (String s : jids_str) {
					jids[j++] = JID.jidInstance(s);
				}
				result = jids;
				
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
public static String valueToString(final Object value) {

  if (value == null) {
    return "<null>";
  }

  if(value.getClass().isArray()) {

    if(Array.getLength(value) == 0) {
      return "";
    }

    String varr = null;
    char t = DataTypes.getTypeId(value);
    switch (t) {
    case 'l' :
      varr = value instanceof long[] ? Arrays.toString((long[]) value) : Arrays.toString((Long[]) value);
      break;

    case 'i' :
      varr = value instanceof int[] ? Arrays.toString((int[]) value) : Arrays.toString((Integer[]) value);
      break;

    case 'b' :
      varr = value instanceof boolean[] ? Arrays.toString((boolean[]) value) : Arrays.toString((Boolean[]) value);
      break;

    case 'f' :
      varr = value instanceof float[] ? Arrays.toString((float[]) value) : Arrays.toString((Float[]) value);
      break;

    case 'd' :
      varr = value instanceof double[] ? Arrays.toString((double[]) value) : Arrays.toString((Double[]) value);
      break;

    default :
      varr = Arrays.toString((Object[]) value);
    }
    return varr.substring(1, varr.length() - 1);
  }

  return value.toString();
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
	public static void main( String[] args ) {
		System.out.println( parseSizeInt( "256k", 1 ) );
		System.out.println( parseNum("256k", Integer.class, 1 ) );
		System.out.println( parseNum("655k", Double.class, 1D ) );
		System.out.println( parseNum("256k", Float.class, 1F ) );
		System.out.println( parseNum("256k", Long.class, 1L ) );
		System.out.println( parseNum("25", Short.class, Short.valueOf( "1") ) );
		System.out.println( parseNum("25", Byte.class, (byte)1 ) );
	}
}
//~ Formatted in Tigase Code Convention on 13/03/04
