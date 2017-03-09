/*
 * ExternalServiceDiscoveryProcessor.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.server.extdisco;

import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.util.DNSResolverFactory;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.annotation.*;

import java.util.Map;
import java.util.Queue;

import static tigase.server.extdisco.ExternalServiceDiscoveryProcessor.ID;
import static tigase.server.extdisco.ExternalServiceDiscoveryProcessor.XMLNS;

/**
 * Created by andrzej on 06.09.2016.
 */
@Bean(name = ID, parent = SessionManager.class, active = false)
@Id(ID)
@DiscoFeatures({ XMLNS })
@Handles({
	@Handle(path = {Iq.ELEM_NAME, "services"}, xmlns = XMLNS)
})
public class ExternalServiceDiscoveryProcessor extends AnnotatedXMPPProcessor implements XMPPProcessorIfc {

	protected static final String XMLNS = "urn:xmpp:extdisco:2";
	protected static final String ID = XMLNS;

	@ConfigField(desc = "JID of External Service Discovery Component", alias = "ext-service-disco-jid")
	private JID extServiceDiscoJid = JID.jidInstanceNS("ext-disco", DNSResolverFactory.getInstance().getDefaultHost());

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
		if (extServiceDiscoJid.equals(packet.getPacketFrom())) {
			// No session, skipping processing result
			if (session == null)
				return;

			Packet forward = packet.copyElementOnly();
			forward.setPacketTo(session.getConnectionId(packet.getStanzaTo()));

			results.offer(forward);
		} else {
			Packet forward = packet.copyElementOnly();
			forward.setPacketTo(extServiceDiscoJid);

			results.offer(forward);
		}
	}

}
