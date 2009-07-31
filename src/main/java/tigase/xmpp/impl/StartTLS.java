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

import java.util.Queue;
import java.util.Map;
import java.util.logging.Logger;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.db.NonAuthUserRepository;

/**
 * Describe class StartTLS here.
 *
 *
 * Created: Fri Mar 24 07:22:57 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StartTLS extends XMPPProcessor
	implements XMPPProcessorIfc {

  private static Logger log = Logger.getLogger(StartTLS.class.getName());

  private static final String XMLNS = "urn:ietf:params:xml:ns:xmpp-tls";
	//private static final String TLS_STARTED_KEY = "TLS-Started";
	public static final String TLS_REQUIRED_KEY = "tls-required";

	private static final String ID = "starttls";
  private static final String[] ELEMENTS = {"starttls", "proceed", "failure"};
  private static final String[] XMLNSS = {XMLNS, XMLNS, XMLNS};
  private static final Element[] F_REQUIRED = {
		new Element("starttls",
			new Element[] { new Element("required") },
			new String[] {"xmlns"}, new String[] {XMLNS})};
  private static final Element[] F_NOT_REQUIRED = {
		new Element("starttls", new String[] {"xmlns"}, new String[] {XMLNS})};

	private Element proceed = new Element("proceed",
					new String[] {"xmlns"}, new String[] {XMLNS});
	private Element failure = new Element("failure",
					new String[] {"xmlns"}, new String[] {XMLNS});

	@Override
	public String id() { return ID; }

	@Override
	public String[] supElements()	{ return ELEMENTS; }

	@Override
  public String[] supNamespaces()	{ return XMLNSS; }

	@Override
  public Element[] supStreamFeatures(final XMPPResourceConnection session)	{
    // If session does not exist, just return null, we don't provide features
		// for non-existen stream
		if (session != null && session.getSessionData(ID) == null) {
      if (session.getSessionData(TLS_REQUIRED_KEY) != null
				&& session.getSessionData(TLS_REQUIRED_KEY).equals("true")) {
        return F_REQUIRED;
      } else {
        return F_NOT_REQUIRED;
      }
    } // end of if (session.isAuthorized())
    else {
      return null;
    } // end of if (session.isAuthorized()) else
	}

	@Override
  public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings) {

		if (session == null) {
			return;
		} // end of if (session == null)

		if (packet.isElement("starttls", XMLNS)) {
			session.putSessionData(ID, "true");
			Packet result = Command.STARTTLS.getPacket(packet.getTo(),
				packet.getFrom(), StanzaType.set, session.nextStanzaId(),
				Command.DataType.submit);
			Command.setData(result, proceed);
			results.offer(result);
		} // end of if (packet.getElement().getName().equals("starttls"))
		else {
      log.warning("Unknown TLS element: " + packet.getStringData());
			results.offer(packet.swapFromTo(failure));
			results.offer(Command.CLOSE.getPacket(packet.getTo(),
					packet.getFrom(), StanzaType.set, session.nextStanzaId()));
		} // end of if (packet.getElement().getName().equals("starttls")) else
	}

} // StartTLS
