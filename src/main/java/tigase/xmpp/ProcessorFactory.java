/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.xmpp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.annotations.TODO;

import tigase.util.ClassUtil;

//~--- JDK imports ------------------------------------------------------------

import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

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
@TODO(
	note = "Make loading processors configurable: exclude specific classes, turn-off automatic"
		+ " loading and include specific classes. In all cases checking agains XMPPProcessor "
			+ "compatibility should be performed.")
public class ProcessorFactory {
	private static final Logger log = Logger.getLogger(ProcessorFactory.class.getName());
	private static final Map<String, XMPPImplIfc> processors = new TreeMap<String, XMPPImplIfc>();

	//~--- static initializers --------------------------------------------------

	static {
		try {
			Set<Class<XMPPImplIfc>> procs = ClassUtil.getClassesImplementing(XMPPImplIfc.class);
			ArrayList<String> elems = new ArrayList<String>(32);

			for (Class<XMPPImplIfc> cproc : procs) {
				if ( !Modifier.isPublic(cproc.getModifiers())) {
					continue;
				}

				XMPPImplIfc xproc = cproc.newInstance();

				processors.put(xproc.id(), xproc);

				String[] els = xproc.supElements();
				String[] nss = xproc.supNamespaces();

				if ((els != null) && (nss != null)) {
					for (int i = 0; i < els.length; i++) {
						elems.add("  <" + els[i] + " xmlns='" + nss[i] + "'/>\n");
					}    // end of for (int i = 0; i < els.length; i++)
				}      // end of if (nss != null)
			}        // end of for ()

			Collections.sort(elems);

			if (log.isLoggable(Level.FINEST)) {
				StringBuilder sb = new StringBuilder(200);

				for (String elm : elems) {
					sb.append(elm);
				}      // end of for ()

				log.log(Level.FINEST, "Loaded XMPPProcessors:\n{0}", sb);
			}
		} catch (Exception e) {
			System.out.println("Can not load XMPPProcessor implementations");
			e.printStackTrace();
			log.log(Level.SEVERE, "Can not load XMPPProcessor implementations", e);
			System.exit(1);
		}          // end of try-catch
	}

	//~--- constructors ---------------------------------------------------------

	private ProcessorFactory() {}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param id
	 *
	 * @return
	 */
	public static XMPPPacketFilterIfc getPacketFilter(String id) {
		XMPPImplIfc imp = processors.get(id);

		if (imp instanceof XMPPPacketFilterIfc) {
			return (XMPPPacketFilterIfc) imp;
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param id
	 *
	 * @return
	 */
	public static XMPPPostprocessorIfc getPostprocessor(String id) {
		XMPPImplIfc imp = processors.get(id);

		if (imp instanceof XMPPPostprocessorIfc) {
			return (XMPPPostprocessorIfc) imp;
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param id
	 *
	 * @return
	 */
	public static XMPPPreprocessorIfc getPreprocessor(String id) {
		XMPPImplIfc imp = processors.get(id);

		if (imp instanceof XMPPPreprocessorIfc) {
			return (XMPPPreprocessorIfc) imp;
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param id
	 *
	 * @return
	 */
	public static XMPPProcessorIfc getProcessor(String id) {
		XMPPImplIfc imp = processors.get(id);

		if (imp instanceof XMPPProcessorIfc) {
			return (XMPPProcessorIfc) imp;
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param id
	 *
	 * @return
	 */
	public static XMPPStopListenerIfc getStopListener(String id) {
		XMPPImplIfc imp = processors.get(id);

		if (imp instanceof XMPPStopListenerIfc) {
			return (XMPPStopListenerIfc) imp;
		}

		return null;
	}
        
        /**
         * Check if plugin implementation is in server jar
         * 
         * @param id
         * @return 
         */
        public static boolean hasImplementation(String id) {
                return processors.containsKey(id);
        }
        
}    // ProcessorFactory


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
