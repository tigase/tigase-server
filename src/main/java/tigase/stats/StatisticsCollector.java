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

package tigase.stats;

import tigase.xml.Element;
import java.util.Map;
import java.util.Queue;
import java.util.List;
import tigase.server.AbstractComponentRegistrator;
import tigase.server.Packet;
import tigase.server.ServerComponent;
import tigase.server.XMPPService;
import tigase.server.Command;

/**
 * Class StatisticsCollector
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StatisticsCollector
	extends AbstractComponentRegistrator<StatisticsContainer> {
	//	implements XMPPService {

  public StatisticsCollector() {}

	public Map<String, String> getStatistics() { return null; }

	public void componentAdded(StatisticsContainer component) {	}

	public boolean isCorrectType(ServerComponent component) {
		return component instanceof StatisticsContainer;
	}

	public void componentRemoved(StatisticsContainer component) {}

	public void processCommand(final Packet packet, final Queue<Packet> results) {
		switch (packet.getCommand()) {
		case GETSTATS:
			Element statistics = new Element("statistics");
			for (StatisticsContainer comp: components) {
				List<StatRecord> stats = comp.getStatistics();
				if (stats != null && stats.size() > 0) {
					// 						Element component = new Element("component");
					// 						component.setAttribute("name", comp.getName());
					for (StatRecord record: stats) {
						Element item = new Element("stat");
						item.addAttribute("name", comp.getName() + "/"
							+ record.getDescription());
						item.addAttribute("units", record.getUnit());
						item.addAttribute("value", record.getValue());
						statistics.addChild(item);
					} // end of for ()
					// 						statistics.addChild(component);
				} // end of if (stats != null && stats.count() > 0)
			} // end of for ()
			Packet result = packet.commandResult("result");
			Command.setData(result, statistics);
			results.offer(result);
			break;
		default:
			break;
		} // end of switch (packet.getCommand())
	}

}
