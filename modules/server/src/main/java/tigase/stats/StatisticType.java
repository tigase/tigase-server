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

	QUEUE_WAITING("Total waiting packets"),
	MAX_QUEUE_SIZE("Max queue size"),
	MSG_RECEIVED_OK("Packets received"),
	MSG_SENT_OK("Packets sent"),
	IN_QUEUE_OVERFLOW("IN Queue overflow"),
	OUT_QUEUE_OVERFLOW("OUT Queue overflow"),
	OTHER(null);


	private String description = null;

	private StatisticType(String description) {
		this.description = description;
	}

	public String getDescription() { return description; }

} // StatisticType
