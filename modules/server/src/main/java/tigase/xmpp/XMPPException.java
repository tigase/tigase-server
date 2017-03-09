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
 * Base exception type used for other eceptions defined for <em>XMPP</em>
 * protocol. This type and all descendants are thrown by this package runtime.
 *
 * <p>
 * Created: Sat Oct 30 08:38:18 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPException extends Exception {

  private static final long serialVersionUID = 1L;

  public XMPPException() { super(); }
  public XMPPException(String message) { super(message); }
  public XMPPException(String message, Throwable cause) {
    super(message, cause);
  }
  public XMPPException(Throwable cause) { super(cause); }

} // XMPPException
