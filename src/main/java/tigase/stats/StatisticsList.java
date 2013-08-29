/*
 * StatisticsList.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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



package tigase.stats;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.QueueType;

import tigase.util.DataTypes;

//~--- JDK imports ------------------------------------------------------------

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.NoSuchElementException;

/**
 * Created: Jul 10, 2009 3:23:23 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StatisticsList
				implements Iterable<StatRecord> {
	private static final Logger log = Logger.getLogger(StatisticsList.class.getName());

	//~--- fields ---------------------------------------------------------------

	private Level                                                    statLevel = Level.ALL;
	private LinkedHashMap<String, LinkedHashMap<String, StatRecord>> stats =
			new LinkedHashMap<String, LinkedHashMap<String, StatRecord>>();

	//~--- constructors ---------------------------------------------------------

	// ~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param level
	 */
	public StatisticsList(Level level) {
		this.statLevel = level;
	}

	//~--- methods --------------------------------------------------------------

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 * @param description
	 * @param value
	 * @param recordLevel
	 *
	 *
	 *
	 * @return a value of boolean
	 */
	public boolean add(String comp, String description, long value, Level recordLevel) {
		if (checkLevel(recordLevel, value)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

			if (compStats == null) {
				compStats = addCompStats(comp);
			}
			compStats.put(description, new StatRecord(comp, description, "long", value,
					recordLevel));

			return true;
		}

		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 * @param description
	 * @param value
	 * @param recordLevel
	 *
	 *
	 *
	 * @return a value of boolean
	 */
	public boolean add(String comp, String description, int value, Level recordLevel) {
		if (checkLevel(recordLevel, value)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

			if (compStats == null) {
				compStats = addCompStats(comp);
			}
			compStats.put(description, new StatRecord(comp, description, "int", value,
					recordLevel));

			return true;
		}

		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 * @param description
	 * @param value
	 * @param recordLevel
	 *
	 *
	 *
	 * @return a value of boolean
	 */
	public boolean add(String comp, String description, String value, Level recordLevel) {
		if (checkLevel(recordLevel)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

			if (compStats == null) {
				compStats = addCompStats(comp);
			}
			compStats.put(description, new StatRecord(comp, description, "String", value,
					recordLevel));

			return true;
		}

		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 * @param description
	 * @param value
	 * @param recordLevel
	 *
	 *
	 *
	 * @return a value of boolean
	 */
	public boolean add(String comp, String description, float value, Level recordLevel) {
		if (checkLevel(recordLevel)) {
			LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

			if (compStats == null) {
				compStats = addCompStats(comp);
			}
			compStats.put(description, new StatRecord(comp, description, "float", value,
					recordLevel));

			return true;
		}

		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of LinkedHashMap<String,StatRecord>
	 */
	public LinkedHashMap<String, StatRecord> addCompStats(String comp) {
		LinkedHashMap<String, StatRecord> compStats = new LinkedHashMap<String, StatRecord>();

		stats.put(comp, compStats);

		return compStats;
	}

	/**
	 * Method description
	 *
	 *
	 * @param recordLevel
	 *
	 *
	 *
	 * @return a value of boolean
	 */
	public boolean checkLevel(Level recordLevel) {
		return recordLevel.intValue() >= statLevel.intValue();
	}

	/**
	 * Method description
	 *
	 *
	 * @param recordLevel
	 * @param value
	 *
	 *
	 *
	 * @return a value of boolean
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @param recordLevel
	 * @param value
	 *
	 *
	 *
	 * @return a value of boolean
	 */
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

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of Iterator<StatRecord>
	 */
	@Override
	public Iterator<StatRecord> iterator() {
		return new StatsIterator();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of String
	 */
	@Override
	public String toString() {
		return stats.toString();
	}

	//~--- get methods ----------------------------------------------------------

	// ~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of int
	 */
	public int getCompConnections(String comp) {
		return getValue(comp, "Open connections", 0);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompIq(String comp) {
		return getCompIqSent(comp) + getCompIqReceived(comp);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompIqReceived(String comp) {
		return getValue(comp, QueueType.IN_QUEUE.name() + " IQ", 0L);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompIqSent(String comp) {
		return getValue(comp, QueueType.OUT_QUEUE.name() + " IQ", 0L);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompMsg(String comp) {
		return getCompMsgSent(comp) + getCompMsgReceived(comp);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompMsgReceived(String comp) {
		return getValue(comp, QueueType.IN_QUEUE.name() + " messages", 0L);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompMsgSent(String comp) {
		return getValue(comp, QueueType.OUT_QUEUE.name() + " messages", 0L);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompPackets(String comp) {
		return getCompSentPackets(comp) + getCompReceivedPackets(comp);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompPres(String comp) {
		return getCompPresSent(comp) + getCompPresReceived(comp);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompPresReceived(String comp) {
		return getValue(comp, QueueType.IN_QUEUE.name() + " presences", 0L);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompPresSent(String comp) {
		return getValue(comp, QueueType.OUT_QUEUE.name() + " presences", 0L);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompReceivedPackets(String comp) {
		return getValue(comp, StatisticType.MSG_RECEIVED_OK.getDescription(), 0L);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getCompSentPackets(String comp) {
		return getValue(comp, StatisticType.MSG_SENT_OK.getDescription(), 0L);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 *
	 *
	 *
	 * @return a value of LinkedHashMap<String,StatRecord>
	 */
	public LinkedHashMap<String, StatRecord> getCompStats(String comp) {
		return stats.get(comp);
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 * @param description
	 * @param def
	 *
	 *
	 *
	 * @return a value of long
	 */
	public long getValue(String comp, String description, long def) {
		long                              result    = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

		if (compStats != null) {
			StatRecord rec = compStats.get(description);

			if (rec != null) {
				result = rec.getLongValue();
			}
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 * @param description
	 * @param def
	 *
	 *
	 *
	 * @return a value of float
	 */
	public float getValue(String comp, String description, float def) {
		float                             result    = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

		if (compStats != null) {
			StatRecord rec = compStats.get(description);

			if (rec != null) {
				result = rec.getFloatValue();
			}
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 * @param description
	 * @param def
	 *
	 *
	 *
	 * @return a value of int
	 */
	public int getValue(String comp, String description, int def) {
		int                               result    = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

		if (compStats != null) {
			StatRecord rec = compStats.get(description);

			if (rec != null) {
				result = rec.getIntValue();
			}
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 * @param description
	 * @param def
	 *
	 *
	 *
	 * @return a value of String
	 */
	public String getValue(String comp, String description, String def) {
		String                            result    = def;
		LinkedHashMap<String, StatRecord> compStats = stats.get(comp);

		if (compStats != null) {
			StatRecord rec = compStats.get(description);

			if (rec != null) {
				result = rec.getValue();
			}
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param dataId
	 *
	 * @return a value of Object
	 */
	public Object getValue(String dataId) {
		char   dataType = DataTypes.decodeTypeIdFromName(dataId);
		String dataName = DataTypes.stripNameFromTypeId(dataId);
		int    idx      = dataName.indexOf('/');
		String comp     = dataName.substring(0, idx);
		String descr    = dataName.substring(idx + 1);

		log.log(Level.FINER,
				"Returning metrics for component: {0}, description: {1} and type: {2}",
				new Object[] { comp,
				descr, dataType });
		switch (dataType) {
		case 'L' :
			return getValue(comp, descr, 0l);

		case 'I' :
			return getValue(comp, descr, 0);

		case 'F' :
			return getValue(comp, descr, 0f);

		default :
			return getValue(comp, descr, " ");
		}
	}

	//~--- inner classes --------------------------------------------------------

	// ~--- inner classes --------------------------------------------------------
	private class StatsIterator
					implements Iterator<StatRecord> {
		Iterator<LinkedHashMap<String, StatRecord>> compsIt = stats.values().iterator();
		Iterator<StatRecord>                        recIt   = null;

		//~--- methods ------------------------------------------------------------

		// ~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 *
		 * @return a value of StatRecord
		 * @throws NoSuchElementException
		 */
		@Override
		public StatRecord next() throws NoSuchElementException {
			if ((recIt == null) ||!recIt.hasNext()) {
				if (compsIt.hasNext()) {
					recIt = compsIt.next().values().iterator();
				} else {
					throw new NoSuchElementException("No more statistics.");
				}
			}

			return recIt.next();
		}

		/**
		 * Method description
		 *
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		//~--- get methods --------------------------------------------------------

		// ~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 *
		 *
		 * @return a value of boolean
		 */
		@Override
		public boolean hasNext() {
			if ((recIt == null) ||!recIt.hasNext()) {
				if (compsIt.hasNext()) {
					recIt = compsIt.next().values().iterator();
				} else {
					return false;
				}
			}

			return recIt.hasNext();
		}
	}
}



// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com


//~ Formatted in Tigase Code Convention on 13/08/28
