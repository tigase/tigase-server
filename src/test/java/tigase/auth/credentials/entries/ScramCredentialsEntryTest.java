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

public class ScramCredentialsEntryTest {

	@Test
	public void testEncodingAndDecoding() {
		String testPassword = "some-password-do-protect";
		BareJID user = BareJID.bareJIDInstanceNS("user@domain");

		ScramCredentialsEntry.Encoder encoder = new ScramCredentialsEntry.Encoder("SHA1");
		String encPassword = encoder.encode(user, testPassword);

		ScramCredentialsEntry.Decoder decoder = new ScramCredentialsEntry.Decoder("SHA1");
		ScramCredentialsEntry entry = (ScramCredentialsEntry) decoder.decode(user, encPassword);

		assertTrue(entry.verifyPlainPassword(testPassword));
	}

	@Test
	public void testDecodingOfStoredValue() {
		String testPassword = "some-password-do-protect";
		BareJID user = BareJID.bareJIDInstanceNS("user@domain");

		String encPassword = "s=a526P5eUQMim7g==,i=4096,p=lMVlJo/obJ9xI7P9+vZdbwrHjoA=";
		ScramCredentialsEntry.Decoder decoder = new ScramCredentialsEntry.Decoder("SHA1");
		ScramCredentialsEntry entry = (ScramCredentialsEntry) decoder.decode(user, encPassword);

		assertTrue(entry.verifyPlainPassword(testPassword));
	}

}
