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

import tigase.server.ConnectionManager;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.stats.StatisticsList;
import tigase.xml.Element;
import tigase.xmpp.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 16.11.2016.
 */
public class RegistrationThrottlingProcessor
		implements XMPPIOProcessor {

	private static final Logger log = Logger.getLogger(RegistrationThrottlingProcessor.class.getCanonicalName());

	public static final String ID = "registration-throttling";
	private static final String[] REGISTER_PATH = Iq.IQ_QUERY_PATH;
	private static final String[] REMOVE_PATH = new String[]{Iq.ELEM_NAME, Iq.QUERY_NAME, "remove"};
	private static final String[] USERNAME_PATH = new String[]{Iq.ELEM_NAME, Iq.QUERY_NAME, "username"};
	private static final String XMLNS = "jabber:iq:register";

	private ConnectionManager connectionManager;

	private Timer timer = new Timer("registration-timer", true);
	private ConcurrentHashMap<String, List<Long>> registrations = new ConcurrentHashMap<>();
	private Integer limit = 4;
	private Duration period = Duration.ofDays(1);
	private AtomicBoolean cleanUpScheduled = new AtomicBoolean(false);

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

		if (checkLimits(service, packet)) {
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
			service.writeRawData(errorPacket.getElement().toString());
			service.writeRawData(result);
			service.writeRawData("</stream:stream>");
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
	public void setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		String tmp = (String) props.get("period");
		if (tmp != null) {
			period = Duration.parse(tmp);
		}
		if (props.containsKey("limit")) {
			limit = (Integer) props.get("limit");
		}
	}

	@Override
	public void streamError(XMPPIOService service, StreamError streamError) {

	}

	protected void cleanUpFromTimer() {
		Iterator<Map.Entry<String, List<Long>>> it = registrations.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, List<Long>> e = it.next();
			List<Long> registrationTimes = e.getValue();
			synchronized (registrationTimes) {
				cleanUp(registrationTimes);
				if (registrationTimes.isEmpty()) {
					it.remove();
				}
			}
		}
		Optional<Long> earliest = registrations.values().stream().flatMap(times -> times.stream()).min(Long::compare);
		if (earliest.isPresent()) {
			timer.schedule(new CleanUpTask(), System.currentTimeMillis() - earliest.get());
		} else {
			cleanUpScheduled.compareAndSet(true, false);
		}
	}

	protected boolean checkLimits(XMPPIOService service, Packet packet) {
		boolean result = checkLimits(service);
		scheduleCleanUpIfNeeded();
		return result;
	}

	protected boolean checkLimits(XMPPIOService service) {
		List<Long> registrationTimes = registrations.computeIfAbsent(service.getRemoteAddress(),
																	 (k) -> new ArrayList<Long>());
		synchronized (registrationTimes) {
			cleanUp(registrationTimes);

			if (registrationTimes.size() <= limit) {
				registrationTimes.add(System.currentTimeMillis());
			}

			return registrationTimes.size() <= limit;
		}
	}

	protected void cleanUp(List<Long> registrationTimes) {
		// Five seconds added to improve performance
		long oldestAllowed = (System.currentTimeMillis() - period.toMillis()) + 5000;
		registrationTimes.removeIf((ts) -> ts < oldestAllowed);
	}

	protected void scheduleCleanUpIfNeeded() {
		if (cleanUpScheduled.compareAndSet(false, true)) {
			timer.schedule(new CleanUpTask(), period.toMillis());
		}
	}

	protected class CleanUpTask
			extends TimerTask {

		@Override
		public void run() {
			RegistrationThrottlingProcessor.this.cleanUpFromTimer();
		}
	}
}
