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
package tigase.net;

/**
 * <code>ServiceCommand</code> is enumerated type defining all possible
 * commands related to <code>ServiceData</code> instances.
 * These commands are processed by <code>MessageDispatcher</code>
 * implementations. Some commands are related to data encapsulated in
 * <code>ServiceData</code> instance like <code>SEND_MESSAGE</code> or
 * <code>BROADCAST</code> others are related to <code>ServerService</code>
 * sending or receiving this message like:
 * <code>STOP</code>, <code>CONNECTED</code> and so on. Please refer to detailed
 * API documentation for more information.
 *
 * <p>
 * Created: Sun Oct 17 22:32:22 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public enum ServiceCommand {

  STOP,
  STOPPED,
  CONNECT,
  CONNECTED,
  SEND_MESSAGE,
  BROADCAST;

} // ServiceCommand
