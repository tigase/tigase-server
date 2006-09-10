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

  public void process(final Packet packet, final XMPPResourceConnection session,
		final Queue<Packet> results) {

		final String ipAddress = session.getParentSession().getUserName();

		// <stream:stream> response
		if (packet == null) {
			// Well, I have to send db:result with unique session ID
			String remote_hostname =
				(String)session.getSessionData(REMOTE_HOSTNAME_KEY);
			final String id = UUID.randomUUID().toString();
			String key = null;
			try {
				key = Algorithms.digest(session.getSessionId(), id, "SHA");
			} catch (NoSuchAlgorithmException e) {
				key = id;
			} // end of try-catch
			session.putSessionData("dialback-key", key);
			Element elem = new Element("db:result", key);
			elem.addAttribute("to", remote_hostname);
			elem.addAttribute("from", session.getDomain());
			Packet result = new Packet(elem);
			result.setTo(JID.getJID(null, ipAddress,
					ConnectionType.connect.toString()));
			results.offer(result);
		} // end of if (packet == null)

		// <db:result> with type 'valid' or 'invalid'
		if (packet != null && packet.getElemName().equals("db:result")) {
			if (packet.getType() == null) {
				// <db:result> with CDATA containing KEY
				Element elem = new Element("db:verify", packet.getElemCData(),
					new String[] {"id", "to", "from"},
					new String[] {session.getSessionId(), packet.getElemFrom(),
												packet.getElemTo()});
				Packet result = new Packet(elem);
				result.setTo(JID.getJID(null, ipAddress,
						ConnectionType.connect.toString()));
				results.offer(result);
			} else {
				switch (packet.getType()) {
				case valid:
					CallbackHandler cbh =
						new AuthCallbackHandler(session.getParentSession());
					try {
						LoginContext lc = new LoginContext("dialback", cbh);
						session.setLoginContext(lc);
						session.login();
					} catch (LoginException e) { e.printStackTrace(); }
					break;
				case invalid:
				default:
					break;
				} // end of switch (packet.getType())
			} // end of if (packet.getType() != null) else
		} // end of if (packet != null && packet.getElemName().equals("db:result"))

		// <db:verify> with type 'valid' or 'invalid'
		if (packet != null && packet.getElemName().equals("db:verify")
			&& packet.getType() != null) {
			Element elem = new Element("db:result", null,
				new String[] {"type", "to", "from"},
				new String[] {packet.getType().toString(),
											packet.getElemFrom(), packet.getElemTo()});
			Packet result = new Packet(elem);
			result.setTo(JID.getJID(null, ipAddress,
						ConnectionType.accept.toString()));
			results.offer(result);
		} // end of if (packet != null && packet.getType() != null)

		// <db:verify> with ID and CDATA containing KEY
		if (packet != null && packet.getType() == null) {
			if (packet.getElemName().equals("db:verify")
				&& packet.getElemId() != null
				&& packet.getElemCData("db:verify") != null) {

				final String key = packet.getElemCData("db:verify");

				final XMPPResourceConnection connect_conn =
					session.getParentSession().getResourceForJID(JID.getJID(null,
							ipAddress, ConnectionType.connect.toString()));
				final String local_key =
					(String)connect_conn.getSessionData("dialback-key");
				Packet result = null;
				if (key.equals(local_key)) {
					result = packet.swapElemFromTo(StanzaType.valid);
				} // end of if (key.equals(local_key))
				else {
					result = packet.swapElemFromTo(StanzaType.invalid);
				} // end of if (key.equals(local_key)) else
				result.getElement().setCData(null);
				result.setTo(JID.getJID(null, ipAddress,
						ConnectionType.accept.toString()));
				results.offer(result);
			} // end of if (packet.getElemName().equals("db:verify"))
		} // end of if (packet != null && packet.getType() == null)
	}

  private class AuthCallbackHandler implements CallbackHandler {

		private XMPPSession session = null;

		public AuthCallbackHandler(final XMPPSession session) {
			this.session = session;
		}

		public void handle(final Callback[] callbacks)
      throws IOException, UnsupportedCallbackException {

      for (int i = 0; i < callbacks.length; i++) {
        log.finest("Callback: " + callbacks[i].getClass().getSimpleName());
				if (callbacks[i] instanceof SessionCallback) {
          log.finest("SessionCallback: " + session.getUserName());
					SessionCallback sc = (SessionCallback)callbacks[i];
					sc.setSession(session);
        } else {
          throw new UnsupportedCallbackException(callbacks[i],
						"Unrecognized Callback");
        }
      }

    }

  }

} // Dialback
