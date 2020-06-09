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

import tigase.kernel.beans.config.ConfigField;
import tigase.server.Packet;
import tigase.server.xmppserver.S2SIOService;
import tigase.server.xmppserver.S2SProcessor;
import tigase.xml.Element;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created: Dec 10, 2010 3:32:11 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public abstract class S2SAbstractProcessor
		extends S2SAbstract
		implements S2SProcessor {

	protected static final Comparator<S2SProcessor> processorsComparator = Comparator.comparingInt(S2SProcessor::order);

	private static final Logger log = Logger.getLogger(S2SAbstractProcessor.class.getName());
	@ConfigField(desc = "Skip StartTLS for domains", alias = "skip-tls-hostnames")
	private String[] skipTlsHosts;

	public void setSkipTlsHosts(String[] skipTlsHosts) {
		this.skipTlsHosts = skipTlsHosts != null
							? Arrays.stream(skipTlsHosts).map(String::toLowerCase).toArray(String[]::new)
							: null;
	}

	@Override
	public boolean process(Packet p, S2SIOService serv, Queue<Packet> results) {
		return false;
	}

	@Override
	public void serviceStarted(S2SIOService serv) {
	}

	@Override
	public void serviceStopped(S2SIOService serv) {
	}

	public boolean skipTLSForHost(String hostname) {
		// Workaround for buggy servers having problems with establishing TLS over s2s
		// http://community.igniterealtime.org/thread/36206
		// http://community.igniterealtime.org/thread/30578
		return skipTlsHosts != null && (Arrays.binarySearch(skipTlsHosts, hostname.toLowerCase()) >= 0);
	}

	@Override
	public void streamClosed(S2SIOService serv) {
	}

	@Override
	public void streamFeatures(S2SIOService serv, List<Element> results) {
	}

	@Override
	public String streamOpened(S2SIOService serv, Map<String, String> attribs) {
		return null;
	}

	@Override
	public int compareTo(S2SProcessor proc) {
		return processorsComparator.compare(this, proc);
	}

	// Order of enum values is important as it is an order in which packet
	// is processed by processors
	protected enum Order {
		// 0
		StreamOpen,
		// 1
		StreamError,
		// 2
		StreamFeatures,
		// 3
		StartTLS,
		// 4
		SaslExternal,
		// 5
		Dialback,
		// 6
		StartZlib,
	}
}
