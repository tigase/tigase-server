/*
 * ReflectionHelper.java
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
package tigase.util;

import tigase.kernel.BeanUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Utility class with useful methods to work with reflections
 * 
 * @author andrzej
 */
public class ReflectionHelper {	
	
	/**
	 * This method collects every method of consumer class annotated with passed
	 * annotation and for each of them executes implementation of Handler.
	 *
	 * @param <A>
	 * @param <T>
	 * @param consumer
	 * @param annotationCls
	 * @param handler
	 * @return	non-null list of results returned by handler
	 */
	public static <A extends Annotation,T> Collection<T> collectAnnotatedMethods(final Object consumer, Class<A> annotationCls, Handler<A,T> handler) {
		ArrayList<T> result = new ArrayList<>();

		Method[] methods = BeanUtils.getAllMethods(consumer.getClass());

		for (Method method : methods) {
			A annotation = method.getAnnotation(annotationCls);
			if (annotation == null)
				continue;
			
			result.add(handler.process(consumer, method, annotation));
		}
		return result;
	}
	
	public interface Handler<A extends Annotation,T> {
		
		T process(Object consumer, Method method, A annotation);
		
	}
	
	public static Class<?> getItemClassOfGenericCollection(Field f) {
		Type genericType = f.getGenericType();
		if (genericType == null || !(genericType instanceof ParameterizedType))
			return null;
		
		Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
		if (actualTypeArguments == null || actualTypeArguments.length != 1)
			return null;
		
		Type type = actualTypeArguments[0];
		return (type instanceof Class) ? (Class) type : null;
	}

	public static Type getCollectionParamter(Type genericType, Class clazz) {
		if (genericType instanceof ParameterizedType) {
			return ((ParameterizedType) genericType).getActualTypeArguments()[0];
//			Type type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
//			if (!(type instanceof Class)) {
//				Map<TypeVariable<?>, Type> map = createGenericsTypeMap(clazz);
//				while (type instanceof TypeVariable) {
//					type = map.get((TypeVariable<?>) type);
//				}
//			}
//
//			if (!(type instanceof Class)) {
//
//			}
//			return (Class) type;
		}
		return null;
	}

	public static boolean classMatchesClassWithParameters(Class clazz, Class requiredType, Type[] requiredTypeParams) {
		ParameterizedType pt = new ParameterizedType() {
			@Override
			public Type[] getActualTypeArguments() {
				return requiredTypeParams;
			}

			@Override
			public Type getRawType() {
				return requiredType;
			}

			@Override
			public Type getOwnerType() {
				return null;
			}
		};
		return compareTypes(pt, clazz, null, null);
	}

	public static boolean boundMatch(Class c1, TypeVariable t2) {
		Type[] b2 = t2.getBounds();
		if (b2.length != 1)
			return false;

		return (b2[0] instanceof Class) && ((Class) b2[0]).isAssignableFrom(c1);
	}

	public static boolean boundMatch(TypeVariable t1, TypeVariable t2) {
		Type[] b1 = t1.getBounds();
		Type[] b2 = t2.getBounds();
		if (b1.length != b2.length)
			return false;

		boolean match = true;
		for (int i=0; i<b1.length; i++) {
			match &= b1[i].equals(b2[i]) || ((b1[i] instanceof Class) && (b2[i] instanceof Class) && ((Class) b1[i]).isAssignableFrom((Class) b2[i]));
		}
		return match;
	}

