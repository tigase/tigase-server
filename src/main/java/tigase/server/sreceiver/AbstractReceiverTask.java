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
import java.util.HashMap;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.util.JID;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;

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
	private Map<String, Object> props = null;

	private SubscrRestrictions subsc_restr = SUBSCR_RESTRICTIONS_PROP_VAL;
	private SenderRestrictions send_restr = ALLOWED_SENDERS_PROP_VAL;
	private MessageType message_type = MESSAGE_TYPE_PROP_VAL;
	private boolean send_to_online_only = ONLINE_ONLY_PROP_VAL;
	private boolean replace_sender_address = REPLACE_SENDER_PROP_VAL;

	private Map<String, RosterItem> roster = new HashMap<String, RosterItem>();

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

	/**
	 * Describe <code>getDescription</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	public String getDescription() {
		return description;
	}

	public boolean isAllowedToSubscribe(String buddy) {
		return true;
	}

	private RosterItem addToRoster(String jid) {
		String id = JID.getNodeID(jid);
		RosterItem ri = new RosterItem(id);
		roster.put(id, ri);
		return ri;
	}

	private RosterItem removeFromRoster(String jid) {
		return roster.remove(JID.getNodeID(jid));
	}

	private RosterItem getRosterItem(String jid) {
		return roster.get(JID.getNodeID(jid));
	}

	/**
	 * Describe <code>addNewSubscribers</code> method here.
	 *
	 * @param new_subscr a <code>String[]</code> value
	 */
	public void addNewSubscribers(Queue<Packet> results, String... new_subscr) {
		for (String buddy: new_subscr) {
			if (isAllowedToSubscribe(buddy)) {
				if (getRosterItem(buddy) == null) {
					addToRoster(buddy);
				} // end of if (getRosterItem(buddy) == null)
				results.offer(new Packet("presence", jid, buddy, StanzaType.subscribe));
			} else {
				results.offer(new Packet("presence", jid, buddy, StanzaType.unsubscribed));
			} // end of else
		} // end of for (String buddy: new_subscr)
	}

	public void removeSubscribers(Queue<Packet> results, String... new_subscr) {
		for (String buddy: new_subscr) {
			RosterItem ri = removeFromRoster(buddy);
			if (ri != null) {
				results.offer(new Packet("presence", jid, buddy, StanzaType.unsubscribed));
			} // end of if (getRosterItem(buddy) == null)
		} // end of for (String buddy: new_subscr)
	}

	/**
	 * Describe <code>setParams</code> method here.
	 *
	 * @param map a <code>Map</code> value
	 */
	public void setParams(final Map<String, Object> map) {
		this.props = map;
		description = ((String)props.get(DESCRIPTION_PROP_KEY) != null ?
			(String)props.get(DESCRIPTION_PROP_KEY) : description);
		String tmp = (String)props.get(SUBSCR_RESTRICTIONS_PROP_KEY);
		if (tmp != null) {
			subsc_restr = SubscrRestrictions.valueOf(tmp);
		} // end of if (tmp != null)
		tmp = (String)props.get(ALLOWED_SENDERS_PROP_KEY);
		if (tmp != null) {
			send_restr = SenderRestrictions.valueOf(tmp);
		} // end of if (tmp != null)
		tmp = (String)props.get(MESSAGE_TYPE_PROP_KEY);
		if (tmp != null) {
			message_type = MessageType.valueOf(tmp);
		} // end of if (tmp != null)
		send_to_online_only =	(props.get(ONLINE_ONLY_PROP_KEY) != null ?
			(Boolean)props.get(ONLINE_ONLY_PROP_KEY) : send_to_online_only);
		replace_sender_address = (props.get(REPLACE_SENDER_PROP_KEY) != null ?
			(Boolean)props.get(REPLACE_SENDER_PROP_KEY) : replace_sender_address);
		tmp = (String)props.get(TASK_OWNER_PROP_KEY);
		if (tmp != null) {
			RosterItem ri = getRosterItem(tmp);
			if (ri == null) {
				ri = addToRoster(tmp);
				ri.setOwner(true);
			} // end of if (ri == null)
		} // end of if (tmp != null)
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
	public void processPacket(final Packet packet, final Queue<Packet> results) {
		if (packet.getElemName().equals("presence")) {
			processPresence(packet, results);
		} // end of if (packet.getElemName().equals("presence))
		if (packet.getElemName().equals("message")) {
			processMessage(packet, results);
		} // end of if (packet.getElemName().equals("message"))
	}

	private void processPresence(Packet packet, Queue<Packet> results) {
		StanzaType presence_type = StanzaType.available;
		if (packet.getType() != null) {
			presence_type = packet.getType();
		}
		switch (presence_type) {
		case available:
		case unavailable:
			RosterItem ri = getRosterItem(packet.getElemFrom());
			if (ri != null) {
				ri.setOnline(true);
				results.offer(packet.swapElemFromTo());
			} // end of if (ri != null)
			break;
		case subscribe:
			addNewSubscribers(results, packet.getElemFrom());
			break;
		case subscribed:
			results.offer(new Packet("presence", jid, packet.getElemFrom(),
					StanzaType.available));
			break;
		case unsubscribe:
		case unsubscribed:
			removeSubscribers(results, packet.getElemFrom());
			break;
		default:
			break;
		} // end of switch (packet.getType())
	}

	private void processMessage(Packet packet, Queue<Packet> results) {
		
	}

} // AbstractReceiverTask
