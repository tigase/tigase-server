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

package tigase.server;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;
import tigase.server.Command;
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
public class XMPPServiceCollector extends AbstractComponentRegistrator {

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

	public void componentAdded(ServerComponent component) {	}

	public void componentRemoved(ServerComponent component) {}

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
					if (node == null) {
						Element identity = new Element("identity",
							new String[] {"category", "type", "name"},
							new String[] {"server", "im", "Tigase"});
						query.addChild(identity);
					}
					HashSet<Element> all_features = new HashSet<Element>();
					for (ServerComponent comp: components) {
						if (comp instanceof XMPPService) {
							List<Element> features =
								((XMPPService)comp).getDiscoFeatures(node, jid);
							if (features != null && features.size() > 0) {
								all_features.addAll(features);
							} // end of if (stats != null && stats.count() > 0)
						} // end of if (component instanceof Configurable)
					} // end of for ()
					for (Element f: all_features) {
// 						Element feature = new Element("feature",
// 							new String[] {"var"}, new String[] {f});
						query.addChild(f);
					}
				} else {
					if (xmlns.equals(ITEMS_XMLNS)) {
						for (ServerComponent comp: components) {
							if (comp instanceof XMPPService) {
								List<Element> items =
									((XMPPService)comp).getDiscoItems(node, jid);
								if (items != null && items.size() > 0) {
									query.addChildren(items);
								} // end of if (stats != null && stats.count() > 0)
							} // end of if (component instanceof Configurable)
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
