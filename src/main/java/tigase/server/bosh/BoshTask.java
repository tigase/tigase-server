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
package tigase.server.bosh;

import tigase.server.Packet;
import tigase.util.common.TimerTask;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class BoshWaitTimer here.
 * <br>
 * Created: Tue Oct 30 16:38:15 2012
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev: $
 */
public class BoshTask
		extends TimerTask {

	private static final Logger log = Logger.getLogger(BoshTask.class.getName());

	protected long timerOrder = System.currentTimeMillis();
	private BoshSession bs = null;
	private BoshConnectionManager manager = null;


	public BoshTask(BoshSession bs, BoshConnectionManager manager) {
		this.bs = bs;
		this.manager = manager;
	}

	@Override
	public void run() {
		Queue<Packet> out_results = new ArrayDeque<Packet>();

		if (bs.task(out_results, this)) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Closing session for BS task: " + bs.getSid());
			}
			manager.sessions.remove(bs.getSid());
		}

		manager.addOutPackets(out_results, bs);
	}
}
