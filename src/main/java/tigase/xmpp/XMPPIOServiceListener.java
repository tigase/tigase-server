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

import tigase.net.IOServiceListener;
import tigase.xml.Element;

import java.util.List;
import java.util.Map;

//~--- JDK imports ------------------------------------------------------------

//~--- interfaces -------------------------------------------------------------

/**
 * Describe interface XMPPIOServiceListener here.
 *
 *
 * Created: Wed Feb  8 10:19:41 2006
 *
 * @param <IO>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public interface XMPPIOServiceListener<IO extends XMPPIOService<?>> extends IOServiceListener<IO> {
	void xmppStreamClosed(IO serv);

	String[] xmppStreamOpened(IO serv, Map<String, String> attribs);
	
	String xmppStreamError(IO serv, List<Element> err_el); 
}    // XMPPIOServiceListener
