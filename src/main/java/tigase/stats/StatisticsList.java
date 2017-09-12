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

package tigase.stats;

import tigase.server.QueueType;
import tigase.util.DataTypes;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created: Jul 10, 2009 3:23:23 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StatisticsList implements Iterable<StatRecord> {
	private static final Logger log = Logger.getLogger(StatisticsList.class.getName());
	private Level statLevel = Level.ALL;
	private final LinkedHashMap<String, LinkedHashMap<String, StatRecord>> stats =
			new LinkedHashMap<String, LinkedHashMap<String, StatRecord>>();

	// ~--- constructors ---------------------------------------------------------

	public StatisticsList(Level level) {
		this.statLevel = level;
	}

	// ~--- methods --------------------------------------------------------------

	public boolean add(String comp, String description, long value, Level recordLevel) {
		return addEntry(comp, description, recordLevel, new StatRecord(comp, description, value, recordLevel));
	}

	public boolean add(String comp, String description, int value, Level recordLevel) {
		return addEntry(comp, description, recordLevel, new StatRecord(comp, description, value, recordLevel));
	}

	public boolean add(String comp, String description, String value, Level recordLevel) {
		return addEntry(comp, description, recordLevel, new StatRecord(comp, description, value, recordLevel));
	}

	public boolean add(String comp, String description, float value, Level recordLevel) {
		return addEntry(comp, description, recordLevel, new StatRecord(comp, description, value, recordLevel));
	}

	public <E extends Number> boolean add(String comp, String description, Collection<E> value, Level recordLevel) {
		return addEntry(comp, description, recordLevel, new StatRecord(comp, description, value, recordLevel));
	}

	private boolean addEntry(String comp, String description, Level recordLevel, StatRecord statRecord) {
		description = description.intern();
		if (checkLevel(recordLevel, statRecord)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

			if (compStats == null) {
				compStats = addCompStats(comp);
			}

			compStats.put(description, statRecord);

			return true;
		}

		return false;
	}

	public LinkedHashMap<String, StatRecord> addCompStats(String comp) {
		LinkedHashMap<String, StatRecord> compStats = new LinkedHashMap<String, StatRecord>();

		stats.put(comp, compStats);

		return compStats;
	}

	public boolean checkLevel(Level recordLevel) {
		return recordLevel.intValue() >= statLevel.intValue();
	}

	public boolean checkLevel(Level recordLevel, long value) {
		return checkLevel(recordLevel) && (value != 0 || checkLevel(Level.FINEST));

	}

	public boolean checkLevel(Level recordLevel, StatRecord record) {
		return checkLevel(recordLevel) && (record.isNonZero() || checkLevel(Level.FINEST));
	}

	public boolean checkLevel(Level recordLevel, int value) {
		return checkLevel(recordLevel) && (value != 0 || checkLevel(Level.FINEST));

	}

	// ~--- get methods ----------------------------------------------------------

	public int getCompConnections(String comp) {
		return getValue(comp, "Open connections", 0);
	}

	public long getCompIq(String comp) {
		return getCompIqSent(comp) + getCompIqReceived(comp);
	}

	public long getCompIqReceived(String comp) {
		return getValue(comp, QueueType.IN_QUEUE.name() + " processed IQ", 0L);
	}

	public long getCompIqSent(String comp) {
		return getValue(comp, QueueType.OUT_QUEUE.name() + " processed IQ", 0L);
	}

	/**
	 * Returns names of every component for which statistics are stored in <code>stats</code> variable
	 * 
	 * @return 
	 */
	public Set<String> getCompNames() {
		return stats.keySet();
	}
	
	public long getCompMsg(String comp) {
		return getCompMsgSent(comp) + getCompMsgReceived(comp);
	}

	public long getCompMsgReceived(String comp) {
		return getValue(comp, QueueType.IN_QUEUE.name() + " processed messages", 0L);
	}

	public long getCompMsgSent(String comp) {
		return getValue(comp, QueueType.OUT_QUEUE.name() + " processed messages", 0L);
	}

	public long getCompPackets(String comp) {
		return getCompSentPackets(comp) + getCompReceivedPackets(comp);
	}

	public long getCompPres(String comp) {
		return getCompPresSent(comp) + getCompPresReceived(comp);
	}

	public long getCompPresReceived(String comp) {
		return getValue(comp, QueueType.IN_QUEUE.name() + " processed presences", 0L);
	}

	public long getCompPresSent(String comp) {
		return getValue(comp, QueueType.OUT_QUEUE.name() + " processed presences", 0L);
	}

	public long getCompReceivedPackets(String comp) {
		return getValue(comp, StatisticType.MSG_RECEIVED_OK.getDescription(), 0L);
	}

	public long getCompSentPackets(String comp) {
		return getValue(comp, StatisticType.MSG_SENT_OK.getDescription(), 0L);
	}

	public LinkedHashMap<String, StatRecord> getCompStats(String comp) {
		return stats.get(comp);
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

	public float getValue(String comp, String description, float def) {
		float result = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

		if (compStats != null) {
			StatRecord rec = compStats.get(description);

			if (rec != null) {
				result = rec.getFloatValue();
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

	public <E> Collection<E> getValue(String comp, String description, Collection<E> def) {
		Collection<E> result = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

		if (compStats != null) {
			StatRecord rec = compStats.get(description);

			if (rec != null) {
				result = rec.getCollection();
			}
		}

		return result;
	}


	public <E> Collection<E> getCollectionValue(String dataId) {
		String dataName = DataTypes.stripNameFromTypeId(dataId);
		int idx = dataName.indexOf('/');
		String comp = dataName.substring(0, idx);
		String descr = dataName.substring(idx + 1);
		return getCollectionValue(comp, descr, null);
	}

	public <E> Collection<E> getCollectionValue(String comp, String description, Collection<E> def) {
		Collection<E> result = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

		if (compStats != null) {
			StatRecord rec = compStats.get(description);

			if (rec != null) {
				result = rec.getCollection();
			}
		}

		return result;
	}

	public Object getValue(String dataId) {
		char dataType = DataTypes.decodeTypeIdFromName(dataId);
		String dataName = DataTypes.stripNameFromTypeId(dataId);
		int idx = dataName.indexOf('/');
		String comp = dataName.substring(0, idx);
		String descr = dataName.substring(idx + 1);
//		log.log(Level.FINEST,
//				"Returning metrics for component: {0}, description: {1} and type: {2}",
//				new Object[] { comp, descr, dataType });
		switch (dataType) {
			case 'L':
				return getValue(comp, descr, 0l);
			case 'I':
				return getValue(comp, descr, 0);
			case 'F':
				return getValue(comp, descr, 0f);
			case 'C':
				return getCollectionValue(comp, descr, null);
			default:
				return getValue(comp, descr, " ");
		}
	}

	// ~--- methods --------------------------------------------------------------

	@Override
	public Iterator<StatRecord> iterator() {
		return new StatsIterator();
	}

	@Override
	public String toString() {
		return stats.toString();
	}

	// ~--- inner classes --------------------------------------------------------

	private class StatsIterator implements Iterator<StatRecord> {
		Iterator<LinkedHashMap<String, StatRecord>> compsIt = stats.values().iterator();
		Iterator<StatRecord> recIt = null;

		// ~--- get methods --------------------------------------------------------

		@Override
		public boolean hasNext() {
			if ((recIt == null) || !recIt.hasNext()) {
				if (compsIt.hasNext()) {
					recIt = compsIt.next().values().iterator();
				} else {
					return false;
				}
			}

			return recIt.hasNext();
		}

		// ~--- methods ------------------------------------------------------------

		@Override
		public StatRecord next() throws NoSuchElementException {
			if ((recIt == null) || !recIt.hasNext()) {
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
