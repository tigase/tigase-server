/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 * 
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.stats;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.logging.Level;

/**
 * Created: Jul 10, 2009 3:23:23 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StatisticsList implements Iterable<StatRecord> {

	private Level statLevel = Level.ALL;
	private LinkedHashMap<String, LinkedHashMap<String, StatRecord>> stats =
					new LinkedHashMap<String, LinkedHashMap<String, StatRecord>>();

	public StatisticsList(Level level) {
		this.statLevel = level;
	}

	public LinkedHashMap<String, StatRecord> addCompStats(String comp) {
		LinkedHashMap<String, StatRecord> compStats = 
						new LinkedHashMap<String, StatRecord>();
		stats.put(comp, compStats);
		return compStats;
	}

	public boolean checkLevel(Level recordLevel) {
		return recordLevel.intValue() >= statLevel.intValue();
	}

	public boolean checkLevel(Level recordLevel, long value) {
		if (checkLevel(recordLevel)) {
			if (value == 0) {
				return checkLevel(Level.FINEST);
			} else {
				return true;
			}
		}
		return false;
	}

	public boolean checkLevel(Level recordLevel, int value) {
		if (checkLevel(recordLevel)) {
			if (value == 0) {
				return checkLevel(Level.FINEST);
			} else {
				return true;
			}
		}
		return false;
	}

	public boolean add(String comp, String description, long value,
					Level recordLevel) {
		if (checkLevel(recordLevel, value)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);
			if (compStats == null) {
				compStats = addCompStats(comp);
			}
			compStats.put(description, new StatRecord(comp, description, "long",
							value, recordLevel));
			return true;
		}
		return false;
	}

	public boolean add(String comp, String description, int value,
					Level recordLevel) {
		if (checkLevel(recordLevel, value)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);
			if (compStats == null) {
				compStats = addCompStats(comp);
			}
			compStats.put(description, new StatRecord(comp, description, "int",
							value, recordLevel));
			return true;
		}
		return false;
	}

	public boolean add(String comp, String description, String value,
					Level recordLevel) {
		if (checkLevel(recordLevel)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);
			if (compStats == null) {
				compStats = addCompStats(comp);
			}
			compStats.put(description, new StatRecord(comp, description, "String",
							value, recordLevel));
			return true;
		}
		return false;
	}

	public long getValue(String comp, String description, long def) {
		long result = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);
		if (compStats != null) {
			StatRecord rec = compStats.get(description);
			if (rec != null) {
				result = rec.getLongValue();
			}
		}
		return result;
	}

	public int getValue(String comp, String description, int def) {
		int result = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);
		if (compStats != null) {
			StatRecord rec = compStats.get(description);
			if (rec != null) {
				result = rec.getIntValue();
			}
		}
		return result;
	}

	public String getValue(String comp, String description, String def) {
		String result = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);
		if (compStats != null) {
			StatRecord rec = compStats.get(description);
			if (rec != null) {
				result = rec.getValue();
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return stats.toString();
	}

	@Override
	public Iterator<StatRecord> iterator() {
		return new StatsIterator();
	}

	private class StatsIterator implements Iterator<StatRecord> {

		Iterator<LinkedHashMap<String, StatRecord>> compsIt = stats.values().iterator();
		Iterator<StatRecord> recIt = null;

		@Override
		public boolean hasNext() {
      if (recIt == null || !recIt.hasNext()) {
				if (compsIt.hasNext()) {
					recIt = compsIt.next().values().iterator();
				} else {
					return false;
				}
			}
			return recIt.hasNext();
		}

		@Override
		public StatRecord next() throws NoSuchElementException {
      if (recIt == null || !recIt.hasNext()) {
				if (compsIt.hasNext()) {
					recIt = compsIt.next().values().iterator();
				} else {
					throw new NoSuchElementException("No more statistics.");
				}
			}
			return recIt.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not supported yet.");
		}

	}

}
