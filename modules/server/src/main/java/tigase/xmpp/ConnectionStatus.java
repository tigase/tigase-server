/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

/**
 * The XMPPResourceConnection Object can be put in special states
 * when all packets received to by this connection are threated in
 * different way:
 * <code>INIT</code> - Behaviour is like in <code>NORMAL</code> state. It is
 * just an indication that the session is in an initial phase before authentication.
 * <code>ON_HOLD</code> - packets are not processed at all, they are
 * collected by instead for later time.
 * <code>REDIRECT</code> - packets received by this connection are
 * being redirected to a different SM for processing.
 * <code>NORMAL</code> - packets are processed in normal way.
 *
 *
 * Created: Wed Aug 13 20:58:33 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Deprecated
public enum ConnectionStatus {

	INIT, ON_HOLD, REDIRECT, NORMAL,
  // The TEMP state is a special state used in the session transfer
  TEMP,
	// The REMOTE state is for user session on remote cluster nodes
	REMOTE;

}
