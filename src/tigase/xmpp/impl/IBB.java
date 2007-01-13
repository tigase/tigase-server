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
package tigase.xmpp.impl;

import java.util.Queue;
import java.util.logging.Logger;
import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.XMPPResourceConnection;

/**
 * XEP-0047: In-Band Bytestreams (IBB)
 *
 *
 * Created: Sat Jan 13 16:37:29 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class IBB extends SimpleForwarder {

  /**
   * Private logger for class instancess.
   */
  private static Logger log = Logger.getLogger("tigase.xmpp.impl.IBB");

  protected static final String XMLNS = "http://jabber.org/protocol/ibb";
	protected static final String ID = XMLNS;
	protected static final String[] ELEMENTS = {"open", "data", "close"};
  protected static final String[] XMLNSS = {XMLNS, XMLNS, XMLNS};

	// Implementation of tigase.xmpp.XMPPImplIfc

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

	public String[] supNamespaces() { return XMLNSS; }

}
