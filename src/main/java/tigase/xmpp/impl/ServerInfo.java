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
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.xmpp.Authorization;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;
import tigase.xmpp.impl.annotation.DiscoFeatures;
import tigase.xmpp.impl.annotation.Id;

import java.util.Map;
import java.util.Queue;

/**
 * ProtoXEP: PubSub Server Information (partial)
 * 
 * Enabling this processor enables 'opt-in' for inclusion of local domain names in the data exposed by other domains
 * (per the domain).
 *
 * @author Andrzej Wójcik
 */
@Id(ServerInfo.ID)
@DiscoFeatures(ServerInfo.XMLNS)
@Bean(name = ServerInfo.ID, active = false, parent = SessionManager.class)
public class ServerInfo extends AnnotatedXMPPProcessor implements XMPPProcessorIfc {

	protected static final String XMLNS = "urn:xmpp:serverinfo:0";
	protected static final String ID = XMLNS;

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
		// this processor does nothing...
		results.offer(
				Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, null, true));
	}
}
