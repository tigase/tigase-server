/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.ext.handlers;

//~--- non-JDK imports --------------------------------------------------------

import tigase.net.SocketType;
import tigase.server.Packet;
import tigase.server.ext.ComponentIOService;
import tigase.server.ext.ComponentProtocolHandler;
import tigase.server.ext.ExtProcessor;
import tigase.server.ext.StreamOpenHandler;
import tigase.xml.Element;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

//~--- JDK imports ------------------------------------------------------------

//~--- classes ----------------------------------------------------------------

/**
 * Created: Oct 31, 2009 4:54:39 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StartTLSProcessor implements ExtProcessor {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(StartTLSProcessor.class.getName());
	private static final String EL_NAME = "starttls";
	private static final String ID = EL_NAME;
	private static final Element FEATURES = new Element(EL_NAME, new String[] { "xmlns" },
		new String[] { "urn:ietf:params:xml:ns:xmpp-tls" });
	private static final Element FEATURES_REQUIRED = new Element(EL_NAME,
		new Element[] { new Element("required") }, new String[] { "xmlns" },
		new String[] { "urn:ietf:params:xml:ns:xmpp-tls" });

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public List<Element> getStreamFeatures(ComponentIOService serv,
			ComponentProtocolHandler handler) {
		if (serv.getSessionData().get(ID) != null) {
			return null;
		} else {
			switch ((SocketType) serv.getSessionData().get("socket")) {
				case ssl:
					return null;
				case plain:
					return Collections.singletonList(FEATURES);
				case tls:
					return Collections.singletonList(FEATURES_REQUIRED);
			}
			return null;
		}
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public boolean process(Packet p, ComponentIOService serv, ComponentProtocolHandler handler,
			Queue<Packet> results) {
		if (p.getElemName() == EL_NAME) {
			serv.getSessionData().put(ID, ID);

			String data = "<proceed xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>";

			initTLS(serv, data, false);
			log.fine("Started server side TLS.");

			return true;
		}

		if (p.getElemName() == "proceed") {
			serv.getSessionData().put(ID, ID);
			initTLS(serv, null, true);
			log.fine("Started client side TLS.");

			StreamOpenHandler soh = handler.getStreamOpenHandler("jabber:client");
			String data = soh.serviceStarted(serv);

			serv.xmppStreamOpen(data);
			log.fine("New stream opened: " + data);

			return true;
		}

		return false;
	}

	@Override
	public void startProcessing(Packet p, ComponentIOService serv,
			ComponentProtocolHandler handler, Queue<Packet> results) {
		results.offer(Packet.packetInstance(new Element(EL_NAME, new String[] { "xmlns" },
				new String[] { "urn:ietf:params:xml:ns:xmpp-tls" }), null, null));
	}

	private void initTLS(ComponentIOService serv, String data, boolean client) {
		try {
			serv.writeRawData(data);
			Thread.sleep(10);

			while (serv.waitingToSend()) {
				serv.writeRawData(null);
				Thread.sleep(10);
			}

			serv.startTLS(client, false, false);
		} catch (Exception e) {
			log.warning("TLS mode start failed: " + e.getMessage());
			serv.forceStop();
		}
	}
}
