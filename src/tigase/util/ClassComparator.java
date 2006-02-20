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

import java.util.Comparator;

/**
 * In a few cases classes have to be kept in <code>Set</code>. This
 * <code>Comparator</code> implementation has been created to return proper
 * value for <code>compare</code> method and to make it possible to store
 * classes in any <code>Set</code>.
 *
 * <p>
 * Created: Sat Oct  9 22:27:54 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */

public class ClassComparator implements Comparator<Class> {

  public ClassComparator() { }

  // Implementation of java.util.Comparator

  /**
   * Method <code>compare</code> is used to perform 
   *
   * @param c1 an <code>Object</code> value
   * @param c2 an <code>Object</code> value
   * @return an <code>int</code> value
   */
  //  @Override
  public int compare(Class c1, Class c2) {
    return c1.getSimpleName().compareTo(c2.getSimpleName());
  }

}// ClassComparator
