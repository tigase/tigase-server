/*
 * ShutdownEvent.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.eventbus.events;

import java.io.Serializable;

/**
 *
 * @author andrzej
 */
public class ShutdownEvent implements Serializable {
	
	private String node;
	private long delay = 0;
	private String msg;
	
	public ShutdownEvent() {}
	
	public ShutdownEvent(String node, long delay, String msg) {
		this.node = node;
		this.delay = delay;
	}
	
	public String getMessage() {
		return msg;
	}
	
	public String getNode() { 
		return node;
	}
	
	public long getDelay() {
		return delay;
	}
	
}
