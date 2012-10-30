/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

/**
 * This exception is usually trown when there is kind of access violation error.
 * If some code try to access data to which it doesn't have permission to or if
 * try to access data before successful authorization process.
 *
 * <p>
 * Created: Sat Oct 30 08:44:37 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class NotAuthorizedException extends XMPPException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new <code>NotAuthorizedException</code> instance.
   *
   */
  public NotAuthorizedException(String message) { super(message); }

  public NotAuthorizedException(String message, Throwable cause) {
    super(message, cause);
  }

} // NotAuthorizedException
