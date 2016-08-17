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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

	public static Class getCollectionParamter(Type genericType, Class clazz) {
		if (genericType instanceof ParameterizedType) {
			Type type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
			if (!(type instanceof Class)) {
				Map<TypeVariable<?>, Type> map = createGenericsTypeMap(clazz);
				while (type instanceof TypeVariable) {
					type = map.get((TypeVariable<?>) type);
				}
			}

			return (Class) type;
		}
		return null;
	}

	public static boolean classMatchesClassWithParameters(Class clazz, Class rt, Type[] ap) {
		Map<TypeVariable<?>, Type> map = createGenericsTypeMap(clazz);
		TypeVariable<?>[] tvs = rt.getTypeParameters();
		boolean match = rt.isAssignableFrom(clazz);
		if (!match)
			return false;

		for (int i = 0; i < tvs.length; i++) {
			Type t = tvs[i];
			while (map.containsKey(t)) {
				t = map.get(t);
			}

			if (ap[i].equals(t))
				continue;

			if (ap[i] instanceof WildcardType)
				continue;

			if ((ap[i] instanceof TypeVariable) && (t instanceof TypeVariable) && boundMatch((TypeVariable) ap[i], (TypeVariable) t))
				continue;

			if (ap[i] instanceof Class) {
				if (t instanceof Class && ((Class) ap[i]).isAssignableFrom((Class) t))
					continue;
				if (t instanceof ParameterizedType && ((Class) ap[i]).isAssignableFrom((Class) ((ParameterizedType)t).getRawType()))
					continue;
				if (t instanceof TypeVariable && boundMatch((Class) ap[i], (TypeVariable) t))
					continue;
			}

			if (ap[i] instanceof ParameterizedType) {
				ParameterizedType apt = (ParameterizedType) ap[i];
				if (t instanceof TypeVariable) {
					Type[] bounds = ((TypeVariable) t).getBounds();
					if (bounds.length == 1) {
						t = bounds[0];
					}
				}

				if (t instanceof Class && apt.getRawType() instanceof Class) {
					if (classMatchesClassWithParameters((Class) t, (Class) apt.getRawType(), apt.getActualTypeArguments()))
						continue;
				}
				if (t instanceof ParameterizedType) {
					ParameterizedType tpt = (ParameterizedType) t;
					if (apt.getRawType() instanceof Class && tpt.getRawType() instanceof Class) {
						if (((Class) apt.getRawType()).isAssignableFrom((Class) tpt.getRawType())) {
							Type[] apta = apt.getActualTypeArguments();
							Type[] tpta = tpt.getActualTypeArguments();
							if (apta.length == tpta.length) {
								boolean ptMatch = true;
								for (int j=0; j<apta.length; j++) {
									Type at = apta[j];
									Type tt = tpta[j];
									while (tt instanceof TypeVariable && map.containsKey(tt)) {
										tt = map.get(tt);
									}
									if (tt instanceof TypeVariable) {
										Type[] bounds = ((TypeVariable) tt).getBounds();
										if (bounds.length == 1) {
											tt = bounds[0];
										}
									}
									if (at instanceof Class && tt instanceof Class) {
										if (((Class) at).isAssignableFrom((Class) tt))
											continue;
									}
									ptMatch = false;
								}
								if (ptMatch)
									continue;
							}
						}
					}
				}
			}

			match &= false;
		}
		return match;
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


	private static void createGenericsTypeMap(Map<TypeVariable<?>, Type> map, ParameterizedType type) {
		TypeVariable<?>[] tvs = ((Class) type.getRawType()).getTypeParameters();
		Type[] ap = type.getActualTypeArguments();
		for (int i=0; i<tvs.length; i++) {
			map.put(tvs[i], ap[i]);
		}
	}

}
