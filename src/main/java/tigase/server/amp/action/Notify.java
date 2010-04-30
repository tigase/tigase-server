
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.server.amp.action;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;
import tigase.server.amp.ActionIfc;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.Queue;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Apr 27, 2010 5:36:03 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Notify implements ActionIfc {
	private static final String name = "notify";

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param rule
	 *
	 * @return
	 */
	@Override
	public Queue<Packet> execute(Element rule) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return name;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
