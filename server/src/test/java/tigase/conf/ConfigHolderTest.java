/*
 * ConfigHolderTest.java
 *
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
package tigase.conf;

import org.junit.Test;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.server.CmdAcl;
import tigase.xmpp.impl.roster.DynamicRosterTest;
import tigase.xmpp.impl.roster.DynamicRosterTest123;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static tigase.conf.ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY;

/**
 * Created by andrzej on 27.04.2017.
 */
public class ConfigHolderTest {

	@Test
	public void testFormatDetection() throws IOException {
		ConfigHolder holder = new ConfigHolder();

		File tmp = File.createTempFile("test_", ".properties");
		try (Writer w = new FileWriter(tmp)) {
			w.append("--auth-db[domain4.com]=tigase-custom")
					.append("\n")
					.append("--auth-db-uri[domain4.com]=jdbc:mysql://db14.domain4.com/dbname?user&password")
					.append("\n")
					.append("basic-conf/auth-repo-params/domain4.com/user-login-query={ call UserLogin(?, ?) }")
					.append("\n")
					.append("basic-conf/auth-repo-params/domain4.com/user-logout-query={ call UserLogout(?) }")
					.append("\n")
					.append("basic-conf/auth-repo-params/domain4.com/sasl-mechs=PLAIN,DIGEST-MD5")
					.append("\n")
					.append("--user-db[domain4.com]=mysql")
					.append("\n")
					.append("--user-db-uri[domain4.com]=jdbc:mysql://db14.domain4.com/dbname?user&password")
					.append("\n");
		}

		holder.getProperties().put(PROPERTY_FILENAME_PROP_KEY, tmp.getAbsolutePath());

		ConfigHolder.Format format = holder.detectPathAndFormat();

		tmp.delete();
		
		assertEquals(ConfigHolder.Format.properties, format);

		tmp = File.createTempFile("test_", ".properties");
		try (Writer w = new FileWriter(tmp)) {
			w.append("authRepository () {")
					.append("\n")
					.append("'domain4.com' () {")
					.append("\n")
					.append("'user-login-query' = '{ call UserLogin(?, ?) }'")
					.append("\n")
					.append("'user-logout-query' = \"{ call UserLogout(?) }\"")
					.append("\n")
					.append("'sasl-mechs' = 'PLAIN,DIGEST-MD5'")
					.append("\n")
					.append("}")
					.append("\n")
					.append("}")
					.append("\n");
		}

		holder.getProperties().put(PROPERTY_FILENAME_PROP_KEY, tmp.getAbsolutePath());

		format = holder.detectPathAndFormat();

		tmp.delete();

		assertEquals(ConfigHolder.Format.dsl, format);
	}

