package tigase.auth.mechanisms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.SaslException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import tigase.auth.XmppSaslException;

public class SaslANONYMOUSTest {

	private SaslANONYMOUS sasl;

	@Before
	public void setUp() {
		Map<? super String, ?> props = new HashMap<String, Object>();
		CallbackHandler callbackHandler = new CallbackHandler() {

			@Override
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback callback : callbacks) {
					if (callback instanceof NameCallback) {
						((NameCallback) callback).setName("somerandomname");
					} else
						throw new UnsupportedCallbackException(callback);
				}
			}
		};
		this.sasl = new SaslANONYMOUS(props, callbackHandler);
	}

	@Test
	public void testMalformedRequest() {
		try {
			sasl.evaluateResponse("a".getBytes());
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
			sasl.evaluateResponse("".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		Assert.assertTrue(sasl.isComplete());
		Assert.assertEquals("somerandomname", sasl.getAuthorizationID());
		Assert.assertTrue((Boolean) sasl.getNegotiatedProperty(SaslANONYMOUS.IS_ANONYMOUS_PROPERTY));

	}
}
