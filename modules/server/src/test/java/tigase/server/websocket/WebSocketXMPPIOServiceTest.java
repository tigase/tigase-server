/*
 * WebSocketXMPPIOServiceTest.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */
package tigase.server.websocket;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Created by andrzej on 18.07.2016.
 */
public class WebSocketXMPPIOServiceTest extends TestCase {

	private HashMap<String, String> headers;
	private WebSocketXMPPIOService service;

	@Override
	protected void setUp() throws Exception {
		service = new WebSocketXMPPIOService(new WebSocketProtocolIfc[0]);
		headers = new HashMap<String, String>();
		headers.put("Connection", "Upgrade");
		headers.put("Host", "test.example.com:5291");
		headers.put("Origin", "test.example.com:5291");
		headers.put("Sec-WebSocket-Key", "JRqGsrthbnle6zl8sFQPpQ==");
		headers.put("Sec-WebSocket-Protocol", "xmpp");
		headers.put("Sec-WebSocket-Version", "13");
		headers.put("Upgrade", "websocket");
	}

	@Override
	protected void tearDown() throws Exception {
		service = null;
	}

	@Test
	public void testHttpHeadersParsingWithSpaces() throws UnsupportedEncodingException {
		byte[] data = prepareHTTPRequest(headers, true);

		Map<String, String> parsedHeaders = new HashMap<>();
		service.parseHttpHeaders(data, parsedHeaders);

		assertMaps(headers, parsedHeaders);
	}

	@Test
	public void testHttpHeadersParsingWithoutSpaces() throws UnsupportedEncodingException {
		byte[] data = prepareHTTPRequest(headers, false);

		Map<String, String> parsedHeaders = new HashMap<>();
		service.parseHttpHeaders(data, parsedHeaders);

		assertMaps(headers, parsedHeaders);
	}

	private void assertMaps(Map<String, String> expected, Map<String, String> actual) {
		List<String> expectedKeys = new ArrayList<>(expected.keySet());
		List<String> actualKeys = new ArrayList<>(actual.keySet());

		Collections.sort(expectedKeys);
		Collections.sort(actualKeys);

		assertEquals(expectedKeys, actualKeys);

		for (String key : expectedKeys) {
			assertEquals(expected.get(key), actual.get(key));
		}
	}

	private byte[] prepareHTTPRequest(Map<String,String> headers, boolean useSpaces) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder("GET HTTP/1.1\r\n");
		for (Map.Entry<String,String> e : headers.entrySet()) {
			sb.append(e.getKey());
			sb.append(':');
			if (useSpaces) {
				sb.append(' ');
			}
			sb.append(e.getValue()).append("\r\n");
		}
		sb.append("\r\n");
		return sb.toString().getBytes("UTF-8");
	}
}
