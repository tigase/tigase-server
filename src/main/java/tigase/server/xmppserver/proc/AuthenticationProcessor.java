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

import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Packet;
import tigase.server.xmppserver.S2SIOService;
import tigase.util.common.TimerTask;

import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class
AuthenticationProcessor
		extends S2SAbstractProcessor {

	private final static Logger log = Logger.getLogger(AuthenticationProcessor.class.getName());
	@Inject
	AuthenticatorSelectorManager authenticatorSelectorManager;
	@ConfigField(desc = "Authentication timeout for S2S connections")
	private long authenticationTimeOut = 30;

	private final static String AUTHENTICATION_TIMER_KEY = "AUTHENTICATION_TIMER_KEY";

	@Override
	public void serviceStarted(S2SIOService serv) {
		log.log(Level.FINEST, "{0}, s2s connection opened, isHandshaking: {1}",
				new Object[]{serv, serv.isHandshakingOnly()});

		if (serv.getSessionData().get(AUTHENTICATION_TIMER_KEY) == null) {
			final AuthenticationTimer task = new AuthenticationTimer(serv);
			handler.addTimerTask(task, authenticationTimeOut, TimeUnit.SECONDS);
			serv.getSessionData().put(AUTHENTICATION_TIMER_KEY, task);
		}
	}

	abstract String getMethodName();

	abstract void restartAuth(Packet packet, S2SIOService serv, Queue<Packet> results);

	/**
	 * Method intends to determine if authenticator can handle received packet/features
	 */
	abstract boolean canHandle(Packet packet, S2SIOService serv, Queue<Packet> results);

	static class AuthenticationTimer
			extends TimerTask {

		private S2SIOService serv = null;

		private AuthenticationTimer(S2SIOService serv) {
			this.serv = serv;
		}

		@Override
		public void run() {
			if (!serv.isAuthenticated() && serv.isConnected()) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Connection not authenticated within timeout, stopping: {0}", serv);
				}
				serv.stop();
			}
		}
	}
}
