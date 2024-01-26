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
package tigase.auth.credentials.entries;

import org.junit.Test;
import tigase.util.Base64;
import tigase.xmpp.jid.BareJID;

import static org.junit.Assert.*;

public class ScramCredentialsEntryTest {

	@Test
	public void testDecoding() {
		BareJID user = BareJID.bareJIDInstanceNS("user@domain");

		String encPassword = "s=QSXCR+Q6sek8bf92,i=123,t=6dlGYMOdZcOPutkcNY8U2g7vK9Y=,e=D+CSWLOshSulAsxiupA+qs2/fTE=";
		ScramCredentialsEntry.Decoder decoder = new ScramCredentialsEntry.Decoder("SHA1");
		ScramCredentialsEntry entry = decoder.decode(user, encPassword);

		assertEquals("6dlGYMOdZcOPutkcNY8U2g7vK9Y=", Base64.encode(entry.getStoredKey()));
		assertEquals("D+CSWLOshSulAsxiupA+qs2/fTE=", Base64.encode(entry.getServerKey()));
		assertEquals("QSXCR+Q6sek8bf92", Base64.encode(entry.getSalt()));
		assertEquals(123, entry.getIterations());
	}

	@Test
	public void testDecodingOfStoredKeysValue() {
		String testPassword = "pencil";
		BareJID user = BareJID.bareJIDInstanceNS("user@domain");

		String encPassword = "s=QSXCR+Q6sek8bf92,i=4096,t=6dlGYMOdZcOPutkcNY8U2g7vK9Y=,e=D+CSWLOshSulAsxiupA+qs2/fTE=";
		ScramCredentialsEntry.Decoder decoder = new ScramCredentialsEntry.Decoder("SHA1");
		ScramCredentialsEntry entry = decoder.decode(user, encPassword);

		assertEquals("6dlGYMOdZcOPutkcNY8U2g7vK9Y=", Base64.encode(entry.getStoredKey()));
		assertEquals("D+CSWLOshSulAsxiupA+qs2/fTE=", Base64.encode(entry.getServerKey()));
		assertEquals("QSXCR+Q6sek8bf92", Base64.encode(entry.getSalt()));
		assertEquals(4096, entry.getIterations());
		assertTrue(entry.verifyPlainPassword(testPassword));
	}

	@Test
	public void testDecodingOfStoredValue() {
		String testPassword = "some-password-do-protect";
		BareJID user = BareJID.bareJIDInstanceNS("user@domain");

		String encPassword = "s=a526P5eUQMim7g==,i=4096,p=lMVlJo/obJ9xI7P9+vZdbwrHjoA=";
		ScramCredentialsEntry.Decoder decoder = new ScramCredentialsEntry.Decoder("SHA1");
		ScramCredentialsEntry entry = decoder.decode(user, encPassword);

		assertTrue(entry.verifyPlainPassword(testPassword));
	}

	@Test
	public void testDecodingOfValueWithTranscoding() {
		String testPassword = "pencil";
		BareJID user = BareJID.bareJIDInstanceNS("user@domain");

		String encPassword = "s=QSXCR+Q6sek8bf92,i=4096,p=HZbuOlKbWl+eR8AfIposuKbhX30=";
		ScramCredentialsEntry.Decoder decoder = new ScramCredentialsEntry.Decoder("SHA1");
		ScramCredentialsEntry entry = decoder.decode(user, encPassword);

		assertEquals("6dlGYMOdZcOPutkcNY8U2g7vK9Y=", Base64.encode(entry.getStoredKey()));
		assertEquals("D+CSWLOshSulAsxiupA+qs2/fTE=", Base64.encode(entry.getServerKey()));
		assertEquals("QSXCR+Q6sek8bf92", Base64.encode(entry.getSalt()));
		assertEquals(4096, entry.getIterations());
		assertTrue(entry.verifyPlainPassword(testPassword));
	}

	@Test
	public void testEncodingAndDecoding() {
		String testPassword = "some-password-do-protect";
		BareJID user = BareJID.bareJIDInstanceNS("user@domain");

		ScramCredentialsEntry.Encoder encoder = new ScramCredentialsEntry.Encoder("SHA1");
		String encPassword = encoder.encode(user, testPassword);

		ScramCredentialsEntry.Decoder decoder = new ScramCredentialsEntry.Decoder("SHA1");
		ScramCredentialsEntry entry = decoder.decode(user, encPassword);

		assertTrue(entry.verifyPlainPassword(testPassword));
		assertEquals(encPassword, encoder.encode(user, entry));
	}

	@Test
	public void testInvalidPassword() {
		BareJID user = BareJID.bareJIDInstanceNS("user@domain");

		String encPassword = "s=QSXCR+Q6sek8bf92,i=4096,t=6dlGYMOdZcOPutkcNY8U2g7vK9Y=,e=D+CSWLOshSulAsxiupA+qs2/fTE=";
		ScramCredentialsEntry.Decoder decoder = new ScramCredentialsEntry.Decoder("SHA1");
		ScramCredentialsEntry entry = decoder.decode(user, encPassword);

		assertFalse(entry.verifyPlainPassword("crayon"));
	}

	@Test
	public void testTranscodingEntries() {
		BareJID user = BareJID.bareJIDInstanceNS("user@domain");
		String encPassword = "s=QSXCR+Q6sek8bf92,i=4096,p=HZbuOlKbWl+eR8AfIposuKbhX30=";
		ScramCredentialsEntry.Decoder decoder = new ScramCredentialsEntry.Decoder("SHA1");
		ScramCredentialsEntry.Encoder encoder = new ScramCredentialsEntry.Encoder("SHA1");

		ScramCredentialsEntry entry = decoder.decode(user, encPassword);

		String encAuthData = encoder.encode(user, entry);

		assertEquals("s=QSXCR+Q6sek8bf92,i=4096,t=6dlGYMOdZcOPutkcNY8U2g7vK9Y=,e=D+CSWLOshSulAsxiupA+qs2/fTE=",
					 encAuthData);
		assertEquals("6dlGYMOdZcOPutkcNY8U2g7vK9Y=", Base64.encode(entry.getStoredKey()));
		assertEquals("D+CSWLOshSulAsxiupA+qs2/fTE=", Base64.encode(entry.getServerKey()));
		assertEquals("QSXCR+Q6sek8bf92", Base64.encode(entry.getSalt()));
		assertEquals(4096, entry.getIterations());
	}

}
