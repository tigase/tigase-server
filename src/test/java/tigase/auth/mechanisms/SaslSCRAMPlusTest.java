package tigase.auth.mechanisms;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tigase.auth.callbacks.ChannelBindingCallback;
import tigase.auth.callbacks.PBKDIterationsCallback;
import tigase.auth.callbacks.SaltCallback;
import tigase.auth.callbacks.SaltedPasswordCallback;
import tigase.util.Base64;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import java.io.IOException;

public class SaslSCRAMPlusTest extends TestCase {

	private AbstractSaslSCRAM m;

	@Override
	@Before
	public void setUp() throws Exception {
		m = new SaslSCRAMPlus(null,
				new TestCallbackHandler(), "5kLrhitKUHVoSOmzdR") {
		};
	}


	@Test
	public void testServerFirstMessageFail_1() {
		try {
			byte[] r = m.evaluateResponse("n,,n=user,r=fyko+d2lbbFgONRv9qkxdawL".getBytes());
			fail();
		} catch (SaslException e) {
			Assert.assertEquals("Invalid request for SCRAM-SHA-1-PLUS", e.getMessage());

		}
	}

	@Test
	public void testServerFirstMessageFail_2() {
		try {
			byte[] r = m.evaluateResponse("y,,n=user,r=fyko+d2lbbFgONRv9qkxdawL".getBytes());
			fail();
		} catch (SaslException e) {
			Assert.assertEquals("Server supports PLUS. Please use 'p'", e.getMessage());
		}
	}



	@Test
	public void testServerFirstMessageWithBinding() {
		try {
			byte[] r = m.evaluateResponse("p=tls-unique,,n=bmalkow,r=SpiXKmhi57DBp5sdE5G3H3ms".getBytes());
			Assert.assertEquals("r=SpiXKmhi57DBp5sdE5G3H3ms5kLrhitKUHVoSOmzdR,s=Ey6OJnGx7JEJAIJp,i=4096", new String(r));

			r = m.evaluateResponse("c=cD10bHMtdW5pcXVlLCxEUEk=,r=SpiXKmhi57DBp5sdE5G3H3ms5kLrhitKUHVoSOmzdR,p=+zQvUd4nQqo03thSCcc2K6gueD4=".getBytes());
			Assert.assertEquals("v=NQ/f8FjeMxUuRK9F88G8tMji4pk=", new String(r));

		} catch (SaslException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertTrue(m.isComplete());
		assertEquals("user@domain.com", m.getAuthorizationID());
	}

	private class TestCallbackHandler implements CallbackHandler {

		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			for (Callback callback : callbacks) {
				if (callback instanceof ChannelBindingCallback) {
					if (((ChannelBindingCallback) callback).getRequestedBindType() == AbstractSaslSCRAM.BindType.tls_unique)
						((ChannelBindingCallback) callback).setBindingData(new byte[]{'D', 'P', 'I'});
				} else if (callback instanceof PBKDIterationsCallback) {
					((PBKDIterationsCallback) callback).setInterations(4096);
				} else if (callback instanceof SaltedPasswordCallback) {
					try {
						byte[] r = AbstractSaslSCRAM.hi("SHA1", "123456".getBytes(), Base64.decode("Ey6OJnGx7JEJAIJp"), 4096);
						((SaltedPasswordCallback) callback).setSaltedPassword(r);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				} else if (callback instanceof NameCallback) {
					((NameCallback) callback).setName("user@domain.com");
				} else if (callback instanceof SaltCallback) {
					((SaltCallback) callback).setSalt(Base64.decode("Ey6OJnGx7JEJAIJp"));
				} else if (callback instanceof AuthorizeCallback) {
					((AuthorizeCallback) callback).setAuthorized(true);
					((AuthorizeCallback) callback).setAuthorizedID("user@domain.com");
				} else {
					throw new UnsupportedCallbackException(callback, "Unrecognized Callback " + callback);
				}
			}
		}
	}

}
