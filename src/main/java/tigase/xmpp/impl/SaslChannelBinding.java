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

import tigase.auth.TigaseSaslProvider;
import tigase.auth.mechanisms.AbstractSaslSCRAM;
import tigase.auth.mechanisms.SaslSCRAMPlus;
import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.jid.JID;

import javax.security.sasl.Sasl;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;

@Bean(name = SaslChannelBinding.ID, parent = SessionManager.class, active = true)
public class SaslChannelBinding extends XMPPProcessorAbstract {

	public static final String ID = "urn:xmpp:sasl-cb:0";

	@Inject
	protected TigaseSaslProvider saslProvider;

	@Override
	public String id() {
		return ID;
	}

	@Override
	public Authorization canHandle(Packet packet, XMPPResourceConnection conn) {
		return null;
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {

	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
											  NonAuthUserRepository repo, Queue<Packet> results,
											  Map<String, Object> settings) throws PacketErrorTypeException {
		
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
										   Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {

	}

	@Override
	public Element[] supStreamFeatures(XMPPResourceConnection session) {
		if ((session == null) || session.isAuthorized()) {
			return null;
		} else {
			Collection<String> auth_mechs = saslProvider.filterMechanisms(Sasl.getSaslServerFactories(), session);
			if (auth_mechs.isEmpty()) {
				return null;
			}

			if (session.isEncrypted() && session.getSessionData(AbstractSaslSCRAM.LOCAL_CERTIFICATE_KEY) != null &&
					SaslSCRAMPlus.containsScramPlus(auth_mechs)) {
				Element bindings = AbstractSaslSCRAM.getSupportedChannelBindings(session);
				return new Element[] { bindings };
			} else {
				return null;
			}
		}
	}
}
