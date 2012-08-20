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

package tigase.server.xmppclient;

import java.util.LinkedHashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created: Sep 11, 2009 12:39:04 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class IPMonitor extends Thread {

  private static final Logger log =
    Logger.getLogger(IPMonitor.class.getName());

	private boolean stopped = false;
	private static final int MAX_SIZE = 50;
	private static final long CLEANUP_RATE = 10000;
	private static final long DISC_THRESHOLD = 200;
	private long[] ip_cnts = new long[MAX_SIZE];
	private LinkedHashSet<String> ips = new LinkedHashSet<String>();
	private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
	private Timer timer = new Timer("IPMonitor Timer");

	public IPMonitor() {
		super();
		setName(IPMonitor.class.getSimpleName());
		setDaemon(true);
		for (int i = 0; i < ip_cnts.length; i++) {
			ip_cnts[i] = 0;
		}
	}

	public void addDisconnect(String ip) {
		queue.offer(ip);
	}

	@Override
	public void run() {
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				for (String ip : ips) {
					int idx = Math.abs(ip.hashCode() % ip_cnts.length);
					if (ip_cnts[idx] > DISC_THRESHOLD) {
						log.warning("Many disconnects for IP: " + ip + " - " + ip_cnts[idx]);
					}
					ip_cnts[idx] = 0;
				}
			}
		}, CLEANUP_RATE, CLEANUP_RATE);
		while (!stopped) {
			try {
				String ip = queue.poll(10, TimeUnit.SECONDS);
				if (ip != null) {
					int idx = Math.abs(ip.hashCode()) % ip_cnts.length;
					++ip_cnts[idx];
					if (ips.size() < MAX_SIZE) {
						ips.add(ip);
					}
				}
			} catch (Exception e) {
				log.warning("Error processing queue: " + e);
			}
		}
		timer.cancel();
	}

	public void stopThread() {
		stopped = true;
	}

}
