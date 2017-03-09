package tigase.xmpp.impl;

import java.util.HashMap;

import junit.framework.TestCase;

import org.junit.Test;

import tigase.xmpp.JID;

/**
 * Test class for JabberIqPrivacyTest
 * 
 * Currently class tests validateList method checking validation of type,
 * subscription and action. Other cases are not tested due to missing instance
 * of XMPPResourceConnection
 */
public class JabberIqRegisterWhitelistTest extends TestCase {

	private static final String CONNECTION_ID = connectionId("127.0.0.2");
	
	private static final String connectionId(String remoteAddress) {
		return "c2s@0123456789/127.0.0.1_5222_" + remoteAddress + "_50123";
	}
	
	@Test
	public void testRegistrationAllowedDefaultSettings() throws Exception {
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(new HashMap<String, Object>());
		assertTrue(jabberIqRegister.isRegistrationAllowedForConnection(
				JID.jidInstance(CONNECTION_ID)));
	}
	
	@Test
	public void testRegistrationAllowedNotInBlacklist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.REGISTRATION_BLACKLIST_PROP_KEY, 
				"127.0.0.3,127.0.0.4");
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertTrue(jabberIqRegister.isRegistrationAllowedForConnection(
				JID.jidInstance(CONNECTION_ID)));
	}
	
	@Test
	public void testRegistrationAllowedInWhitelist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, 
				Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, 
				"127.0.0.2,127.0.0.3");
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertTrue(jabberIqRegister.isRegistrationAllowedForConnection(
				JID.jidInstance(CONNECTION_ID)));
	}
	
	@Test
	public void testRegistrationAllowedInCIDRWhitelist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, 
				Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, 
				"127.0.0.0/24");
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertTrue(jabberIqRegister.isRegistrationAllowedForConnection(
				JID.jidInstance(CONNECTION_ID)));
	}
	
	@Test
	public void testRegistrationAllowedInCIDRMultipleWhitelist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, 
				Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, 
				"127.0.0.0/24,127.0.0.1/24");
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertTrue(jabberIqRegister.isRegistrationAllowedForConnection(
				JID.jidInstance(CONNECTION_ID)));
	}
	
	@Test
	public void testRegistrationFromMultipleSourcesAllowedInCIDRWhitelist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, 
				Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, 
				"127.0.0.0/24");
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		for (int i = 0; i <= 255; i++) {
			assertTrue(jabberIqRegister.isRegistrationAllowedForConnection(
					JID.jidInstance(connectionId("127.0.0." + i))));
		}
	}
	
	@Test
	public void testRegistrationNotAllowedWhitelistOnlyEmpty() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, 
				Boolean.TRUE.toString());
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(
				JID.jidInstance(CONNECTION_ID)));
	}
	
	@Test
	public void testRegistrationNotAllowedWhitelistOnly() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, 
				Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, 
				"127.0.0.3,127.0.0.4");
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(
				JID.jidInstance(CONNECTION_ID)));
	}
	
	@Test
	public void testRegistrationNotAllowedCIDRWhitelistOnly() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, 
				Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, 
				"127.0.0.0/24");
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		for (int i = 0; i <= 255; i++) {
			assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(
					JID.jidInstance(connectionId("127.0.1." + i))));
		}
	}
	
	public void testRegistrationNotAllowedWhitelistSingle() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.WHITELIST_REGISTRATION_ONLY_PROP_KEY, 
				Boolean.TRUE.toString());
		settings.put(JabberIqRegister.REGISTRATION_WHITELIST_PROP_KEY, 
				"127.0.0.3");
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(
				JID.jidInstance(CONNECTION_ID)));
	}
	
	@Test
	public void testRegistrationNotAllowedInBlacklist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.REGISTRATION_BLACKLIST_PROP_KEY, 
				"127.0.0.2,127.0.0.3");
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(
				JID.jidInstance(CONNECTION_ID)));
	}
	
	@Test
	public void testRegistrationNotAllowedInBlacklistSingle() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.REGISTRATION_BLACKLIST_PROP_KEY, 
				"127.0.0.2");
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(
				JID.jidInstance(CONNECTION_ID)));
	}
	
	@Test
	public void testRegistrationNotAllowedInCIDRBlacklist() throws Exception {
		HashMap<String, Object> settings = new HashMap<String, Object>();
		settings.put(JabberIqRegister.REGISTRATION_BLACKLIST_PROP_KEY, 
				"127.0.0.0/24");
		
		JabberIqRegister jabberIqRegister = new JabberIqRegister();
		jabberIqRegister.init(settings);
		for (int i = 0; i <= 255; i++) {
			assertFalse(jabberIqRegister.isRegistrationAllowedForConnection(
					JID.jidInstance(connectionId("127.0.0." + i))));
		}
	}

}