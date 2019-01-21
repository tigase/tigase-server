/**
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
package tigase.auth.credentials.entries;

import org.junit.Test;
import tigase.xmpp.jid.BareJID;

import static org.junit.Assert.assertTrue;

public class MD5UserIdPasswordCredentialsEntryTest {

	@Test
	public void testEncodingAndDecoding() {
		String testPassword = "some-password-do-protect";
		BareJID user = BareJID.bareJIDInstanceNS("user@domain");

		MD5UserIdPasswordCredentialsEntry.Encoder encoder = new MD5UserIdPasswordCredentialsEntry.Encoder();
		String encPassword = encoder.encode(user, testPassword);
		System.out.println(encPassword);

		MD5UserIdPasswordCredentialsEntry.Decoder decoder = new MD5UserIdPasswordCredentialsEntry.Decoder();
		MD5UserIdPasswordCredentialsEntry entry = (MD5UserIdPasswordCredentialsEntry) decoder.decode(user, encPassword);

		assertTrue(entry.verifyPlainPassword(testPassword));
	}

	@Test
	public void testDecodingOfStoredValue() {
		String testPassword = "some-password-do-protect";
		BareJID user = BareJID.bareJIDInstanceNS("user@domain");

		String encPassword = "29681c1fd36931cff65deb22d77c115d";
		MD5UserIdPasswordCredentialsEntry.Decoder decoder = new MD5UserIdPasswordCredentialsEntry.Decoder();
		MD5UserIdPasswordCredentialsEntry entry = (MD5UserIdPasswordCredentialsEntry) decoder.decode(user, encPassword);

		assertTrue(entry.verifyPlainPassword(testPassword));
	}

}
