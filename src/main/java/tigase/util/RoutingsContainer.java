/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Describe class RoutingsContainer here.
 *
 *
 * Created: Sat Feb 11 16:30:42 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RoutingsContainer {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.util.RoutingsContainer");

	private RoutingComputer comp = null;

	/**
	 * Creates a new <code>RoutingsContainer</code> instance.
	 *
	 */
	public RoutingsContainer(boolean multiMode) {
		if (multiMode) {
			comp = new MultiMode();
		} // end of if (mode)
		else {
			comp = new SingleMode();
		} // end of if (mode) else
	}

	public void addRouting(final String pattern, final String address) {
		comp.addRouting(pattern, address);
	}

	public String computeRouting(final String pattern) {
		return comp.computeRouting(pattern);
	}

	protected interface RoutingComputer {

		void addRouting(final String pattern, final String address);

		String computeRouting(final String pattern);

	}

	protected class SingleMode implements RoutingComputer {

		private String routing = null;

		public void addRouting(final String pattern, final String address) {
			routing = address;
		}

		public String computeRouting(final String address) {
			return routing;
		}

	}

	protected class MultiMode implements RoutingComputer {

		private Map<Pattern, String> routings =	new LinkedHashMap<Pattern, String>();
		private String def = null;

		public void addRouting(final String pattern, final String address) {
			log.fine("Adding routing: " + pattern + " --> " + address);
			routings.put(Pattern.compile(pattern), address);
			if (def == null) {
				def = address;
			} // end of if (def == null)
		}

		public String computeRouting(final String address) {
			if (address == null) {
				log.finer("For null address returning default routing: " + def);
				return def;
			} // end of if (address == null)
			for (Map.Entry<Pattern, String> entry: routings.entrySet()) {
				if (entry.getKey().matcher(address).find()) {
					log.finest("For address: " + address + " pattern: "
						+ entry.getKey().pattern() + " matched.");
					return entry.getValue();
				} // end of if (pattern.matcher(address).find())
			} // end of for ()
			return def;
		}

	}

} // RoutingsContainer
