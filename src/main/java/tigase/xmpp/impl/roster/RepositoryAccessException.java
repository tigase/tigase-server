/*
 * RepositoryAccessException.java
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



package tigase.xmpp.impl.roster;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.XMPPException;

/**
 *
 * @author kobit
 */
public class RepositoryAccessException
				extends XMPPException {
	private static final long serialVersionUID = 1L;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>PacketErrorTypeException</code> instance.
	 *
	 * @param message
	 */
	public RepositoryAccessException(String message) {
		super(message);
	}

	/**
	 * Constructs ...
	 *
	 *
	 * @param message
	 * @param cause
	 */
	public RepositoryAccessException(String message, Throwable cause) {
		super(message, cause);
	}
}


//~ Formatted in Tigase Code Convention on 13/11/27
