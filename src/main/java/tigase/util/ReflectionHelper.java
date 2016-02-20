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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import tigase.kernel.BeanUtils;

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

}
