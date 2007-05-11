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

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Packet;

/**
 * Describe class AbstractReceiverTask here.
 *
 *
 * Created: Fri May 11 08:34:04 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class AbstractReceiverTask implements ReceiverTaskIfc {

  private static Logger log =
		Logger.getLogger("tigase.server.sreceiver.AbstractReceiverTask");

	private String jid = null;
	private String description = null;
	protected Map<String, Object> props = null;

	// Implementation of tigase.server.sreceiver.ReceiverTaskIfc

	/**
	 * Describe <code>getInstance</code> method here.
	 *
	 * @return a <code>ReceiverTaskIfc</code> value
	 */
	public ReceiverTaskIfc getInstance() {
		try {
			return getClass().newInstance();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can't instantiate receiver task: " +
				getClass().getName(), e);
			return null;
		} // end of try-catch
	}

	/**
	 * Describe <code>setJID</code> method here.
	 *
	 * @param jid a <code>String</code> value
	 */
	public void setJID(final String jid) {
		this.jid = jid;
	}

	/**
	 * Describe <code>getJID</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	public String getJID() {
		return jid;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Describe <code>setParams</code> method here.
	 *
	 * @param map a <code>Map</code> value
	 */
	public void setParams(final Map<String, Object> map) {
		description = (String)map.get(DESCRIPTION_PROP_KEY);
		props = map;
	}

	/**
	 * Describe <code>getParams</code> method here.
	 *
	 * @return a <code>Map</code> value
	 */
	public Map<String, Object> getParams() {
		return props;
	}

	/**
	 * Describe <code>processPacket</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param results a <code>Queue</code> value
	 */
	public void processPacket(final Packet packet, final Queue results) {

	}

} // AbstractReceiverTask
