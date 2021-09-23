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
package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.jid.JID;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * XEP-0199: XMPP Ping
 *
 * @author <a href="mailto:bmalkow@tigase.org">Bartosz Małkowski</a>
*/
@Bean(name = UrnXmppPing.ID, parent = SessionManager.class, active = true)
public class UrnXmppPing
		extends XMPPProcessorAbstract {

	private static final Logger log = Logger.getLogger(UrnXmppPing.class.getName());
	private static final String[][] ELEMENTS = {{Iq.ELEM_NAME, "ping"}};
	private static final String XMLNS = "urn:xmpp:ping";
	protected static final String ID = XMLNS;
	private static final Element[] DISCO_FEATURES = {new Element("feature", new String[]{"var"}, new String[]{XMLNS})};
	private static final String[] XMLNSS = {XMLNS};

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
											  NonAuthUserRepository repo, Queue<Packet> results,
											  Map<String, Object> settings) {
		results.offer(packet.okResult((Element) null, 0));
	}

	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo, Queue<Packet> results,
										 Map<String, Object> settings) throws PacketErrorTypeException {

		if (packet.getElemName() == Iq.ELEM_NAME && (packet.getType().equals(StanzaType.error) || packet.getType().equals(StanzaType.result))) {
			// https://xmpp.org/rfcs/rfc6120.html#stanzas-semantics-iq
			// An entity that receives a stanza of type "result" or "error" MUST NOT respond to the stanza
			// by sending a further IQ response of type "result" or "error"; however, the requesting entity
			// MAY send another request (e.g., an IQ of type "set" to provide obligatory information discovered
			// through a get/result pair).
			return;
		} else {
			results.offer(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, "Service not available.", true));
		}
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
										   Queue<Packet> results, Map<String, Object> settings) {
		results.offer(packet.okResult((Element) null, 0));
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}
}
