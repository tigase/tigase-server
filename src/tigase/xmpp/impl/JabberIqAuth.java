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
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import tigase.auth.ResourceConnectionCallback;
import tigase.db.UserNotFoundException;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.util.JID;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPResourceConnection;

/**
 * JEP-0078: Non-SASL Authentication
 *
 *
 * Created: Thu Feb 16 17:46:16 2006
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqAuth extends XMPPProcessor {

  /**
   * Private logger for class instancess.
   */
  private static final Logger log =
		Logger.getLogger("tigase.xmpp.impl.JabberIqAuth");

	protected static final String ID = "jabber:iq:auth";
	protected static final String[] ELEMENTS = {"query"};
  protected static final String[] XMLNSS = {"jabber:iq:auth"};
  protected static final String[] FEATURES = {
    "<auth xmlns='http://jabber.org/features/iq-auth'/>"};

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

  public String[] supStreamFeatures(final XMPPResourceConnection session)	{
    if (session != null && session.isAuthorized()) {
      return null;
    } else {
      return FEATURES;
    } // end of if (session.isAuthorized()) else
	}

	public void process(final Packet packet, final XMPPResourceConnection session,
		final Queue<Packet> results) {

		Element request = packet.getElement();
    StanzaType type = packet.getType();
		switch (type) {
		case get:
			results.offer(packet.okResult(
											//"<username/><password/><digest/><resource/>",
											"<username/><password/><resource/>",
											1));
			break;
		case set:
      String user_name = request.getChildCData("/iq/query/username");
      String resource = request.getChildCData("/iq/query/resource");
      String password = request.getChildCData("/iq/query/password");
			String digest = request.getChildCData("/iq/query/digest");
			String user_pass = null;
			String auth_mod = null;
			if (password != null) {
				user_pass = password;
				auth_mod = "auth-plain";
			} // end of if (password != null)
			if (digest != null) {
				user_pass = digest;
				auth_mod = "auth-digest";
			} // end of if (digest != null)
			CallbackHandler cbh =
				new AuthCallbackHandler(user_name, user_pass, resource, session);
			LoginContext lc = null;
			try {
				lc = new LoginContext(auth_mod, cbh);
				session.setLoginContext(lc);
				session.login();
				session.setResource(resource);
				results.offer(session.getAuthState().getResponseMessage(packet,
					"Authentication successful.", false));
			} catch (Exception e) {
				log.info("Authentication failed: " + user_name);
				log.log(Level.FINER, "Authentication failed: ", e);
				results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"Authentication failed", false));
				results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(),
						StanzaType.set, packet.getElemId()));
			}
			break;
		default:
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
					"Message type is incorrect", false));
				results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(),
						StanzaType.set, packet.getElemId()));
			break;
		} // end of switch (type)

	}

  private class AuthCallbackHandler implements CallbackHandler {

		private String name = null;
		private String password = null;
		private String resource = null;
		private XMPPResourceConnection connection = null;

		public AuthCallbackHandler(final String name, final String pass,
			final String resource, final XMPPResourceConnection connection) {
			// Some Jabber/XMPP clients send user name as full JID, others
			// send just nick name. Let's resolve this here.
			this.name = JID.getNodeNick(name);
			if (this.name == null || this.name.equals("")) {
				this.name = name;
			} // end of if (name == null || name.equals(""))
			this.password = pass;
			this.resource = resource;
			this.connection = connection;
		}

		public void handle(final Callback[] callbacks)
      throws IOException, UnsupportedCallbackException {

      for (int i = 0; i < callbacks.length; i++) {
        log.finest("Callback: " + callbacks[i].getClass().getSimpleName());
				if (callbacks[i] instanceof NameCallback) {
          log.finest("NameCallback: " + name);
					NameCallback nc = (NameCallback)callbacks[i];
					nc.setName(name);
        } else if (callbacks[i] instanceof PasswordCallback) {
          log.finest("NameCallback: " + password);
					PasswordCallback pc = (PasswordCallback)callbacks[i];
					pc.setPassword(password.toCharArray());
        } else if (callbacks[i] instanceof ResourceConnectionCallback) {
          log.finest("ResourceConnectionCallback: "
						+ connection.getConnectionId());
					ResourceConnectionCallback rcc =
						(ResourceConnectionCallback)callbacks[i];
					rcc.setResourceConnection(connection);
        } else {
          throw new UnsupportedCallbackException(callbacks[i],
						"Unrecognized Callback");
        }
      }

    }

  }

} // JabberIqAuth
