/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
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
package tigase.vhosts.filter;

import org.junit.Test;
import tigase.TestLogger;

import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Wojtek
 */
public class DomainFilterPolicyTest {

	private static final Logger log = TestLogger.getLogger(DomainFilterPolicyTest.class);

	public DomainFilterPolicyTest() {
	}


	@Test
	public void testValuePoliciesWithDomainListStr() {
		log.log(Level.FINE, "valuePoliciesWithDomainListStr" );
		HashSet<String> result = DomainFilterPolicy.valuePoliciesWithDomainListStr();
		assertEquals( true, result.contains( DomainFilterPolicy.CUSTOM.name()) );
		assertEquals( true, result.contains( DomainFilterPolicy.LIST.name()) );
		assertEquals( true, result.contains( DomainFilterPolicy.BLACKLIST.name()) );
		assertEquals( false, result.contains( DomainFilterPolicy.ALL.name()) );
		assertEquals( false, result.contains( DomainFilterPolicy.BLOCK.name()) );
		assertEquals( false, result.contains( DomainFilterPolicy.LOCAL.name()) );
		assertEquals( false, result.contains( DomainFilterPolicy.OWN.name()) );
	}

	@Test
	public void testIsDomainListRequired() {
		log.log(Level.FINE,  "isDomainListRequired" );
		assertEquals( false, DomainFilterPolicy.ALL.isDomainListRequired() );
		assertEquals( true, DomainFilterPolicy.BLACKLIST.isDomainListRequired() );
		assertEquals( false, DomainFilterPolicy.BLOCK.isDomainListRequired() );
		assertEquals( true, DomainFilterPolicy.CUSTOM.isDomainListRequired() );
		assertEquals( true, DomainFilterPolicy.LIST.isDomainListRequired() );
		assertEquals( false, DomainFilterPolicy.LOCAL.isDomainListRequired() );
		assertEquals( false, DomainFilterPolicy.OWN.isDomainListRequired() );


	}

}
