/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.util.reflection;

import tigase.util.ClassComparator;
import tigase.util.ClassUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andrzej on 08.09.2016.
 */
public class ClassUtilBean {

	private static final String[] DEFAULT_PACKAGES_TO_SKIP = {"com.fasterxml.jackson", "com.mongodb", "org.bson",
															  "com.mysql", "com.notnoop", "javax.jmdns", "javax.mail",
															  "javax.servlet", "org.apache.commons", "org.apache.derby",
															  "org.apache.felix", "org.apache.http.client",
															  "org.apache.xml", "org.bouncycastle", "org.eclipse.jetty",
															  "org.hamcrest.core", "org.postgresql", "org.slf4j",
															  "ch.qos.logback", "com.sun", "groovy",
															  "org.codehaus.groovy", "org.netbeans", "org.python",
															  "tigase.jaxmpp", "tigase.pubsub.Utils"};

	private static ClassUtilBean instance;
	private static Logger log = Logger.getLogger(ClassUtilBean.class.getCanonicalName());
	protected HashSet<Class<?>> classes = new HashSet<>();

	public static List<String> getPackagesToSkip(String[] packagesToSkip) {
		if (packagesToSkip == null) {
			return Arrays.asList(DEFAULT_PACKAGES_TO_SKIP);
		} else {
			return Stream.concat(Arrays.stream(DEFAULT_PACKAGES_TO_SKIP), Arrays.stream(packagesToSkip))
					.distinct()
					.collect(Collectors.toList());
		}
	}

	public static ClassUtilBean getInstance() {
		synchronized (ClassUtilBean.class) {
			if (instance == null) {
				ClassUtilBean instance = new ClassUtilBean();
				instance.initialize(getPackagesToSkip(null));
			}
			return instance;
		}
	}

	public ClassUtilBean() {
	}

	public void initialize(Collection<String> skipPackages) {
		try {
			Predicate<String> filter = null;
			if (skipPackages == null) {
				filter = className -> true;
			} else {
				List<String> packages = skipPackages.stream().map(packageName -> packageName + ".").collect(Collectors.toList());
				filter = className -> {
					for (String packageName : packages) {
						if (className.startsWith(packageName)) {
							return false;
						}
					}
					return true;
				};
			}
			classes.addAll(ClassUtil.getClassesFromClassPath(filter));
			// support for handling debugging test cases started by Maven Surefire Plugin
			// as without it Tigase Kernel is not able to see annotated beans
			classes.addAll(getClassesFromSurefireClassLoader());
		} catch (IOException | ClassNotFoundException e) {
			log.log(Level.SEVERE, "Could not initialize list of classes", e);
		}
		synchronized (ClassUtilBean.class) {
			instance = this;
		}
	}

	public Set<Class<?>> getAllClasses() {
		return Collections.unmodifiableSet(classes);
	}

	private Set<Class<?>> getClassesFromSurefireClassLoader() {
		Set<Class<?>> classes_set = new TreeSet<Class<?>>(new ClassComparator());
		String classpath = System.getProperty("surefire.test.class.path");

		if (classpath == null) {
			return classes_set;
		}
		// System.out.println("classpath: "+classpath);
		StringTokenizer stok = new StringTokenizer(classpath, File.pathSeparator, false);

		while (stok.hasMoreTokens()) {
			String path = stok.nextToken();
			File file = new File(path);

			if (file.exists()) {
				try {
					if (file.isDirectory()) {

						// System.out.println("directory: "+path);
						Set<String> class_names = ClassUtil.getClassNamesFromDir(file);

						tigase.osgi.util.ClassUtil.getClassesFromNames(Thread.currentThread().getContextClassLoader(),
																	   class_names).stream().forEach(classes_set::add);
					} // end of if (file.isDirectory())

					if (file.isFile()) {

						// System.out.println("jar file: "+path);
						Set<String> class_names = ClassUtil.getClassNamesFromJar(file);

						//classes_set.addAll(tigase.osgi.util.ClassUtil.getClassesFromNames(class_names));
						tigase.osgi.util.ClassUtil.getClassesFromNames(Thread.currentThread().getContextClassLoader(),
																	   class_names).stream().forEach(classes_set::add);

						// System.out.println("Loaded jar file: "+path);
					} // end of if (file.isFile())
				} catch (ClassNotFoundException | IOException ex) {
					log.log(Level.WARNING, "Could not load classes for " + file.getAbsolutePath());
				}
			} // end of if (file.exists())
		} // end of while (stok.hasMoreTokens())

		return classes_set;
	}
}
