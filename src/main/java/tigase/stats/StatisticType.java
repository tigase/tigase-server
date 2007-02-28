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

/**
 * Describe class StatisticType here.
 *
 *
 * Created: Wed Nov 23 21:20:20 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public enum StatisticType {

	QUEUE_SIZE("Queue size", "int"),
	MSG_RECEIVED_OK("Packets received", "long"),
	QUEUE_OVERFLOW("Queue overflow", "long"),
	LIST(null, "list"),
	OTHER(null, null);


	private String description = null;
	private String unit = null;

	private StatisticType(String description, String unit) {
		this.description = description;
		this.unit = unit;
	}

	public String getDescription() { return description; }
	public String getUnit() { return unit; }

} // StatisticType
