/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

package tigase.server.ext.handlers;

import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.server.ext.CompRepoItem;
import tigase.server.ext.ComponentConnection;
import tigase.server.ext.ComponentProtocolHandler;
import tigase.server.ext.ExtProcessor;
import tigase.util.Algorithms;
import tigase.xml.Element;
import tigase.xmpp.XMPPIOService;

/**
 * Created: Oct 21, 2009 1:58:56 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class HandshakeProcessor implements ExtProcessor {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger(HandshakeProcessor.class.getName());

	@Override
	public boolean process(Packet p, XMPPIOService<ComponentConnection> serv,
			ComponentProtocolHandler handler, Queue<Packet> results) {
		boolean result = false;
		if (p.getElemName() == "handshake") {
			result = true;
			switch (serv.connectionType()) {
				case connect: {
					String data = p.getElemCData();
					if (data == null) {
						// According to XEP-0114 the authentication was successful
						handler.authenticated(serv);
					} else {
						log.warning("Incorrect packet received: " + p.getStringData());
					}
					break;
				}
				case accept: {
					String digest = p.getElemCData();
					CompRepoItem comp = (CompRepoItem)serv.getSessionData().get(
							ComponentProtocolHandler.REPO_ITEM_KEY);
					String id =
							(String)serv.getSessionData().get(XMPPIOService.SESSION_ID_KEY);
					String secret = comp.getAuthPasswd();
					try {
						String loc_digest = Algorithms.hexDigest(id, secret, "SHA");
						if (log.isLoggable(Level.FINEST)) {
							log.finest("Calculating digest: id=" + id + ", secret=" + secret
 +
									", digest=" + loc_digest);
						}
						// Password digest matches, authentication OK
						if (digest != null && digest.equals(loc_digest)) {
							handler.authenticated(serv);
							Packet resp = new Packet(new Element("handshake"));
							results.offer(resp);
						} else {
							log.info("Handshaking passwords don't match, disconnecting...");
							serv.stop();
						}
					} catch (Exception e) {
						log.log(Level.SEVERE, "Handshaking error.", e);
					}
					break;
				}
				default:
					// Do nothing, more data should come soon...
					break;
			} // end of switch (service.connectionType())
		}
		return result;
	}

}
