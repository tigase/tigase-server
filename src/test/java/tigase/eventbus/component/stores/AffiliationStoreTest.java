/*
 * AffiliationStoreTest.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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

package tigase.eventbus.component.stores;

import org.junit.Assert;
import org.junit.Test;

import tigase.xmpp.JID;

public class AffiliationStoreTest {

	@Test
	public void testGetAffiliation() throws Exception {
		final AffiliationStore store = new AffiliationStore();
		store.setAllowedSubscribers(new JID[] { JID.jidInstance("a@b.c/d"), JID.jidInstance("x@y.z") });

		Assert.assertEquals(Affiliation.member, store.getAffiliation(JID.jidInstance("a@b.c/d")));
		Assert.assertEquals(Affiliation.member, store.getAffiliation(JID.jidInstance("x@y.z")));
		Assert.assertEquals(Affiliation.none, store.getAffiliation(JID.jidInstance("NONE@y.z")));

		Assert.assertEquals(Affiliation.none, store.getAffiliation(JID.jidInstance("a@b.c")));
		Assert.assertEquals(Affiliation.none, store.getAffiliation(JID.jidInstance("a@b.c/DIFFERENT")));
		Assert.assertEquals(Affiliation.member, store.getAffiliation(JID.jidInstance("x@y.z/any")));

	}
}