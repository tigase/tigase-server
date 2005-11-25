/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
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

/**
 * Describe class StatRecord here.
 *
 *
 * Created: Wed Nov 23 21:28:53 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StatRecord {

	private StatisticType type = null;
	private long longValue = -1;
	private int intValue = -1;

	private String description = null;
	private String unit = null;

	/**
	 * Creates a new <code>StatRecord</code> instance.
	 *
	 * @param type a <code>StatisticType</code> value
	 * @param value a <code>long</code> value
	 */
	public StatRecord(StatisticType type, long value) {
		this.type = type;
		longValue = value;
	}

	public StatRecord(StatisticType type, int value) {
		this.type = type;
		intValue = value;
	}

	public StatRecord(String description, String unit, int value) {
		this.description = description;
		this.unit = unit;
		intValue = value;
	}

} // StatRecord
