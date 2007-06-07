/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.NotAuthorizedException;
import tigase.db.NonAuthUserRepository;
import tigase.util.JIDUtils;

/**
 * JEP-0077: In-Band Registration
 *
 *
 * Created: Thu Feb 16 13:14:06 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqRegister extends XMPPProcessor
	implements XMPPProcessorIfc {

	private static final String ID = "jabber:iq:register";
	private static final String[] ELEMENTS = {"query"};
  private static final String[] XMLNSS = {"jabber:iq:register"};
  private static final Element[] FEATURES = {
		new Element("register", new String[] {"xmlns"},
			new String[] {"http://jabber.org/features/iq-register"})
	};
  private static final Element[] DISCO_FEATURES =
	{
		new Element("feature",
			new String[] {"var"},
			new String[] {"jabber:iq:register"})
	};

	public String id() { return ID; }

	public String[] supElements()
	{ return Arrays.copyOf(ELEMENTS, ELEMENTS.length); }

	public String[] supNamespaces()
	{ return Arrays.copyOf(XMLNSS, XMLNSS.length); }

  public Element[] supStreamFeatures(final XMPPResourceConnection session)
	{ return Arrays.copyOf(FEATURES, FEATURES.length); }

  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return Arrays.copyOf(DISCO_FEATURES, DISCO_FEATURES.length); }

	public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results) {

		if (session == null) {
			return;
		} // end of if (session == null)

		String id = session.getDomain();
		if (packet.getElemTo() != null) {
			id = JIDUtils.getNodeID(packet.getElemTo());
		}

		try {
			if (session.isAuthorized()
				&& packet.getFrom().equals(session.getConnectionId())) {
				packet.getElement().setAttribute("from", session.getJID());
			}

			if ((id.equals(session.getDomain())
					|| id.equals(session.getUserId()))
				&& packet.getFrom().equals(session.getConnectionId())) {
				Authorization result = Authorization.NOT_AUTHORIZED;
				Element request = packet.getElement();
				StanzaType type = packet.getType();
				switch (type) {
				case set:
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
								result = session.unregister(packet.getElemFrom());
							} catch (NotAuthorizedException e) {
								results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
										"You must authorize session first.", true));
							} // end of try-catch
						}
					} else {
						// No, so assuming this is registration of new
						// user or change registration details for existing user
						String user_name = request.getChildCData("/iq/query/username");
						String password = request.getChildCData("/iq/query/password");
						String email = request.getChildCData("/iq/query/email");
						result = session.register(user_name, password, email);
					}
					if (result == Authorization.AUTHORIZED) {
						results.offer(result.getResponseMessage(packet, null, false));
					} else {
						results.offer(result.getResponseMessage(packet,
								"Unsuccessful registration attempt", true));
					}
					break;
				case get:
					results.offer(packet.okResult(
							"<instructions>" +
							"Choose a user name and password for use with this service." +
							"Please provide also your e-mail address." +
							"</instructions>" +
							"<username/>" +
							"<password/>" +
							"<email/>", 1));
					break;
				case result:
					// It might be a registration request from transport for example...
					Element elem_res = packet.getElement().clone();
					Packet pack_res = new Packet(elem_res);
					pack_res.setTo(session.getConnectionId());
					results.offer(pack_res);
					break;
				default:
					results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
							"Message type is incorrect", true));
					break;
				} // end of switch (type)
			} else {
				Element result = packet.getElement().clone();
				results.offer(new Packet(result));
			}
		} catch (NotAuthorizedException e) {
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You are not authorized to change registration settings.", true));
		} // end of try-catch
	}

} // JabberIqRegister
