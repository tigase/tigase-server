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

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import tigase.auth.ResourceConnectionCallback;
import tigase.db.NonAuthUserRepository;
import tigase.db.UserAuthRepository;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Describe class SaslAuth here.
 *
 *
 * Created: Mon Feb 20 16:28:13 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SaslAuth extends XMPPProcessor
	implements XMPPProcessorIfc {

  private static final String XMLNS = "urn:ietf:params:xml:ns:xmpp-sasl";

  private static final Logger log = Logger.getLogger("tigase.xmpp.impl.SaslAuth");

	protected static final String ID = XMLNS;
  protected static final String[] ELEMENTS = {
    "auth", "response", "challenge", "failure", "success", "abort"};
  protected static final String[] XMLNSS = {
    XMLNS, XMLNS, XMLNS, XMLNS, XMLNS, XMLNS};

  public enum ElementType {
    auth,
    abort,
    response,
    challenge,
		failure,
		success;
  }

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

  public Element[] supStreamFeatures(final XMPPResourceConnection session)	{
    if (session.isAuthorized()) {
      return null;
    } else {
			Map<String, Object> query = new HashMap<String, Object>();
			query.put(UserAuthRepository.PROTOCOL_KEY,
				UserAuthRepository.PROTOCOL_VAL_SASL);
			session.queryAuth(query);
			String[] auth_mechs = (String[])query.get(UserAuthRepository.RESULT_KEY);
			Element[] mechs = new Element[auth_mechs.length];
			int idx = 0;
			for (String mech: auth_mechs) {
				mechs[idx++] = new Element("mechanism", mech);
			} // end of for (String mech: mechs)
      return new Element[] {new Element("mechanisms", mechs,
					new String[] {"xmlns"}, new String[] {XMLNS})};
    } // end of if (session.isAuthorized()) else
	}

  public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results) {

		if (session == null) {
			return;
		} // end of if (session == null)

		Element request = packet.getElement();
		ElementType type = null;
		try {
			type = ElementType.valueOf(request.getName());
		} catch (IllegalArgumentException e) {
			log.warning("Incorrect stanza type: " + request.getName());
			results.offer(packet.swapFromTo(createReply(ElementType.failure,
						"<temporary-auth-failure/>")));
			results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(),
					StanzaType.set, packet.getElemId()));
			return;
		} // end of try-catch
		Map<String, Object> authProps =
			(Map) session.getSessionData(XMLNS+"-authProps");
		if (authProps == null) {
			authProps = new HashMap<String, Object>();
			authProps.put(UserAuthRepository.PROTOCOL_KEY,
				UserAuthRepository.PROTOCOL_VAL_SASL);
			authProps.put(UserAuthRepository.MACHANISM_KEY,
				request.getAttribute("/auth", "mechanism"));
			authProps.put(UserAuthRepository.REALM_KEY, session.getDomain());
			authProps.put(UserAuthRepository.SERVER_NAME_KEY, session.getDomain());
			session.putSessionData(XMLNS+"-authProps", authProps);
		} // end of if (authProps == null)
		String user = (String)authProps.get(UserAuthRepository.USER_ID_KEY);
		authProps.put(UserAuthRepository.DATA_KEY, request.getCData());
		try {
			Authorization result = session.loginOther(authProps);
			String challenge_data =
				(String)authProps.get(UserAuthRepository.RESULT_KEY);
			if (result == Authorization.AUTHORIZED) {
				results.offer(packet.swapFromTo(createReply(ElementType.success,
							challenge_data)));
				authProps.clear();
			} else {
				results.offer(packet.swapFromTo(createReply(ElementType.challenge,
							challenge_data)));
			}
		} catch (Exception e) {
			e.printStackTrace();
			results.offer(packet.swapFromTo(createReply(ElementType.failure,
 						"<temporary-auth-failure/>")));
		} // end of try-catch
