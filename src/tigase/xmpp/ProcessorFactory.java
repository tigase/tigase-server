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

package tigase.xmpp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.annotations.TODO;
import tigase.util.ClassUtil;

/**
 * <code>ProcessorFactory</code> class contains functionality to load and
 * provide all classes which are <code>XMPPProcessor</code> extensions (not
 * abstract extensions) available in classpath.
 * These extensions are normally used for processing data transfered between
 * <em>XMPP</em> entities.<br/>
 * It automatically loads and provides all available processors unless
 * configuration says to behave differently. You can for example exclude in
 * configuration some processors from loading or even you can turn off automatic
 * loading processors and provide explicity names of classes which should be
 * loaded as <em>XMPP</em> processors. In all cases loaded classes ae checked
 * whether they are <code>XMPPProcessor</code> extensions, because only those
 * classes can be used as processors.
 *
 * <p>
 * Created: Tue Oct  5 20:45:33 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
@TODO(note="Make loading processors configurable: exclude specific classes, turn-off automatic loading and include specific classes. In all cases checking agains XMPPProcessor compatibility should be performed.")
public class ProcessorFactory {

  private static Logger log = Logger.getLogger("tigase.xmpp.ProcessorFactory");
  private static Map<String, XMPPProcessor> processors = null;

  static {
    try {
			Set<Class<XMPPProcessor>> procs =
				ClassUtil.getClassesImplementing(XMPPProcessor.class);

			processors = new TreeMap<String, XMPPProcessor>();

      LinkedList<String> elems = new LinkedList<String>();
      for (Class<XMPPProcessor> cproc: procs) {
 				XMPPProcessor xproc = cproc.newInstance();
				processors.put(xproc.id(), xproc);
        String[] els = xproc.supElements();
        String[] nss = xproc.supNamespaces();
        if (els != null && nss != null) {
          for (int i = 0; i < els.length; i++) {
            elems.add("  <" + els[i] + " xmlns='" + nss[i] + "'/>\n");
          } // end of for (int i = 0; i < els.length; i++)
        } // end of if (nss != null)
      } // end of for ()
      Collections.sort(elems);
      StringBuilder sb = new StringBuilder();
      for (String elm : elems) {
        sb.append(elm);
      } // end of for ()
      log.finest("Loaded XMPPProcessors:\n" + sb.toString());

    } catch (Exception e) {
      log.log(Level.SEVERE, "Can not load XMPPProcessor implementations", e);
      System.exit(1);
    } // end of try-catch
  }

  private ProcessorFactory() {}

  public static XMPPProcessor getProcessor(String id) {
		return processors.get(id);
  }

}// ProcessorFactory

