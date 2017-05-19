/*
 * S2SAbstract.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

import tigase.kernel.beans.Inject;
import tigase.server.xmppserver.S2SConnectionHandlerIfc;
import tigase.server.xmppserver.S2SIOService;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 19.05.2017.
 */
public class S2SAbstract {

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

	/** Field description */
	public static boolean FORCE_VERSION = false;

	private static final Logger log                        =
			Logger.getLogger(S2SAbstract.class.getName());


	//~--- fields ---------------------------------------------------------------

	/** Field description */
	@Inject(nullAllowed = true)
	protected S2SConnectionHandlerIfc<S2SIOService> handler = null;

	public void init(S2SConnectionHandlerIfc<S2SIOService> handler,
					 Map<String, Object> props) {
		this.handler = handler;
	}

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

}
