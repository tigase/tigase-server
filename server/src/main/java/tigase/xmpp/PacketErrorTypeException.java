/*
 * PacketErrorTypeException.java
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



package tigase.xmpp;

/**
 * Describe class PacketErrorTypeException here.
 *
 *
 * Created: Tue Oct  9 13:41:43 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class PacketErrorTypeException
				extends XMPPException {
	private static final long serialVersionUID = 1L;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>PacketErrorTypeException</code> instance.
	 *
	 *
	 * @param message
	 */
	public PacketErrorTypeException(String message) {
		super(message);
	}

	/**
	 * Constructs ...
	 *
	 *
	 * @param message
	 * @param cause
	 */
	public PacketErrorTypeException(String message, Throwable cause) {
		super(message, cause);
	}
}


//~ Formatted in Tigase Code Convention on 13/11/29
