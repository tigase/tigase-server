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
package tigase.xmpp.impl.push;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EncryptedPushNotificationExtensionTest {

	// for testing message truncation
	@Test
	public void testMessageBodyTruncation() {
		int maxSize = 3000;
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<1000; i++) {
			sb.append("\uD83D\uDE21");
		}
		for (int i=0; i<(8000-64); i++) {
			if (i % 10 ==0) {
				sb.append("\uD83D\uDE21");
			} else {
				sb.append(String.valueOf(i % 10));
			}
		}
		String origBody = sb.toString();
		String body = EncryptedPushNotificationExtension.trimBodyToSize(maxSize, origBody);
		assertTrue(body.getBytes(StandardCharsets.UTF_8).length <= 3000);
		assertTrue(origBody.contains(body));
	}

	@Test
	public void testMessageBodyTruncation1() {
		runCharTest(1, "\u0800", 2999);
	}

	@Test
	public void testMessageBodyTruncation2() {
		runCharTest(2, "\uD83D\uDC75\uD83C\uDFFB", 2999);
	}

	@Test
	public void testMessageBodyTruncation3() {
		runCharTest(3, "\uD83D\uDE21", 2999);
	}

	@Test
	public void testMessageBodyTruncation4() {
		runCharTest(4, "\u07DF", 2999);
	}

	@Test
	public void testMessageBodyTruncation5() {
		runCharTest(5, "\uFFFD", 2999);
	}

	@Test
	public void testJsonEncoding() {
		String body = "To be, \\ or \"not\" to \b\t\r be:\n that is / the question";
		StringBuilder sb = new StringBuilder();
		EncryptedPushNotificationExtension.valueToString(body, sb);
		assertEquals("\"To be, \\\\ or \\\"not\\\" to \\b\\t\\r be:\\n that is \\/ the question\"", sb.toString());
	}
	
	private void runCharTest(int tryNo, String ch, int maxSize) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<5000; i++) {
			sb.append(ch);
		}
		String origBody = sb.toString();
		String body = EncryptedPushNotificationExtension.trimBodyToSize(maxSize, origBody);
		// DO NOT REMOVE: left intentionally for better analysis of the issue with a code when needed
		//System.out.println("" + tryNo + ": maxSize: " + maxSize + ", size: " + body.getBytes(StandardCharsets.UTF_8).length + ", char size:" + ch.toCharArray().length + ":" + ch.getBytes(StandardCharsets.UTF_8).length);
		assertTrue(body.getBytes(StandardCharsets.UTF_8).length <= 3000);
		assertTrue(origBody.contains(body));
	}

}
