/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.osgi.util;

//~--- JDK imports ------------------------------------------------------------
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import org.osgi.framework.Bundle;
import tigase.osgi.Activator;
import tigase.util.ClassComparator;
import tigase.util.ObjectComparator;

//~--- classes ----------------------------------------------------------------
/**
 * <code>ClassUtil</code> file contains code used for loading all
 * implementations of specified <em>interface</em> or <em>abstract class</em>
 * found in classpath. As a result of calling some functions you can have
 * <code>Set</code> containing all required classes.
 *
 * <p>
 * Created: Wed Oct  6 08:25:52 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev: 609 $
 */
public class ClassUtil {

        private static final String[] SKIP_CONTAINS = {
                ".ui.", ".swing", ".awt", ".sql.", ".xml.", ".terracotta."
        };
        private static final String[] SKIP_STARTS = {
                "com.mysql", "tigase.pubsub.Utils", "org.apache.derby", "org.apache.xml", "org.postgresql",
                "com.sun", "groovy", "org.codehaus.groovy", "org.netbeans", "org.python", "com.google.gwt",
                "xtigase.http.client.BasePanel", "org.osgi"
        };

        //~--- get methods ----------------------------------------------------------
        /**
         * Method description
         *
         *
         * @param fileName
         *
         * 
         */
        public static String getClassNameFromFileName(String fileName) {
                String class_name = null;
                if (fileName.endsWith(".class")) {
//       class_name = fileName.substring(0,
//         fileName.length()-6).replace(File.separatorChar, '.');
                        // Above code does not works on MS Windows if we load
                        // files from jar file. Jar manipulation code always returns
                        // file names with unix style separators
                        int off = fileName.charAt(0) == '/' ? 1 : 0;
                        String tmp_class_name = fileName.substring(off,
                                fileName.length() - 6).replace('\\', '.');
                        class_name = tmp_class_name.replace('/', '.');
                } // end of if (entry_name.endsWith(".class"))
                return class_name;
        }

        public static void walkInDirForFiles(File base_dir,
                String path, Set<String> set) {
                File tmp_file = new File(base_dir, path);
                if (tmp_file.isDirectory()) {
                        String[] files = tmp_file.list();
                        for (String file : files) {
                                walkInDirForFiles(base_dir, new File(path, file).toString(), set);
                        } // end of for ()
                }
                else {
                        if (path.toString().contains("Plugin")) {
                                System.out.println("File: " + path.toString());
                        }
                        set.add(path);
                } // end of if (file.isDirectory()) else
        }

        public static Set<String> getFileListDeep(File path) {
                Set<String> set = new TreeSet<String>();
                if (path.isDirectory()) {
                        String[] files = path.list();
                        for (String file : files) {
                                walkInDirForFiles(path, file, set);
                        } // end of for ()
                }
                else {
                        set.add(path.toString());
                } // end of if (file.isDirectory()) else
                return set;
        }

        public static Set<String> getClassNamesFromDir(File dir) {
                Set<String> tmp_set = getFileListDeep(dir);
                Set<String> result = new TreeSet<String>();
                for (String elem : tmp_set) {
                        String class_name = getClassNameFromFileName(elem);
                        if (class_name != null) {
                                result.add(class_name);
                                if (class_name.contains("Plugin")) {
                                        System.out.println("class name: " + class_name);
                                }
                        } // end of if (class_name != null)
                } // end of for ()
                return result;
        }

        public static Set<String> getClassNamesFromJar(File jarFile)
                throws IOException {
                Set<String> result = new TreeSet<String>();
                JarFile jar = new JarFile(jarFile);
                Enumeration<JarEntry> jar_entries = jar.entries();
                while (jar_entries.hasMoreElements()) {
                        JarEntry jar_entry = jar_entries.nextElement();
                        String class_name = getClassNameFromFileName(jar_entry.getName());
                        if (class_name != null) {
                                result.add(class_name);
                                //        System.out.println("class name: "+class_name);
                        } // end of if (entry_name.endsWith(".class"))
                } // end of while (jar_entries.hasMoreElements())

                return result;
        }

        public static Set<String> getClassNamesFromWar(File jarFile)
                throws IOException {
                Set<String> result = new TreeSet<String>();
                JarFile jar = new JarFile(jarFile);
                Enumeration<JarEntry> jar_entries = jar.entries();
                while (jar_entries.hasMoreElements()) {
                        JarEntry jar_entry = jar_entries.nextElement();
                        String name = jar_entry.getName();
                        if (!name.startsWith("WEB-INF/classes/")) {
                                continue;
                        }
                        name = name.substring(16);
                        String class_name = getClassNameFromFileName(name);
                        if (class_name != null) {
                                result.add(class_name);
                                //        System.out.println("class name: "+class_name);
                        } // end of if (entry_name.endsWith(".class"))
                } // end of while (jar_entries.hasMoreElements())

                return result;
        }

