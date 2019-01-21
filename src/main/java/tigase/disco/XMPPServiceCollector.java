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
package tigase.disco;

import tigase.server.AbstractComponentRegistrator;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.ServerComponent;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * Class XMPPServiceCollector
 * <br>
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class XMPPServiceCollector
		extends AbstractComponentRegistrator<XMPPService> {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(XMPPServiceCollector.class.getName());

	private ServiceEntity serviceEntity = null;

	public XMPPServiceCollector() {
		serviceEntity = new ServiceEntity("Tigase", "server", "Session manager");
		serviceEntity.addIdentities(new ServiceIdentity[]{new ServiceIdentity("server", "im",
																			  tigase.server.XMPPServer.NAME + " ver. " +
																					  tigase.server.XMPPServer.getImplementationVersion())});
	}

	@Override
	public void componentAdded(XMPPService component) {
	}

	@Override
	public void componentRemoved(XMPPService component) {
	}

	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof XMPPService;
	}

	@Override
	public void processPacket(final Packet packet, final Queue<Packet> results) {
		if (packet.isXMLNSStaticStr(Iq.IQ_QUERY_PATH, INFO_XMLNS) ||
				packet.isXMLNSStaticStr(Iq.IQ_QUERY_PATH, ITEMS_XMLNS)) {
			JID jid = packet.getStanzaTo();
			JID from = packet.getStanzaFrom();
			String node = packet.getAttributeStaticStr(Iq.IQ_QUERY_PATH, "node");
			Element query = packet.getElement().getChild("query").clone();

			if (packet.isXMLNSStaticStr(Iq.IQ_QUERY_PATH, INFO_XMLNS)) {
				for (XMPPService comp : components.values()) {
					Element resp = comp.getDiscoInfo(node, jid, from);

					if (resp != null) {
						query = resp;

						break;
					}
				}      // end of for ()
			}
			if (packet.isXMLNSStaticStr(Iq.IQ_QUERY_PATH, ITEMS_XMLNS)) {
				for (XMPPService comp : components.values()) {
					List<Element> items = comp.getDiscoItems(node, jid, from);

					if ((items != null) && (items.size() > 0)) {
						query.addChildren(items);
					}    // end of if (stats != null && stats.count() > 0)
				}      // end of for ()
			}
			results.offer(packet.okResult(query, 0));
		}
	}
}