// 		boolean failure = false;
//     switch (type) {
//     case auth:
//       String mechanism = request.getAttribute("/auth", "mechanism");
//       try {
// 				 String challenge_data = null;
//         Map<String, String> props = new TreeMap<String, String>();
//         props.put(Sasl.QOP, "auth");
//         SaslServer ss = Sasl.createSaslServer(mechanism, "xmpp",
//           session.getDomain(), props, new SaslCallbackHandler(session));
// 				byte[] data = null;
// 				if (request.getChildCData("/auth") != null) {
// 					data = Base64.decode(request.getChildCData("/auth"));
// 					log.finest("SASL auth: " + new String(data));
// 				} else {
// 					data = new byte[0];
// 				} // end of else
//         // evaluateResponse doesn't like null parameter
//         byte[] challenge = ss.evaluateResponse(data);
//         log.finest("challenge: "
// 					+ new String(challenge != null ? challenge : new byte[0]));
// 				challenge_data = (challenge != null && challenge.length > 0
// 					? Base64.encode(challenge) : null);
// 				reply(ss, packet, challenge_data, session, results);
//         session.putSessionData("SaslServer", ss);
// 				session.putSessionData("is-sasl", true);
//       } catch (SaslException e) {
//         log.log(Level.WARNING, "SaslException", e);
// 				failure = true;
//       } // end of try-catch
//       break;
//     case abort:
//       log.finer("Abort received, terminating...");
// 			failure = true;
//       break;
//     case response:
//       SaslServer ss = (SaslServer)session.getSessionData("SaslServer");
//       if (ss != null) {
//         try {
// 					String challenge_data = null;
// 					if (request.getChildCData("/response") != null) {
// 						byte[] data = Base64.decode(request.getChildCData("/response"));
// 						log.finest("SASL response: " + new String(data));
// 						// evaluateResponse doesn't like null parameter
// 						if (data == null) { data = new byte[0]; } // end of if (data == null)
// 						byte[] challenge = ss.evaluateResponse(data);
// 						log.finest("SASL challenge: "
// 							+ new String(challenge != null ? challenge : new byte[0]));
// 						challenge_data = (challenge != null && challenge.length > 0
// 							? Base64.encode(challenge) : null);
// 					}
// 					reply(ss, packet, challenge_data, session, results);
//         } catch (SaslException e) {
//           log.log(Level.FINEST, "SaslException", e);
// 					failure = true;
//         } // end of try-catch
//       } else {
//         log.severe("SaslServer == null, should be valid object instead.");
// 				failure = true;
//       } // end of else
//       break;
//     default:
//       // Ignore
//       break;
//     } // end of switch (type)
// 		if (failure) {
// 			results.offer(packet.swapFromTo(createReply(ElementType.failure,
// 						"<temporary-auth-failure/>")));
// 			results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(),
// 					StanzaType.set, packet.getElemId()));
// 		} // end of if (failure)
  }

// 	private void reply(SaslServer ss, Packet packet, String challenge_data,
// 		XMPPResourceConnection session, Queue<Packet> results) {
// 		if (ss.isComplete()) {
// 			results.offer(packet.swapFromTo(createReply(ElementType.success,
// 						challenge_data)));
// 			if (!session.isAuthorized()) {
// 				log.severe("!!!!!! Session not authorized after sasl success.");
// 			} // end of if (!session.isAuthorized())
// 			try { ss.dispose();	} catch (SaslException e) {	}
// 		} else {
// 			results.offer(packet.swapFromTo(createReply(ElementType.challenge,
// 						challenge_data)));
// 		} // end of if (ss.isComplete()) else
// 	}

	private Element createReply(final ElementType type, final String cdata) {
		Element reply = new Element(type.toString());
		reply.setXMLNS(XMLNS);
		if (cdata != null) {
			reply.setCData(cdata);
		} // end of if (cdata != null)
		return reply;
	}

// 	private class SaslCallbackHandler implements CallbackHandler {

// 		private XMPPResourceConnection session = null;

// 		private SaslCallbackHandler(XMPPResourceConnection session) {
// 			this.session = session;
// 		}

// 		public void handle(Callback[] callbacks)
// 			throws UnsupportedCallbackException {
// 			for (int i = 0; i < callbacks.length; i++) {
// 				log.finest("Callback: " + callbacks[i].getClass().getSimpleName());
// 				if (callbacks[i] instanceof ResourceConnectionCallback) {
// 					log.finest("ResourceConnectionCallback: "
// 						+ session.getConnectionId());
// 					ResourceConnectionCallback rcc =
// 						(ResourceConnectionCallback)callbacks[i];
// 					rcc.setResourceConnection(session);
// 				} else if (callbacks[i] instanceof RealmCallback) {
// 					log.finest("ResourceConnectionCallback: "
// 						+ session.getConnectionId());
// 					RealmCallback rc =
// 						(RealmCallback)callbacks[i];
// 					rc.setText(session.getDomain());
// 				} else {
// 					throw new UnsupportedCallbackException(callbacks[i],
// 						"Unrecognized Callback");
// 				}
// 			}
// 		}
// 	}

} // SaslAuth
