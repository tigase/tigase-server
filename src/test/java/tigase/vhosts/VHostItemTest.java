/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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
package tigase.vhosts;

import tigase.util.TigaseStringprepException;
import tigase.xml.Element;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 *
 * @author Wojciech Kapcia <wojciech.kapcia@tigase.org>
 */
public class VHostItemTest extends TestCase {

	public void testVHostItem() throws TigaseStringprepException {
		assertEquals( new VHostItem( "lowercase.com" ), new VHostItem( "lowercase.com" ) );
		assertEquals( new VHostItem( "CAPITAL.COM" ), new VHostItem( "capital.com" ) );
		assertNotSame( new VHostItem( "CAPITAL.COM" ), new VHostItem( "lowercase.com" ) );

	}

	public void testVHostDomainPolicy() throws TigaseStringprepException {

		Element el;
		VHostItem vHostItem;

		vHostItem = new VHostItem();
		vHostItem.initFromPropertyString( "domain1:domain-filter=LOCAL:max-users=1000" );
		assertEquals( DomainFilterPolicy.LOCAL, vHostItem.getDomainFilter() );
		assertTrue( vHostItem.getDomainFilterDomains() == null );

		vHostItem = new VHostItem();
		vHostItem.initFromPropertyString( "domain1:domain-filter=LIST=domain1;domain2;domain3:max-users=1000" );
		assertEquals( DomainFilterPolicy.LIST, vHostItem.getDomainFilter() );
		assertTrue( Arrays.asList( vHostItem.getDomainFilterDomains() ).contains( "domain1" ) );
		assertTrue( Arrays.asList( vHostItem.getDomainFilterDomains() ).contains( "domain3" ) );
		assertFalse( Arrays.asList( vHostItem.getDomainFilterDomains() ).contains( "domain5" ) );

		vHostItem = new VHostItem();
		el = new Element( "vhost",
											new String[] { "hostname", "domain-filter", "domain-filter-domains" },
											new String[] { "domain3", "ALL", "domain1;domain2;domain3" }
		);

		vHostItem.initFromElement( el );

		assertEquals( DomainFilterPolicy.ALL, vHostItem.getDomainFilter() );
		assertTrue( vHostItem.getDomainFilterDomains() == null );
		assertTrue( vHostItem.toPropertyString().contains( "domain-filter=ALL" ) );

		vHostItem = new VHostItem();
		el = new Element( "vhost",
											new String[] { "hostname", "domain-filter", "domain-filter-domains" },
											new String[] { "domain3", "BLACKLIST", "domain1;domain2;domain3" }
		);

		vHostItem.initFromElement( el );
		assertEquals( DomainFilterPolicy.BLACKLIST, vHostItem.getDomainFilter() );
		assertTrue( Arrays.asList( vHostItem.getDomainFilterDomains() ).contains( "domain1" ) );
		assertTrue( Arrays.asList( vHostItem.getDomainFilterDomains() ).contains( "domain3" ) );
		assertFalse( Arrays.asList( vHostItem.getDomainFilterDomains() ).contains( "domain5" ) );
		assertTrue( vHostItem.toPropertyString().contains( "domain-filter=BLACKLIST" ) );

	}

}
