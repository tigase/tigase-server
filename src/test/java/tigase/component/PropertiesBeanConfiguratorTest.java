/*
 * PropertiesBeanConfiguratorTest.java
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

package tigase.component;

import org.junit.Assert;
import org.junit.Test;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.core.Kernel;
import tigase.xmpp.JID;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bmalkow on 19.10.2015.
 */
public class PropertiesBeanConfiguratorTest {

	@Test
	public void testConfigure() throws Exception {
		Map<String, Object> props = new HashMap<>();
		System.setProperty("bean-config-access-to-all", "true");
		props.put("bean1/field1", "abc");
		props.put("bean1/field2", "124");
		props.put("bean1/field3", "a1@b.c/d");
		props.put("alias1", "a0@b.c/d");
		props.put("alias2", "a2@b.c/d");


		Kernel k = new Kernel("test");
		k.registerBean(DefaultTypesConverter.class).exec();
		k.registerBean(PropertiesBeanConfigurator.class).exec();
		k.registerBean(Bean1.class).exec();


		k.getInstance(PropertiesBeanConfigurator.class).setProperties(props);


		Assert.assertNull(k.getInstance(Bean1.class).getField1());
		Assert.assertEquals(124, k.getInstance(Bean1.class).getField2());
		Assert.assertEquals(JID.jidInstanceNS("a1@b.c/d"), k.getInstance(Bean1.class).getField3());
		Assert.assertEquals(JID.jidInstanceNS("a2@b.c/d"), k.getInstance(Bean1.class).getField4());
	}

	@Test
	public void testConfigure2() throws Exception {
		Map<String, Object> props = new HashMap<>();
		System.setProperty("bean-config-access-to-all", "true");
		props.put("bean1/field1", "abc");
		props.put("bean1/field2", "124");
		props.put("bean1/field3", "a1@b.c/d");
		props.put("alias1", "a0@b.c/d");
		props.put("alias2", "a2@b.c/d");


		Kernel k = new Kernel("test");
		k.registerBean(DefaultTypesConverter.class).exec();
		k.registerBean(PropertiesBeanConfigurator.class).exec();
		k.registerBean(Bean1.class).exec();

		k.getInstance(PropertiesBeanConfigurator.class).setAccessToAllFields(true);
		k.getInstance(PropertiesBeanConfigurator.class).setProperties(props);


		Assert.assertEquals("abc", k.getInstance(Bean1.class).getField1());
		Assert.assertEquals(124, k.getInstance(Bean1.class).getField2());
		Assert.assertEquals(JID.jidInstanceNS("a1@b.c/d"), k.getInstance(Bean1.class).getField3());
		Assert.assertEquals(JID.jidInstanceNS("a2@b.c/d"), k.getInstance(Bean1.class).getField4());
	}
}