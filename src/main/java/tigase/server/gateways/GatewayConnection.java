/*
 *   Tigase Jabber/XMPP Server
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

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

//~--- interfaces -------------------------------------------------------------

/**
 * Instance of the GatewayConnection identifies a single user connection
 * to the external network. The gateway connection is responsinble for
 * maintaining the connection and translating all the data between
 * Jabber/XMPP protocol and the protocol of the external network.
 * <p/>
 * It is recommended that the library for connecting to the exernal
 * network uses internal thread pooling and all it's method work in
 * non-blocking mode. Otherwise delays on one connection may affect
 * all other connections.
 * <p/>
 * Please consider also using NIO for better networking performnce.
 * Recommended library for NIO is the one available in Tigase server
 * core package.
 *
 * Created: Mon Nov 12 15:07:33 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface GatewayConnection {

	/**
	 * The <code>addBuddy</code> method is called when the Jabber user has requested
	 * adding new buddy to his roster from the external network. The ID is a buddy
	 * name in the external network, already decoded from the Jabber's form.
	 *
	 * @param id a <code>String</code> value of the buddy ID in the external network.
	 * @param nick a <code>String</code> value of the buddy screen name.
	 * @exception GatewayException if an error occurs
	 */
	void addBuddy(String id, String nick) throws GatewayException;

///**
// * The <code>setGatewayDomain</code> method sets the transort domain name.
// * This might be used by the connection to set correct from address in packets
// * send from the transport. This is no longer recommended. Please use
// * <code>listener.formatJID(...)</code> instead to format <code>from</code>
// * address.
// *
// * @param domain a <code>String</code> value is a transport component domain name.
// * @Deprecated
// */
//@Deprecated
//void setGatewayDomain(String domain);

	/**
	 * The <code>addJid</code> method is called when the user's Jabber has been
	 * activated another connection to the Jabber server. In other words another
	 * user resource is active. The transport has to decide where to send messages
	 * coming from the external network.
	 *
	 * @param jid a <code>String</code> value of the Jabber user full JID including
	 * resource part.
	 */
	void addJid(JID jid);

	//~--- get methods ----------------------------------------------------------

	/**
	 * The <code>getAllJids</code> method returns list of all Jabber user JIDs which
	 * have been added. In other words this is a list of all user's active resources.
	 *
	 * @return a <code>String[]</code> value list of all active Jabber's IDs.
	 */
	JID[] getAllJids();

	/**
	 * The <code>getName</code> method returns the transport screen name which
	 * is presented the end user in the service discovery function.
	 *
	 * @return a <code>String</code> value of the gateway screen name.
	 */
	String getName();

	/**
	 * The <code>getPromptMessage</code> method returns the prompt message sent to
	 * the user upon registration request.
	 *
	 * @return a <code>String</code> value of the prompt message.
	 */
	String getPromptMessage();

	/**
	 * The <code>getRoster</code> method returns the user roster from the external
	 * network account if known. If the roster is not (yet) known the method should
	 * return <code>null</code>. The method can be called at any time during the
	 * live time of the object.
	 *
	 * @return a <code>String</code> value of the list with roster items..
	 */
	List<RosterItem> getRoster();

	/**
	 * The <code>getType</code> method returns the transport type as described in
	 * <a href="http://www.xmpp.org/extensions/xep-0100.html#addressing-gateway">
	 * Addressing Gateway</a> section. The type is used to return service discovery
	 * for the gateway using this <code>GatewayConnection</code> implementation.
	 * The transport type is normally used by the client application to apply proper
	 * icon for the transport.
	 *
	 * @return a <code>String</code> value of the gateway type.
	 */
	String getType();

	//~--- methods --------------------------------------------------------------

	/**
	 * The <code>init</code> method is called to initialize the gateway connection
	 * for earlier specified user in <code>setLogin()</code> method. Normally the
	 * method should not open network connection yet. This is to initialize internal
	 * variables and structures.
	 *
	 * @exception GatewayException if an error occurs
	 */
	void init() throws GatewayException;

	/**
	 * The <code>login</code> method is called after <code>setLogin()</code> and
	 * <code>init()</code> method. This call is supposed to open network connection
	 * to the external network.
	 *
	 * @exception LoginGatewayException if an error occurs
	 */
	void login() throws LoginGatewayException;

	/**
	 * The <code>logout</code> method is called to logout from the external
	 * network and close the network connection. Normally it is called after the
	 * user sends <code>unavailable</code> presence stanza from the last active
	 * Jabber connection. Or when the gateway is shuting down.
	 *
	 */
	void logout();

	/**
	 * The <code>removeBuddy</code> method is called when the Jabber user has
	 * requested removal of the buddy from his roster. The ID is a buddy
	 * name in the external network, already decoded from the Jabber's form.
	 *
	 * @param id a <code>String</code> value of the buddy ID in the external network.
	 * @exception GatewayException if an error occurs
	 */
	void removeBuddy(String id) throws GatewayException;

	/**
	 * The <code>removeJid</code> method is called when the user's Jabber connection
	 * has been closed and the resource is no longer available. If the last user
	 * connection has been closed the next method called after <code>removeJid</code>
	 * is <code>logout()</code>.
	 *
	 * @param jid a <code>String</code> value of the Jabber user full JID including
	 * resource part.
	 */
	void removeJid(JID jid);

	/**
	 * The <code>sendMessage</code> method is called to submit a message from the
	 * Jabber network to the externl network. The destination address is in the
	 * JID nick part and normally it can be obtained by calling:
	 * <code>listener.decodeLegacyName(packet.getElemTo())</code>.
	 *
	 * @param message a <code>Packet</code> value
	 * @exception GatewayException if an error occurs
	 */
	void sendMessage(Packet message) throws GatewayException;

	//~--- set methods ----------------------------------------------------------

	/**
	 * The <code>setGatewayListener</code> method sets the gateway listener. The
	 * object which can receive packets from the external network.
	 *
	 * @param listener a <code>GatewayListener</code> value of the gateway listener.
	 */
	void setGatewayListener(GatewayListener listener);

	/**
	 * The <code>setLogin</code> method initializes the instance of the
	 * <code>GatewayConnection</code> with user ID and password used for
	 * loging into the external network.
	 *
	 * @param username a <code>String</code> value of the user ID for the external
	 * network.
	 * @param password a <code>String</code> value of the user password for the
	 * external network.
	 */
	void setLogin(String username, String password);
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
