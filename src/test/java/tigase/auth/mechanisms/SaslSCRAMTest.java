package tigase.auth.mechanisms;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import tigase.auth.callbacks.*;
import tigase.util.Base64;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class SaslSCRAMTest
		extends TestCase {

	@Test
	public void testH() {
		try {
			SaslSCRAM m = create("QSXCR+Q6sek8bf92", "3rfcNHYJY1ZVvWVs7j", "pencil");
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
			Assert.assertArrayEquals(
					new byte[]{(byte) 0x0c, (byte) 0x60, (byte) 0xc8, (byte) 0x0f, (byte) 0x96, (byte) 0x1f,
							   (byte) 0x0e, (byte) 0x71, (byte) 0xf3, (byte) 0xa9, (byte) 0xb5, (byte) 0x24,
							   (byte) 0xaf, (byte) 0x60, (byte) 0x12, (byte) 0x06, (byte) 0x2f, (byte) 0xe0,
							   (byte) 0x37, (byte) 0xa6}, r);

			r = AbstractSaslSCRAM.hi("SHA1", "password".getBytes(), "salt".getBytes(), 2);
			Assert.assertArrayEquals(
					new byte[]{(byte) 0xea, (byte) 0x6c, (byte) 0x01, (byte) 0x4d, (byte) 0xc7, (byte) 0x2d,
							   (byte) 0x6f, (byte) 0x8c, (byte) 0xcd, (byte) 0x1e, (byte) 0xd9, (byte) 0x2a,
							   (byte) 0xce, (byte) 0x1d, (byte) 0x41, (byte) 0xf0, (byte) 0xd8, (byte) 0xde,
							   (byte) 0x89, (byte) 0x57}, r);

			r = AbstractSaslSCRAM.hi("SHA1", "password".getBytes(), "salt".getBytes(), 4096);
			Assert.assertArrayEquals(
					new byte[]{(byte) 0x4b, (byte) 0x00, (byte) 0x79, (byte) 0x01, (byte) 0xb7, (byte) 0x65,
							   (byte) 0x48, (byte) 0x9a, (byte) 0xbe, (byte) 0xad, (byte) 0x49, (byte) 0xd9,
							   (byte) 0x26, (byte) 0xf7, (byte) 0x21, (byte) 0xd0, (byte) 0x65, (byte) 0xa4,
							   (byte) 0x29, (byte) 0xc1}, r);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Category(tigase.tests.SlowTest.class)
	@Test
	public void testHiLong() {
		try {
			byte[] r = AbstractSaslSCRAM.hi("SHA1", "password".getBytes(), "salt".getBytes(), 16777216);
			Assert.assertArrayEquals(
					new byte[]{(byte) 0xee, (byte) 0xfe, (byte) 0x3d, (byte) 0x61, (byte) 0xcd, (byte) 0x4d,
							   (byte) 0xa4, (byte) 0xe4, (byte) 0xe9, (byte) 0x94, (byte) 0x5b, (byte) 0x3d,
							   (byte) 0x6b, (byte) 0xa2, (byte) 0x15, (byte) 0x8c, (byte) 0x26, (byte) 0x34,
							   (byte) 0xe9, (byte) 0x84}, r);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * @param salt password salt
	 * @param snonce server once
	 * @param password user password (plaintext)
	 *
	 * @return
	 */
	private SaslSCRAM create(String salt, String snonce, String password) {
		TestCallbackHandler h = new TestCallbackHandler();
		h.setSalt(salt);
		h.setPassword(password);
		SaslSCRAM m = new SaslSCRAM(null, h, snonce) {
		};

		return m;
	}

	@Test
	public void testHmac() {
		SaslSCRAM m = create("QSXCR+Q6sek8bf92", "3rfcNHYJY1ZVvWVs7j", "pencil");
		try {
			byte[] r = AbstractSaslSCRAM.hmac(m.key("foo".getBytes()), "foobar".getBytes());

			Assert.assertArrayEquals(
					new byte[]{(byte) 0xa4, (byte) 0xee, (byte) 0xba, (byte) 0x8e, 0x63, 0x3d, 0x77, (byte) 0x88, 0x69,
							   (byte) 0xf5, 0x68, (byte) 0xd0, 0x5a, 0x1b, 0x3d, (byte) 0xc7, 0x2b, (byte) 0xfd, 0x4,
							   (byte) 0xdd}, r);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testServerFirstMessage() {
		final byte[] CFM = "n,,n=user,r=fyko+d2lbbFgONRv9qkxdawL".getBytes();
		final byte[] SFM = "r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,s=QSXCR+Q6sek8bf92,i=4096".getBytes();
		final byte[] CSM = "c=biws,r=fyko+d2lbbFgONRv9qkxdawL3rfcNHYJY1ZVvWVs7j,p=v0X8v3Bz2T0CJGbJQyF0X+HI4Ts=".getBytes();
		final byte[] SSM = "v=rmF9pqV8S7suAoZWja4dJRkFsKQ=".getBytes();

		SaslSCRAM m = create("QSXCR+Q6sek8bf92", "3rfcNHYJY1ZVvWVs7j", "pencil");
		try {
			System.out.println(">> " + new String(CFM));
			byte[] sfm = m.evaluateResponse(CFM);
			System.out.println("<< " + new String(sfm));
			Assert.assertArrayEquals(SFM, sfm);
			System.out.println(">> " + new String(CSM));
			byte[] ssm = m.evaluateResponse(CSM);
			System.out.println("<< " + new String(ssm));
			Assert.assertArrayEquals(SSM, ssm);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertTrue(m.isComplete());
		assertEquals("user@domain.com", m.getAuthorizationID());

	}

	@Test
	public void testServerFirstMessageFail_1() {
		try {
			SaslSCRAM m = create("QSXCR+Q6sek8bf92", "3rfcNHYJY1ZVvWVs7j", "pencil");
			byte[] r = m.evaluateResponse("p=tls-unique,,n=bmalkow,r=SpiXKmhi57DBp5sdE5G3H3ms".getBytes());
			fail();
		} catch (SaslException e) {
			Assert.assertEquals("Invalid request for SCRAM-SHA-1", e.getMessage());

		}
	}

//	@Test
//	public void testServerFirstMessageFail_2() {
//		try {
//			SaslSCRAM m = create("QSXCR+Q6sek8bf92", "3rfcNHYJY1ZVvWVs7j", "pencil");
//			byte[] r = m.evaluateResponse("y,,n=bmalkow,r=SpiXKmhi57DBp5sdE5G3H3ms".getBytes());
//			fail();
//		} catch (SaslException e) {
//			Assert.assertEquals("Server supports PLUS. Please use 'p'", e.getMessage());
//
//		}
//	}

	@Test
	public void testDataExchange01() throws Exception {
		final byte[] CFM = Base64.decode("biwsbj1qZW5raW5zLHI9YmdId0xRSEJkNFMrK3F2VEIzZis0QT09");
		final byte[] SFM = Base64.decode(
				"cj1iZ0h3TFFIQmQ0UysrcXZUQjNmKzRBPT1lWXY4REhIMk81dHRxNlRtV3pncyxzPUZSelkraGM5TitMc0FnPT0saT00MDk2");
		final byte[] CSM = Base64.decode(
				"Yz1iaXdzLHI9YmdId0xRSEJkNFMrK3F2VEIzZis0QT09ZVl2OERISDJPNXR0cTZUbVd6Z3MscD1JTlpKaDljTkQyeFJlYzZBQytSYlBoRVdVakk9");

		SaslSCRAM m = create("FRzY+hc9N+LsAg==", "eYv8DHH2O5ttq6TmWzgs", "test");
		try {
			System.out.println(">> " + new String(CFM));
			byte[] sfm = m.evaluateResponse(CFM);
			System.out.println("<< " + new String(sfm));
			Assert.assertArrayEquals(SFM, sfm);
			System.out.println(">> " + new String(CSM));
			byte[] ssm = m.evaluateResponse(CSM);
			System.out.println("<< " + new String(ssm));
//			Assert.assertArrayEquals(SSM, ssm);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	static class TestCallbackHandler
			implements CallbackHandler {

		public byte[] getBindingData() {
			return bindingData;
		}

		public String getPassword() {
			return password;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAuthorizedId() {
			return authorizedId;
		}

		public void setAuthorizedId(String authorizedId) {
			this.authorizedId = authorizedId;
		}

		public String getSalt() {
			return salt;
		}

		public int getIterations() {
			return iterations;
		}

		public void setIterations(int iterations) {
			this.iterations = iterations;
		}

		private byte[] bindingData;
		private String password;
		private String name = "user@domain.com";
		private String authorizedId = "user@domain.com";
		private String salt;
		private int iterations = 4096;

		public void setBindingData(byte[] bindingData) {
			this.bindingData = bindingData;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public void setSalt(String salt) {
			this.salt = salt;
		}

		public TestCallbackHandler() {
		}

		@Override
		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			for (Callback callback : callbacks) {
				if (callback instanceof XMPPSessionCallback) {
				} else if (callback instanceof ChannelBindingCallback) {
					if (((ChannelBindingCallback) callback).getRequestedBindType() ==
							AbstractSaslSCRAM.BindType.tls_unique) {
						((ChannelBindingCallback) callback).setBindingData(bindingData);
					}
				} else if (callback instanceof PBKDIterationsCallback) {
					((PBKDIterationsCallback) callback).setInterations(iterations);
				} else if (callback instanceof SaltedPasswordCallback) {
					try {
						byte[] r = AbstractSaslSCRAM.hi("SHA1", password.getBytes(), Base64.decode(salt), iterations);
						((SaltedPasswordCallback) callback).setSaltedPassword(r);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				} else if (callback instanceof NameCallback) {
					((NameCallback) callback).setName(name);
				} else if (callback instanceof SaltCallback) {
					((SaltCallback) callback).setSalt(Base64.decode(salt));
				} else if (callback instanceof AuthorizeCallback) {
					((AuthorizeCallback) callback).setAuthorized(true);
					((AuthorizeCallback) callback).setAuthorizedID(authorizedId);
				} else {
					throw new UnsupportedCallbackException(callback, "Unrecognized Callback " + callback);
				}
			}
		}
	}

}
