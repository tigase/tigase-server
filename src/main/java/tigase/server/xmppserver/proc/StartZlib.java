
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
package tigase.server.xmppserver.proc;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.xmppserver.S2SIOService;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Dec 9, 2010 2:01:12 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StartZlib extends S2SAbstractProcessor {
	private static final Logger log = Logger.getLogger(StartZlib.class.getName());
	private static final Element features = new Element("compression",
		new Element[] { new Element("method", "zlib") }, new String[] { "xmlns" },
		new String[] { "http://jabber.org/features/compress" });

	//~--- methods --------------------------------------------------------------

	@Override
	public int order() {
		return Order.StartZlib.ordinal();
	}
	
	@Override
	public void streamFeatures(S2SIOService serv, List<Element> results) {

		// results.add(features);
	}
}
