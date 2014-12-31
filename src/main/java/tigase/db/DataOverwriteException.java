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
package tigase.db;

/**
 * The <code>DataOverwriteException</code> exception is thrown when application
 * tries to ovrewrite data in repository but does not have permission to do so.
 * <br>
 * Created: Wed Oct 27 14:17:44 2004
 * <br>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class DataOverwriteException extends TigaseDBException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new <code>DataOverwriteException</code> instance.
   *
   */
  public DataOverwriteException(String message) { super(message); }

  /**
   * Creates a new <code>DataOverwriteException</code> instance.
   *
   */
  public DataOverwriteException(String message, Throwable cause) {
    super(message, cause);
  }

} // DataOverwriteException
