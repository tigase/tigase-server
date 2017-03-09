/*
 * RegistrationThrottlingProcessor.java
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
package tigase.server.xmppclient;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.stats.StatisticsList;
import tigase.xml.Element;
import tigase.xmpp.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 16.11.2016.
 */
@Bean(name = RegistrationThrottlingProcessor.ID, active = true)
public class RegistrationThrottlingProcessor
		implements XMPPIOProcessor {

	private static final Logger log = Logger.getLogger(RegistrationThrottlingProcessor.class.getCanonicalName());

	public static final String ID = RegistrationThrottling.ID + "-processor";

	private static final String[] REGISTER_PATH = Iq.IQ_QUERY_PATH;
	private static final String[] REMOVE_PATH = new String[]{Iq.ELEM_NAME, Iq.QUERY_NAME, "remove"};
	private static final String[] USERNAME_PATH = new String[]{Iq.ELEM_NAME, Iq.QUERY_NAME, "username"};
	private static final String XMLNS = "jabber:iq:register";

	@Inject(bean = "service")
	private ClientConnectionManager connectionManager;

	@Inject(bean = RegistrationThrottling.ID)
	private RegistrationThrottling throttler;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void getStatistics(StatisticsList list) {

	}

	@Override
	public Element[] supStreamFeatures(XMPPIOService service) {
		return new Element[0];
	}

	@Override
	public boolean processIncoming(XMPPIOService service, Packet packet) {
		if (packet.getType() != StanzaType.set || !XMLNS.equals(packet.getAttributeStaticStr(REGISTER_PATH, "xmlns"))) {
			return false;
		}

		JID to = packet.getStanzaTo();
		if (to != null && (to.getLocalpart() != null || !connectionManager.isLocalDomain(to.getDomain()))) {
			return false;
		}

		if (packet.getElement().findChild(REMOVE_PATH) != null) {
			return false;
		}

		if (throttler.checkLimits(service, packet)) {
			return false;
		}

		try {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "User from IP {0} exceeded registration limit trying to register account {1}",
						new Object[] { service.getRemoteAddress(), packet.getElemCDataStaticStr(USERNAME_PATH) });
			}
			Packet errorPacket = Authorization.POLICY_VIOLATION.getResponseMessage(packet, "Policy violation", true);

			Element streamError = new Element("policy-violation");
			streamError.setXMLNS("urn:ietf:params:xml:ns:xmpp-stanzas");
			String result = connectionManager.xmppStreamError(service, Arrays.asList(streamError));
			service.writeRawData(errorPacket.getElement().toString() + result + "</stream:stream>");
		} catch (PacketErrorTypeException | IOException ex) {
			log.log(Level.FINEST, "Exception while registration request to check policy violation");
		}
		service.stop();
		return true;
	}

	@Override
	public boolean processOutgoing(XMPPIOService service, Packet packet) {
		return false;
	}

	@Override
	public void packetsSent(XMPPIOService service) throws IOException {

	}

	@Override
	public void processCommand(XMPPIOService service, Packet packet) {

	}

	@Override
	public boolean serviceStopped(XMPPIOService service, boolean streamClosed) {
		return false;
	}

	@Override
	public void streamError(XMPPIOService service, StreamError streamError) {

	}

}
