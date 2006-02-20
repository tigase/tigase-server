/*  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * $Author$
 * $Date$
 */

package tigase.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <code>ClassUtil</code> file contains code used for loading all
 * implementations of specified <em>interface</em> or <em>abstract class</em>
 * found in classpath. As a result of calling some functions you can have
 * <code>Set</code> containing all required classes.
 *
 * <p>
 * Created: Wed Oct  6 08:25:52 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ClassUtil {

  public static String getClassNameFromFileName(String fileName) {
    String class_name = null;
    if (fileName.endsWith(".class")) {
//       class_name = fileName.substring(0,
//         fileName.length()-6).replace(File.separatorChar, '.');
      // Above code does not works on MS Windows if we load
      // files from jar file. Jar manipulation code always returns
      // file names with unix style separators
      String tmp_class_name = fileName.substring(0,
          fileName.length()-6).replace('\\', '.');
      class_name = tmp_class_name.replace('/', '.');
    } // end of if (entry_name.endsWith(".class"))
    return class_name;
  }

  public static void walkInDirForFiles(File path, Set<String> set) {
    if (path.isDirectory()) {
      String[] files = path.list();
      for (String file : files) {
        walkInDirForFiles(new File(path, file), set);
      } // end of for ()
    } // end of if (file.isDirectory())
    else {
      set.add(path.toString());
    } // end of if (file.isDirectory()) else
  }

  public static Set<String> getFileListDeep(File path) {
    Set<String> set = new TreeSet<String>();
    if (path.isDirectory()) {
      String[] files = path.list();
      for (String file : files) {
        walkInDirForFiles(new File(file), set);
      } // end of for ()
    } // end of if (file.isDirectory())
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
        //        System.out.println("class name: "+class_name);
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

  public static Set<Class> getClassesFromNames(Set<String> names)
    throws ClassNotFoundException{
    Set<Class> classes = new TreeSet<Class>(new ClassComparator());
    for (String name : names) {
      //      System.out.println("Class name: "+name);
      classes.add(Class.forName(name));
    } // end of for ()
    return classes;
  }

  public static Set<Class> getClassesFromClassPath()
    throws IOException, ClassNotFoundException {

    Set<Class> classes_set = new TreeSet<Class>(new ClassComparator());

    String classpath = System.getProperty("java.class.path");
    //    System.out.println("classpath: "+classpath);
    StringTokenizer stok =
      new StringTokenizer(classpath, File.pathSeparator, false);
    while (stok.hasMoreTokens()) {
      String path = stok.nextToken();
      File file = new File(path);
      if (file.exists()) {
        if (file.isDirectory()) {
          //          System.out.println("directory: "+path);
          Set<String> class_names = getClassNamesFromDir(file);
          classes_set.addAll(getClassesFromNames(class_names));
        } // end of if (file.isDirectory())
        if (file.isFile()) {
          //          System.out.println("jar file: "+path);
          Set<String> class_names = getClassNamesFromJar(file);
          classes_set.addAll(getClassesFromNames(class_names));
        } // end of if (file.isFile())
      } // end of if (file.exists())
    } // end of while (stok.hasMoreTokens())

    return classes_set;
  }

  @SuppressWarnings({"unchecked"})
  public static <T extends Class>
    Set<T> getClassesImplementing(Set<Class> classes, T cls) {

    Set<T> classes_set = new TreeSet<T>(new ClassComparator());

    for (Class c : classes) {
      if (cls.isAssignableFrom(c)) {
        int mod = c.getModifiers();
        if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod)) {
          classes_set.add((T)c);
        } // end of if (!Modifier.isAbstract(mod) && !Modifier.isInterface(mod))
      } // end of if (cls.isAssignableFrom(c))
    } // end of for ()

    return classes_set;
  }

  public static <T extends Class> Set<T> getClassesImplementing(T cls)
    throws IOException, ClassNotFoundException {
    return getClassesImplementing(getClassesFromClassPath(), cls);
  }

  @SuppressWarnings("unchecked")
  public static <T> Set<T> getImplementations(Class<T> obj)
    throws IOException, ClassNotFoundException,
           InstantiationException, IllegalAccessException {

    Set<T> result = new TreeSet<T>(new ObjectComparator());

    for (Class cls : getClassesImplementing(obj)) {
      result.add((T)cls.newInstance());
    } // end of for ()
    return result;
  }

}// ClassUtil
