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
import java.util.logging.Logger;
import tigase.db.NonAuthUserRepository;
import tigase.db.UserAuthRepository;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
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

	private static final String ID = XMLNS;
  private static final String[] ELEMENTS = {
    "auth", "response", "challenge", "failure", "success", "abort"};
  private static final String[] XMLNSS = {
    XMLNS, XMLNS, XMLNS, XMLNS, XMLNS, XMLNS};
  private static final Element[] DISCO_FEATURES =	{
		new Element("feature", new String[] {"var"}, new String[] {XMLNS})
	};

  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }


  public enum ElementType {
    auth,
    abort,
    response,
    challenge,
		failure,
		success;
  }

	public String id() { return ID; }

	public String[] supElements()
	{ return ELEMENTS; }

  public String[] supNamespaces()
	{ return XMLNSS; }

  public Element[] supStreamFeatures(final XMPPResourceConnection session)	{
    if (session == null || session.isAuthorized()) {
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

  @SuppressWarnings({"unchecked"})
  public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings) {

		if (session == null) {
			return;
		} // end of if (session == null)

		Element request = packet.getElement();
// 		ElementType type = null;
// 		try {
// 			type = ElementType.valueOf(request.getName());
// 		} catch (IllegalArgumentException e) {
// 			log.warning("Incorrect stanza type: " + request.getName());
// 			results.offer(packet.swapFromTo(createReply(ElementType.failure,
// 						"<temporary-auth-failure/>")));
// 			results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(),
// 					StanzaType.set, packet.getElemId()));
// 			return;
// 		} // end of try-catch
		Map<String, Object> authProps =
			(Map<String, Object>)(session.getSessionData(XMLNS+"-authProps"));
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
		//		String user = (String)authProps.get(UserAuthRepository.USER_ID_KEY);
		authProps.put(UserAuthRepository.DATA_KEY, request.getCData());
		try {
			Authorization result = session.loginOther(authProps);
			String challenge_data =
				(String)authProps.get(UserAuthRepository.RESULT_KEY);
			if (result == Authorization.AUTHORIZED) {
				results.offer(packet.swapFromTo(createReply(ElementType.success,
							challenge_data)));
				authProps.clear();
				session.removeSessionData(XMLNS+"-authProps");
			} else {
				results.offer(packet.swapFromTo(createReply(ElementType.challenge,
							challenge_data)));
			}
		} catch (Exception e) {
			//e.printStackTrace();
			session.removeSessionData(XMLNS+"-authProps");
			results.offer(packet.swapFromTo(createReply(ElementType.failure,
						"<not-authorized/>")));
			Integer retries = (Integer)session.getSessionData("auth-retries");
			if (retries == null) {
				retries = new Integer(0);
			}
			if (retries.intValue() < 3) {
				session.putSessionData("auth-retries", new Integer(retries.intValue() + 1));
			} else {
				results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(),
						StanzaType.set, packet.getElemId()));
			}
		} // end of try-catch
  }

	private Element createReply(final ElementType type, final String cdata) {
		Element reply = new Element(type.toString());
		reply.setXMLNS(XMLNS);
		if (cdata != null) {
			reply.setCData(cdata);
		} // end of if (cdata != null)
		return reply;
	}

} // SaslAuth
