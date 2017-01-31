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

import static gnu.inet.encoding.DecompositionMappings.m;

public class SaslSCRAMPlusTest
		extends TestCase {

	private SaslSCRAMPlus create(String salt, String snonce, String password, byte[] bindingData) {
		SaslSCRAMTest.TestCallbackHandler h = new SaslSCRAMTest.TestCallbackHandler();
		h.setBindingData(bindingData);
		h.setPassword(password);
		h.setSalt(salt);
		return new SaslSCRAMPlus(null, h, snonce) {
		};
	}

	@Test
	public void testChannelBindingEncodingEncoding() throws SaslException {
		String CFM = "cD10bHMtdW5pcXVlLCxuPWJtYWxrb3cscj1TanF1Y3NIdmkzQjR0c1lrTkpCS0lJdHM=";
		String SFM = "cj1TanF1Y3NIdmkzQjR0c1lrTkpCS0lJdHNjdUVZWGMvU210dWtTUjIycVoscz1NZzRxWVlUckh6VkUyUUdZLGk9NDA5Ng==";
		String CSM = "Yz1jRDEwYkhNdGRXNXBjWFZsTEN4WHhZRFpldWFPU01qWTE3cG5QZXE2K2FDaUdIODg1dW9PVlFKMm5rQlk4dz09LHI9U2pxdWNzSHZpM0I0dHNZa05KQktJSXRzY3VFWVhjL1NtdHVrU1IyMnFaLHA9NC9ZeHptOUZsV24xT1duaUJVQ08yeC9jMXo4PQ==";

		SaslSCRAMPlus m = create("Mg4qYYTrHzVE2QGY", "cuEYXc/SmtukSR22qZ", "123456",
								 Base64.decode("V8WA2XrmjkjI2Ne6Zz3quvmgohh/PObqDlUCdp5AWPM="));

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
		SaslSCRAMPlus m = create("AecUfGKyBAbZjjXW", "k5m3fXaEqPQ0zxIjpl", "123456", new byte[]{'D', 'P', 'I'});

		try {
			byte[] r = m.evaluateResponse("p=tls-unique,,n=bmalkow,r=mnKBtk4+09BtRQM3AkSsjsE5".getBytes());
			Assert.assertEquals("r=mnKBtk4+09BtRQM3AkSsjsE5k5m3fXaEqPQ0zxIjpl,s=AecUfGKyBAbZjjXW,i=4096",
								new String(r));

			r = m.evaluateResponse(
					"c=cD10bHMtdW5pcXVlLCxEVVBB,r=mnKBtk4+09BtRQM3AkSsjsE5k5m3fXaEqPQ0zxIjpl,p=BatbnZpQ+UolSyWBozXyvS8Yl78="
							.getBytes());
			fail();

		} catch (SaslException e) {
			Assert.assertEquals("Channel bindings does not match", e.getMessage());
		}
	}

	@Test
	public void testModifiedBinding() {
		final SaslSCRAMPlus m = create("AecUfGKyBAbZjjXW", "k5m3fXaEqPQ0zxIjpl", "123456", new byte[]{'D', 'P', 'I'});

		try {
			byte[] r = m.evaluateResponse("p=tls-unique,,n=bmalkow,r=mnKBtk4+09BtRQM3AkSsjsE5".getBytes());
			Assert.assertEquals("r=mnKBtk4+09BtRQM3AkSsjsE5k5m3fXaEqPQ0zxIjpl,s=AecUfGKyBAbZjjXW,i=4096",
								new String(r));

			// Channel binding data modified by Mallet to value expected by server
			r = m.evaluateResponse(
					"c=cD10bHMtdW5pcXVlLCxEUEk=,r=mnKBtk4+09BtRQM3AkSsjsE5k5m3fXaEqPQ0zxIjpl,p=BatbnZpQ+UolSyWBozXyvS8Yl78="
							.getBytes());
			fail();
		} catch (SaslException e) {
			Assert.assertEquals("Password not verified", e.getMessage());
		}
	}

	@Test
	public void testServerFirstMessageFail_1() {
		SaslSCRAMPlus m = create("Ey6OJnGx7JEJAIJp", "5kLrhitKUHVoSOmzdR", "123456", new byte[]{'D', 'P', 'I'});
		try {
			byte[] r = m.evaluateResponse("n,,n=user,r=fyko+d2lbbFgONRv9qkxdawL".getBytes());
			fail();
		} catch (SaslException e) {
			Assert.assertEquals("Invalid request for SCRAM-SHA-1-PLUS", e.getMessage());

		}
	}

	@Test
	public void testServerFirstMessageFail_2() {
		SaslSCRAMPlus m = create("Ey6OJnGx7JEJAIJp", "5kLrhitKUHVoSOmzdR", "123456", new byte[]{'D', 'P', 'I'});
		try {
			byte[] r = m.evaluateResponse("y,,n=user,r=fyko+d2lbbFgONRv9qkxdawL".getBytes());
			fail();
		} catch (SaslException e) {
			Assert.assertEquals("Server supports PLUS. Please use 'p'", e.getMessage());
		}
	}

	@Test
	public void testServerFirstMessageWithBinding() {
		SaslSCRAMPlus m = create("Ey6OJnGx7JEJAIJp", "5kLrhitKUHVoSOmzdR", "123456", new byte[]{'D', 'P', 'I'});
		try {
			byte[] r = m.evaluateResponse("p=tls-unique,,n=bmalkow,r=SpiXKmhi57DBp5sdE5G3H3ms".getBytes());
			Assert.assertEquals("r=SpiXKmhi57DBp5sdE5G3H3ms5kLrhitKUHVoSOmzdR,s=Ey6OJnGx7JEJAIJp,i=4096",
								new String(r));

			r = m.evaluateResponse(
					"c=cD10bHMtdW5pcXVlLCxEUEk=,r=SpiXKmhi57DBp5sdE5G3H3ms5kLrhitKUHVoSOmzdR,p=+zQvUd4nQqo03thSCcc2K6gueD4="
							.getBytes());
			Assert.assertEquals("v=NQ/f8FjeMxUuRK9F88G8tMji4pk=", new String(r));

		} catch (SaslException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertTrue(m.isComplete());
		assertEquals("user@domain.com", m.getAuthorizationID());
	}

}


