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
}