        public static Set<Class> getClassesFromNames(ClassLoader loader, Set<String> names)
                throws ClassNotFoundException {
                Set<Class> classes = new TreeSet<Class>(new ClassComparator());
                for (String name : names) {
                        try {
                                boolean skip_class = false;
                                for (String test_str : SKIP_CONTAINS) {
                                        skip_class = name.contains(test_str);
                                        if (skip_class) {
                                                break;
                                        }
                                }
                                if (!skip_class) {
                                        for (String test_str : SKIP_STARTS) {
                                                skip_class = name.startsWith(test_str);
                                                if (skip_class) {
                                                        break;
                                                }
                                        }
                                }
                                if (!skip_class) {
                                        //Class cls = Class.forName(name);
                                        Class cls = loader.loadClass(name);
                                        classes.add(cls);
                                }
                        }
                        catch (NoClassDefFoundError e) {
                                System.out.println("Class not found name: " + name);
                        }
                        catch (UnsatisfiedLinkError e) {
                                System.out.println("Class unsatisfied name: " + name);
                        }
                        catch (Throwable e) {
                                Throwable cause = e.getCause();
                                System.out.println("Class name: " + name);
                                e.printStackTrace();
                                if (cause != null) {
                                        cause.printStackTrace();
                                }
                        }
                } // end of for ()
                return classes;
        }

        public static Set<Class> getClassesFromClassPath()
                throws IOException, ClassNotFoundException {

                Set<Class> classes_set = new TreeSet<Class>(new ClassComparator());

                String classpath = System.getProperty("java.class.path");
                System.out.println("classpath: " + classpath);
                ClassLoader loader = ClassUtil.class.getClassLoader();
                StringTokenizer stok =
                        new StringTokenizer(classpath, File.pathSeparator, false);
                while (stok.hasMoreTokens()) {
                        String path = stok.nextToken();
                        File file = new File(path);
                        if (file.exists()) {
                                if (file.isDirectory()) {
                                        System.out.println("directory: " + path);
                                        Set<String> class_names = getClassNamesFromDir(file);
                                        classes_set.addAll(getClassesFromNames(loader, class_names));
                                } // end of if (file.isDirectory())
                                if (file.isFile()) {
                                        //System.out.println("jar file: "+path);
                                        Set<String> class_names = getClassNamesFromJar(file);
                                        classes_set.addAll(getClassesFromNames(loader, class_names));
                                        //System.out.println("Loaded jar file: "+path);
                                } // end of if (file.isFile())
                        } // end of if (file.exists())
                } // end of while (stok.hasMoreTokens())

                return classes_set;
        }

        @SuppressWarnings({"unchecked"})
        public static <T extends Class> Set<T> getClassesImplementing(Collection<Class> classes, T cls) {

                Set<T> classes_set = new TreeSet<T>(new ClassComparator());

                for (Class c : classes) {
                        if (c.getName().contains("Plugin")) {
                                System.out.println(c.getName() + " " + cls.isAssignableFrom(c));
                        }
                        if (cls.isAssignableFrom(c)) {
                                int mod = c.getModifiers();
                                if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {
                                        classes_set.add((T) c);
                                } // end of if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod))
                        } // end of if (cls.isAssignableFrom(c))
                } // end of for ()

                return classes_set;
        }

        public static <T extends Class> Set<T> getClassesImplementing(T cls)
                throws IOException, ClassNotFoundException {
                // @TODO: Temporary?? fix for xmpp processors loading
                return getClassesImplementing(Activator.getBundle(), cls);
        }

        @SuppressWarnings("unchecked")
        public static <T> Set<T> getImplementations(Class<T> obj)
                throws IOException, ClassNotFoundException,
                InstantiationException, IllegalAccessException {

                Set<T> result = new TreeSet<T>(new ObjectComparator());

                for (Class cls : getClassesImplementing(obj)) {
                        result.add((T) cls.newInstance());
                } // end of for ()
                return result;
        }

        @SuppressWarnings({"unchecked"})
        public static <T extends Class> Set<T> getClassesImplementing(ClassLoader loader, File f, T cls) throws IOException, ClassNotFoundException {

                Set<String> class_names = (f.getName().endsWith(".war")) ? ClassUtil.getClassNamesFromWar(f) : ClassUtil.getClassNamesFromJar(f);
                Set<Class> classes = ClassUtil.getClassesFromNames(loader, class_names);

                return ClassUtil.getClassesImplementing(classes, cls);
        }

