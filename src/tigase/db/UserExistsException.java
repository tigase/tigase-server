/*  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
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
package tigase.db;

/**
 * The <code>UserExistsException</code> is thrown when application tries to add
 * new user with user ID which already exists in repository.
 * According to <code>UserRepository</code> specification there can be the only
 * one registered user with particular ID.
 * <p>
 * Created: Wed Oct 27 14:17:44 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UserExistsException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new <code>UserExistsException</code> instance.
   *
   */
  public UserExistsException(String message) {
    super(message);
  }

  /**
   * Creates a new <code>UserExistsException</code> instance.
   *
   */
  public UserExistsException(String message, Throwable cause) {
    super(message, cause);
  }

} // UserExistsException
