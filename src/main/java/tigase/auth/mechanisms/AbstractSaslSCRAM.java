package tigase.auth.mechanisms;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.SaslException;

import tigase.auth.XmppSaslException;
import tigase.auth.XmppSaslException.SaslError;
import tigase.auth.callbacks.PBKDIterationsCallback;
import tigase.auth.callbacks.SaltCallback;
import tigase.auth.callbacks.SaltedPasswordCallback;
import tigase.util.Base64;

public abstract class AbstractSaslSCRAM extends AbstractSasl {

	private enum Step {
		clientFinalMessage,
		clientFirstMessage,
		finished;
	}

	private final static String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	private static final Charset CHARSET = Charset.forName("UTF-8");

	private final static Pattern CLIENT_FIRST_MESSAGE = Pattern.compile("^(?<gs2Header>(?:y|n|p=(?<cbName>[a-zA-z0-9.-]+)),"
			+ "(?:a=(?<authzid>(?:[\\x21-\\x2B\\x2D-\\x7E]|=2C|=3D)+))?,)(?<clientFirstBare>(?<mext>m=[^\\000=]+,)"
			+ "?n=(?<username>(?:[\\x21-\\x2B\\x2D-\\x7E]|=2C|=3D)+),r=(?<nonce>[\\x21-\\x2B\\x2D-\\x7E]+)(?:,.*)?)$");

	private final static Pattern CLIENT_LAST_MESSAGE = Pattern.compile("^(?<withoutPoof>c=(?<cb>[a-zA-Z0-9/+=]+),"
			+ "(?:r=(?<nonce>[\\x21-\\x2B\\x2D-\\x7E]+))(?:,.*)?),p=(?<proof>[a-zA-Z0-9/+=]+)$");

	public static byte[] hi(String algorithm, byte[] password, final byte[] salt, final int iterations)
			throws InvalidKeyException, NoSuchAlgorithmException {
		final SecretKeySpec k = new SecretKeySpec(password, "Hmac" + algorithm);

		byte[] z = new byte[salt.length + 4];
		System.arraycopy(salt, 0, z, 0, salt.length);
		System.arraycopy(new byte[] { 0, 0, 0, 1 }, 0, z, salt.length, 4);

		byte[] u = hmac(k, z);
		byte[] result = new byte[u.length];
		System.arraycopy(u, 0, result, 0, result.length);

		int i = 1;
		while (i < iterations) {
			u = hmac(k, u);
			for (int j = 0; j < u.length; j++) {
				result[j] ^= u[j];
			}
			++i;
		}

		return result;
	}

	protected static byte[] hmac(final SecretKey key, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
		Mac mac = Mac.getInstance(key.getAlgorithm());
		mac.init(key);
		return mac.doFinal(data);
	}

	public static byte[] normalize(String str) {
		return str.getBytes(CHARSET);
	}

	private final String algorithm;

	private String cfmAuthzid;

	private String cfmBareMessage;

	private String cfmCbname;

	private String cfmGs2header;

	private String cfmUsername;

	private byte[] clientKey;

	private final byte[] clientKeyData;

	private final String mechanismName;

	private Random random = new SecureRandom();

	private byte[] saltedPassword;

	private final byte[] serverKeyData;

	private final String serverNonce;

	private String sfmMessage;

	private String sfmNonce;

	private Step step = Step.clientFirstMessage;

	private byte[] storedKey;

	protected AbstractSaslSCRAM(String mechanismName, String algorithm, byte[] clientKey, byte[] serverKey,
			Map<? super String, ?> props, CallbackHandler callbackHandler) {
		super(props, callbackHandler);
		this.mechanismName = mechanismName;
		this.algorithm = algorithm;
		this.clientKeyData = clientKey;
		this.serverKeyData = serverKey;
		serverNonce = randomString();
	}

	protected AbstractSaslSCRAM(String mechanismName, String algorithm, byte[] clientKey, byte[] serverKey,
			Map<? super String, ?> props, CallbackHandler callbackHandler, String serverOnce) {
		super(props, callbackHandler);
		this.mechanismName = mechanismName;
		this.algorithm = algorithm;
		this.clientKeyData = clientKey;
		this.serverKeyData = serverKey;
		this.serverNonce = serverOnce;
	}

	@Override
	public byte[] evaluateResponse(byte[] response) throws SaslException {
		try {
			switch (step) {
			case clientFirstMessage:
				return processClientFirstMessage(response);
			case clientFinalMessage:
				return processClientLastMessage(response);
			default:
				throw new SaslException(getMechanismName() + ": Server at illegal state");
			}
		} catch (SaslException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new SaslException("SASL Failed", e);
		}
	}

	@Override
	public String getAuthorizationID() {
		return authorizedId;
	}

	@Override
	public String getMechanismName() {
		return mechanismName;
	}

	protected byte[] h(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(algorithm);
		return digest.digest(data);
	}

