/*  Tigase Project
 *  Copyright (C) 2001-2007
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.bosh;

import java.util.UUID;
import java.util.Map;
import java.util.LinkedHashMap;
import tigase.server.Packet;

import static tigase.server.bosh.Constants.*;

/**
 * Describe class BoshSession here.
 *
 *
 * Created: Tue Jun  5 18:07:23 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BoshSession {

	private UUID sid = null;
	private Map<UUID, BoshIOService> connections =
		new LinkedHashMap<UUID, BoshIOService>();
	private long max_wait = MAX_WAIT_DEF_PROP_VAL;
	private long min_pooling = MIN_POOLING_PROP_VAL;
	private long max_inactivity = MAX_INACTIVITY_PROP_VAL;
	private int concurrent_requests = CONCURRENT_REQUESTS_PROP_VAL;
	private int hold_requests = HOLD_REQUESTS_PROP_VAL;
	private long max_pause = MAX_PAUSE_PROP_VAL;

	/**
	 * Creates a new <code>BoshSession</code> instance.
	 *
	 */
	public BoshSession() {
		this.sid = UUID.randomUUID();
	}

	public Packet init(Packet packet, BoshIOService service,
		long max_wait, long min_pooling, long max_inactivity,
		int concurrent_requests, int hold_requests, long max_pause) {
		long wait_l = max_wait;
		String wait_s = packet.getAttribute("wait");
		if (wait_s != null) {
			try {
				wait_l = Long.parseLong(wait_s);
			} catch (NumberFormatException e) {
				wait_l = max_wait;
			}
		}
		this.max_wait = Math.min(wait_l, max_wait);
		this.min_pooling = min_pooling;
		this.max_inactivity = max_inactivity;
		this.concurrent_requests = concurrent_requests;
		this.hold_requests = hold_requests;
		this.max_pause = max_pause;
		return null;
	}


}
