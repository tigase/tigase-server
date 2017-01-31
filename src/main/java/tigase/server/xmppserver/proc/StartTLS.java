/*
 * StartTLS.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */



package tigase.server.xmppserver.proc;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;
import tigase.server.xmppserver.CID;
import tigase.server.xmppserver.S2SIOService;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Queue;

/**
 * Created: Dec 9, 2010 2:01:01 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StartTLS
				extends S2SAbstractProcessor {
	private static final Logger log       = Logger.getLogger(StartTLS.class.getName());
	private static final Element features = new Element(START_TLS_EL,
																						new String[] { "xmlns" },
																						new String[] { START_TLS_NS });
	private static final Element features_required = new Element(START_TLS_EL, new Element[] { new Element( "required" ) },
																						new String[] { "xmlns" },
																						new String[] { START_TLS_NS });
	private static final Element starttls_el = new Element(START_TLS_EL,
																							 new String[] { "xmlns" },
																							 new String[] { START_TLS_NS });
	private static final Element proceed_el = new Element(PROCEED_TLS_EL,
																							new String[] { "xmlns" },
																							new String[] { START_TLS_NS });

	//~--- methods --------------------------------------------------------------

	@Override
	public int order() {
		return Order.StartTLS.ordinal();
	}	
	
	@Override
	public boolean process(Packet p, S2SIOService serv, Queue<Packet> results) {
		if (p.isElement(START_TLS_EL, START_TLS_NS)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Sending packet: {1}", new Object[] { serv,
								proceed_el });
			}
			handler.writeRawData(serv, proceed_el.toString());
			try {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Starting TLS handshaking server side.", serv);
				}
				serv.getSessionData().put("TLS", "TLS");
				serv.startTLS(false, handler.isTlsWantClientAuthEnabled(), handler.isTlsNeedClientAuthEnabled());
			} catch (IOException ex) {
				log.log(Level.INFO, "Problem with TLS initialization.", ex);
			}

			return true;
		}
		if (p.isElement(PROCEED_TLS_EL, START_TLS_NS)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Received TLS proceed.", serv);
			}
			try {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Starting TLS handshaking client side.", serv);
				}
				serv.getSessionData().put("TLS", "TLS");
				serv.startTLS(true, handler.isTlsWantClientAuthEnabled(), handler.isTlsNeedClientAuthEnabled());
			} catch (IOException ex) {
				log.log(Level.INFO, "Problem with TLS initialization.", ex);
			}

			return true;
		}
		if (p.isElement(FEATURES_EL, FEATURES_NS)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Stream features received: {1}", new Object[] { serv,
								p });
			}

			CID cid         = (CID) serv.getSessionData().get("cid");
			boolean skipTLS = (cid == null)
												? false
												: skipTLSForHost(cid.getRemoteHost());

			if (p.isXMLNSStaticStr(FEATURES_STARTTLS_PATH, START_TLS_NS) &&!skipTLS) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Sending packet: {1}", new Object[] { serv,
									starttls_el });
				}
				handler.writeRawData(serv, starttls_el.toString());

				return true;
			}
		}

		return false;
	}

	@Override
	public void streamFeatures(S2SIOService serv, List<Element> results) {
		if (!serv.getSessionData().containsKey("TLS")) {
			CID cid = (CID) serv.getSessionData().get("cid");
			if (cid != null && !skipTLSForHost(cid.getRemoteHost()) 
					&& handler.isTlsRequired(cid.getLocalHost())) {
				results.add(features_required);
			}
			else {
				results.add(features);
			}
		}
	}
}
