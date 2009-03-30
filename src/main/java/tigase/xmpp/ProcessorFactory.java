/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.xmpp;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@TODO(note="Make loading processors configurable: exclude specific classes, turn-off automatic loading and include specific classes. In all cases checking agains XMPPProcessor compatibility should be performed.")
public class ProcessorFactory {

  private static Logger log = Logger.getLogger("tigase.xmpp.ProcessorFactory");
  private static Map<String, XMPPImplIfc> processors = null;

  static {
    try {
			Set<Class<XMPPImplIfc>> procs =
				ClassUtil.getClassesImplementing(XMPPImplIfc.class);

			processors = new TreeMap<String, XMPPImplIfc>();

      LinkedList<String> elems = new LinkedList<String>();
      for (Class<XMPPImplIfc> cproc: procs) {
 				XMPPImplIfc xproc = cproc.newInstance();
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
		if (log.isLoggable(Level.FINEST)) {
	      log.finest("Loaded XMPPProcessors:\n" + sb.toString());
		 }

    } catch (Exception e) {
			System.out.println("Can not load XMPPProcessor implementations");
			e.printStackTrace();
      log.log(Level.SEVERE, "Can not load XMPPProcessor implementations", e);
      System.exit(1);
    } // end of try-catch
  }

	public static XMPPPacketFilterIfc getPacketFilter(String id) {
		XMPPImplIfc imp = processors.get(id);
		if (imp instanceof XMPPPacketFilterIfc) {
			return (XMPPPacketFilterIfc)imp;
		}
		return null;
	}

  private ProcessorFactory() {}

  public static XMPPProcessorIfc getProcessor(String id) {
		XMPPImplIfc imp = processors.get(id);
		if (imp instanceof XMPPProcessorIfc) {
			return (XMPPProcessorIfc)imp;
		}
		return null;
  }

  public static XMPPPreprocessorIfc getPreprocessor(String id) {
		XMPPImplIfc imp = processors.get(id);
		if (imp instanceof XMPPPreprocessorIfc) {
			return (XMPPPreprocessorIfc)imp;
		}
		return null;
  }

  public static XMPPPostprocessorIfc getPostprocessor(String id) {
		XMPPImplIfc imp = processors.get(id);
		if (imp instanceof XMPPPostprocessorIfc) {
			return (XMPPPostprocessorIfc)imp;
		}
		return null;
  }

  public static XMPPStopListenerIfc getStopListener(String id) {
		XMPPImplIfc imp = processors.get(id);
		if (imp instanceof XMPPStopListenerIfc) {
			return (XMPPStopListenerIfc)imp;
		}
		return null;
  }

}// ProcessorFactory
