/*
 *   Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

package tigase.xmpp.impl.roster;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class RosterFactory here.
 *
 *
 * Created: Thu Sep  4 18:33:11 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class RosterFactory {
	private static RosterAbstract shared = null;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param shared_impl
	 *
	 * @return
	 */
	public static RosterAbstract getRosterImplementation(boolean shared_impl) {
		if (shared_impl) {
			if (shared == null) {
				shared = new RosterFlat();
			}

			return shared;
		}

		return new RosterFlat();
	}

	/**
	 * Method description
	 *
	 *
	 * @param class_name
	 * @param shared_impl
	 *
	 * @return
	 *
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static RosterAbstract getRosterImplementation(String class_name, boolean shared_impl)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (shared_impl) {
			if (shared == null) {
				shared = (RosterAbstract) Class.forName(class_name).newInstance();
			}

			return shared;
		}

		return (RosterAbstract) Class.forName(class_name).newInstance();
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
