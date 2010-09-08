
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
package tigase.db;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xml.Element;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.Queue;

//~--- interfaces -------------------------------------------------------------

/**
 * Created: May 11, 2010 6:56:14 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface MsgRepositoryIfc {
	Element getMessageExpired(long time, boolean delete);

	//~--- methods --------------------------------------------------------------

	Queue<Element> loadMessagesToJID(JID to, boolean delete) throws UserNotFoundException;

	void storeMessage(JID from, JID to, Date expired, Element msg) throws UserNotFoundException;
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
