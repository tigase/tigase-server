/*
 * CounterValue.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
 */package tigase.stats;

import java.util.logging.Level;
 
/**
 *
 * @author andrzej
 */
public class CounterValue {

	protected String name;
	protected final Level level;
	
	protected long counter = 0;

	public CounterValue(String name, Level level) {
		this.name = name;
		this.level = level;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void inc() {
		++counter;
	}
	
	public Level getLevel() {
		return level;
	}
	
	public long getValue() {
		return counter;
	}

	@Override
	public String toString() {
		return "CounterValue{" + "name='" + name + '\'' + ", counter=" + counter + '}';
	}
}
