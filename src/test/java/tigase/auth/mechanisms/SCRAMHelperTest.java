package tigase.auth.mechanisms;

import junit.framework.TestCase;
import org.junit.Assert;
import tigase.util.Base64;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SCRAMHelperTest
		extends TestCase {

	public void testEncodePlainPassword() throws NoSuchAlgorithmException, InvalidKeyException {
		var authData = SCRAMHelper.encodePlainPassword("SHA1", Base64.decode("QSXCR+Q6sek8bf92"), 4096, "pencil");
		Assert.assertArrayEquals(Base64.decode("D+CSWLOshSulAsxiupA+qs2/fTE="), authData.serverKey());
		Assert.assertArrayEquals(Base64.decode("6dlGYMOdZcOPutkcNY8U2g7vK9Y="), authData.storedKey());
	}

	public void testTranscode() throws NoSuchAlgorithmException, InvalidKeyException {
		var authData = SCRAMHelper.transcode("SHA1", Base64.decode("HZbuOlKbWl+eR8AfIposuKbhX30="));
		Assert.assertArrayEquals(Base64.decode("D+CSWLOshSulAsxiupA+qs2/fTE="), authData.serverKey());
		Assert.assertArrayEquals(Base64.decode("6dlGYMOdZcOPutkcNY8U2g7vK9Y="), authData.storedKey());
	}
}