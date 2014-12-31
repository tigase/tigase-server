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

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.server.xmppserver.S2SConnectionHandlerIfc;
import tigase.server.xmppserver.S2SIOService;
import tigase.server.xmppserver.S2SProcessor;
import tigase.xml.Element;

/**
 * Created: Dec 10, 2010 3:32:11 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class S2SAbstractProcessor
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
	
	/** Field description */
	protected static final String DB_RESULT_EL_NAME = "db:result";

	/** Field description */
	protected static final String DB_VERIFY_EL_NAME = "db:verify";

	/** Field description */
	protected static final String DIALBACK_EL = "dialback";

	/** Field description */
	protected static final String DIALBACK_NS = "urn:xmpp:features:dialback";

	/** Field description */
	protected static final String FEATURES_EL = "features";

	/** Field description */
	protected static final String FEATURES_NS = "http://etherx.jabber.org/streams";

	/** Field description */
	protected static final String PROCEED_TLS_EL = "proceed";

	/** Field description */
	protected static final String RESULT_EL_NAME = "result";

	/** Field description */
	protected static final String START_TLS_EL = "starttls";

	/** Field description */
	protected static final String START_TLS_NS = "urn:ietf:params:xml:ns:xmpp-tls";

	/** Field description */
	protected static final String STREAM_FEATURES_EL = "stream:features";

	/** Field description */
	protected static final String VERIFY_EL_NAME = "verify";

	/** Field description */
	protected static final String VERSION_ATT_NAME = "version";

	/** Field description */
	protected static final String XMLNS_CLIENT_VAL = "jabber:client";

	/** Field description */
	protected static final String XMLNS_DB_ATT = "xmlns:db";

	/** Field description */
	protected static final String XMLNS_DB_VAL = "jabber:server:dialback";

	/** Field description */
	protected static final String XMLNS_SERVER_VAL = "jabber:server";

	/** Field description */
	protected static final String[] FEATURES_STARTTLS_PATH = { FEATURES_EL, START_TLS_EL };

	/** Field description */
	protected static final String[] FEATURES_DIALBACK_PATH = { FEATURES_EL, DIALBACK_EL };
	private static final Logger log                        =
		Logger.getLogger(S2SAbstractProcessor.class.getName());

	/** Field description */
	public static boolean FORCE_VERSION = false;

	//~--- fields ---------------------------------------------------------------

	/** Field description */
	protected S2SConnectionHandlerIfc<S2SIOService> handler = null;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param initStream
	 * @param error_el
	 * @param serv
	 */
	public void generateStreamError(boolean initStream, String error_el,
																	S2SIOService serv) {
		String strError = "";

		if (initStream) {
			strError += "<?xml version='1.0'?><stream:stream" + " xmlns='" + XMLNS_SERVER_VAL +
									"'" + " xmlns:stream='http://etherx.jabber.org/streams'" +
									" id='tigase-server-error'" + " from='" + handler.getDefHostName() +
									"'" + " xml:lang='en'>";
		}
		strError += "<stream:error>" + "<" + error_el +
								" xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" + "</stream:error>" +
								"</stream:stream>";
		try {
			if (log.isLoggable(Level.FINEST)) {
				Throwable thr = new Throwable();

				thr.fillInStackTrace();
				log.log(Level.FINEST, "Called from: ", thr);
				log.log(Level.FINEST, "{0}, Sending stream error: {1}", new Object[] { serv,
								strError });
			}
			handler.writeRawData(serv, strError);
			serv.stop();
		} catch (Exception e) {
			serv.forceStop();
		}
	}

	@Override
	public void init(S2SConnectionHandlerIfc<S2SIOService> handler,
									 Map<String, Object> props) {
		this.handler = handler;
	}

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
