package tigase.auth.mechanisms;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import tigase.auth.callbacks.PBKDIterationsCallback;
import tigase.auth.callbacks.SaltCallback;
import tigase.auth.callbacks.SaltedPasswordCallback;
import tigase.util.Base64;

public class SaslSCRAMTest {

	private class TestCallbackHandler implements CallbackHandler {

		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			for (Callback callback : callbacks) {
				if (callback instanceof PBKDIterationsCallback) {
					((PBKDIterationsCallback) callback).setInterations(4096);
				} else if (callback instanceof SaltedPasswordCallback) {
					try {
						byte[] r = AbstractSaslSCRAM.hi("SHA1", "pencil".getBytes(), Base64.decode("QSXCR+Q6sek8bf92"), 4096);
						((SaltedPasswordCallback) callback).setSaltedPassword(r);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				} else if (callback instanceof NameCallback) {
					((NameCallback) callback).setName("user");
				} else if (callback instanceof SaltCallback) {
					((SaltCallback) callback).setSalt(Base64.decode("QSXCR+Q6sek8bf92"));
				} else if (callback instanceof AuthorizeCallback) {
					((AuthorizeCallback) callback).setAuthorized(true);
					((AuthorizeCallback) callback).setAuthorizedID("user");
				} else {
					throw new UnsupportedCallbackException(callback, "Unrecognized Callback " + callback);
				}
			}
		}
	}

	private AbstractSaslSCRAM m;

	@Before
	public void setUp() throws Exception {
		m = new AbstractSaslSCRAM("SCRAM-SHA-1", "SHA1", "Client Key".getBytes(), "Server Key".getBytes(), null,
				new TestCallbackHandler(), "3rfcNHYJY1ZVvWVs7j") {
		};
	}

	@Test
	public void testH() {
		try {
			byte[] r = m.h("The quick brown fox jumps over the lazy cog".getBytes());
			Assert.assertArrayEquals(Base64.decode("3p8sf9JeGzr60+haC9F9mxANtLM="), r);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testHi() {
		try {
			byte[] r = AbstractSaslSCRAM.hi("SHA1", "password".getBytes(), "salt".getBytes(), 1);
			Assert.assertArrayEquals(new byte[] { (byte) 0x0c, (byte) 0x60, (byte) 0xc8, (byte) 0x0f, (byte) 0x96, (byte) 0x1f,
					(byte) 0x0e, (byte) 0x71, (byte) 0xf3, (byte) 0xa9, (byte) 0xb5, (byte) 0x24, (byte) 0xaf, (byte) 0x60,
					(byte) 0x12, (byte) 0x06, (byte) 0x2f, (byte) 0xe0, (byte) 0x37, (byte) 0xa6 }, r);

			r = AbstractSaslSCRAM.hi("SHA1", "password".getBytes(), "salt".getBytes(), 2);
			Assert.assertArrayEquals(new byte[] { (byte) 0xea, (byte) 0x6c, (byte) 0x01, (byte) 0x4d, (byte) 0xc7, (byte) 0x2d,
					(byte) 0x6f, (byte) 0x8c, (byte) 0xcd, (byte) 0x1e, (byte) 0xd9, (byte) 0x2a, (byte) 0xce, (byte) 0x1d,
					(byte) 0x41, (byte) 0xf0, (byte) 0xd8, (byte) 0xde, (byte) 0x89, (byte) 0x57 }, r);

			r = AbstractSaslSCRAM.hi("SHA1", "password".getBytes(), "salt".getBytes(), 4096);
			Assert.assertArrayEquals(new byte[] { (byte) 0x4b, (byte) 0x00, (byte) 0x79, (byte) 0x01, (byte) 0xb7, (byte) 0x65,
					(byte) 0x48, (byte) 0x9a, (byte) 0xbe, (byte) 0xad, (byte) 0x49, (byte) 0xd9, (byte) 0x26, (byte) 0xf7,
					(byte) 0x21, (byte) 0xd0, (byte) 0x65, (byte) 0xa4, (byte) 0x29, (byte) 0xc1 }, r);

			r = AbstractSaslSCRAM.hi("SHA1", "password".getBytes(), "salt".getBytes(), 16777216);
			Assert.assertArrayEquals(new byte[] { (byte) 0xee, (byte) 0xfe, (byte) 0x3d, (byte) 0x61, (byte) 0xcd, (byte) 0x4d,
					(byte) 0xa4, (byte) 0xe4, (byte) 0xe9, (byte) 0x94, (byte) 0x5b, (byte) 0x3d, (byte) 0x6b, (byte) 0xa2,
					(byte) 0x15, (byte) 0x8c, (byte) 0x26, (byte) 0x34, (byte) 0xe9, (byte) 0x84 }, r);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testHmac() {
		try {
			byte[] r = AbstractSaslSCRAM.hmac(m.key("foo".getBytes()), "foobar".getBytes());

			Assert.assertArrayEquals(new byte[] { (byte) 0xa4, (byte) 0xee, (byte) 0xba, (byte) 0x8e, 0x63, 0x3d, 0x77,
					(byte) 0x88, 0x69, (byte) 0xf5, 0x68, (byte) 0xd0, 0x5a, 0x1b, 0x3d, (byte) 0xc7, 0x2b, (byte) 0xfd, 0x4,
					(byte) 0xdd }, r);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testServerFirstMessage() {
		try {
			byte[] r = m.evaluateResponse("n,,n=user,r=fyko+d2lbbFgONRv9qkxdawL".getBytes());
			Assert.assertEquals("r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,s=QSXCR+Q6sek8bf92,i=4096", new String(r));

			r = m.evaluateResponse("c=biws,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,p=v0X8v3Bz2T0CJGbJQyF0X+HI4Ts=".getBytes());
			Assert.assertEquals("v=rmF9pqV8S7suAoZWja4dJRkFsKQ=", new String(r));

		} catch (SaslException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
