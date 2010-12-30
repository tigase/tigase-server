
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
package tigase.server.xmppserver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.Queue;

//~--- interfaces -------------------------------------------------------------

/**
 * Created: Dec 9, 2010 1:50:09 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface S2SProcessor {

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	public void serviceStarted(S2SIOService serv);

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	public void serviceStopped(S2SIOService serv);

	/**
	 * Method description
	 *
	 *
	 *
	 * @param serv
	 * @param results
	 */
	public void streamFeatures(S2SIOService serv, List<Element> results);

	void init(S2SConnectionHandlerIfc<S2SIOService> handler);

	boolean process(Packet p, S2SIOService serv, Queue<Packet> results);

	void streamClosed(S2SIOService serv);

	String streamOpened(S2SIOService serv, Map<String, String> attribs);
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
