/**
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
package tigase.xmpp.impl;

import junit.framework.TestCase;
import org.junit.Test;
import tigase.db.TigaseDBException;
import tigase.kernel.BeanUtils;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.DependencyManager;
import tigase.xmpp.jid.JID;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Test class for JabberIqPrivacyTest
 * <br>
 * Currently class tests validateList method checking validation of type, subscription and action. Other cases are not
 * tested due to missing instance of XMPPResourceConnection
 */
public class JabberIqRegisterWhitelistTest
		extends TestCase {

	private static final String CONNECTION_ID = connectionId("127.0.0.2");

	private static final String connectionId(String remoteAddress) {
		return "c2s@0123456789/127.0.0.1_5222_" + remoteAddress + "_50123";
	}

	@Test
	public void testRegistrationAllowedDefaultSettings() throws Exception {
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		assertTrue(jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(CONNECTION_ID)));
	}

	@Test
	public void testRegistrationAllowedNotInBlacklist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.REGISTRATION_BLACKLIST_PROP_KEY, "127.0.0.3,127.0.0.4");

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertTrue(jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(CONNECTION_ID)));
	}

	@Test
	public void testRegistrationAllowedInWhitelist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, "127.0.0.2,127.0.0.3");

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertTrue(jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(CONNECTION_ID)));
	}

	@Test
	public void testRegistrationAllowedInCIDRWhitelist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, "127.0.0.0/24");

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertTrue(jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(CONNECTION_ID)));
	}

	@Test
	public void testRegistrationAllowedInCIDRMultipleWhitelist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, "127.0.0.0/24,127.0.0.1/24");

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertTrue(jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(CONNECTION_ID)));
	}

	@Test
	public void testRegistrationFromMultipleSourcesAllowedInCIDRWhitelist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, "127.0.0.0/24");

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		for (int i = 0; i <= 255; i++) {
			assertTrue(
					jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(connectionId("127.0.0." + i))));
		}
	}

	@Test
	public void testRegistrationNotAllowedWhitelistOnlyEmpty() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, Boolean.TRUE.toString());

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(CONNECTION_ID)));
	}

	@Test
	public void testRegistrationNotAllowedWhitelistOnly() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, "127.0.0.3,127.0.0.4");

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(CONNECTION_ID)));
	}

	@Test
	public void testRegistrationNotAllowedCIDRWhitelistOnly() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, "127.0.0.0/24");

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		for (int i = 0; i <= 255; i++) {
			assertFalse(
					jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(connectionId("127.0.1." + i))));
		}
	}

	public void testRegistrationNotAllowedWhitelistSingle() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, "127.0.0.3");

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(CONNECTION_ID)));
	}

	@Test
	public void testRegistrationNotAllowedInBlacklist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.REGISTRATION_BLACKLIST_PROP_KEY, "127.0.0.2,127.0.0.3");

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(CONNECTION_ID)));
	}

	@Test
	public void testRegistrationNotAllowedInBlacklistSingle() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.REGISTRATION_BLACKLIST_PROP_KEY, "127.0.0.2");

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(CONNECTION_ID)));
	}

	@Test
	public void testRegistrationNotAllowedInCIDRBlacklist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.REGISTRATION_BLACKLIST_PROP_KEY, "127.0.0.0/24");

		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		for (int i = 0; i <= 255; i++) {
			assertFalse(
					jabberIqRegister.isRegistrationAllowedForConnection(JID.jidInstance(connectionId("127.0.0." + i))));
		}
	}

	@Test
	public void testCIDR() {
		String val = "192.168.122.0/24";
		tigase.xmpp.impl.JabberIqRegister.CIDRAddress addr = tigase.xmpp.impl.JabberIqRegister.CIDRAddress.parse(val);
		assertEquals(val, addr.toString());
		val = "193.34.32.0/19";
		addr = tigase.xmpp.impl.JabberIqRegister.CIDRAddress.parse(val);
		assertEquals(val, addr.toString());
	}

	private class JabberIqRegister
			extends tigase.xmpp.impl.JabberIqRegister {

		@Override
		public void init(Map<String, Object> settings) throws TigaseDBException {
			final Field[] fields = DependencyManager.getAllFields(this.getClass());
			for (Field field : fields) {

				ConfigField configField = field.getAnnotation(ConfigField.class);

				if (configField == null) {
					continue;
				} else {
					try {
						Object value = settings.getOrDefault(field.getName(), settings.get(configField.alias()));
						if (value != null) {
							if (boolean.class.equals(field.getType())) {
								value = Boolean.valueOf(value.toString());
							}
							if (LinkedList.class.equals(field.getType())) {
								value = new LinkedList<String>(Arrays.asList(value.toString().split(",")));
							}
							BeanUtils.setValue(this, field, value);
						}
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}
			}
			;
		}
	}

}