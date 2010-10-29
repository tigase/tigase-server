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

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.AuthRepository;
import tigase.db.NonAuthUserRepository;

import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.Priority;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class SaslAuth here.
 *
 *
 * Created: Mon Feb 20 16:28:13 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SaslAuth extends XMPPProcessor implements XMPPProcessorIfc {
	private static final String XMLNS = "urn:ietf:params:xml:ns:xmpp-sasl";
	private static final Logger log = Logger.getLogger(SaslAuth.class.getName());
	private static final String ID = XMLNS;
	private static final String[] ELEMENTS = {
		"auth", "response", "challenge", "failure", "success", "abort"
	};
	private static final String[] XMLNSS = {
		XMLNS, XMLNS, XMLNS, XMLNS, XMLNS, XMLNS
	};
	private static final Element[] DISCO_FEATURES = {
		new Element("feature", new String[] { "var" }, new String[] { XMLNS }) };

	//~--- constant enums -------------------------------------------------------

	/**
	 * Enum description
	 *
	 */
	public enum ElementType {
		auth, abort, response, challenge, failure, success;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int concurrentQueuesNo() {
		return Runtime.getRuntime().availableProcessors();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String id() {
		return ID;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results,
				final Map<String, Object> settings) {
		if (session == null) {
			return;
		}    // end of if (session == null)

		if (session.isAuthorized()) {

			// Multiple authentication attempts....
			// Another authentication request on already authenticated connection
			// This is not allowed and must be forbidden.
			Packet res = packet.swapFromTo(createReply(ElementType.failure, "<not-authorized/>"), null,
				null);

			// Make sure it gets delivered before stream close
			res.setPriority(Priority.SYSTEM);
			results.offer(res);

			// Optionally close the connection to make sure there is no
			// confusion about the connection state.
			results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set,
					session.nextStanzaId()));

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Discovered second authentication attempt: {0}, packet: {1}",
						new Object[] { session.toString(),
						packet.toString() });
			}

			try {
				session.logout();
			} catch (NotAuthorizedException ex) {
				log.log(Level.FINER, "Unsuccessful session logout: {0}", session.toString());
			}

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Session after logout: {0}", session.toString());
			}
		}

		Element request = packet.getElement();

//  ElementType type = null;
//  try {
//    type = ElementType.valueOf(request.getName());
//  } catch (IllegalArgumentException e) {
//    log.warning("Incorrect stanza type: " + request.getName());
//    results.offer(packet.swapFromTo(createReply(ElementType.failure,
//          "<temporary-auth-failure/>")));
//    results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(),
//        StanzaType.set, packet.getElemId()));
//    return;
//  } // end of try-catch
		Map<String, Object> authProps = (Map<String,
			Object>) (session.getSessionData(XMLNS + "-authProps"));

		if (authProps == null) {
			authProps = new HashMap<String, Object>(10, 0.75f);
			authProps.put(AuthRepository.PROTOCOL_KEY, AuthRepository.PROTOCOL_VAL_SASL);
			authProps.put(AuthRepository.MACHANISM_KEY, request.getAttribute("/auth", "mechanism"));
			authProps.put(AuthRepository.REALM_KEY, session.getDomain().getVhost().getDomain());
			authProps.put(AuthRepository.SERVER_NAME_KEY, session.getDomain().getVhost().getDomain());
			session.putSessionData(XMLNS + "-authProps", authProps);
		}    // end of if (authProps == null)

		// String user = (String)authProps.get(AuthRepository.USER_ID_KEY);
		authProps.put(AuthRepository.DATA_KEY, request.getCData());

		try {
			Authorization result = session.loginOther(authProps);
			String challenge_data = (String) authProps.get(AuthRepository.RESULT_KEY);

			if (result == Authorization.AUTHORIZED) {
				results.offer(packet.swapFromTo(createReply(ElementType.success, challenge_data), null,
						null));
				authProps.clear();
				session.removeSessionData(XMLNS + "-authProps");
			} else {
				results.offer(packet.swapFromTo(createReply(ElementType.challenge, challenge_data), null,
						null));
			}
		} catch (Exception e) {
			log.log(Level.INFO, "Authentication failed: ", e);

			// e.printStackTrace();
			session.removeSessionData(XMLNS + "-authProps");
			results.offer(packet.swapFromTo(createReply(ElementType.failure, "<not-authorized/>"), null,
					null));

			Integer retries = (Integer) session.getSessionData("auth-retries");

			if (retries == null) {
				retries = new Integer(0);
			}

			if (retries.intValue() < 3) {
				session.putSessionData("auth-retries", new Integer(retries.intValue() + 1));
			} else {
				results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set,
						session.nextStanzaId()));
			}
		}    // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supElements() {
		return ELEMENTS;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		if ((session == null) || session.isAuthorized()) {
			return null;
		} else {
			Map<String, Object> query = new HashMap<String, Object>();

			query.put(AuthRepository.PROTOCOL_KEY, AuthRepository.PROTOCOL_VAL_SASL);
			session.queryAuth(query);

			String[] auth_mechs = (String[]) query.get(AuthRepository.RESULT_KEY);
			Element[] mechs = new Element[auth_mechs.length];
			int idx = 0;

			for (String mech : auth_mechs) {
				mechs[idx++] = new Element("mechanism", mech);
			}    // end of for (String mech: mechs)

			return new Element[] {
				new Element("mechanisms", mechs, new String[] { "xmlns" }, new String[] { XMLNS }) };
		}    // end of if (session.isAuthorized()) else
	}

	private Element createReply(final ElementType type, final String cdata) {
		Element reply = new Element(type.toString());

		reply.setXMLNS(XMLNS);

		if (cdata != null) {
			reply.setCData(cdata);
		}    // end of if (cdata != null)

		return reply;
	}
}    // SaslAuth


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
