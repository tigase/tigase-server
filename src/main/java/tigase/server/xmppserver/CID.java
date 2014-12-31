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

package tigase.server.xmppserver;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jan 7, 2010 12:51:33 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CID {
	private static final Logger log = Logger.getLogger(CID.class.getName());

	//~--- fields ---------------------------------------------------------------

	private int hash = 3;
	private String localHost = null;
	private String remoteHost = null;
	private String to_string = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param cid
	 */
	public CID(String cid) {
		String[] cid_a = cid.split("@");

		this.localHost = cid_a[0].intern();
		this.remoteHost = cid_a[1].intern();
		updateToString();
	}

	/**
	 * Constructs ...
	 *
	 *
	 *
	 * @param localHost
	 * @param remoteHost
	 */
	public CID(String localHost, String remoteHost) {
		this.localHost = ((localHost == null) ? null : localHost.intern());
		this.remoteHost = ((remoteHost == null) ? null : remoteHost.intern());
		updateToString();
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public boolean equals(Object o) {
		if (o instanceof CID) {
			return (localHost == ((CID) o).localHost) && (remoteHost == ((CID) o).remoteHost);
		} else {
			return false;
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getLocalHost() {
		return localHost;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public String getRemoteHost() {
		return remoteHost;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		return to_string;
	}

	private void updateToString() {
		this.to_string = "" + this.localHost + "@" + this.remoteHost;
		hash = 47 * hash + ((this.localHost != null) ? this.localHost.hashCode() : 0);
		hash = 47 * hash + ((this.remoteHost != null) ? this.remoteHost.hashCode() : 0);
	}
}
