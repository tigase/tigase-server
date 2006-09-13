/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
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
package tigase.xmpp.impl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Queue;
import java.util.UUID;
import java.util.logging.Logger;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import tigase.auth.SessionCallback;
import tigase.net.ConnectionType;
import tigase.server.Packet;
import tigase.util.Algorithms;
import tigase.util.JID;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

/**
 * Describe class Dialback here.
 *
 *
 * Created: Wed Aug 16 18:27:31 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Dialback extends XMPPProcessor {

  /**
   * Private logger for class instancess.
   */
  private static final Logger log =
		Logger.getLogger("tigase.xmpp.impl.Dialback");

	public static final String MY_HOSTNAME_KEY = "my-hostname";
	public static final String REMOTE_HOSTNAME_KEY = "remote-hostname";

	private static final String XMLNS = "jabber:server:dialback";
	protected static final String ID = XMLNS;
  protected static final String[] ELEMENTS =
	{"db:result", "db:verify"};
  protected static final String[] XMLNSS = {XMLNS, XMLNS};

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

// 	public void process(Packet packet, Map<String, Object> sessionData,
// 		Queue<Packet> results) {

// 		String remote_hostname = JID.getNodeHost(packet.getElemFrom());
// 		String connect_jid = JID.getJID(null, remote_hostname,
// 			ConnectionType.connect.toString());

// 		// <db:result>
// 		if (packet != null && packet.getElemName().equals("db:result")) {
// 			if (packet.getType() == null) {
// 				// db:result with key to validate from accept connection
// 				// Assuming this is the first packet from that connection which
// 				// tells us for what domain this connection is we have to map
// 				// somehow this IP address to hostname
// 				sessionData.put(remote_hostname + "-session-id",
// 					packet.getElemId());
// 				sessionData.put(remote_hostname + "-dialback-key",
// 					packet.getElemCData());

// 				// <db:result> with CDATA containing KEY
// 				Element elem = new Element("db:verify", packet.getElemCData(),
// 					new String[] {"id", "to", "from"},
// 					new String[] {packet.getElemId(), packet.getElemFrom(),
// 												packet.getElemTo()});
// 				Packet result = new Packet(elem);
// 				result.setTo(connect_jid);
// 				results.offer(result);
// 			} else {
// 				// <db:result> with type 'valid' or 'invalid'
// 				// It means that session has been validated now....
// 				switch (packet.getType()) {
// 				case valid:
// 					CallbackHandler cbh =
// 						new AuthCallbackHandler(conn.getParentSession());
// 					try {
// 						LoginContext lc = new LoginContext("dialback", cbh);
// 						conn.setLoginContext(lc);
// 						conn.login();
// 					} catch (LoginException e) { e.printStackTrace(); }
// 					break;
// 				case invalid:
// 				default:
// 					break;
// 				} // end of switch (packet.getType())
// 			} // end of if (packet.getType() != null) else
// 		} // end of if (packet != null && packet.getElemName().equals("db:result"))

// 		// <db:verify> with type 'valid' or 'invalid'
// 		if (packet != null && packet.getElemName().equals("db:verify")
// 			&& packet.getType() != null) {
// 			Element elem = new Element("db:result", null,
// 				new String[] {"type", "to", "from"},
// 				new String[] {packet.getType().toString(),
// 											packet.getElemFrom(), packet.getElemTo()});
// 			Packet result = new Packet(elem);
// 			result.setTo(JID.getJID(null, ipAddress,
// 						ConnectionType.accept.toString()));
// 			results.offer(result);
// 		} // end of if (packet != null && packet.getType() != null)

// 		// <db:verify> with ID and CDATA containing KEY
// 		if (packet != null && packet.getType() == null) {
// 			if (packet.getElemName().equals("db:verify")
// 				&& packet.getElemId() != null
// 				&& packet.getElemCData("db:verify") != null) {

// 				final String key = packet.getElemCData("db:verify");

// 				final XMPPResourceConnection connect_conn =
// 					conn.getParentSession().getResourceForJID(JID.getJID(null,
// 							ipAddress, ConnectionType.connect.toString()));

// 				if (connect_conn == null) {
// 					log.warning("!!!!! connect_conn == NULL for session="
// 						+conn.getParentSession().getUserName());
// 				} // end of if (connect_conn == null)

// 				final String local_key =
// 					(String)session.getSharedObject("-dialback-key");

// 				Packet result = null;

// 				if (key.equals(local_key)) {
// 					result = packet.swapElemFromTo(StanzaType.valid);
// 				} // end of if (key.equals(local_key))
// 				else {
// 					result = packet.swapElemFromTo(StanzaType.invalid);
// 				} // end of if (key.equals(local_key)) else
// 				result.getElement().setCData(null);
// 				result.setTo(JID.getJID(null, ipAddress,
// 						ConnectionType.accept.toString()));
// 				results.offer(result);
// 			} // end of if (packet.getElemName().equals("db:verify"))
// 		} // end of if (packet != null && packet.getType() == null)

