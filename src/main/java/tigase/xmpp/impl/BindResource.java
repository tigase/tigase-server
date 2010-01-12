/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.xmpp.impl;

import java.util.Queue;
import java.util.Map;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPException;
import tigase.db.NonAuthUserRepository;

/**
 * RFC-3920, 7. Resource Binding
 *
 *
 * Created: Mon Feb 20 21:07:29 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BindResource extends XMPPProcessor
	implements XMPPProcessorIfc {

  protected static final String RESOURCE_KEY = "Resource-Binded";
  private static final String XMLNS = "urn:ietf:params:xml:ns:xmpp-bind";
  private static final Logger log =
		Logger.getLogger("tigase.xmpp.impl.BindResource");

	private static final String ID = XMLNS;
  private static final String[] ELEMENTS = {"bind"};
  private static final String[] XMLNSS = {XMLNS};
  private static final Element[] FEATURES = {
		new Element("bind",	new String[] {"xmlns"}, new String[] {XMLNS})
	};
  private static final Element[] DISCO_FEATURES = {
		new Element("feature", new String[] {"var"}, new String[] {XMLNS})
	};

  private static int resGenerator = 0;

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
		if (session != null &&
						session.getSessionData(RESOURCE_KEY) == null &&
						session.isAuthorized()) {
      return FEATURES;
    } else {
      return null;
    } // end of if (session.isAuthorized()) else
	}

	@Override
  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

	@Override
  public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings)
		throws XMPPException {

		if (session == null) {
			return;
		} // end of if (session == null)

		if (!session.isAuthorized()) {
      results.offer(session.getAuthState().getResponseMessage(packet,
          "Session is not yet authorized.", false));
			return;
		} // end of if (!session.isAuthorized())

		Element request = packet.getElement();
    StanzaType type = packet.getType();
    try {
			switch (type) {
			case set:
        String resource = request.getChildCData("/iq/bind/resource");
				try {
					if (resource == null || resource.trim().isEmpty()) {
						resource = "tigase-" + (++resGenerator);
						session.setResource(resource);
					} else {
						try {
							session.setResource(resource);
						} catch (TigaseStringprepException ex) {
							// User provided resource is invalid, generating different server one
							log.info("Incrrect resource provided by the user: " + resource
									+ ", generating a different one by the server.");
							resource = "tigase-" + (++resGenerator);
							session.setResource(resource);
						}
					} // end of if (resource == null) else
				} catch (TigaseStringprepException ex) {
					log.warning("stringprep problem with the server generated resource: "
							+ resource);
				}
				packet.getElement().setAttribute("from", session.getJID().toString());
        session.putSessionData(RESOURCE_KEY, "true");
				results.offer(packet.okResult("<jid>" + session.getJID() + "</jid>", 1));
				break;
			default:
        results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
            "Bind type is incorrect", false));
				break;
			} // end of switch (type)
    } catch (NotAuthorizedException e) {
      results.offer(session.getAuthState().getResponseMessage(packet,
          "Session is not yet authorized.", false));
    } // end of try-catch
	}

} // BindResource
