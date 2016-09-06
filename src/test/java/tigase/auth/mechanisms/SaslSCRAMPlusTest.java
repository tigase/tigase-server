package tigase.auth.mechanisms;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tigase.auth.callbacks.*;
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
				new TestCallbackHandler("Ey6OJnGx7JEJAIJp", new byte[]{'D', 'P', 'I'}), "5kLrhitKUHVoSOmzdR") {
		};
	}


	@Test
	public void testChannelBindingEncodingEncoding() throws SaslException {
		String CFM = "cD10bHMtdW5pcXVlLCxuPWJtYWxrb3cscj1TanF1Y3NIdmkzQjR0c1lrTkpCS0lJdHM=";
		String SFM = "cj1TanF1Y3NIdmkzQjR0c1lrTkpCS0lJdHNjdUVZWGMvU210dWtTUjIycVoscz1NZzRxWVlUckh6VkUyUUdZLGk9NDA5Ng==";
		String CSM = "Yz1jRDEwYkhNdGRXNXBjWFZsTEN4WHhZRFpldWFPU01qWTE3cG5QZXE2K2FDaUdIODg1dW9PVlFKMm5rQlk4dz09LHI9U2pxdWNzSHZpM0I0dHNZa05KQktJSXRzY3VFWVhjL1NtdHVrU1IyMnFaLHA9NC9ZeHptOUZsV24xT1duaUJVQ08yeC9jMXo4PQ==";


		SaslSCRAMPlus m = new SaslSCRAMPlus(null,
				new TestCallbackHandler("Mg4qYYTrHzVE2QGY", Base64.decode("V8WA2XrmjkjI2Ne6Zz3quvmgohh/PObqDlUCdp5AWPM=")), "cuEYXc/SmtukSR22qZ") {
		};

		byte[] req;
		byte[] rsp;

		req = Base64.decode(CFM);
		rsp = m.evaluateResponse(req);

		System.out.println(new String(rsp));
		System.out.println(new String(Base64.decode(SFM)));

		req = Base64.decode(CSM);
		rsp = m.evaluateResponse(req);
	}

	@Test
	public void testInvalidBinding() {
		SaslSCRAMPlus m = new SaslSCRAMPlus(null,
				new TestCallbackHandler("AecUfGKyBAbZjjXW", new byte[]{'D', 'P', 'I'}), "k5m3fXaEqPQ0zxIjpl") {
		};

		try {
			byte[] r = m.evaluateResponse("p=tls-unique,,n=bmalkow,r=mnKBtk4+09BtRQM3AkSsjsE5".getBytes());
			Assert.assertEquals("r=mnKBtk4+09BtRQM3AkSsjsE5k5m3fXaEqPQ0zxIjpl,s=AecUfGKyBAbZjjXW,i=4096", new String(r));

			r = m.evaluateResponse("c=cD10bHMtdW5pcXVlLCxEVVBB,r=mnKBtk4+09BtRQM3AkSsjsE5k5m3fXaEqPQ0zxIjpl,p=BatbnZpQ+UolSyWBozXyvS8Yl78=".getBytes());
			fail();

		} catch (SaslException e) {
			Assert.assertEquals("Channel bindings does not match", e.getMessage());
		}
	}

	@Test
	public void testModifiedBinding() {
		final SaslSCRAMPlus m = new SaslSCRAMPlus(null,
				new TestCallbackHandler("AecUfGKyBAbZjjXW", new byte[]{'D', 'P', 'I'}), "k5m3fXaEqPQ0zxIjpl");

		try {
			byte[] r = m.evaluateResponse("p=tls-unique,,n=bmalkow,r=mnKBtk4+09BtRQM3AkSsjsE5".getBytes());
			Assert.assertEquals("r=mnKBtk4+09BtRQM3AkSsjsE5k5m3fXaEqPQ0zxIjpl,s=AecUfGKyBAbZjjXW,i=4096", new String(r));

			// Channel binding data modified by Mallet to value expected by server
			r = m.evaluateResponse("c=cD10bHMtdW5pcXVlLCxEUEk=,r=mnKBtk4+09BtRQM3AkSsjsE5k5m3fXaEqPQ0zxIjpl,p=BatbnZpQ+UolSyWBozXyvS8Yl78=".getBytes());
			fail();
		} catch (SaslException e) {
			Assert.assertEquals("Password not verified", e.getMessage());
		}
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

		private final byte[] bindingData;

		private final String salt;

		public TestCallbackHandler(String salt, byte[] bindingData) {
			this.bindingData = bindingData;
			this.salt = salt;
		}

		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			for (Callback callback : callbacks) {
				if (callback instanceof XMPPSessionCallback) {
				} else if (callback instanceof ChannelBindingCallback) {
					if (((ChannelBindingCallback) callback).getRequestedBindType() == AbstractSaslSCRAM.BindType.tls_unique)
						((ChannelBindingCallback) callback).setBindingData(bindingData);
				} else if (callback instanceof PBKDIterationsCallback) {
					((PBKDIterationsCallback) callback).setInterations(4096);
				} else if (callback instanceof SaltedPasswordCallback) {
					try {
						byte[] r = AbstractSaslSCRAM.hi("SHA1", "123456".getBytes(), Base64.decode(salt), 4096);
						((SaltedPasswordCallback) callback).setSaltedPassword(r);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				} else if (callback instanceof NameCallback) {
					((NameCallback) callback).setName("user@domain.com");
				} else if (callback instanceof SaltCallback) {
					((SaltCallback) callback).setSalt(Base64.decode(salt));
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
