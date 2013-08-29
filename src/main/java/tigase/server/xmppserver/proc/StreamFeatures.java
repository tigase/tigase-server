/*
 * StreamFeatures.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

import tigase.net.ConnectionType;

import tigase.server.Packet;
import tigase.server.xmppserver.S2SIOService;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

/**
 * Created: Dec 9, 2010 2:00:25 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StreamFeatures
				extends S2SAbstractProcessor {
	private static final Logger log = Logger.getLogger(StreamFeatures.class.getName());

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param p
	 * @param serv
	 * @param results
	 *
	 *
	 *
	 * @return a value of boolean
	 */
	@Override
	public boolean process(Packet p, S2SIOService serv, Queue<Packet> results) {
		if (p.isElement(FEATURES_EL, FEATURES_NS)) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "{0}, Stream features received: {1}", new Object[] { serv,
						p });
			}

			return true;
		}

		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 * @param attribs
	 *
	 *
	 *
	 * @return a value of String
	 */
	@Override
	public String streamOpened(S2SIOService serv, Map<String, String> attribs) {
		if (attribs.containsKey("version")) {

			// A version=1.0,  sending features
			if (serv.connectionType() == ConnectionType.accept) {

				// Send features only for accept connections
				List<Element> features        = handler.getStreamFeatures(serv);
				Element       featuresElement = new Element(STREAM_FEATURES_EL);

				featuresElement.addChildren(features);

				// do not send stattls feature to hosts in skip tls list
				if (attribs.containsKey("from")) {
					if (skipTLSForHost(attribs.get("from"))) {
						Element startTls = featuresElement.getChild(START_TLS_EL, START_TLS_NS);

						featuresElement.removeChild(startTls);
					}
				}
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "{0}, Sending stream features: {1}", new Object[] { serv,
							featuresElement });
				}
				serv.addPacketToSend(Packet.packetInstance(featuresElement, null, null));
			}
		}

		return null;
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
