
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
package tigase.server.amp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- interfaces -------------------------------------------------------------

/**
 * Created: Apr 26, 2010 5:05:58 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ActionIfc extends AmpFeatureIfc {
	boolean execute(Packet packet, Element rule);

	//~--- get methods ----------------------------------------------------------

	Map<String, Object> getDefaults(Map<String, Object> params);

	//~--- set methods ----------------------------------------------------------

	void setProperties(Map<String, Object> props, ActionResultsHandlerIfc resultsHandler);
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
