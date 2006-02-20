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
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import tigase.auth.TigaseSasl;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Describe class SaslAuth here.
 *
 *
 * Created: Mon Feb 20 16:28:13 2006
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SaslAuth extends XMPPProcessor {

  private final static String XMLNS = "urn:ietf:params:xml:ns:xmpp-sasl";

  private static Logger log = Logger.getLogger("tigase.xmpp.impl.AuthXmppSasl");

	protected static final String ID = XMLNS;
  protected static final String[] ELEMENTS = {
    "auth", "response", "challenge", "failure", "success", "abort"};
  protected static final String[] XMLNSS = {
    XMLNS, XMLNS, XMLNS, XMLNS, XMLNS, XMLNS};
  protected static final String[] FEATURES = {
    "<mechanisms xmlns='" + XMLNS + "'>"
    + "<mechanism>CRAM-MD5</mechanism>"
    + "<mechanism>DIGEST-MD5</mechanism>"
    + "<mechanism>PLAIN</mechanism>"
    + "</mechanisms>"
  };

  public enum ElementType {
    auth,
    abort,
    response,
    challenge,
		failure;
  }

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

  public String[] supStreamFeatures(final XMPPResourceConnection session)	{
    if (session.isAuthorized()) {
      return null;
    } else {
      return FEATURES;
    } // end of if (session.isAuthorized()) else
	}

  public void processRequest(final Packet packet,
		final XMPPResourceConnection session,	final Queue<Packet> results) {
    ElementType type = ElementType.valueOf(request.getRootName());
		Element request = packet.getElement();
    switch (type) {
    case AUTH:
      String mechanism = request.getAttribute("/auth", "mechanism");
      try {
        Map<String, String> props = new TreeMap<String, String>();
        props.put(Sasl.QOP, "auth");
        SaslServer ss = TigaseSasl.createSaslServer(mechanism, "xmpp",
          user_data.getDomain(), props, session);
        // evaluateResponse doesn't like null parameter
        byte[] challenge = ss.evaluateResponse(new byte[0]);
        log.finest("challenge: " + new String(challenge));
        String reply = "<challenge xmlns='" + XMLNS + "'>" +
          Base64.encode(challenge) + "</challenge>";
        addReply(reply);
        user_data.setSessionData("SaslServer", ss);
      } // end of try
      catch (SaslException e) {
        log.log(Level.WARNING, "SaslException", e);
        addReply("<failure xmlns='" + XMLNS + "'>"
          + "<temporary-auth-failure/></failure></stream:stream>");
      } // end of try-catch
      break;
    case ABORT:
      log.fine("Abort received, terminating...");
      addReply("<failure xmlns='" + XMLNS + "'>"
        + "<temporary-auth-failure/></failure></stream:stream>");
      addReply(Reply.Type.STOP);
      break;
    case RESPONSE:
      SaslServer ss = (SaslServer)user_data.getSessionData("SaslServer");
      if (ss != null) {
        try {
          byte[] data = Base64.decode(request.getChildCData("/response"));
          // evaluateResponse doesn't like null parameter
          if (data == null) { data = new byte[0]; } // end of if (data == null)
          byte[] challenge = ss.evaluateResponse(data);
          String reply = null;
          if (ss.isComplete()) {
            reply = "<success xmlns='" + XMLNS + "'/>";
          } // end of if (ss.isComplete())
          else {
            reply = "<challenge xmlns='" + XMLNS + "'>" +
              Base64.encode(challenge) + "</challenge>";
          } // end of if (ss.isComplete()) else
          addReply(reply);
        } catch (SaslException e) {
          log.log(Level.FINEST, "SaslException", e);
          addReply("<failure xmlns='" + XMLNS + "'>"
            + "<temporary-auth-failure/></failure></stream:stream>");
          addReply(Reply.Type.STOP);
        } // end of try-catch
      } // end of if (ss != null)
      else {
        log.severe("SaslServer == null, should be valid object instead.");
        addReply(Reply.Type.STOP);
      } // end of else
      break;
    default:
      // Ignore
      break;
    } // end of switch (type)
  }

} // SaslAuth
