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

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.Priority;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * JEP-0077: In-Band Registration
 *
 *
 * Created: Thu Feb 16 13:14:06 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqRegister extends XMPPProcessor implements XMPPProcessorIfc {

	/**
	 * Private logger for class instancess.
	 */
	private static Logger log = Logger.getLogger(JabberIqRegister.class.getName());
	private static final String ID = "jabber:iq:register";
	private static final String[] ELEMENTS = { "query" };
	private static final String[] XMLNSS = { "jabber:iq:register" };
	private static final Element[] FEATURES = {
		new Element("register", new String[] { "xmlns" },
			new String[] { "http://jabber.org/features/iq-register" }) };
	private static final Element[] DISCO_FEATURES = {
		new Element("feature", new String[] { "var" }, new String[] { "jabber:iq:register" }) };

	//~--- methods --------------------------------------------------------------

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
	 *
	 * @throws XMPPException
	 * TODO: Implement registration form configurable and loading all the fields from
	 * the registration form
	 * TODO: rewrite the plugin using the XMPPProcessorAbstract API
	 */
	@Override
	public void process(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
			throws XMPPException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Processing packet: " + packet.toString());
		}

		if (session == null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Session is null, ignoring");
			}

			return;
		}    // end of if (session == null)

		BareJID id = session.getDomainAsJID().getBareJID();

		if (packet.getStanzaTo() != null) {
			id = packet.getStanzaTo().getBareJID();
		}

		try {

			// I think it does not make sense to check the 'to', just the connection ID
//    if ((id.equals(session.getDomain()) || id.equals(session.getUserId().toString()))
//        && packet.getFrom().equals(session.getConnectionId())) {
			// Wrong thinking. The user may send an request from his own account
			// to register with a transport or any other sevice, then the connection ID
			// matches the session id but this is still not a request to the local
			// server. The TO address must be checked too.....
			// if (packet.getPacketFrom().equals(session.getConnectionId())) {
			if ((packet.getPacketFrom() != null)
					&& packet.getPacketFrom().equals(session.getConnectionId())
						&& ( !session.isAuthorized()
							|| (session.isUserId(id) || session.isLocalDomain(id.toString(), false)))) {
				Authorization result = Authorization.NOT_AUTHORIZED;
				Element request = packet.getElement();
				StanzaType type = packet.getType();

				switch (type) {
					case set :

						// Is it registration cancel request?
						Element elem = request.findChild("/iq/query/remove");

						if (elem != null) {

							// Yes this is registration cancel request
							// According to JEP-0077 there must not be any
							// more subelemets apart from <remove/>
							elem = request.findChild("/iq/query");

							if (elem.getChildren().size() > 1) {
								result = Authorization.BAD_REQUEST;
							} else {
								try {
									result = session.unregister(packet.getStanzaFrom().toString());

									Packet ok_result = packet.okResult((String) null, 0);

									// We have to set SYSTEM priority for the packet here,
									// otherwise the network connection is closed before the
									// client received a response
									ok_result.setPriority(Priority.SYSTEM);
									results.offer(ok_result);
									results.offer(Command.CLOSE.getPacket(session.getSMComponentId(),
											session.getConnectionId(), StanzaType.set, session.nextStanzaId()));
								} catch (NotAuthorizedException e) {
									results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
											"You must authorize session first.", true));
								}    // end of try-catch
							}
						} else {

							// No, so assuming this is registration of a new
							// user or change registration details for existing user
							String user_name = request.getChildCData("/iq/query/username");
							String password = request.getChildCData("/iq/query/password");
							String email = request.getChildCData("/iq/query/email");
							Map<String, String> reg_params = null;

							if ((email != null) &&!email.trim().isEmpty()) {
								reg_params = new LinkedHashMap<String, String>();
								reg_params.put("email", email);
							}

							result = session.register(user_name, password, reg_params);

							if (result == Authorization.AUTHORIZED) {
								results.offer(result.getResponseMessage(packet, null, false));
							} else {
								results.offer(result.getResponseMessage(packet,
										"Unsuccessful registration attempt", true));
							}
						}

						break;

					case get :
						results.offer(packet.okResult("<instructions>"
								+ "Choose a user name and password for use with this service."
									+ "Please provide also your e-mail address." + "</instructions>"
										+ "<username/>" + "<password/>" + "<email/>", 1));

						break;

					case result :

						// It might be a registration request from transport for example...
						Packet pack_res = packet.copyElementOnly();

						pack_res.setPacketTo(session.getConnectionId());
						results.offer(pack_res);

						break;

					default :
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
								"Message type is incorrect", true));

						break;
				}    // end of switch (type)
			} else {
				if (session.isUserId(id)) {

					// It might be a registration request from transport for example...
					Packet pack_res = packet.copyElementOnly();

					pack_res.setPacketTo(session.getConnectionId());
					results.offer(pack_res);
				} else {
					results.offer(packet.copyElementOnly());
				}
			}
		} catch (TigaseStringprepException ex) {
			results.offer(Authorization.JID_MALFORMED.getResponseMessage(packet,
					"Incorrect user name, stringprep processing failed.", true));
		} catch (NotAuthorizedException e) {
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You are not authorized to change registration settings.\n" + e.getMessage(), true));
		} catch (TigaseDBException e) {
			log.warning("Database proble, please contact admin: " + e);
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
					"Database access problem, please contact administrator.", true));
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
	public Element[] supDiscoFeatures(XMPPResourceConnection session) {
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
	public Element[] supStreamFeatures(XMPPResourceConnection session) {
		return FEATURES;
	}
}    // JabberIqRegister


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
