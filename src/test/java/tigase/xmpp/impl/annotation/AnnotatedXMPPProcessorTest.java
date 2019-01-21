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
package tigase.xmpp.impl.annotation;

import org.junit.Assert;
import org.junit.Test;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.SessionBind;

/**
 * @author andrzej
 */
public class AnnotatedXMPPProcessorTest {

	@Test
	public void test1() {
		TestAnnotatedXMPPProcessor test1 = new TestAnnotatedXMPPProcessor();
		for (String[] path : test1.supElementNamePaths()) {
			Assert.assertArrayEquals("Wrong element paths", new String[]{"iq", "query"}, path);
		}
		Assert.assertArrayEquals("Wrong xmlnss", new String[]{"tigase:test1", "tigase:test2"}, test1.supNamespaces());

		Assert.assertArrayEquals("Wrong disco features", new Element[]{
										 new Element("feature", new String[]{"var"}, new String[]{"tigase:test1"}),
										 new Element("feature", new String[]{"var"}, new String[]{"tigase:test2"})},
								 test1.supDiscoFeatures(null));
		Assert.assertArrayEquals("Wrong stream features", new Element[]{
										 new Element("bind", new String[]{"xmlns"}, new String[]{"urn:ietf:params:xml:ns:xmpp-bind"})},
								 test1.supStreamFeatures(null));
		Assert.assertTrue("Stanza type not set as 'get'", test1.supTypes().contains(StanzaType.get));
	}

	@Test
	public void test2() {
		TestAnnotatedXMPPProcessor2 test2 = new TestAnnotatedXMPPProcessor2();
		Assert.assertNull("Wrong element paths, should not be inherited", test2.supElementNamePaths());

		Assert.assertNull("Wrong xmlnss", test2.supNamespaces());

		Assert.assertNull("Wrong disco features", test2.supDiscoFeatures(null));
		Assert.assertNull("Wrong stream features", test2.supStreamFeatures(null));
	}

	@Test
	public void testSessionBind() throws NotAuthorizedException, TigaseStringprepException {
		SessionBind sessionBind = new SessionBind();
		Assert.assertEquals("Wrong processor id", "urn:ietf:params:xml:ns:xmpp-session", sessionBind.id());
		for (String[] path : sessionBind.supElementNamePaths()) {
			Assert.assertArrayEquals("Wrong element paths", new String[]{"iq", "session"}, path);
		}
		Assert.assertArrayEquals("Wrong xmlnss", new String[]{"urn:ietf:params:xml:ns:xmpp-session"},
								 sessionBind.supNamespaces());

		Assert.assertArrayEquals("Wrong disco features", new Element[]{
										 new Element("feature", new String[]{"var"}, new String[]{"urn:ietf:params:xml:ns:xmpp-session"})},
								 sessionBind.supDiscoFeatures(null));

		final XMPPResourceConnection session = getSession();

		Element xmppSession = new Element("session");
		xmppSession.setXMLNS("urn:ietf:params:xml:ns:xmpp-session");
		xmppSession.addChild(new Element("optional"));

		Assert.assertArrayEquals("Lack optional child", new Element[]{xmppSession},
								 sessionBind.supStreamFeatures(session));
		session.putSessionData("Session-Set", "true");
		Assert.assertArrayEquals("Lack optional child", null, sessionBind.supStreamFeatures(session));
	}

	private XMPPResourceConnection getSession() throws NotAuthorizedException, TigaseStringprepException {
		XMPPResourceConnection conn = new XMPPResourceConnection(null, null, null, null) {
			@Override
			public boolean isAuthorized() {
				return true;
			}
		};
		return conn;
	}
}
