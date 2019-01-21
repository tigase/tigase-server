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
package tigase.auth.mechanisms;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import tigase.auth.XmppSaslException;
import tigase.auth.callbacks.AuthorizationIdCallback;
import tigase.auth.callbacks.VerifyPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaslPLAINTest
		extends TestCase {

	private SaslPLAIN sasl;

	@Override
	@Before
	public void setUp() {
		Map<? super String, ?> props = new HashMap<String, Object>();
		CallbackHandler callbackHandler = new CallbackHandler() {

			private String username;

			@Override
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback callback : callbacks) {
					if (callback instanceof NameCallback) {
						username = ((NameCallback) callback).getDefaultName() + "@domain.com";
						((NameCallback) callback).setName(username);
					} else if (callback instanceof VerifyPasswordCallback) {
						((VerifyPasswordCallback) callback).setVerified("juliet@domain.com:xsecret".equals(
								username + ":" + ((VerifyPasswordCallback) callback).getPassword()));
					} else if (callback instanceof AuthorizationIdCallback) {
						// there is nothing to do..
					} else if (callback instanceof AuthorizeCallback) {
						boolean a = ((AuthorizeCallback) callback).getAuthorizationID()
								.equals(((AuthorizeCallback) callback).getAuthenticationID());
						((AuthorizeCallback) callback).setAuthorized(a);
						if (a) {
							((AuthorizeCallback) callback).setAuthorizedID(
									((AuthorizeCallback) callback).getAuthorizationID());
						}
					} else {
						throw new UnsupportedCallbackException(callback);
					}
				}
			}
		};
		this.sasl = new SaslPLAIN(props, callbackHandler);
	}

	@Test
	public void testEmptyPassword() {
		try {
			sasl.evaluateResponse("\0juliet\0".getBytes());
			fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			assertEquals("not-authorized", e.getSaslErrorElementName());
			assertEquals("Password not verified", e.getMessage());
		} catch (SaslException e) {
			assertEquals("Password not verified", e.getMessage());
		}

		assertFalse(sasl.isComplete());
		assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testEmptyUsername() {
		try {
			sasl.evaluateResponse("\0\0qaz".getBytes());
			fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		assertFalse(sasl.isComplete());
		assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testEmptyUsernamePassword() {
		try {
			sasl.evaluateResponse("\0\0".getBytes());
			fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		assertFalse(sasl.isComplete());
		assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testInvalidAuthzId() {
		try {
			sasl.evaluateResponse("romeo\0juliet\0xsecret".getBytes());
			fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			assertEquals("invalid-authzid", e.getSaslErrorElementName());
			assertEquals("PLAIN: juliet is not authorized to act as romeo", e.getMessage());
		} catch (SaslException e) {
			assertEquals("PLAIN: juliet is not authorized to act as romeo", e.getMessage());
		}

		assertFalse(sasl.isComplete());
		assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testInvalidPassword() {
		try {
			sasl.evaluateResponse("\0juliet\0ysecret".getBytes());
			fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			assertEquals("not-authorized", e.getSaslErrorElementName());
			assertEquals("Password not verified", e.getMessage());
		} catch (SaslException e) {
			assertEquals("Password not verified", e.getMessage());
		}

		assertFalse(sasl.isComplete());
		assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testInvalidUsername() {
		try {
			sasl.evaluateResponse("\0romeo\0xsecret".getBytes());
			fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			assertEquals("not-authorized", e.getSaslErrorElementName());
			assertEquals("Password not verified", e.getMessage());
		} catch (SaslException e) {
			assertEquals("Password not verified", e.getMessage());
		}

		assertFalse(sasl.isComplete());
		assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testmalformedRequest1() {
		try {
			sasl.evaluateResponse("".getBytes());
			fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		assertFalse(sasl.isComplete());
		assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testmalformedRequest2() {
		try {
			sasl.evaluateResponse(null);
			fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		assertFalse(sasl.isComplete());
		assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testmalformedRequest3() {
		try {
			sasl.evaluateResponse("\0juliet\0xsecret\0".getBytes());
			fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		assertFalse(sasl.isComplete());
		assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testmalformedRequest4() {
		try {
			sasl.evaluateResponse("xyz".getBytes());
			fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		assertFalse(sasl.isComplete());
		assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testSuccess() {

		try {
			sasl.evaluateResponse("\0juliet\0xsecret".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertTrue(sasl.isComplete());
		assertEquals("juliet@domain.com", sasl.getAuthorizationID());

	}

	@Test
	public void testSuccessWithAuthzId() {

		try {
			sasl.evaluateResponse("juliet@domain.com\0juliet\0xsecret".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertTrue(sasl.isComplete());
		assertEquals("juliet@domain.com", sasl.getAuthorizationID());
	}

	@Test
	public void testSuccessWithAuthzId2() {

		Map<? super String, ?> props = new HashMap<String, Object>();
		CallbackHandler callbackHandler = new CallbackHandler() {

			private String username;

			@Override
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback callback : callbacks) {
					if (callback instanceof NameCallback) {
						username = ((NameCallback) callback).getDefaultName();
					} else if (callback instanceof VerifyPasswordCallback) {
						((VerifyPasswordCallback) callback).setVerified("secondwitch:shakespeare".equals(
								username + ":" + ((VerifyPasswordCallback) callback).getPassword()));
					} else if (callback instanceof AuthorizationIdCallback) {
						// nothing to do..
					} else if (callback instanceof AuthorizeCallback) {
						boolean a = ((AuthorizeCallback) callback).getAuthorizationID().equals("romeo@example.net");
						a = a && username.equals("secondwitch");
						((AuthorizeCallback) callback).setAuthorized(a);
						if (a) {
							((AuthorizeCallback) callback).setAuthorizedID(
									((AuthorizeCallback) callback).getAuthorizationID());
						}
					} else {
						throw new UnsupportedCallbackException(callback);
					}
				}
			}
		};
		final SaslPLAIN sasl = new SaslPLAIN(props, callbackHandler);

		try {
			sasl.evaluateResponse("romeo@example.net\0secondwitch\0shakespeare".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertTrue(sasl.isComplete());
		assertEquals("romeo@example.net", sasl.getAuthorizationID());
	}

}
