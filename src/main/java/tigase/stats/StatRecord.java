/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
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
package tigase.stats;

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

	private String description = null;
	private String unit = null;
	private String value = null;
	private String component = null;

	/**
	 * Creates a new <code>StatRecord</code> instance.
	 *
	 * @param type a <code>StatisticType</code> value
	 * @param value a <code>long</code> value
	 */
	public StatRecord(String comp, StatisticType type, long value, Level level) {
		this.type = type;
		longValue = value;
		this.description = type.getDescription();
		this.unit = type.getUnit();
		this.value = "" + value;
		this.level = level;
		this.component = comp;
	}

	public StatRecord(String comp, StatisticType type, int value, Level level) {
		this.type = type;
		intValue = value;
		this.description = type.getDescription();
		this.unit = type.getUnit();
		this.value = "" + value;
		this.level = level;
		this.component = comp;
	}

	public StatRecord(String comp, String description, String unit, int value,
		Level level) {
		this.description = description;
		this.unit = unit;
		intValue = value;
		this.value = "" + value;
		this.level = level;
		this.component = comp;
	}

	public StatRecord(String comp, String description, String unit, long value,
		Level level) {
		this.description = description;
		this.unit = unit;
		longValue = value;
		this.value = "" + value;
		this.level = level;
		this.component = comp;
	}

	public StatRecord(String comp, String description, String unit,	String value,
		Level level) {
		this.description = description;
		this.unit = unit;
		this.value = value;
		this.level = level;
		this.component = comp;
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

	public Level getLevel() {
		return level;
	}

	public String getComponent() {
		return component;
	}

} // StatRecord
