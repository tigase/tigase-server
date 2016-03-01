/*
 * ReflectEventListenerHandlerFactoryTest.java
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

package tigase.eventbus.impl;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by bmalkow on 26.01.2016.
 */
public class ReflectEventListenerHandlerFactoryTest {

	@Test
	public void testCreate() throws Exception {
		final ReflectEventListenerHandlerFactory f = new ReflectEventListenerHandlerFactory();
		EventBusImplementationTest.Consumer c = new EventBusImplementationTest.Consumer();
		Collection<AbstractHandler> handlers1 = f.create(c);
		Collection<AbstractHandler> handlers2 = f.create(c);

		Assert.assertEquals(handlers1.size(), handlers2.size());
		Assert.assertEquals(handlers1, handlers2);
	}
}