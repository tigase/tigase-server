/*  Tigase Project
 *  Copyright (C) 2001-2007
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
package tigase.server.sreceiver;

import java.util.Queue;
import tigase.server.Packet;

/**
 * Describe class NewsDistributor here.
 *
 *
 * Created: Fri May 11 08:53:16 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class NewsDistributor extends AbstractReceiverTask {

	public static final String TASK_TYPE = "News Distribution";
	public static final String TASK_HELP =
		"The task acts as a newsletter over Jabber/XMPP protocol. Users can"
		+ " subscribe to the news and unsubscribe in very simple way - they have to"
		+ " add the task to their roster or remove. Then they will be receiving"
		+ " news information.";

	public String getType() {
		return TASK_TYPE;
	}

	public String getHelp() {
		return TASK_HELP;
	}

	/**
	 * Describe <code>processPacket</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param results a <code>Queue</code> value
	 */
	public void processPacket(final Packet packet, final Queue<Packet> results) {
		super.processPacket(packet, results);
	}

} // NewsDistributor
