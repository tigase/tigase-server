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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.junit.Test;

import static tigase.vhosts.VHostJDBCRepository.*;

public class VHostJDBCRepositoryTest extends TestCase {

	VHostJDBCRepository vHostJDBCRepository;
	String domain = "domain.com";

	@Override
	protected void setUp() throws Exception {
		Map<String, Object> properties = new HashMap<>();
		properties.put( DOMAINS_PER_USER_LIMIT_PROP_KEY, DOMAINS_PER_USER_LIMIT_PROP_VAL );
		vHostJDBCRepository = new VHostJDBCRepository();
		vHostJDBCRepository.setProperties(properties);
	}

	@Test
	public void testDomainNameCases() throws TigaseStringprepException {
		VHostItem vHostItem = new VHostItem( domain );
		vHostJDBCRepository.addItem( vHostItem );
		assertEquals( vHostItem, vHostJDBCRepository.getItem( domain.toUpperCase() ) );
	}
}
