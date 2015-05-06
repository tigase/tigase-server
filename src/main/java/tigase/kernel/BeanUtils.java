package tigase.kernel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BeanUtils {

	public static Object getValue(Object fromBean, Field field) throws IllegalAccessException, IllegalArgumentException,
	InvocationTargetException {
		Method setter = BeanUtils.prepareGetterMethod(field);
		if (setter != null) {
			return setter.invoke(fromBean);
		} else {
			field.setAccessible(true);
			return field.get(fromBean);
		}

	}

	public static String prepareAccessorMainPartName(final String fieldName) {
		if (fieldName.length() == 1) {
			return fieldName.toUpperCase();
		}

		String r;
		if (Character.isUpperCase(fieldName.charAt(1))) {
			r = fieldName.substring(0, 1);
		} else {
			r = fieldName.substring(0, 1).toUpperCase();
		}

		r += fieldName.substring(1);

		return r;
	}

	public static Method prepareGetterMethod(Field f) {
		String t = prepareAccessorMainPartName(f.getName());
		@SuppressWarnings("unused")
		String sm;
		String gm;
		if (f.getType().isPrimitive() && f.getType().equals(boolean.class)) {
			sm = "set" + t;
			gm = "is" + t;
		} else {
			sm = "set" + t;
			gm = "get" + t;
		}

		try {
			Method m = f.getDeclaringClass().getMethod(gm);
			return m;
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	public static Method prepareSetterMethod(Field f) {
		String t = prepareAccessorMainPartName(f.getName());
		String sm;
		@SuppressWarnings("unused")
		String gm;
		if (f.getType().isPrimitive() && f.getType().equals(boolean.class)) {
			sm = "set" + t;
			gm = "is" + t;
		} else {
			sm = "set" + t;
			gm = "get" + t;
		}

		try {
			Method m = f.getDeclaringClass().getMethod(sm, f.getType());
			return m;
		} catch (NoSuchMethodException e) {
			return null;
			// throw new KernelException("Class " +
			// f.getDeclaringClass().getName() + " has no setter of field " +
			// f.getName(), e);
		}
	}

	public static void setValue(Object toBean, Field field, Object valueToSet) throws IllegalAccessException,
	IllegalArgumentException, InvocationTargetException {
		Method setter = BeanUtils.prepareSetterMethod(field);
		if (setter != null) {
			setter.invoke(toBean, valueToSet);
		} else {
			field.setAccessible(true);
			field.set(toBean, valueToSet);
		}

	}

	private BeanUtils() {
	}

}
