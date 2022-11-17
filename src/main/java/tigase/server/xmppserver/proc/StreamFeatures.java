/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.server.xmppserver.proc;

import tigase.kernel.beans.Bean;
import tigase.net.ConnectionType;
import tigase.server.Packet;
import tigase.server.xmppserver.*;
import tigase.xml.Element;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Dec 9, 2010 2:00:25 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
@Bean(name = "streamFeatures", parent = S2SConnectionManager.class, active = true)
public class StreamFeatures
		extends S2SAbstractProcessor
		implements S2SFilterIfc {

	private static final Logger log = Logger.getLogger(StreamFeatures.class.getName());

	private static boolean hasEmptyOrOnlyObligatoryFeatures(Packet p) {
		return hasEmptyOrOnlyObligatoryFeatures(p.getElement());
	}

	@Override
	public boolean shouldSkipUndelivered(Packet packet) {
		return packet.getElemName() == STREAM_FEATURES_EL;
	}

	private static boolean hasEmptyOrOnlyObligatoryFeatures(Element featuresElement) {
		return featuresElement.getChildren() == null || featuresElement.getChildren().isEmpty() ||
				featuresElement.findChildren(item -> item.getChild("required") != null).isEmpty();
	}

	@Override
	public boolean filter(Packet p, S2SIOService serv, Queue<Packet> results) {
		// https://xmpp.org/rfcs/rfc6120.html#streams-negotiation-complete
		// The receiving entity indicates completion of the stream negotiation process by sending to the initiating entity
		// either an empty `<features/>` element or a `<features/>` element that contains only voluntary-to-negotiate features.
		//
		// if there is a stream features packet and we are authenticated and either it doesn't have any features or only have
		// non-mandatory features we should mark the service as "completely negotiated" as no other Processor handled the packet
		//
		// This should be done in filter to make sure there aren't any other processors that would want to negotiate more features.

		final boolean hasEmptyOrNonObligatoryFeatures = hasEmptyOrOnlyObligatoryFeatures(p);
		log.log(Level.FINEST,
				"Processing stream features and verifying if steam negotiation is complete; authenticated: {0}, completed: {1}, hasEmptyOrNonObligatoryFeatures: {2}, packet: {3} [{4}]",
				new Object[]{serv.isAuthenticated(), serv.isStreamNegotiationCompleted(),
							 hasEmptyOrNonObligatoryFeatures, p, serv});

		if (p.isElement(FEATURES_EL, FEATURES_NS) && serv.isAuthenticated() && hasEmptyOrNonObligatoryFeatures) {
			stremNegotiationComplete(serv);
			return true;
		}

		return false;
	}

	@Override
	public int order() {
		return Order.StreamFeatures.ordinal();
	}

	@Override
	public boolean process(Packet p, S2SIOService serv, Queue<Packet> results) {
//		if (p.isElement(FEATURES_EL, FEATURES_NS) && p.getElement().getChildren() != null && !p.getElement().getChildren().isEmpty()) {
//			if (log.isLoggable(Level.FINEST)) {
//				log.log(Level.FINEST, "Stream features received: {1} [{0}]", new Object[]{serv, p});
//			}
//
//			return true;
//		}

		return false;
	}

	@Override
	public String streamOpened(S2SIOService serv, Map<String, String> attribs) {
		if (attribs.containsKey("version")) {

			// A version=1.0,  sending features
			if (serv.connectionType() == ConnectionType.accept) {

				// Send features only for accept connections
				List<Element> features = handler.getStreamFeatures(serv);
				Element featuresElement = new Element(STREAM_FEATURES_EL);

				featuresElement.addChildren(features);

				// do not send stattls feature to hosts in skip tls list
				if (attribs.containsKey("from")) {
					if (skipTLSForHost(attribs.get("from"))) {
						Element startTls = featuresElement.getChild(START_TLS_EL, START_TLS_NS);
						featuresElement.removeChild(startTls);
					}
				}

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Sending stream features: {1} [{0}]", new Object[]{serv, featuresElement});
				}

				serv.addPacketToSend(Packet.packetInstance(featuresElement, null, null));

				final boolean hasEmptyOrNonObligatoryFeatures = hasEmptyOrOnlyObligatoryFeatures(featuresElement);
				log.log(Level.FINEST,
						"Sending stream features and verifying if steam negotiation is complete; authenticated: {0}, completed: {1}, hasEmptyOrNonObligatoryFeatures: {2}, features: {3} [{4}]",
						new Object[]{serv.isAuthenticated(), serv.isStreamNegotiationCompleted(),
									 hasEmptyOrNonObligatoryFeatures, featuresElement, serv});

				if (serv.isAuthenticated() && hasEmptyOrNonObligatoryFeatures) {
					stremNegotiationComplete(serv);
				}
			}
		}

		return null;
	}

	private void stremNegotiationComplete(S2SIOService serv) {
		if (!serv.isStreamNegotiationCompleted()) {
			CID cid = (CID) serv.getSessionData().get(S2SConnectionManager.CID_KEY);
			if (cid != null) {
				CIDConnections cid_conns = null;
				try {
					cid_conns = handler.getCIDConnections(cid, false);
					if (cid_conns != null) {
						cid_conns.streamNegotiationCompleted(serv);
					}
				} catch (NotLocalhostException | LocalhostException e) {
					//can be ignored
				}
			}
			serv.streamNegotiationCompleted();
		}
	}
}

