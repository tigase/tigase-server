
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
package tigase.xmpp;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author kobit
 */
public class NoConnectionIdException extends XMPPException {
	private static final long serialVersionUID = 1L;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	public NoConnectionIdException() {
		super();
	}

	/**
	 * Constructs ...
	 *
	 *
	 * @param message
	 */
	public NoConnectionIdException(String message) {
		super(message);
	}

	/**
	 * Constructs ...
	 *
	 *
	 * @param cause
	 */
	public NoConnectionIdException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs ...
	 *
	 *
	 * @param message
	 * @param cause
	 */
	public NoConnectionIdException(String message, Throwable cause) {
		super(message, cause);
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
