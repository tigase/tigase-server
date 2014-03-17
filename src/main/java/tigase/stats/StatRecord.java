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

import java.util.logging.Level;
import java.util.List;

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

	private String description = null;
	private String unit = null;
	private String value = null;
	private List<String> listValue = null;
	private String component = null;

	public StatRecord(String comp, String description, String unit,	String value,
		Level level) {
		this.description = description;
		this.unit = unit;
		this.value = value;
		this.level = level;
		this.component = comp;
	}

	public StatRecord(String comp, String description, String unit, int value,
		Level level) {
		this(comp, description, unit, "" + value, level);
		this.intValue = value;
	}

	public StatRecord(String comp, StatisticType type, long value, Level level) {
		this(comp, type.getDescription(), type.getUnit(), "" + value, level);
		this.type = type;
		this.longValue = value;
	}

	public StatRecord(String comp, StatisticType type, int value, Level level) {
		this(comp, type.getDescription(), type.getUnit(), "" + value, level);
		this.type = type;
		this.intValue = value;
	}

	public StatRecord(String comp, String description, List<String> value, Level level) {
		this(comp, description, StatisticType.LIST.getUnit(), null, level);
		this.type = StatisticType.LIST;
		this.listValue = value;
	}

	public StatRecord(String comp, String description, String unit, long value,
		Level level) {
		this(comp, description, unit, "" + value, level);
		this.longValue = value;
	}

	StatRecord(String comp, String description, String unit, float value, Level level) {
		this(comp, description, unit, "" + value, level);
		this.floatValue = value;
	}

	public String getDescription() {
		return description;
	}

	public String getUnit() {
		return unit;
	}

	public String getValue() {
		return value;
	}

	public StatisticType getType() {
		return type;
	}

	public List<String> getListValue() {
		return listValue;
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(component).append('/').append(description);
		sb.append('[').append(unit).append(']').append(" = ").append(value);
		return sb.toString();
	}

	float getFloatValue() {
		return this.floatValue;
	}

} // StatRecord
