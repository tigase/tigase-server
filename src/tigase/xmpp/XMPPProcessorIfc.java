/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
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
package tigase.xmpp;

import java.util.Queue;
import tigase.server.Packet;
import tigase.xml.Element;

/**
 * Describe interface XMPPProcessorIfc here.
 *
 *
 * Created: Wed Feb  8 13:47:56 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface XMPPProcessorIfc {

	String id();

	String[] supElements();

	String[] supNamespaces();

	Element[] supStreamFeatures(XMPPResourceConnection session);

	String[] supDiscoFeatures(XMPPResourceConnection session);

	boolean isSupporting(String elem, String ns);

	void process(Packet packet, XMPPResourceConnection session,
		Queue<Packet> results);

	void stopped(XMPPResourceConnection session, Queue<Packet> results);

} // XMPPProcessorIfc
