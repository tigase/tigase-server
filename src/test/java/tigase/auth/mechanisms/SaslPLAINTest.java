package tigase.auth.mechanisms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import tigase.auth.XmppSaslException;
import tigase.auth.callbacks.VerifyPasswordCallback;

public class SaslPLAINTest {

	private SaslPLAIN sasl;

	@Before
	public void setUp() {
		Map<? super String, ?> props = new HashMap<String, Object>();
		CallbackHandler callbackHandler = new CallbackHandler() {

			private String username;

			@Override
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback callback : callbacks) {
					if (callback instanceof NameCallback) {
						username = ((NameCallback) callback).getDefaultName();
					} else if (callback instanceof VerifyPasswordCallback) {
						((VerifyPasswordCallback) callback).setVerified("juliet:xsecret".equals(username + ":"
								+ ((VerifyPasswordCallback) callback).getPassword()));
					} else if (callback instanceof AuthorizeCallback) {
						boolean a = ((AuthorizeCallback) callback).getAuthorizationID().equals(
								((AuthorizeCallback) callback).getAuthenticationID());
						((AuthorizeCallback) callback).setAuthorized(a);
						if (a)
							((AuthorizeCallback) callback).setAuthorizedID(((AuthorizeCallback) callback).getAuthorizationID());
					} else
						throw new UnsupportedCallbackException(callback);
				}
			}
		};
		this.sasl = new SaslPLAIN(props, callbackHandler);
	}

	@Test
	public void testEmptyPassword() {
		try {
			sasl.evaluateResponse("\0juliet\0".getBytes());
			Assert.fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			Assert.assertEquals("not-authorized", e.getSaslErrorElementName());
			Assert.assertEquals("Password not verified", e.getMessage());
		} catch (SaslException e) {
			Assert.assertEquals("Password not verified", e.getMessage());
		}

		Assert.assertFalse(sasl.isComplete());
		Assert.assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testEmptyUsername() {
		try {
			sasl.evaluateResponse("\0\0qaz".getBytes());
			Assert.fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			Assert.assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		Assert.assertFalse(sasl.isComplete());
		Assert.assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testEmptyUsernamePassword() {
		try {
			sasl.evaluateResponse("\0\0".getBytes());
			Assert.fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			Assert.assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		Assert.assertFalse(sasl.isComplete());
		Assert.assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testInvalidAuthzId() {
		try {
			sasl.evaluateResponse("romeo\0juliet\0xsecret".getBytes());
			Assert.fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			Assert.assertEquals("invalid-authzid", e.getSaslErrorElementName());
			Assert.assertEquals("PLAIN: juliet is not authorized to act as romeo", e.getMessage());
		} catch (SaslException e) {
			Assert.assertEquals("PLAIN: juliet is not authorized to act as romeo", e.getMessage());
		}

		Assert.assertFalse(sasl.isComplete());
		Assert.assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testInvalidPassword() {
		try {
			sasl.evaluateResponse("\0juliet\0ysecret".getBytes());
			Assert.fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			Assert.assertEquals("not-authorized", e.getSaslErrorElementName());
			Assert.assertEquals("Password not verified", e.getMessage());
		} catch (SaslException e) {
			Assert.assertEquals("Password not verified", e.getMessage());
		}

		Assert.assertFalse(sasl.isComplete());
		Assert.assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testInvalidUsername() {
		try {
			sasl.evaluateResponse("\0romeo\0xsecret".getBytes());
			Assert.fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			Assert.assertEquals("not-authorized", e.getSaslErrorElementName());
			Assert.assertEquals("Password not verified", e.getMessage());
		} catch (SaslException e) {
			Assert.assertEquals("Password not verified", e.getMessage());
		}

		Assert.assertFalse(sasl.isComplete());
		Assert.assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testmalformedRequest1() {
		try {
			sasl.evaluateResponse("".getBytes());
			Assert.fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			Assert.assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		Assert.assertFalse(sasl.isComplete());
		Assert.assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testmalformedRequest2() {
		try {
			sasl.evaluateResponse(null);
			Assert.fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			Assert.assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		Assert.assertFalse(sasl.isComplete());
		Assert.assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testmalformedRequest3() {
		try {
			sasl.evaluateResponse("\0juliet\0xsecret\0".getBytes());
			Assert.fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			Assert.assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		Assert.assertFalse(sasl.isComplete());
		Assert.assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testmalformedRequest4() {
		try {
			sasl.evaluateResponse("xyz".getBytes());
			Assert.fail("Exception must be throwed");
		} catch (XmppSaslException e) {
			Assert.assertEquals("malformed-request", e.getSaslErrorElementName());
		} catch (SaslException e) {
		}

		Assert.assertFalse(sasl.isComplete());
		Assert.assertNull("Authorization ID must be null", sasl.getAuthorizationID());
	}

	@Test
	public void testSuccess() {

		try {
			sasl.evaluateResponse("\0juliet\0xsecret".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		Assert.assertTrue(sasl.isComplete());
		Assert.assertEquals("juliet", sasl.getAuthorizationID());

	}

	@Test
	public void testSuccessWithAuthzId() {

		try {
			sasl.evaluateResponse("juliet\0juliet\0xsecret".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		Assert.assertTrue(sasl.isComplete());
		Assert.assertEquals("juliet", sasl.getAuthorizationID());
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
						((VerifyPasswordCallback) callback).setVerified("secondwitch:shakespeare".equals(username + ":"
								+ ((VerifyPasswordCallback) callback).getPassword()));
					} else if (callback instanceof AuthorizeCallback) {
						boolean a = ((AuthorizeCallback) callback).getAuthorizationID().equals("romeo@example.net");
						a = a && username.equals("secondwitch");
						((AuthorizeCallback) callback).setAuthorized(a);
						if (a)
							((AuthorizeCallback) callback).setAuthorizedID(((AuthorizeCallback) callback).getAuthorizationID());
					} else
						throw new UnsupportedCallbackException(callback);
				}
			}
		};
		final SaslPLAIN sasl = new SaslPLAIN(props, callbackHandler);

		try {
			sasl.evaluateResponse("romeo@example.net\0secondwitch\0shakespeare".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		Assert.assertTrue(sasl.isComplete());
		Assert.assertEquals("romeo@example.net", sasl.getAuthorizationID());
	}

}
