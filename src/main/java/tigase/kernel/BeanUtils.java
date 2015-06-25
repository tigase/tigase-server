package tigase.kernel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BeanUtils {

	public static Method[] getAllMethods(Class<?> klass) {
		List<Method> fields = new ArrayList<Method>();
		fields.addAll(Arrays.asList(klass.getDeclaredMethods()));
		if (klass.getSuperclass() != null) {
			fields.addAll(Arrays.asList(getAllMethods(klass.getSuperclass())));
		}
		return fields.toArray(new Method[] {});
	}

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

	public static ArrayList<Method> prepareSetterMethods(Class<?> destination, String fieldName) {
		String t = prepareAccessorMainPartName(fieldName);
		ArrayList<Method> result = new ArrayList<Method>();
		try {
			for (Method m : getAllMethods(destination)) {
				if (m.getName().equals("set" + t)) {
					result.add(m);
				}
			}
			return result;
		} catch (Exception e) {
			return null;
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

	public static void setValue(Object toBean, String fieldName, Object valueToSet) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
		ArrayList<Method> setters = BeanUtils.prepareSetterMethods(toBean.getClass(), fieldName);

		if (setters == null || setters.isEmpty()) {
			throw new NoSuchMethodException("No setter for property '" + fieldName + "'.");
		}

		for (final Method s : setters) {
			try {
				s.invoke(toBean, valueToSet);
				return;
			} catch (Exception e) {
			}
		}

		throw new IllegalArgumentException("Cannot set value type " + valueToSet.getClass().getName() + " to property '"
				+ fieldName + "'.");
	}

	private BeanUtils() {
	}

}
