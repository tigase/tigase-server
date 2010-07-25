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

package tigase.db;

//~--- classes ----------------------------------------------------------------

/**
 * The <code>UserExistsException</code> is thrown when application tries to add
 * new user with user ID which already exists in repository.
 * According to <code>UserRepository</code> specification there can be the only
 * one registered user with particular ID.
 * <p>
 * Created: Wed Oct 27 14:17:44 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UserExistsException extends TigaseDBException {
	private static final long serialVersionUID = 1L;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>UserExistsException</code> instance.
	 *
	 * @param message
	 */
	public UserExistsException(String message) {
		super(message);
	}

	/**
	 * Creates a new <code>UserExistsException</code> instance.
	 *
	 * @param message
	 * @param cause
	 */
	public UserExistsException(String message, Throwable cause) {
		super(message, cause);
	}
}    // UserExistsException


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
