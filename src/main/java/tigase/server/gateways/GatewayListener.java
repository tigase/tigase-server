/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
package tigase.server.gateways;

import tigase.server.Packet;

/**
 * Implementation of this class can listen for an XMPP packet received from
 * somewhere.
 *
 *
 * Created: Mon Nov 12 13:00:01 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface GatewayListener {

	/**
	 * The <code>packetReceived</code> method is called when data packet has
	 * been received from the external network. The data received from the
	 * external must be translated to one of packets known in the XMPP network:
	 * <code>message</code> or <code>presence</code> or <code>iq</code>.
	 *
	 * @param packet a <code>Packet</code> value with the data packet received
	 * from the external network.
	 */
	void packetReceived(Packet packet);

	/**
	 * The <code>logout</code> method is called when the connection to the external
	 * network has been terminated (closed).
	 *
	 * @param gc a <code>GatewayConnection</code> value of <code>this</code> object
	 * identifying the user connection to the external network.
	 */
	void logout(GatewayConnection gc);

	/**
	 * The <code>loginCompleted</code> method is called when the login to the
	 * external network has been completed. Normally when <code>login()</code>
	 * method is called the whole process make take some time (establishing network
	 * connectio, authenticating and so on...). Therefore it is recommended that
	 * the <code>login()</code> method returns immediately and the process is run
	 * in background (in threads pooling). Therefore on the successful loging
	 * completion this method must be called.
	 *
	 * @param gc a <code>GatewayConnection</code> value of <code>this</code> object
	 * identifying the user connection to the external network.
	 */
	void loginCompleted(GatewayConnection gc);

	/**
	 * The <code>gatewayException</code> method should be called when the exception
	 * occurs in side the gateway connection library. Normally the exception is
	 * recorded in the log file and no more actions are performed. If the exception
	 * is severe and irrecoverable then the <code>GatewayConnection</code> should
	 * call <code>logout(...)</code> method.
	 *
	 * @param gc a <code>GatewayConnection</code> value of <code>this</code> object
	 * identifying the user connection to the external network.
	 * @param exc a <code>Throwable</code> value of the exception thrown.
	 */
	void gatewayException(GatewayConnection gc, Throwable exc);

	/**
	 * The <code>userRoster</code> method should be called whenever the user roster
	 * in the external network has changed. After (inside) this call
	 * <code>GatewayConnection.getRoster()</code> method is called to retrieve
	 * user roster from the gateway connection.
	 *
	 * @param gc a <code>GatewayConnection</code> value of <code>this</code> object
	 * identifying the user connection to the external network.
	 */
	void userRoster(GatewayConnection gc);

	/**
	 * The <code>updateStatus</code> method is called to update status of the single
	 * roster contact.
	 *
	 * @param gc a <code>GatewayConnection</code> value of <code>this</code> object
	 * identifying the user connection to the external network.
	 * @param item a <code>RosterItem</code> value of the roster contact.
	 */
	void updateStatus(GatewayConnection gc, RosterItem item);

	/**
	 * The <code>formatJID</code> method is used to transform the external network
	 * user ID to the Jabber ID for this gateway. Normally the method calls:
	 * <code>XMLUtils.escape(legacyName.replace("@", "%") + "@" + myDomain())</code>
	 * but this maybe changed in the future therefore it is recomended to use this
	 * method instead of internal implementation.
	 *
	 * @param legacyName a <code>String</code> value of the user ID in the external
	 * network.
	 * @return a <code>String</code> value of the transormed user ID to the Jabber's
	 * form valid for this gateway deployment - including correct gateway domain name.
	 */
	String formatJID(String legacyName);

	/**
	 * The <code>decodeLegacyName</code> method is used to do the opposite processing
	 * to the <code>formatJID(...)</code> method. It extracts user ID used in the
	 * external network from the Jabber's ID used in this Gateway deployment.
	 * Normally it calls the following method:
	 * <code>XMLUtils.unescape(jid).split("@")[0].replace("%", "@")</code> but
	 * this can be changed in the future, therefore using this method is recommended
	 * over own implementations.
	 *
	 * @param jid a <code>String</code> value
	 * @return a <code>String</code> value
	 */
	String decodeLegacyName(String jid);

}