	@Test
	public void testConversionOfAdHocCommandsACLs() throws IOException {
		ConfigHolder.PropertiesConfigReader reader = new ConfigHolder.PropertiesConfigReader();
		Map<String, Object> props = reader.loadFromPropertyStrings(Arrays.asList(
				new String[]{"sess-man/command/http\\://jabber.org/protocol/admin#add-user=LOCAL",
							 "sess-man/command/http\\://jabber.org/protocol/admin#delete-user=DOMAIN:test.com",
							 "sess-man/command/http\\://jabber.org/protocol/admin#change-user-password=LOCAL",
							 "sess-man/command/http\\://jabber.org/protocol/admin#get-user-roster=JID:ala1@test.com",
							 "sess-man/command/http\\://jabber.org/protocol/admin#user-stats=LOCAL",
							 "sess-man/command/http\\://jabber.org/protocol/admin#get-active-users-num=LOCAL,DOMAIN:test.com",
							 "sess-man/command/http\\://jabber.org/protocol/admin#get-idle-users-num=LOCAL",
							 "sess-man/command/http\\://jabber.org/protocol/admin#get-registered-users-list=JID:ala@test.coms",
							 "sess-man/command/http\\://jabber.org/protocol/admin#get-online-users-list=LOCAL",
							 "sess-man/command/http\\://jabber.org/protocol/admin#get-active-users=DOMAIN:example.com",
							 "sess-man/command/http\\://jabber.org/protocol/admin#get-idle-users=LOCAL",
							 "sess-man/command/http\\://jabber.org/protocol/admin#announce=LOCAL",
							 "sess-man/command/http\\://jabber.org/protocol/admin#add-user-tracker=LOCAL",
							 "sess-man/command/http\\://jabber.org/protocol/admin#get-top-active-users=LOCAL",
							 "sess-man/command/http\\://jabber.org/protocol/admin#remove-user-tracker=LOCAL",
							 "basic-conf/command/user-domain-perm=LOCAL", "sess-man/command/connection-time=LOCAL",
							 "s2s/command/roster-fixer=LOCAL", "sess-man/command/roster-fixer-cluster=LOCAL",
							 "s2s/command/user-roster-management=LOCAL",
							 "c2s/command/user-roster-management-ext=LOCAL"}));

		reader.convertFromOldFormat();

		Map<String, Object> result = ConfigWriter.buildTree(props);
		result.forEach((comp, properties) -> {
			assertTrue(Map.class.isAssignableFrom(properties.getClass()));
			Map<String, Object> map = (Map<String, Object>) properties;
			map.values().forEach(x -> {
				Map<String, Object> map1 = (Map<String, Object>) x;
				map1.values().forEach(y -> {
					if (y instanceof List) {
						((List) y).forEach(z -> {
							assertNotNull(new CmdAcl((String) z));
						});
					} else {
						assertNotNull(new CmdAcl((String) y));
					}
				});
			});
		});
	}

	@Test
	public void testConversionOfDynamicRosterClasses() throws IOException {
		ConfigHolder.PropertiesConfigReader reader = new ConfigHolder.PropertiesConfigReader();
		Map<String, Object> props = reader.loadFromPropertyStrings(Arrays.asList(
				new String[]{"sess-man/plugins-conf/dynamic-roster-classes=tigase.xmpp.impl.roster.DynamicRosterTest,tigase.xmpp.impl.roster.DynamicRosterTest123"}));

		reader.convertFromOldFormat();

		Map<String, Object> result = ConfigWriter.buildTree(props);
		Map<String, Object> sessMan = (Map<String, Object>) result.get("sess-man");
		assertNotNull(sessMan);
		AbstractBeanConfigurator.BeanDefinition dynamicRoster = (AbstractBeanConfigurator.BeanDefinition) sessMan.get("dynamic-rosters");
		assertNotNull(dynamicRoster);
		assertTrue(dynamicRoster.isActive());
		assertNull(dynamicRoster.getClazzName());

		assertFalse(dynamicRoster.isEmpty());

		AbstractBeanConfigurator.BeanDefinition roster = (AbstractBeanConfigurator.BeanDefinition) dynamicRoster.get("DynamicRosterTest");
		assertNotNull(roster);
		assertTrue(roster.isActive());
		assertEquals(DynamicRosterTest.class.getCanonicalName(), roster.getClazzName());

		roster = (AbstractBeanConfigurator.BeanDefinition) dynamicRoster.get("DynamicRosterTest123");
		assertNotNull(roster);
		assertTrue(roster.isActive());
		assertEquals(DynamicRosterTest123.class.getCanonicalName(), roster.getClazzName());
	}

	@Test
	public void testConversionOfGlobalProperties() throws IOException {
		ConfigHolder.PropertiesConfigReader reader = new ConfigHolder.PropertiesConfigReader();
		Map<String, Object> props = reader.loadFromPropertyStrings(Arrays.asList("--max-queue-size=10000"));

		reader.convertFromOldFormat();

		Map<String, Object> result = ConfigWriter.buildTree(props);
		assertEquals(10000, result.get("max-queue-size"));
	}

}