	public static boolean classMatchesType(Class clazz, Type required) {
		if(required instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) required;
			Class rt = (Class) pt.getRawType();
			Type[] ap = pt.getActualTypeArguments();
			return classMatchesClassWithParameters(clazz, rt, ap);
		} else if (required instanceof Class) {
			return ((Class) required).isAssignableFrom(clazz);
		}
		return false;
	}

	public static Map<TypeVariable<?>, Type> createGenericsTypeMap(Class<?> cls) {
		Map<TypeVariable<?>, Type> map = new HashMap<>();
		createGenericsTypeMap(map, cls.getGenericInterfaces());
		Type genericType = cls.getGenericSuperclass();
		Class<?> type = cls.getSuperclass();
		while (type != null && !Object.class.equals(type)) {
			if (genericType instanceof ParameterizedType)
				createGenericsTypeMap(map, (ParameterizedType) genericType);
			createGenericsTypeMap(map, type.getGenericInterfaces());
			genericType = type.getGenericSuperclass();
			type = type.getSuperclass();
		}
		return map;
	}

	private static void createGenericsTypeMap(Map<TypeVariable<?>, Type> map, Type[] ifcs) {
		for (Type ifc : ifcs) {
			if (ifc instanceof ParameterizedType)
				createGenericsTypeMap(map, (ParameterizedType) ifc);
		}
	}


	private static Map<TypeVariable<?>, Type> createGenericsTypeMap(Map<TypeVariable<?>, Type> map, ParameterizedType type) {
		TypeVariable<?>[] tvs = ((Class) type.getRawType()).getTypeParameters();
		Type[] ap = type.getActualTypeArguments();
		for (int i=0; i<tvs.length; i++) {
			map.put(tvs[i], ap[i]);
		}
		return map;
	}

	public static boolean compareTypes(Type expectedType, Type actualType, Map<TypeVariable<?>, Type> ownerExpectedTypesMap, Map<TypeVariable<?>, Type> ownerActualTypesMap) {
		if (expectedType.equals(actualType))
			return true;

		if (expectedType instanceof WildcardType)
			return true;

		if (expectedType instanceof TypeVariable) {
			expectedType = ReflectionHelper.resolveType(expectedType, ownerExpectedTypesMap);
			if (expectedType instanceof TypeVariable) {
				if (actualType instanceof TypeVariable) {
					return ReflectionHelper.boundMatch((TypeVariable) expectedType, (TypeVariable) actualType);
				} else if (actualType instanceof Class) {
					Type expectedBound = ((TypeVariable) expectedType).getBounds()[0];
					return compareTypes(expectedBound, actualType, ownerExpectedTypesMap, ownerActualTypesMap);
				}
			}
			else {
				return compareTypes(expectedType, actualType, ownerExpectedTypesMap, ownerActualTypesMap);
			}
		}

		if (expectedType instanceof Class) {
			if (actualType instanceof Class) {
				return (((Class) expectedType).isAssignableFrom((Class) actualType));
			} else if (actualType instanceof ParameterizedType) {
				return expectedType.equals(((ParameterizedType) actualType).getRawType());
			} else if (actualType instanceof TypeVariable) {
				return ReflectionHelper.boundMatch((Class) expectedType, (TypeVariable) actualType);
			} else {
				return false;
			}
		} else if (expectedType instanceof ParameterizedType) {
			if (actualType instanceof ParameterizedType) {
				return compareParameterizedTypes((ParameterizedType) expectedType, (ParameterizedType) actualType, ownerExpectedTypesMap, ownerActualTypesMap);
			} else if (actualType instanceof Class) {
				Type tmp = actualType;
				if (ownerActualTypesMap == null) {
					ownerActualTypesMap = ReflectionHelper.createGenericsTypeMap((Class) actualType);
				}
				Class expectedClass = (Class) ((ParameterizedType) expectedType).getRawType();
				Class classToCheck;
				while (tmp != null) {
					if (tmp instanceof ParameterizedType) {
						ParameterizedType pt = (ParameterizedType) tmp;
						if (compareParameterizedTypes((ParameterizedType) expectedType, pt, ownerExpectedTypesMap, ownerActualTypesMap)) {
							return true;
						}
						classToCheck = (Class) pt.getRawType();
					} else if (tmp instanceof Class) {
						classToCheck = (Class) tmp;
					} else {
						classToCheck = null;
					}
					if (classToCheck != null) {
						Type[] ifcs = classToCheck.getGenericInterfaces();
						for (Type ifc : ifcs) {
							if (ifc instanceof ParameterizedType) {
								if (compareParameterizedTypes((ParameterizedType) expectedType, (ParameterizedType) ifc, ownerExpectedTypesMap, ownerActualTypesMap)) {
									return true;
								}
							}
						}

						if (expectedClass.equals(classToCheck)) {
							if (compareParameterizedTypes((ParameterizedType) expectedType, (ParameterizedType) tmp, ownerExpectedTypesMap, ownerActualTypesMap)) {
								return true;
							}
						}

						tmp = classToCheck.getGenericSuperclass();
					} else {
						tmp = null;
					}
				}

				return false;
			}

		}

		return false;
	}

	private static boolean compareParameterizedTypes(ParameterizedType expected, ParameterizedType actual, Map<TypeVariable<?>,Type> ownerExpectedTypesMap, Map<TypeVariable<?>, Type> ownerActualTypesMap) {
		Class expectedRawType = (Class) expected.getRawType();
		Class actualRawType = (Class) actual.getRawType();
		if (!expectedRawType.isAssignableFrom(actualRawType)) {
			return false;
		}

		boolean result = true;
		Type[] actualTypes = actual.getActualTypeArguments();
		Type[] expectedTypes = expected.getActualTypeArguments();
		if (actualTypes.length == expectedTypes.length) {
			for (int i = 0; i < expectedTypes.length; i++) {
				Type expectedType = resolveType(expectedTypes[i], ownerExpectedTypesMap);
				Type actualType = resolveType(actualTypes[i], ownerActualTypesMap);

				if (!compareTypes(expectedType, actualType, ownerExpectedTypesMap, ownerActualTypesMap)) {
					result = false;
				}
			}
			if (result) {
				return true;
			}
		} else {
			if (actualRawType.isInterface()) {
				for (Type ifc : actualRawType.getGenericInterfaces()) {
					if (compareTypes(expected, ifc, ownerExpectedTypesMap, ownerActualTypesMap)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private static Type resolveType(Type t, Map<TypeVariable<?>,Type> ownerMap) {
		if (ownerMap == null) {
			return t;
		}
		while (t instanceof TypeVariable && ownerMap.containsKey(t)) {
			t = ownerMap.get(t);
		}
		if (t instanceof TypeVariable && ((TypeVariable) t).getBounds() != null && ((TypeVariable) t).getBounds().length > 0) {
			t = ((TypeVariable) t).getBounds()[0];
		}
		return t;
	}
}