        /**
         * Scans OSGI bundle for classes implementing class T
         * 
         * @param <T>       return type param
         * @param bundle    bundle to scan
         * @param cls       base class
         * @return          returns set of implementations
         * @throws IOException
         * @throws ClassNotFoundException 
         */
        @SuppressWarnings({"unchecked"})
        public static <T extends Class> Set<T> getClassesImplementing(Bundle bundle, T cls) throws IOException, ClassNotFoundException {

                Set<T> classes_set = new TreeSet<T>(new ClassComparator());
                Enumeration e = bundle.findEntries("/", "*.class", true);
                if (e != null) {
                        while (e.hasMoreElements()) {
                                URL clsUrl = (URL) e.nextElement();
                                String clsName = getClassNameFromFileName(clsUrl.getPath());

                                boolean skip_class = false;
                                for (String prefix : SKIP_STARTS) {
                                        if (clsName.startsWith(prefix)) {
                                                skip_class = true;
                                                break;
                                        }
                                }
                                if (skip_class) {
                                        continue;
                                }

                                for (String part : SKIP_CONTAINS) {
                                        if (clsName.contains(part)) {
                                                skip_class = true;
                                                break;
                                        }
                                }
                                if (skip_class) {
                                        continue;
                                }


                                try {
                                        Class c = bundle.loadClass(clsName);
                                        if (cls.isAssignableFrom(c)) {
                                                int mod = c.getModifiers();
                                                if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {
                                                        classes_set.add((T) c);
                                                } // end of if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod))
                                        } // end of if (cls.isAssignableFrom(c))
                                }
                                catch (ClassNotFoundException ex) {
                                        Logger.getLogger(ClassUtil.class.getCanonicalName()).warning("Could not find class = " + clsName);
                                }
                        }
                }
                return classes_set;
                //return ClassUtil.getClassesImplementing(classes, cls);
        }

        /**
         * Scans file for classes annotated with annotation T
         * 
         * @param <T>
         * @param <S>
         * @param loader    class loader used to load classes
         * @param f         file to scan for classes
         * @param cls       annotation class
         * @return          set of annotated classes
         * @throws IOException
         * @throws ClassNotFoundException 
         */
        @SuppressWarnings({"unchecked"})
        public static <T extends Class, S extends Class> Set<S> getClassesAnnotated(ClassLoader loader, File f, T cls) throws IOException, ClassNotFoundException {

                Set<String> class_names = (f.getName().endsWith(".war")) ? ClassUtil.getClassNamesFromWar(f) : ClassUtil.getClassNamesFromJar(f);
                Set<Class> classes = ClassUtil.getClassesFromNames(loader, class_names);
                Set<S> classes_set = new TreeSet<S>(new ClassComparator());

                for (Class c : classes) {
                        Annotation[] annots = c.getAnnotations();
                        if (c.isAnnotationPresent(cls)) {
                                int mod = c.getModifiers();
                                if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {
                                        classes_set.add((S) c);
                                } // end of if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod))
                        } // end of if (cls.isAssignableFrom(c))
                } // end of for ()

                return classes_set;
        }

        /**
         * Scans OSGI bundle for classes annotated with annotation T
         * 
         * @param <T>
         * @param <S>
         * @param bundle    bundle to scan for annotated classes
         * @param cls       annotation class
         * @return          set of annotated classes
         * @throws IOException
         * @throws ClassNotFoundException 
         */
        @SuppressWarnings({"unchecked"})
        public static <T extends Class, S extends Class> Set<S> getClassesAnnotated(Bundle bundle, T cls) throws IOException, ClassNotFoundException {

                Set<S> classes_set = new TreeSet<S>(new ClassComparator());
                Enumeration e = bundle.findEntries("/", "*.class", true);
                if (e != null) {
                        while (e.hasMoreElements()) {
                                URL clsUrl = (URL) e.nextElement();
                                String clsName = getClassNameFromFileName(clsUrl.getPath());

                                boolean skip_class = false;
                                for (String prefix : SKIP_STARTS) {
                                        if (clsName.startsWith(prefix)) {
                                                skip_class = true;
                                                break;
                                        }
                                }
                                if (skip_class) {
                                        continue;
                                }

                                for (String part : SKIP_CONTAINS) {
                                        if (clsName.contains(part)) {
                                                skip_class = true;
                                                break;
                                        }
                                }
                                if (skip_class) {
                                        continue;
                                }


                                try {
                                        Class c = bundle.loadClass(clsName);
                                        Annotation[] annots = c.getAnnotations();
                                        if (c.isAnnotationPresent(cls)) {
                                                int mod = c.getModifiers();
                                                if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {
                                                        classes_set.add((S) c);
                                                } // end of if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod))
                                        } // end of if (cls.isAssignableFrom(c))
                                }
                                catch (ClassNotFoundException ex) {
                                        Logger.getLogger(ClassUtil.class.getCanonicalName()).warning("Could not find class = " + clsName);
                                }
                        }
                }

                return classes_set;
        }
}
