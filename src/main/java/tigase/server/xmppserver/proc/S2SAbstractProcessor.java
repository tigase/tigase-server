
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
*
* $Rev$
* Last modified by $Author$
* $Date$
 */
package tigase.server.xmppserver.proc;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;
import tigase.server.xmppserver.S2SConnectionHandlerIfc;
import tigase.server.xmppserver.S2SIOService;
import tigase.server.xmppserver.S2SProcessor;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Dec 10, 2010 3:32:11 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class S2SAbstractProcessor implements S2SProcessor {
	protected static final String XMLNS_SERVER_VAL = "jabber:server";
	protected static final String XMLNS_CLIENT_VAL = "jabber:client";
	protected static final String XMLNS_DB_VAL = "jabber:server:dialback";
	protected static final String RESULT_EL_NAME = "result";
	protected static final String VERIFY_EL_NAME = "verify";
	protected static final String DB_RESULT_EL_NAME = "db:result";
	protected static final String DB_VERIFY_EL_NAME = "db:verify";
	protected static final String XMLNS_DB_ATT = "xmlns:db";
	protected static final String STREAM_FEATURES_EL = "stream:features";
	protected static final String FEATURES_EL = "features";
	protected static final String FEATURES_NS = "http://etherx.jabber.org/streams";
	protected static final String START_TLS_EL = "starttls";
	protected static final String DIALBACK_TLS_EL = "dialback";
	protected static final String PROCEED_TLS_EL = "proceed";
	protected static final String START_TLS_NS = "urn:ietf:params:xml:ns:xmpp-tls";
	protected static final String DIALBACK_TLS_NS = "urn:xmpp:features:dialback";
	protected static final String VERSION_ATT_NAME = "version";
	private static final Logger log = Logger.getLogger(S2SAbstractProcessor.class.getName());
	public static boolean FORCE_VERSION = false;

	//~--- fields ---------------------------------------------------------------

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
	public void generateStreamError(boolean initStream, String error_el, S2SIOService serv) {
		String strError = "";

		if (initStream) {
			strError += "<?xml version='1.0'?><stream:stream" + " xmlns='" + XMLNS_SERVER_VAL + "'"
					+ " xmlns:stream='http://etherx.jabber.org/streams'" + " id='tigase-server-error'"
						+ " from='" + handler.getDefHostName() + "'" + " xml:lang='en'>";
		}

		strError += "<stream:error>" + "<" + error_el
				+ " xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" + "</stream:error>" + "</stream:stream>";

		try {
			if (log.isLoggable(Level.FINEST)) {
				Throwable thr = new Throwable();

				thr.fillInStackTrace();
				log.log(Level.FINEST, "Called from: ", thr);
				log.log(Level.FINEST, "{0}, Sending stream error: {1}", new Object[] { serv, strError });
			}

			handler.writeRawData(serv, strError);
			serv.stop();
		} catch (Exception e) {
			serv.forceStop();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param handler
	 */
	@Override
	public void init(S2SConnectionHandlerIfc<S2SIOService> handler, Map<String,Object> props) {
		this.handler = handler;
	}

	/**
	 * Method description
	 *
	 *
	 * @param p
	 * @param serv
	 * @param results
	 *
	 * @return
	 */
	@Override
	public boolean process(Packet p, S2SIOService serv, Queue<Packet> results) {
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	@Override
	public void serviceStarted(S2SIOService serv) {}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	@Override
	public void serviceStopped(S2SIOService serv) {}

	/**
	 * Method description
	 *
	 *
	 * @param hostname
	 *
	 * @return
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

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 */
	@Override
	public void streamClosed(S2SIOService serv) {}

	/**
	 * Method description
	 *
	 *
	 *
	 * @param serv
	 * @param results
	 */
	@Override
	public void streamFeatures(S2SIOService serv, List<Element> results) {}

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 * @param attribs
	 *
	 * @return
	 */
	@Override
	public String streamOpened(S2SIOService serv, Map<String, String> attribs) {
		return null;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
