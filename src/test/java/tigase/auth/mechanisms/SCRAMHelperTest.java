/*
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