// 	}

	public void process(Packet packet, XMPPResourceConnection session,
		Queue<Packet> results) { }

// 	public void process(final Packet packet, final Map<String, Object> sessionData,
// 		final Queue<Packet> results) {

// 		XMPPSession session = conn.getParentSession();
// 		String ipAddress = session.getUserName();
// 		String connect_jid = JID.getJID(null, ipAddress,
// 			ConnectionType.connect.toString());
// 		String remote_hostname = null;
// 		if (packet != null) {
// 			remote_hostname = JID.getNodeHost(packet.getElemFrom());
// 		} // end of if (packet != null)


// 		// <db:result>
// 		if (packet != null && packet.getElemName().equals("db:result")) {
// 			if (packet.getType() == null) {
// 				session.putSharedObject(remote_hostname + "-session-id",
// 					conn.getSessionId());
// 				session.putSharedObject(remote_hostname + "-dialback-key",
// 					packet.getElemCData());

// 				// <db:result> with CDATA containing KEY
// 				Element elem = new Element("db:verify", packet.getElemCData(),
// 					new String[] {"id", "to", "from"},
// 					new String[] {conn.getSessionId(), packet.getElemFrom(),
// 												packet.getElemTo()});
// 				Packet result = new Packet(elem);
// 				result.setTo(connect_jid);
// 				results.offer(result);
// 			} else {
// 				// <db:result> with type 'valid' or 'invalid'
// 				switch (packet.getType()) {
// 				case valid:
// 					CallbackHandler cbh =
// 						new AuthCallbackHandler(conn.getParentSession());
// 					try {
// 						LoginContext lc = new LoginContext("dialback", cbh);
// 						conn.setLoginContext(lc);
// 						conn.login();
// 					} catch (LoginException e) { e.printStackTrace(); }
// 					break;
// 				case invalid:
// 				default:
// 					break;
// 				} // end of switch (packet.getType())
// 			} // end of if (packet.getType() != null) else
// 		} // end of if (packet != null && packet.getElemName().equals("db:result"))

// 		// <db:verify> with type 'valid' or 'invalid'
// 		if (packet != null && packet.getElemName().equals("db:verify")
// 			&& packet.getType() != null) {
// 			Element elem = new Element("db:result", null,
// 				new String[] {"type", "to", "from"},
// 				new String[] {packet.getType().toString(),
// 											packet.getElemFrom(), packet.getElemTo()});
// 			Packet result = new Packet(elem);
// 			result.setTo(JID.getJID(null, ipAddress,
// 						ConnectionType.accept.toString()));
// 			results.offer(result);
// 		} // end of if (packet != null && packet.getType() != null)

// 		// <db:verify> with ID and CDATA containing KEY
// 		if (packet != null && packet.getType() == null) {
// 			if (packet.getElemName().equals("db:verify")
// 				&& packet.getElemId() != null
// 				&& packet.getElemCData("db:verify") != null) {

// 				final String key = packet.getElemCData("db:verify");

// 				final XMPPResourceConnection connect_conn =
// 					conn.getParentSession().getResourceForJID(JID.getJID(null,
// 							ipAddress, ConnectionType.connect.toString()));

// 				if (connect_conn == null) {
// 					log.warning("!!!!! connect_conn == NULL for session="
// 						+conn.getParentSession().getUserName());
// 				} // end of if (connect_conn == null)

// 				final String local_key =
// 					(String)session.getSharedObject("-dialback-key");

// 				Packet result = null;

// 				if (key.equals(local_key)) {
// 					result = packet.swapElemFromTo(StanzaType.valid);
// 				} // end of if (key.equals(local_key))
// 				else {
// 					result = packet.swapElemFromTo(StanzaType.invalid);
// 				} // end of if (key.equals(local_key)) else
// 				result.getElement().setCData(null);
// 				result.setTo(JID.getJID(null, ipAddress,
// 						ConnectionType.accept.toString()));
// 				results.offer(result);
// 			} // end of if (packet.getElemName().equals("db:verify"))
// 		} // end of if (packet != null && packet.getType() == null)
// 	}

//   private class AuthCallbackHandler implements CallbackHandler {

// 		private XMPPSession session = null;

// 		public AuthCallbackHandler(final XMPPSession session) {
// 			this.session = session;
// 		}

// 		public void handle(final Callback[] callbacks)
//       throws IOException, UnsupportedCallbackException {

//       for (int i = 0; i < callbacks.length; i++) {
//         log.finest("Callback: " + callbacks[i].getClass().getSimpleName());
// 				if (callbacks[i] instanceof SessionCallback) {
//           log.finest("SessionCallback: " + session.getUserName());
// 					SessionCallback sc = (SessionCallback)callbacks[i];
// 					sc.setSession(session);
//         } else {
//           throw new UnsupportedCallbackException(callbacks[i],
// 						"Unrecognized Callback");
//         }
//       }

//     }

//   }

} // Dialback
