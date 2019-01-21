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
package tigase.server.xmppserver.proc;

import tigase.kernel.beans.Bean;
import tigase.server.xmppserver.S2SConnectionManager;
import tigase.server.xmppserver.S2SIOService;
import tigase.xml.Element;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created: Dec 9, 2010 2:01:12 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = "startZlib", parent = S2SConnectionManager.class, active = true)
public class StartZlib
		extends S2SAbstractProcessor {

	private static final Logger log = Logger.getLogger(StartZlib.class.getName());
	private static final Element features = new Element("compression", new Element[]{new Element("method", "zlib")},
														new String[]{"xmlns"},
														new String[]{"http://jabber.org/features/compress"});

	@Override
	public int order() {
		return Order.StartZlib.ordinal();
	}

	@Override
	public void streamFeatures(S2SIOService serv, List<Element> results) {

		// results.add(features);
	}
}
