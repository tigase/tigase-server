package tigase.kernel;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import tigase.util.Base64;
import tigase.util.TigaseStringprepException;
import tigase.xml.XMLUtils;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

/**
 * Util class to convert types.
 */
public class TypesConverter {

	private static final String[] decoded = { "," };
	private static final String[] encoded = { "\\," };
	private static final String[] decoded_1 = { "," };
	private static final String[] encoded_1 = { "\\," };

	private final static String regex = "(?<!\\\\)" + Pattern.quote(",");

	private TypesConverter() {
	}

	/**
	 * Converts object to String.
	 *
	 * @param value
	 *            object to convert.
	 * @return text representation of value.
	 */
	public static String toString(final Object value) {
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

	/**
	 * Converts value to expected type.
	 *
	 * @param value
	 *            value to be converted.
	 * @param expectedType
	 *            class of expected type.
	 * @param <T>
	 *            expected type.
	 * @return converted value.
	 */
	public static <T> T convert(final Object value, final Class<T> expectedType) {
		return convert(value, expectedType, null);
	}
	
	public static <T> T convert(final Object value, final Class<T> expectedType, Class<?> itemType) {		
		try {
			if (value == null)
				return null;

			final Class<?> currentType = value.getClass();

			if (expectedType.isAssignableFrom(currentType)) {
				return expectedType.cast(value);
			}

			if (expectedType.equals(Class.class)) {
				try {
					return expectedType.cast(Class.forName(value.toString().trim()));
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
				String[] a_str = value.toString().split(regex);
				Object result = Array.newInstance(expectedType.getComponentType(), a_str.length);
				for (int i = 0; i < a_str.length; i++) {
					Array.set(result, i, TypesConverter.convert(unescape(a_str[i]), expectedType.getComponentType()));
				}
				return (T) result;
			} else if (Collection.class.isAssignableFrom(expectedType) && itemType != null) {
				int mod = expectedType.getModifiers();
				if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {
					String[] a_str = value.toString().split(regex);
					try {
						Collection result = (Collection) expectedType.newInstance();
						for (int i = 0; i < a_str.length; i++) {
							result.add(TypesConverter.convert(unescape(a_str[i]), itemType));
						}
						return (T) result;
					} catch (InstantiationException | IllegalAccessException ex) {
						throw new RuntimeException("Unsupported conversion to " + expectedType, ex);
					}
				}
			}

			throw new RuntimeException("Unsupported conversion to " + expectedType);
		} catch (TigaseStringprepException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
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

}
