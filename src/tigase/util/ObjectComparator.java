/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
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
 * Last modified by $Author$
 * $Date$
 */
package tigase.util;

import java.util.Comparator;

/**
 * Describe class ObjectComparator here.
 *
 *
 * Created: Tue May 17 23:53:20 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ObjectComparator implements Comparator<Object> {

  /**
   * Creates a new <code>ObjectComparator</code> instance.
   *
   */
  public ObjectComparator() {

  }

  // Implementation of java.util.Comparator

  /**
   * Describe <code>compare</code> method here.
   *
   * @param object an <code>Object</code> value
   * @param object1 an <code>Object</code> value
   * @return an <code>int</code> value
   */
  public int compare(final Object o1, final Object o2) {
    return o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
  }

} // ObjectComparator
