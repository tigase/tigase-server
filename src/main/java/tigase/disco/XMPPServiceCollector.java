/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.disco;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;
import tigase.server.ServerComponent;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.MessageRouter;
import tigase.server.AbstractComponentRegistrator;
import tigase.stats.StatRecord;
import tigase.stats.StatisticsContainer;
import tigase.xml.Element;

/**
 * Class XMPPServiceCollector
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPServiceCollector
	extends AbstractComponentRegistrator<XMPPService> {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.XMPPServiceCollector");

	public static final String INFO_XMLNS =
		"http://jabber.org/protocol/disco#info";
	public static final String ITEMS_XMLNS =
		"http://jabber.org/protocol/disco#items";

	private ServiceEntity serviceEntity = null;

	public XMPPServiceCollector() {
		serviceEntity = new ServiceEntity("Tigase", "server", "Session manager");
		serviceEntity.addIdentities(new ServiceIdentity[] {
				new ServiceIdentity("server", "im", tigase.server.XMPPServer.NAME +
					" ver. " + tigase.server.XMPPServer.getImplementationVersion())});
	}

	public void componentAdded(XMPPService component) {	}

	public boolean isCorrectType(ServerComponent component) {
		return component instanceof XMPPService;
	}

	public void componentRemoved(XMPPService component) {}

	public void processPacket(final Packet packet, final Queue<Packet> results) {

		if (packet.isXMLNS("/iq/query", INFO_XMLNS)
			|| packet.isXMLNS("/iq/query", ITEMS_XMLNS)) {

			String jid = packet.getElemTo();
			String node = packet.getAttribute("/iq/query", "node");
			Element query =
				(Element)packet.getElement().getChild("query").clone();

			if (packet.isXMLNS("/iq/query", INFO_XMLNS)) {
				if (node == null && MessageRouter.isLocalDomain(jid)) {
					query = serviceEntity.getDiscoInfo(null);
				} else {
					for (XMPPService comp: components.values()) {
						Element resp = comp.getDiscoInfo(node, jid);
						if (resp != null) {
							query = resp;
							break;
						}
					} // end of for ()
				}
			}

			if (packet.isXMLNS("/iq/query", ITEMS_XMLNS)) {
				for (XMPPService comp: components.values()) {
					List<Element> items =	comp.getDiscoItems(node, jid);
					if (items != null && items.size() > 0) {
						query.addChildren(items);
					} // end of if (stats != null && stats.count() > 0)
				} // end of for ()
			}
			results.offer(packet.okResult(query, 0));
		}

	}

}
