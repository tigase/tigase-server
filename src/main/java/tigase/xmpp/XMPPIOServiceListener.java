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

import java.util.Map;
import tigase.net.IOServiceListener;

/**
 * Describe interface XMPPIOServiceListener here.
 *
 *
 * Created: Wed Feb  8 10:19:41 2006
 *
 * @param <IO> 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface XMPPIOServiceListener<IO extends XMPPIOService> 
		extends IOServiceListener<IO> {

	String xmppStreamOpened(IO serv, Map<String, String> attribs);

	void xmppStreamClosed(IO serv);

} // XMPPIOServiceListener