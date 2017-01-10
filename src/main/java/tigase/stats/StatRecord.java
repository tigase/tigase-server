/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

import java.util.Collection;
import java.util.logging.Level;

/**
 * Describe class StatRecord here.
 *
 *
 * Created: Wed Nov 23 21:28:53 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StatRecord {

	private StatisticType type = StatisticType.OTHER;
	private Level level = Level.INFO;
	private long longValue = -1;
 	private int intValue = -1;
	private float floatValue = -1f;
	private Collection collection = null;

	private String description = null;
	private String value = null;
	private String component = null;

	public StatRecord(String comp, String description, String value, Level level) {
		this.description = description.intern();
		if (value != null) {
			this.value = value.intern();
		}
		this.level = level;
		this.component = comp.intern();
	}

	public StatRecord(String comp, String description, int value,
		Level level) {
		this(comp, description, "" + value, level);
		this.intValue = value;
	}

	public StatRecord(String comp, StatisticType type, long value, Level level) {
		this(comp, type.getDescription(), "" + value, level);
		this.type = type;
		this.longValue = value;
	}

	public StatRecord(String comp, StatisticType type, int value, Level level) {
		this(comp, type.getDescription(), "" + value, level);
		this.type = type;
		this.intValue = value;
	}

	public StatRecord(String comp, String description, long value,
		Level level) {
		this(comp, description, "" + value, level);
		this.longValue = value;
	}

	StatRecord(String comp, String description, float value, Level level) {
		this(comp, description, "" + value, level);
		this.floatValue = value;
	}

	StatRecord(String comp, String description, Collection value, Level level) {
		this(comp, description, (value != null ? value.toString() : ""), level);
		this.collection = value;
	}

	public String getDescription() {
		return description;
	}

	public String getValue() {
		return value;
	}

	public StatisticType getType() {
		return type;
	}

	public Level getLevel() {
		return level;
	}

	public String getComponent() {
		return component;
	}

	public long getLongValue() {
		return this.longValue;
	}

	public int getIntValue() {
		return this.intValue;
	}

	public <E> Collection<E> getCollection() { return this.collection; };

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(component).append('/').append(description);

		sb.append('[');
		if (longValue > -1) { sb.append('L');
		} else if (intValue > -1) { sb.append('I');
		} else if (floatValue > -1f) { sb.append('F');
		} else if (collection != null) { sb.append('C');
		} else { sb.append('S'); }
		sb.append(']');

		sb.append(" = ").append(value);
		return sb.toString();
	}

	float getFloatValue() {
		return this.floatValue;
	}

} // StatRecord
