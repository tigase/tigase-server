/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigFieldType;
import tigase.kernel.core.Kernel;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

public class DSLBeanConfiguratorTest {

	@Test
	public void dumpConfiguration() throws IOException {

		final Kernel kernel = new Kernel();
		kernel.registerBean(DefaultTypesConverter.class).exec();
		kernel.registerBean(DSLBeanConfigurator.class).exec();
		final HashMap<String, Object> props = new HashMap<>();
		final HashMap<String, Object> testBeanProps = new HashMap<>();
		final String plainFieldValue = "PlainFieldValue";
		final String jdbcUrlPassword = "tigase_password";
		testBeanProps.put("plainConfigField", plainFieldValue);
		testBeanProps.put("jdbcUrl",
						  "jdbc:postgresql://localhost/tigasedb?user=tigasedb&password=" + jdbcUrlPassword + "&useSSL=false");
		final String passwordFieldValue = "PasswordFieldValue";
		testBeanProps.put("passwordField", passwordFieldValue);
		props.put("TestBean", testBeanProps);
		final DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
		configurator.setProperties(props);

		kernel.registerBean(TestBean.class).exec();

		final StringWriter stringWriter = new StringWriter();

		configurator.dumpConfiguration(stringWriter);

		Assert.assertTrue(stringWriter.toString().contains(plainFieldValue));
		Assert.assertFalse(stringWriter.toString().contains(jdbcUrlPassword));
		Assert.assertFalse(stringWriter.toString().contains(passwordFieldValue));
	}

	@Bean(name = "TestBean", active = true, parent = Kernel.class)
	public static class TestBean {

		@ConfigField(desc = "JDBC URL field that should have only password obfuscated", type = ConfigFieldType.JdbcUrl)
		String jdbcUrl;
		@ConfigField(desc = "Password field that should be completely obfuscated", type = ConfigFieldType.Password)
		String passwordField;
		@ConfigField(desc = "Plain field without obfuscation")
		String plainConfigField;

	}
}