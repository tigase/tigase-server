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

import junit.framework.TestCase;

/**
 *
 * @author Wojciech Kapcia <wojciech.kapcia@tigase.org>
 */
public class VHostItemTest extends TestCase {

	public void testVHostItem() throws TigaseStringprepException {
		assertEquals(new VHostItem("lowercase.com"),new VHostItem("lowercase.com"));
		assertEquals(new VHostItem("CAPITAL.COM"),new VHostItem("capital.com"));
		assertNotSame(new VHostItem("CAPITAL.COM"),new VHostItem("lowercase.com"));
	}


}