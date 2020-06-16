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
import tigase.kernel.beans.Inject;
import tigase.net.ConnectionType;
import tigase.server.Packet;
import tigase.server.xmppserver.*;
import tigase.stats.StatisticsList;
import tigase.stats.StatisticsProviderIfc;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "authenticator-selector-manager", parent = S2SConnectionManager.class, active = true)
public class AuthenticatorSelectorManager
		implements StatisticsProviderIfc {

	public static final String S2S_METHOD_USED = "S2S_METHOD_USED";
	public static final String S2S_METHODS_ADVERTISED = "S2S_METHODS_ADVERTISED";
	public static final String S2S_METHODS_AVAILABLE = "S2S_METHODS_AVAILABLE";
	private static final Logger log = Logger.getLogger(AuthenticatorSelectorManager.class.getName());
	@Inject
	public List<AuthenticationProcessor> authenticationProcessors;

	private Map<String, AtomicInteger> failedAuthenticationDomains = new ConcurrentHashMap<>();

	public AuthenticatorSelectorManager() {}

	/**
	 * Method determines if given authenticator is allowed to proceed: takes into consideration authenticators
	 * priority, currently used authenticator as well as received stream futures
	 */
	public boolean isAllowed(Packet p, S2SIOService serv, AuthenticationProcessor processor, Queue<Packet> results) {

		final boolean authenticated = serv.isAuthenticated();
		final SortedSet<AuthenticationProcessor> methodsAvailable = getAuthenticationProcessors(serv);
		final Optional<AuthenticationProcessor> currentAuthenticationProcessor = getCurrentAuthenticationProcessor(
				serv);
		if (authenticated) {
			log.log(Level.FINE,
					"{0}, Connection already authenticated, skipping processor: {1}, methodsAvailable: {2}, checking packet: {3}",
					new Object[]{serv, processor, methodsAvailable, p});
			return false;
		}
		log.log(Level.FINE, "{0}, Processor {1}, methodsAvailable: {2}, checking packet: {3}",
				new Object[]{serv, processor.getMethodName(), methodsAvailable, p});

		boolean canHandle = processor.canHandle(p, serv, results);
		if (canHandle) {
			methodsAvailable.add(processor);
		}

		final boolean result = !currentAuthenticationProcessor.isPresent() && canHandle;
		log.log(Level.FINEST,
				"{0}, Processor {1} canHandle: {2}, currentAuthenticationProcessor: {3}, result: {4}, methodsAvailable: {5}, packet: {6}",
				new Object[]{serv, processor.getMethodName(), canHandle, currentAuthenticationProcessor, result,
							 methodsAvailable, p});
		if (result) {
			methodsAvailable.remove(processor);
			serv.getSessionData().put(S2S_METHOD_USED, processor);
			log.log(Level.FINE, "{0}, Allowing auth for: {1}, remaining: {2}",
					new Object[]{serv, processor.getMethodName(), methodsAvailable});
		}
		return result;
	}

	public void authenticateConnection(String sessionId, CIDConnections cid_conns, CID cidPacket) {
		S2SConnection s2s_conn = cid_conns.getS2SConnectionForSessionId(sessionId);

		if (s2s_conn != null) {
			authenticateConnection(s2s_conn.getS2SIOService(), cid_conns, cidPacket);
		}
	}

	public void authenticateConnection(S2SIOService serv, CIDConnections cid_conns, CID cidPacket) {
		log.log(Level.FINE, "{0}, Authenticating connection", new Object[]{serv});
		serv.getSessionData().remove(S2S_METHOD_USED);
		cid_conns.connectionAuthenticated(serv, cidPacket);
	}

	@Override
	public void getStatistics(String compName, StatisticsList list) {
		final String keyName = compName + "/AuthenticationFailures";
		for (Map.Entry<String, AtomicInteger> entry : failedAuthenticationDomains.entrySet()) {
			list.add(keyName, entry.getKey(), entry.getValue().intValue(), Level.FINER);
		}
	}

	public void authenticationFailed(Packet packet, S2SIOService serv, AuthenticationProcessor processor,
									 Queue<Packet> results) {

		markConnectionAsFailed(processor.getMethodName(), serv);
		serv.getSessionData().remove(S2S_METHOD_USED);
		final SortedSet<AuthenticationProcessor> methodsAvailable = getAuthenticationProcessors(serv);
		methodsAvailable.remove(processor);
		log.log(Level.FINE, "{0}, Authentication failed for: {1}, remaining methodsAvailable: {2}",
				new Object[]{serv, processor.getMethodName(), methodsAvailable});

		if (methodsAvailable.isEmpty()) {
			log.log(Level.WARNING, "{0}, All authentication methods failed, stopping connection", new Object[]{serv});
			flushRemainingPackets(serv, results);
			serv.forceStop();
		}

		if (serv.connectionType() == ConnectionType.connect) {
			Optional<AuthenticationProcessor> nextAuthenticationProcessor = methodsAvailable.stream().findFirst();
			if (nextAuthenticationProcessor.isPresent()) {
				log.log(Level.FINE, "{0}, Restarting authentication with: {1}",
						new Object[]{serv, nextAuthenticationProcessor.get().getMethodName()});
				methodsAvailable.remove(nextAuthenticationProcessor.get());
				nextAuthenticationProcessor.get().restartAuth(packet, serv, results);
			} else {
				log.log(Level.WARNING, "{0}, No more authenticators for outgoing connections, stopping",
						new Object[]{serv});
				flushRemainingPackets(serv, results);
				serv.forceStop();
			}
		}
	}

	public void markConnectionAsFailed(String prefix, S2SIOService serv) {
		CID cid = (CID) serv.getSessionData().get("cid");
		log.log(Level.FINEST, () -> "Adding entry to stats, prefix: " + prefix + ", cid: " + cid);
		failedAuthenticationDomains.computeIfAbsent(prefix + "/" + cid, s -> new AtomicInteger()).incrementAndGet();
	}

	SortedSet<AuthenticationProcessor> getAuthenticationProcessors(S2SIOService serv) {
		SortedSet<AuthenticationProcessor> mechanisms = (SortedSet<AuthenticationProcessor>) serv.getSessionData()
				.get(S2S_METHODS_AVAILABLE);
		if (mechanisms == null) {
			mechanisms = getAuthenticationProcessors();
			serv.getSessionData().put(S2S_METHODS_AVAILABLE, mechanisms);
		}
		return mechanisms;
	}

	private void flushRemainingPackets(S2SIOService serv, Queue<Packet> results) {
		for (Packet result : results) {
			serv.addPacketToSend(result);
		}
		try {
			serv.processWaitingPackets();
		} catch (IOException e) {
			log.log(Level.WARNING, "Error while writing packets before closing the stream", e);
		}
	}

	private SortedSet<AuthenticationProcessor> getAuthenticationProcessors() {
		log.log(Level.FINEST, "preparing empty processor list!");
		return new ConcurrentSkipListSet<>();
	}

	public void setAuthenticationProcessors(List<AuthenticationProcessor> authenticationProcessors) {
		this.authenticationProcessors = new CopyOnWriteArrayList<>(authenticationProcessors);
	}

	private Optional<AuthenticationProcessor> getCurrentAuthenticationProcessor(S2SIOService serv) {
		return Optional.ofNullable((AuthenticationProcessor) serv.getSessionData().get(S2S_METHOD_USED));
	}
}
