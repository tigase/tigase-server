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

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Describe class XMPPSession here.
 *
 *
 * Created: Wed Feb  8 22:14:28 2006
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPSession {

  /**
   * Private logger for class instancess.
   */
  private static Logger log = Logger.getLogger("tigase.xmpp.XMPPSession");

  /**
   * User name - part of user's JID
   */
  private String username = null;

	private ArrayList<XMPPResourceConnection> activeResources = null;

	/**
	 * Creates a new <code>XMPPSession</code> instance.
	 *
	 */
	public XMPPSession(final String username) {
		activeResources = new ArrayList<XMPPResourceConnection>();
		this.username = username;
	}

	public void streamClosed(XMPPResourceConnection conn) {
		removeResourceConnection(conn);
	}

	public String getUserName() {
		return username;
	}

	public void addResourceConnection(XMPPResourceConnection conn) {
		activeResources.add(conn);
		conn.setParentSession(this);
	}

	public void removeResourceConnection(XMPPResourceConnection conn) {
		activeResources.remove(conn);
		conn.setParentSession(null);
	}

} // XMPPSession