	protected SecretKey key(final byte[] key) {
		return new SecretKeySpec(key, "Hmac" + algorithm);
	}

	protected byte[] processClientFirstMessage(byte[] data) throws SaslException, InvalidKeyException, NoSuchAlgorithmException {
		Matcher r = CLIENT_FIRST_MESSAGE.matcher(new String(data));
		if (!r.matches())
			throw new SaslException("Bad challenge syntax");

		this.cfmGs2header = r.group("gs2Header");
		this.cfmCbname = r.group("cbName");
		this.cfmAuthzid = r.group("authzid");
		this.cfmBareMessage = r.group("clientFirstBare");
		final String cfmMext = r.group("mext");
		this.cfmUsername = r.group("username");
		final String cfmNonce = r.group("nonce");

		if (this.cfmAuthzid == null)
			this.cfmAuthzid = this.cfmUsername;

		final NameCallback nc = new NameCallback("Authentication identity", cfmUsername);
		final PBKDIterationsCallback ic = new PBKDIterationsCallback("PBKD2 iterations");
		final SaltCallback sc = new SaltCallback("Salt");
		final SaltedPasswordCallback pc = new SaltedPasswordCallback("Salted password");
		// final PasswordCallback pc = new PasswordCallback("Password", false);

		handleCallbacks(nc, ic, sc, pc);

		if (pc.getSaltedPassword() == null)
			throw new SaslException("Unknown user");

		this.sfmNonce = cfmNonce + serverNonce;

		this.saltedPassword = pc.getSaltedPassword();
		this.clientKey = hmac(key(saltedPassword), clientKeyData);
		this.storedKey = h(clientKey);

		final StringBuilder serverStringMessage = new StringBuilder();
		serverStringMessage.append("r=").append(sfmNonce).append(",");
		serverStringMessage.append("s=").append(Base64.encode(sc.getSalt())).append(",");
		serverStringMessage.append("i=").append(ic.getInterations());

		this.sfmMessage = serverStringMessage.toString();
		step = Step.clientFinalMessage;
		return sfmMessage.getBytes();
	}

	protected byte[] processClientLastMessage(byte[] data) throws SaslException, InvalidKeyException, NoSuchAlgorithmException {
		Matcher r = CLIENT_LAST_MESSAGE.matcher(new String(data));
		if (!r.matches())
			throw new SaslException("Bad challenge syntax");

		final String clmWithoutPoof = r.group("withoutPoof");
		final String clmCb = new String(Base64.decode(r.group("cb")));
		final String clmNonce = r.group("nonce");
		final String clmProof = r.group("proof");

		if (!clmCb.startsWith(this.cfmGs2header)) {
			throw new XmppSaslException(SaslError.not_authorized, "Invalid GS2 header");
		}

		if (!clmNonce.equals(sfmNonce)) {
			throw new XmppSaslException(SaslError.not_authorized, "Wrong nonce");
		}

		final String authMessage = cfmBareMessage + "," + sfmMessage + "," + clmWithoutPoof;
		byte[] clientSignature = hmac(key(storedKey), authMessage.getBytes());
		byte[] clientProof = xor(clientKey, clientSignature);

		byte[] dcp = Base64.decode(clmProof);
		boolean proofMatch = Arrays.equals(clientProof, dcp);

		if (proofMatch == false) {
			throw new XmppSaslException(SaslError.not_authorized, "Password not verified");
		}

		final AuthorizeCallback ac = new AuthorizeCallback(cfmUsername, cfmAuthzid);
		handleCallbacks(ac);
		if (ac.isAuthorized() == true) {
			authorizedId = ac.getAuthorizedID();
		} else {
			throw new XmppSaslException(SaslError.invalid_authzid, "SCRAM: " + cfmAuthzid + " is not authorized to act as "
					+ cfmAuthzid);
		}

		byte[] serverKey = hmac(key(saltedPassword), serverKeyData);
		byte[] serverSignature = hmac(key(serverKey), authMessage.getBytes());

		final StringBuilder serverStringMessage = new StringBuilder();
		serverStringMessage.append("v=").append(Base64.encode(serverSignature));
		step = Step.finished;
		complete = true;
		return serverStringMessage.toString().getBytes();
	}

	private String randomString() {
		final int length = 20;
		final int x = ALPHABET.length();
		char[] buffer = new char[length];
		for (int i = 0; i < length; i++) {
			int r = random.nextInt(x);
			buffer[i] = ALPHABET.charAt(r);
		}
		return new String(buffer);
	}

	@Override
	public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
		return null;
	}

	@Override
	public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
		return null;
	}

	protected byte[] xor(final byte[] a, final byte[] b) {
		final int l = a.length;
		byte[] r = new byte[l];
		for (int i = 0; i < l; i++) {
			r[i] = (byte) (a[i] ^ b[i]);
		}
		return r;
	}

}
