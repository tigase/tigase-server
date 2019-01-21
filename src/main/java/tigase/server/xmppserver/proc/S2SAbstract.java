/**
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

	protected static final String DB_RESULT_EL_NAME = "db:result";

	protected static final String DB_VERIFY_EL_NAME = "db:verify";

	protected static final String DIALBACK_EL = "dialback";

	protected static final String DIALBACK_NS = "urn:xmpp:features:dialback";

	protected static final String FEATURES_EL = "features";

	protected static final String FEATURES_NS = "http://etherx.jabber.org/streams";

	protected static final String PROCEED_TLS_EL = "proceed";

	protected static final String RESULT_EL_NAME = "result";

	protected static final String START_TLS_EL = "starttls";

	protected static final String START_TLS_NS = "urn:ietf:params:xml:ns:xmpp-tls";

	protected static final String STREAM_FEATURES_EL = "stream:features";

	protected static final String VERIFY_EL_NAME = "verify";

	protected static final String VERSION_ATT_NAME = "version";

	protected static final String XMLNS_CLIENT_VAL = "jabber:client";

	protected static final String XMLNS_DB_ATT = "xmlns:db";

	protected static final String XMLNS_DB_VAL = "jabber:server:dialback";

	protected static final String XMLNS_SERVER_VAL = "jabber:server";

	protected static final String[] FEATURES_STARTTLS_PATH = {FEATURES_EL, START_TLS_EL};

	protected static final String[] FEATURES_DIALBACK_PATH = {FEATURES_EL, DIALBACK_EL};
	private static final Logger log = Logger.getLogger(S2SAbstract.class.getName());
	public static boolean FORCE_VERSION = false;

	@Inject(nullAllowed = true)
	protected S2SConnectionHandlerIfc<S2SIOService> handler = null;

	public void init(S2SConnectionHandlerIfc<S2SIOService> handler, Map<String, Object> props) {
		this.handler = handler;
	}

	public void generateStreamError(boolean initStream, String error_el, S2SIOService serv) {
		String strError = "";

		if (initStream) {
			strError += "<?xml version='1.0'?><stream:stream" + " xmlns='" + XMLNS_SERVER_VAL + "'" +
					" xmlns:stream='http://etherx.jabber.org/streams'" + " id='tigase-server-error'" + " from='" +
					handler.getDefHostName() + "'" + " xml:lang='en'>";
		}
		strError += "<stream:error>" + "<" + error_el + " xmlns='urn:ietf:params:xml:ns:xmpp-streams'/>" +
				"</stream:error>" + "</stream:stream>";
		try {
			if (log.isLoggable(Level.FINEST)) {
				Throwable thr = new Throwable();

				thr.fillInStackTrace();
				log.log(Level.FINEST, "Called from: ", thr);
				log.log(Level.FINEST, "{0}, Sending stream error: {1}", new Object[]{serv, strError});
			}
			handler.writeRawData(serv, strError);
			serv.stop();
		} catch (Exception e) {
			serv.forceStop();
		}
	}

}
