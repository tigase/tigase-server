/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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
 */

package tigase.stats;

import org.junit.Test;

import java.util.*;
import java.util.logging.Level;

import static org.junit.Assert.*;

public class StatisticsListTest {

	@Test
	public void testNonZeroRecords() {

		final String compName = "comp";

		StatisticsList statRecords = new StatisticsList(Level.INFO);
		statRecords.add(compName, "long", 1L, Level.INFO);
		statRecords.add(compName, "int", 2, Level.INFO);
		statRecords.add(compName, "string", "string", Level.INFO);
		statRecords.add(compName, "float", 3.4F, Level.INFO);

		statRecords.add(compName, "long-zero", 0L, Level.INFO);
		statRecords.add(compName, "int-zero", 0, Level.INFO);
		statRecords.add(compName, "string-zero", "", Level.INFO);
		statRecords.add(compName, "float-zero", 0F, Level.INFO);

		LinkedHashMap<String, StatRecord> compStats = statRecords.getCompStats(compName);
		assertTrue("Wrong collection size!" , compStats.size() == 4);
		assertNotNull(compStats.get("long"));
		assertNotNull(compStats.get("int"));
		assertNotNull(compStats.get("string"));
		assertNotNull(compStats.get("float"));
		assertNull(compStats.get("long-zero"));
		assertNull(compStats.get("int-zero"));
		assertNull(compStats.get("string-zero"));
		assertNull(compStats.get("float-zero"));

		statRecords = new StatisticsList(Level.FINEST);
		statRecords.add(compName, "long", 1L, Level.INFO);
		statRecords.add(compName, "int", 2, Level.INFO);
		statRecords.add(compName, "string", "string", Level.INFO);
		statRecords.add(compName, "float", 3.4F, Level.INFO);

		statRecords.add(compName, "long-zero", 0L, Level.INFO);
		statRecords.add(compName, "int-zero", 0, Level.INFO);
		statRecords.add(compName, "string-zero", "", Level.INFO);
		statRecords.add(compName, "float-zero", 0F, Level.INFO);

		compStats = statRecords.getCompStats(compName);
		assertTrue("Wrong collection size!" , compStats.size() == 8);

		assertNotNull(compStats.get("long"));
		assertNotNull(compStats.get("int"));
		assertNotNull(compStats.get("string"));
		assertNotNull(compStats.get("float"));
		assertNotNull(compStats.get("long-zero"));
		assertNotNull(compStats.get("int-zero"));
		assertNotNull(compStats.get("string-zero"));
		assertNotNull(compStats.get("float-zero"));


	}

}