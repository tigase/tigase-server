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
package tigase.auth.mechanisms;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaslANONYMOUSTest
		extends TestCase {

	private SaslANONYMOUS sasl;

	@Override
	@Before
	public void setUp() {
		Map<? super String, ?> props = new HashMap<String, Object>();
		CallbackHandler callbackHandler = new CallbackHandler() {

			@Override
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback callback : callbacks) {
					if (callback instanceof NameCallback) {
						((NameCallback) callback).setName("somerandomname@domain.com");
					} else {
						throw new UnsupportedCallbackException(callback);
					}
				}
			}
		};
		this.sasl = new SaslANONYMOUS(props, callbackHandler);
	}

	@Test
	public void testSuccess() {

		try {
			sasl.evaluateResponse("".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		assertTrue(sasl.isComplete());
		assertEquals("somerandomname@domain.com", sasl.getAuthorizationID());
		assertTrue((Boolean) sasl.getNegotiatedProperty(SaslANONYMOUS.IS_ANONYMOUS_PROPERTY));

	}
}
