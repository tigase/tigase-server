/*
 * S2SAbstractProcessor.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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



package tigase.server.xmppserver.proc;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;
import tigase.server.xmppserver.S2SIOService;
import tigase.server.xmppserver.S2SProcessor;
import tigase.xml.Element;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * Created: Dec 10, 2010 3:32:11 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class S2SAbstractProcessor
	extends S2SAbstract
				implements S2SProcessor {
	
	// Order of enum values is important as it is an order in which packet 
	// is processed by processors
	protected static enum Order {
		StreamOpen,				// 0
		StreamError,			// 1
		StreamFeatures,			// 2
		StartTLS,				// 3
		StartZlib,				// 4
		Dialback				// 5
	}

	private static final Logger log                        =
			Logger.getLogger(S2SAbstractProcessor.class.getName());

	//~--- methods --------------------------------------------------------------
	
	@Override
	public boolean process(Packet p, S2SIOService serv, Queue<Packet> results) {
		return false;
	}

	@Override
	public void serviceStarted(S2SIOService serv) {}

	@Override
	public void serviceStopped(S2SIOService serv) {}

	/**
	 * Method description
	 *
	 *
	 * @param hostname
	 *
	 * 
	 */
	public boolean skipTLSForHost(String hostname) {

		// TODO: this is slow, optimize it somehow!!!
		// Workaround for buggy servers having problems with establishing TLS over s2s
		// http://community.igniterealtime.org/thread/36206
		// http://community.igniterealtime.org/thread/30578
		String hostnames = System.getProperty("s2s-skip-tls-hostnames");

		if (hostnames != null) {
			String[] hosts = hostnames.split(",");

			for (String host : hosts) {
				if (hostname.equalsIgnoreCase(host)) {
					return true;
				}
			}

			return false;
		} else {
			return false;
		}
	}

	@Override
	public void streamClosed(S2SIOService serv) {}

	@Override
	public void streamFeatures(S2SIOService serv, List<Element> results) {}

	@Override
	public String streamOpened(S2SIOService serv, Map<String, String> attribs) {
		return null;
	}
	
	@Override
	public int compareTo(S2SProcessor proc) {
		return (-1) * (proc.order() - order());
	}
}
