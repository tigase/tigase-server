package tigase.kernel;

import tigase.util.Base64;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

public class TypesConverter {

	public static String toString(final Object value) {
		if (value.getClass().isEnum()) {
			return ((Enum) value).name();
		} else if (value instanceof Collection) {
			StringBuilder sb = new StringBuilder();
			Iterator it = ((Collection) value).iterator();
			while (it.hasNext()) {
				sb.append(toString(it.next()));
				if (it.hasNext())
					sb.append(',');
			}
			return sb.toString();
		} else if (value.getClass().isArray()) {
			StringBuilder sb = new StringBuilder();
			final int l = Array.getLength(value);
			for (int i = 0; i < l; i++) {
				Object o = Array.get(value, i);
				sb.append(toString(o));
				if (i + 1 < l)
					sb.append(',');

			}
			return sb.toString();
		} else
			return value.toString();
	}

	public static <T> T convert(final Object value, final Class<T> expectedType) {
		try {
			if (value == null)
				return null;

			final Class<?> currentType = value.getClass();

			if (expectedType.isAssignableFrom(currentType)) {
				return expectedType.cast(value);
			}

			if (expectedType.isEnum()) {
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
				boolean b = (val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equalsIgnoreCase("on") || val.equals("1"));
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
				boolean b = (val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("true") || val.equalsIgnoreCase("on") || val.equals("1"));
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
				String[] a_str = value.toString().split(",");
				Object result = Array.newInstance(expectedType.getComponentType(), a_str.length);
				for (int i = 0; i < a_str.length; i++) {
					Array.set(result, i, TypesConverter.convert(a_str[i], expectedType.getComponentType()));
				}
				return (T) result;
			}

			throw new RuntimeException("Unsupported conversion to " + expectedType);
		} catch (TigaseStringprepException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
