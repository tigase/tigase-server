/*
 * ConfigHelperTest.java
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
 *
 */
package tigase.conf;

import org.junit.Test;
import tigase.cluster.ClientConnectionClustered;
import tigase.cluster.SessionManagerClustered;
import tigase.cluster.strategy.DefaultClusteringStrategy;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.server.xmppclient.ClientConnectionManager;
import tigase.server.xmppclient.StreamManagementIOProcessor;
import tigase.xmpp.impl.JabberIqRegister;
import tigase.xmpp.impl.SaslAuth;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by andrzej on 08.11.2016.
 */
public class ConfigHelperTest {

	@Test
	public void test_merge() {
		Map<String, Object> prop1 = new HashMap<>();
		// setting global properties
		prop1.put("test1", "123");
		prop1.put("test2", "456");

		AbstractBeanConfigurator.BeanDefinition bean1 = new AbstractBeanConfigurator.BeanDefinition();
		bean1.setBeanName("sess-man");
		bean1.setClazzName(SessionManagerClustered.class.getCanonicalName());
		bean1.setActive(true);

		bean1.put("plugins", Arrays.asList(JabberIqRegister.ID, SaslAuth.ID));
		bean1.computeIfAbsent("map", o -> {
			HashMap<String, Object> tmp = new HashMap<>();
			tmp.put("left", "right");
			return tmp;
		});
		bean1.computeIfAbsent("strategy", name -> {
			AbstractBeanConfigurator.BeanDefinition strategyBean = new AbstractBeanConfigurator.BeanDefinition();
			strategyBean.setBeanName((String) name);
			strategyBean.setClazzName(DefaultClusteringStrategy.class.getCanonicalName());
			strategyBean.setActive(true);
			strategyBean.put("force", true);
			return strategyBean;
		});

		prop1.put(bean1.getBeanName(), bean1);

		AbstractBeanConfigurator.BeanDefinition bean2 = new AbstractBeanConfigurator.BeanDefinition();
		bean2.setBeanName("c2s");
		bean2.setClazzName(ClientConnectionManager.class.getCanonicalName());
		bean2.setActive(true);

		bean2.put("processors", Arrays.asList(StreamManagementIOProcessor.XMLNS));

		prop1.put(bean2.getBeanName(), bean2);

		Map<String, Object> prop2 = new HashMap<>();
		prop2.put("test3", 789);

		prop2.computeIfAbsent("sess-man", s -> {
			Map<String, Object> sessMan = new HashMap<>();
			sessMan.put("plugins", Arrays.asList("presence-state", "presence-subscription"));
			sessMan.computeIfAbsent("map", o -> {
				HashMap<String, Object> tmp = new HashMap<>();
				tmp.put("up", "down");
				return tmp;
			});
			sessMan.computeIfAbsent("strategy", name -> {
				Map<String, Object> sMap = new HashMap<>();
				sMap.put("force", false);
				return sMap;
			});
			return sessMan;
		});

		AbstractBeanConfigurator.BeanDefinition bean3 = new AbstractBeanConfigurator.BeanDefinition();
		bean3.setBeanName("c2s");
		bean3.setClazzName(ClientConnectionClustered.class.getCanonicalName());
		bean3.setActive(true);

		bean3.put("test4", 987);

		prop2.put(bean3.getBeanName(), bean3);

		Map<String, Object> result = ConfigHelper.merge(prop1, prop2);

		assertEquals("123", result.get("test1"));
		assertEquals("456", result.get("test2"));

		AbstractBeanConfigurator.BeanDefinition bean1a = ((AbstractBeanConfigurator.BeanDefinition) result.get(bean1.getBeanName()));
		assertEquals(bean1.getClazzName(), bean1a.getClazzName());
		assertEquals(bean1.isActive(), bean1a.isActive());
		assertEquals(((Map) prop2.get("sess-man")).get("plugins"), bean1a.get("plugins"));
		assertEquals("right", ((Map) bean1a.get("map")).get("left"));
		assertEquals("down", ((Map) bean1a.get("map")).get("up"));

		AbstractBeanConfigurator.BeanDefinition sBean = (AbstractBeanConfigurator.BeanDefinition) bean1a.get("strategy");
		assertEquals(DefaultClusteringStrategy.class.getCanonicalName(), sBean.getClazzName());
		assertFalse((Boolean) sBean.get("force"));

		AbstractBeanConfigurator.BeanDefinition bean2a = (AbstractBeanConfigurator.BeanDefinition) result.get(bean2.getBeanName());
		assertEquals(bean3.getClazzName(), bean2a.getClazzName());
		assertEquals(bean3.isActive(), bean2a.isActive());
		assertEquals(987, bean2a.get("test4"));
		assertNull(bean2a.get("test3"));
	}

}
