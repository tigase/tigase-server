/*
 * S2SConnectionManagerDomainServerNameMapperTest.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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
package tigase.server.xmppserver;

import junit.framework.TestCase;
import org.junit.Test;
import tigase.server.xmppserver.S2SConnectionManager.DomainServerNameMapper;

/**
 *
 * @author andrzej
 */
public class S2SConnectionManagerDomainServerNameMapperTest extends TestCase {
	
	@Test
	public void testSortingOfMappings() {
		DomainServerNameMapper mapper = new DomainServerNameMapper();
		
		// adding mapping entries
		mapper.addEntry("*", "test1");
		mapper.addEntry("*.local", "test2");
		mapper.addEntry("*.test", "test3");
		mapper.addEntry("*.test.local", "test4");
		mapper.addEntry("test", "test5");
		mapper.addEntry("local", "test6");
		mapper.addEntry("test1.test", "test7");
		mapper.addEntry("test1.test.local", "test8");
		
		//System.out.println(mapper.toString());
		
		// checking assertions to make sure that due to sorting of mappings
		// mappings are used in proper order
		assertEquals("test1", mapper.getServerNameForDomain("tigase.org"));
		assertEquals("test4", mapper.getServerNameForDomain("test.local"));
		assertEquals("test2", mapper.getServerNameForDomain("test1.local"));
		assertEquals("test5", mapper.getServerNameForDomain("test"));
		assertEquals("test6", mapper.getServerNameForDomain("local"));
		assertEquals("test3", mapper.getServerNameForDomain("test.test"));
		assertEquals("test4", mapper.getServerNameForDomain("test.test.local"));
		assertEquals("test7", mapper.getServerNameForDomain("test1.test"));
		assertEquals("test8", mapper.getServerNameForDomain("test1.test.local"));
	}
	
}
