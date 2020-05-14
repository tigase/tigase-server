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
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.xmpp.*;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import static tigase.xmpp.impl.Message.ELEM_NAME;
import static tigase.xmpp.impl.Message.XMLNS;

/**
 * Message forwarder class. Forwards <code>Message</code> packet to it's destination address.
 * <br>
 * Created: Tue Feb 21 15:49:08 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
@Id(ELEM_NAME)
@Handles({@Handle(path = {ELEM_NAME}, xmlns = XMLNS), @Handle(path = {Iq.ELEM_NAME, "fin"}, xmlns = "urn:xmpp:mam:2"),
		  @Handle(path = {Iq.ELEM_NAME, "fin"}, xmlns = "urn:xmpp:mam:1")})
@Bean(name = ELEM_NAME, parent = SessionManager.class, active = false, exportable = true)
public class Message
		extends AnnotatedXMPPProcessor
		implements XMPPProcessorIfc, XMPPPreprocessorIfc, XMPPPacketFilterIfc {

	@Inject
	private MessageDeliveryLogic messageDeliveryLogic;
	
	protected static final String ELEM_NAME = tigase.server.Message.ELEM_NAME;
	protected static final String XMLNS = "jabber:client";
	private static final Logger log = Logger.getLogger(Message.class.getName());

	@Override
	public void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
					   Queue<Packet> results) {
		C2SDeliveryErrorProcessor.filter(packet, session, repo, results, null);
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
		messageDeliveryLogic.handleDelivery(packet, session, repo, results, settings);
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
							  Queue<Packet> results, Map<String, Object> settings) {
		boolean result = C2SDeliveryErrorProcessor.preProcess(packet, session, repo, results, settings, messageDeliveryLogic);
		if (result) {
			packet.processedBy(id());
		}
		return result;
	}

}    // Message

