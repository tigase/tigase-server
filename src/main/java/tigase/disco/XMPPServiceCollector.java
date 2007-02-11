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

	public XMPPServiceCollector() {}

	public void componentAdded(XMPPService component) {	}

	public boolean isCorrectType(ServerComponent component) {
		return component instanceof XMPPService;
	}

	public void componentRemoved(XMPPService component) {}

	public void processCommand(final Packet packet, final Queue<Packet> results) {
		switch (packet.getCommand()) {
		case GETDISCO:
			Element query = new Element("query");
			String xmlns = Command.getFieldValue(packet, "xmlns");
			String node = Command.getFieldValue(packet, "node");
			String jid = Command.getFieldValue(packet, "jid");
			query.setXMLNS(xmlns);
			if (node != null) {
				query.setAttribute("node", node);
			}
			if (xmlns != null) {
				if (xmlns.equals(INFO_XMLNS)) {
					for (XMPPService comp: components) {
						Element resp = comp.getDiscoInfo(node, jid);
						if (resp != null) {
							query = resp;
							break;
						}
					} // end of for ()
				} else {
					if (xmlns.equals(ITEMS_XMLNS)) {
						for (XMPPService comp: components) {
							List<Element> items =	comp.getDiscoItems(node, jid);
							if (items != null && items.size() > 0) {
									query.addChildren(items);
							} // end of if (stats != null && stats.count() > 0)
						} // end of for ()
					} else {
						log.warning("Uknown GETDISCO xmlns: " + xmlns);
					}
				}
			} else {
				log.warning("Wrong GETDISCO xmlns: " + xmlns);
			}
			Packet result = packet.commandResult("result");
			Command.setData(result, query);
			Command.addFieldValue(result, "jid", jid);
			results.offer(result);
			break;
		default:
			break;
		} // end of switch (packet.getCommand())
	}

}
