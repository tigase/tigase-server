/*
 * DataSourceHelper.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.org>
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
package tigase.db;

import tigase.osgi.ModulesManagerImpl;
import tigase.util.ClassUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by andrzej on 15.03.2016.
 */
public class DataSourceHelper {

	private static final Logger log = Logger.getLogger(DataSourceHelper.class.getCanonicalName());
	private static final CopyOnWriteArraySet<Class> annotatedClasses = new CopyOnWriteArraySet<Class>();

	static {
		try {
			Set<Class<?>> classes = ClassUtil.getClassesFromClassPath();
			initialize(classes);
		} catch (Exception ex) {
		}
	}

	public static void initialize(Collection<Class<?>> classes) {
		for (Class<?> clazz : classes) {
			Repository.Meta annotation = clazz.getAnnotation(Repository.Meta.class);
			if (annotation == null)
				continue;
			annotatedClasses.add(clazz);
		}
	}

	public static String getDefaultClassName(Class cls, String uri) throws DBInitException {
		Class result = getDefaultClass(cls, uri);
		return result.getCanonicalName();
	}

	/**
	 * Method returns class which would be by default used as implementation of class
	 *
	 * @param cls
	 * @param uri
	 * @return
	 * @throws tigase.db.DBInitException
	 *
	 *
	 */
	public static <T extends Class<?>> T getDefaultClass(T cls, String uri) throws DBInitException {
		Set<T> classes = ModulesManagerImpl.getInstance().getImplementations(cls);
		classes.addAll(getAnnotatedClasses(cls));
		Set<T> supported = new HashSet<T>();
		for (T clazz : classes) {
			Repository.Meta annotation = (Repository.Meta) clazz.getAnnotation(Repository.Meta.class);
			if (annotation != null) {
				String[] supportedUris = annotation.supportedUris();
				if (supportedUris != null) {
					for (String supportedUri : supportedUris) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "checking if {0} for {1} supports {2} while it supports {3} result = {4}",
									new Object[]{clazz.getCanonicalName(), cls.getCanonicalName(), uri, supportedUri, Pattern.matches(supportedUri, uri)});
						}
						if (Pattern.matches(supportedUri, uri))
							supported.add(clazz);
					}
				} else {
					supported.add(clazz);
				}
			} else {
				supported.add(clazz);
			}
		}
		if (supported.isEmpty())
			throw new DBInitException("Not found class supporting uri = " + uri);
		T result = null;
		for (T clazz : supported) {
			if (result == null)
				result = clazz;
			else {
				Repository.Meta ar = (Repository.Meta) result.getAnnotation(Repository.Meta.class);
				Repository.Meta ac = (Repository.Meta) clazz.getAnnotation(Repository.Meta.class);
				if (ac == null)
					continue;
				if ((ar == null && ac != null) || (!ar.isDefault() && ((ar.supportedUris() == null && ac.supportedUris() != null) || ac.isDefault()))) {
					result = clazz;
				}
			}
		}
		return result;
	}

	public static <T extends Class<?>> Set<T> getAnnotatedClasses(T cls) {
		Set<T> classes = new HashSet<>();
		for (Class<?> clazz : annotatedClasses) {
			if (cls.isAssignableFrom(clazz))
				classes.add((T) clazz);
		}
		return classes;
	}


}
