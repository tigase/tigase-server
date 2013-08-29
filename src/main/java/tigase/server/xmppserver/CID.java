/*
 * CID.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



package tigase.server.xmppserver;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;

/**
 * Created: Jan 7, 2010 12:51:33 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CID {
	private static final Logger log = Logger.getLogger(CID.class.getName());

	//~--- fields ---------------------------------------------------------------

	private int    hash       = 3;
	private String localHost  = null;
	private String remoteHost = null;
	private String to_string  = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param cid
	 */
	public CID(String cid) {
		String[] cid_a = cid.split("@");

		this.localHost  = cid_a[0].intern();
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
		this.localHost  = ((localHost == null)
				? null
				: localHost.intern());
		this.remoteHost = ((remoteHost == null)
				? null
				: remoteHost.intern());
		updateToString();
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param o
	 *
	 *
	 *
	 * @return a value of boolean
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof CID) {
			return (localHost == ((CID) o).localHost) && (remoteHost == ((CID) o).remoteHost);
		} else {
			return false;
		}
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of int
	 */
	@Override
	public int hashCode() {
		return hash;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of String
	 */
	@Override
	public String toString() {
		return to_string;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of String
	 */
	public String getLocalHost() {
		return localHost;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of String
	 */
	public String getRemoteHost() {
		return remoteHost;
	}

	//~--- methods --------------------------------------------------------------

	private void updateToString() {
		this.to_string = "" + this.localHost + "@" + this.remoteHost;
		hash           = 47 * hash + ((this.localHost != null)
				? this.localHost.hashCode()
				: 0);
		hash           = 47 * hash + ((this.remoteHost != null)
				? this.remoteHost.hashCode()
				: 0);
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
