/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package tigase.xmpp.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.NonAuthUserRepository;
import tigase.db.UserAuthRepository;
import tigase.xmpp.NotAuthorizedException;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPException;

/**
 * JEP-0078: Non-SASL Authentication
 *
 *
 * Created: Thu Feb 16 17:46:16 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqAuth extends XMPPProcessor
	implements XMPPProcessorIfc {

	/**
	 * Private logger for class instancess.
	 */
	private static final Logger log =
		Logger.getLogger("tigase.xmpp.impl.JabberIqAuth");

	private static final String XMLNS = "jabber:iq:auth";
	private static final String ID = XMLNS;
	private static final String[] ELEMENTS = {"query"};
	private static final String[] XMLNSS = {XMLNS};
	private static final Element[] FEATURES = {
		new Element("auth",	new String[] {"xmlns"},
			new String[] {"http://jabber.org/features/iq-auth"})
	};
  private static final Element[] DISCO_FEATURES =	{
		new Element("feature", new String[] {"var"}, new String[] {XMLNS})
	};

	@Override
  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

	@Override
	public String id() { return ID; }

	@Override
	public String[] supElements()
	{ return ELEMENTS; }

	@Override
	public String[] supNamespaces()
	{ return XMLNSS; }

	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session)	{
		if (session == null || session.isAuthorized()) {
			return null;
		} else {
			return FEATURES;
		} // end of if (session.isAuthorized()) else
	}

	@Override
	public int concurrentQueuesNo() {
		return 1 + Runtime.getRuntime().availableProcessors() / 2;
	}

	@Override
	public int concurrentThreadsPerQueue() {
		// Packet processing does matter for roster/presence therefore
		// we need a single thread for each queue.
		return 2;
	}

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings)
		throws XMPPException {

		if (session == null) {
			return;
		} // end of if (session == null)

		Element request = packet.getElement();
		StanzaType type = packet.getType();
		switch (type) {
		case get:
			Map<String, Object> query = new HashMap<String, Object>();
			query.put(UserAuthRepository.PROTOCOL_KEY,
				UserAuthRepository.PROTOCOL_VAL_NONSASL);
			session.queryAuth(query);
			String[] auth_mechs = (String[])query.get(UserAuthRepository.RESULT_KEY);
			StringBuilder response = new StringBuilder("<username/>");
			for (String mech: auth_mechs) {
				response.append("<").append(mech).append("/>");
			} // end of for (String mech: auth_mechs)
			response.append("<resource/>");
			results.offer(packet.okResult(response.toString(), 1));
			break;
		case set:
			String user_name = request.getChildCData("/iq/query/username");
			String resource = request.getChildCData("/iq/query/resource");
			String password = request.getChildCData("/iq/query/password");
			String digest = request.getChildCData("/iq/query/digest");
			//String user_pass = null;
			String auth_mod = null;
			try {
				Authorization result = null;
				if (password != null) {
// 					user_pass = password;
// 					result = session.loginPlain(user_name, user_pass);
					result = session.loginPlain(user_name, password);
				} // end of if (password != null)
				if (digest != null) {
					//user_pass = digest;
					result = session.loginDigest(user_name, digest,
						session.getSessionId(), "SHA");
				} // end of if (digest != null)
				if (result == Authorization.AUTHORIZED) {
					session.setResource(resource);
					results.offer(session.getAuthState().getResponseMessage(packet,
							"Authentication successful.", false));
				} else {
					results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
							"Authentication failed", false));
					results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(),
							StanzaType.set, packet.getElemId()));
				} // end of else
			} catch (NotAuthorizedException e) {
				log.info("Authentication failed: " + user_name);
				results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
						e.getMessage(), false));
				Integer retries = (Integer)session.getSessionData("auth-retries");
				if (retries == null) {
					retries = new Integer(0);
				}
				if (retries.intValue() < 3) {
					session.putSessionData("auth-retries",
						new Integer(retries.intValue() + 1));
				} else {
					results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(),
							StanzaType.set, packet.getElemId()));
				}
			} catch (Exception e) {
				log.info("Authentication failed: " + user_name);
				log.log(Level.WARNING, "Authentication failed: ", e);
				results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
						e.getMessage(), false));
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

} // JabberIqAuth
