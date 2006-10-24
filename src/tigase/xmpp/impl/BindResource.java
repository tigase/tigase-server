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

import java.util.Queue;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
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

  private static final String RESOURCE_KEY = "Resource-Binded";
  private static final String XMLNS = "urn:ietf:params:xml:ns:xmpp-bind";
  private static final Logger log =
		Logger.getLogger("tigase.xmpp.impl.BindResource");

	protected static final String ID = XMLNS;
  protected static final String[] ELEMENTS = {"bind"};
  protected static final String[] XMLNSS = {XMLNS};
  protected static final Element[] FEATURES = {
		new Element("bind",	new String[] {"xmlns"}, new String[] {XMLNS})
	};

  private static int resGenerator = 0;

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

  public Element[] supStreamFeatures(final XMPPResourceConnection session)	{
    if (session.getSessionData(RESOURCE_KEY) != null) {
      return null;
    } else {
      return FEATURES;
    } // end of if (session.isAuthorized()) else
	}

  public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results) {

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
        final String resource = request.getChildCData("/iq/bind/resource");
        if (resource == null || resource.equals("")) {
          session.setResource("tigase-"+(++resGenerator));
        } // end of if (resource == null)
        else {
          session.setResource(resource);
        } // end of if (resource == null) else
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
