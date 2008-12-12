/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.sreceiver;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import tigase.xml.Element;
import tigase.server.Packet;
import tigase.stats.StatRecord;
import tigase.util.JIDUtils;
import tigase.xmpp.StanzaType;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import java.util.LinkedList;

import static tigase.server.sreceiver.PropertyConstants.*;
import static tigase.server.sreceiver.TaskCommons.*;

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
	private String name = null;
	private String local_domain = null;
	private String description = null;
	private Map<String, PropertyItem> props = null;

	private SubscrRestrictions subsc_restr = SUBSCR_RESTRICTIONS_PROP_VAL;
	private SenderRestrictions send_restr = ALLOWED_SENDERS_PROP_VAL;
	private MessageType message_type = MESSAGE_TYPE_PROP_VAL;
	private boolean send_to_online_only = ONLINE_ONLY_PROP_VAL;
	private SenderAddress replace_sender_address = REPLACE_SENDER_PROP_VAL;
	private Pattern subscr_restr_regex =
		Pattern.compile(SUBSCR_RESTR_REGEX_PROP_VAL);
	private String owner = TASK_OWNER_PROP_VAL;
	private String[] admins = {};
	private StanzaReceiverIfc srecv = null;

	private long packets_received = 0;
	private long packets_sent = 0;

	private Map<String, RosterItem> roster = new HashMap<String, RosterItem>();

	// Implementation of tigase.server.sreceiver.ReceiverTaskIfc

	public void setStanzaReceiver(StanzaReceiverIfc srecv) {
		this.srecv = srecv;
	}

	protected boolean addOutPacket(Packet packet) {
		return srecv.addOutPacket(packet);
	}

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
		log.fine("JID set to: " + this.jid);
		int idx = jid.indexOf(".");
		this.local_domain = jid.substring(idx+1);
		log.fine("Local domain set to: " + this.local_domain);
		this.name = JIDUtils.getNodeNick(jid);
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
		boolean result = false;
		switch (subsc_restr) {
		case LOCAL:
			String buddy_domain = JIDUtils.getNodeHost(buddy);
			if (buddy_domain.equals(local_domain)) {
				result = true;
			} // end of if (buddy_domain.equals(local_domain))
			break;
		case REGEX:
			result = subscr_restr_regex.matcher(buddy).matches();
			break;
		default:
			result = true;
			break;
		} // end of switch (subsc_restr)
		return result;
	}

	public boolean isAllowedToPost(String buddy) {
		boolean result = false;
		RosterItem ri = getRosterItem(buddy);
		switch (send_restr) {
		case SUBSCRIBED:
			result = ri != null && ri.isSubscribed() && ri.isModerationAccepted();
			break;
		case OWNER:
			result = ri != null && ri.isOwner();
			break;
		default:
			result = true;
			break;
		} // end of switch (send_restr)
		return result;
	}

	public void addToRoster(RosterItem ri) {
		roster.put(ri.getJid(), ri);
	}

	public RosterItem addToRoster(String jid) {
		String id = JIDUtils.getNodeID(jid);
		RosterItem ri = new RosterItem(id);
		if (id.equals(owner)) {
			ri.setOwner(true);
		} // end of if (id.equals(owner))
		if (subsc_restr != SubscrRestrictions.MODERATED) {
			ri.setModerationAccepted(true);
		} // end of if (subsc_restr != SubscrRestrictions.MODERATED)
		addToRoster(ri);
		return ri;
	}

	public RosterItem removeFromRoster(String jid) {
		return roster.remove(JIDUtils.getNodeID(jid));
	}

	public RosterItem getRosterItem(String jid) {
		return roster.get(JIDUtils.getNodeID(jid));
	}

	public void setRosterItemOnline(RosterItem ri, boolean online) {
		ri.setOnline(online);
	}

	public void setRosterItemAdmin(RosterItem ri, boolean admin) {
		ri.setAdmin(admin);
	}

	public void setRosterItemOwner(RosterItem ri, boolean owner) {
		ri.setOwner(owner);
	}

	public void setRosterItemSubscribed(RosterItem ri, boolean subscribed) {
		log.fine(getJID() + ": " +
			"Updating subscription for " + ri.getJid() + " to " + subscribed);
		ri.setSubscribed(subscribed);
	}

	public void setRosterItemModerationAccepted(RosterItem ri, boolean accepted) {
		ri.setModerationAccepted(accepted);
	}

	/**
	 * Describe <code>addNewSubscribers</code> method here.
	 *
	 * @param new_subscr a <code>String[]</code> value
	 */
	public void addNewSubscribers(Queue<Packet> results, String... new_subscr) {
		for (String buddy: new_subscr) {
			Packet presence = null;
			if (isAllowedToSubscribe(buddy)) {
				if (getRosterItem(buddy) == null) {
					addToRoster(buddy);
				} // end of if (getRosterItem(buddy) == null)
				log.info(getJID() + ": " + "Adding buddy to roster: " + buddy);
				presence = getPresence(buddy, jid, StanzaType.subscribe,
					JIDUtils.getNodeNick(jid), null);
			} else {
				log.info(getJID() + ": " +
					"Not allowed to subscribe, rejecting: " + buddy);
				presence = getPresence(buddy, jid, StanzaType.unsubscribed);
			} // end of else
			log.finest(getJID() + ": " +
				"Sending back: " + presence.toString());
			results.offer(presence);
		} // end of for (String buddy: new_subscr)
	}

	public void removeSubscribers(Queue<Packet> results, String... subscr) {
		for (String buddy: subscr) {
			RosterItem ri = removeFromRoster(buddy);
			if (ri != null) {
				log.info(getJID() + ": "
					+ "Removing buddy from roster: " + buddy);
				results.offer(getPresence(buddy, jid, StanzaType.unsubscribed));
			} // end of if (getRosterItem(buddy) == null)
		} // end of for (String buddy: new_subscr)
	}

	/**
	 * Describe <code>setParams</code> method here.
	 *
	 * @param map a <code>Map</code> value
	 */
	public void setParams(final Map<String, Object> map) {
		if (props == null) {
			props = new TreeMap<String, PropertyItem>();
		} // end of if (props == null)
		if (map.get(DESCRIPTION_PROP_KEY) != null) {
			description = (String)map.get(DESCRIPTION_PROP_KEY);
			props.put(DESCRIPTION_PROP_KEY, new PropertyItem(DESCRIPTION_PROP_KEY,
					DESCRIPTION_DISPL_NAME, description));
		} // end of if (map.get(DESCRIPTION_PROP_KEY) != null)
		if (map.get(SUBSCR_RESTR_REGEX_PROP_KEY) != null) {
			subscr_restr_regex =
				Pattern.compile((String)map.get(SUBSCR_RESTR_REGEX_PROP_KEY));
			props.put(SUBSCR_RESTR_REGEX_PROP_KEY,
				new PropertyItem(SUBSCR_RESTR_REGEX_PROP_KEY,
				SUBSCR_RESTR_REGEX_DISPL_NAME, subscr_restr_regex));
		} // end of if (map.get(SUBSCR_RESTR_REGEX_PROP_KEY) != null)
		String tmp = (String)map.get(SUBSCR_RESTRICTIONS_PROP_KEY);
		if (tmp != null) {
			subsc_restr = SubscrRestrictions.valueOf(tmp);
			props.put(SUBSCR_RESTRICTIONS_PROP_KEY,
				new PropertyItem(SUBSCR_RESTRICTIONS_PROP_KEY,
					SUBSCR_RESTRICTIONS_DISPL_NAME, subsc_restr));
		} // end of if (tmp != null)
		tmp = (String)map.get(ALLOWED_SENDERS_PROP_KEY);
		if (tmp != null) {
			send_restr = SenderRestrictions.valueOf(tmp);
			props.put(ALLOWED_SENDERS_PROP_KEY,
				new PropertyItem(ALLOWED_SENDERS_PROP_KEY,
					ALLOWED_SENDERS_DISPL_NAME, send_restr));
		} // end of if (tmp != null)
		tmp = (String)map.get(MESSAGE_TYPE_PROP_KEY);
		if (tmp != null) {
			message_type = MessageType.valueOf(tmp);
			props.put(MESSAGE_TYPE_PROP_KEY,
				new PropertyItem(MESSAGE_TYPE_PROP_KEY,
					MESSAGE_TYPE_DISPL_NAME, message_type));
		} // end of if (tmp != null)
		if (map.get(ONLINE_ONLY_PROP_KEY) != null) {
			send_to_online_only =	parseBool(map.get(ONLINE_ONLY_PROP_KEY));
			props.put(ONLINE_ONLY_PROP_KEY,
				new PropertyItem(ONLINE_ONLY_PROP_KEY,
					ONLINE_ONLY_DISPL_NAME, send_to_online_only));
		} // end of if (map.get(ONLINE_ONLY_PROP_KEY) != null)
		tmp = (String)map.get(REPLACE_SENDER_PROP_KEY);
		if (tmp != null) {
			replace_sender_address = SenderAddress.valueOf(tmp);
			props.put(REPLACE_SENDER_PROP_KEY,
				new PropertyItem(REPLACE_SENDER_PROP_KEY,
					REPLACE_SENDER_DISPL_NAME, replace_sender_address));
		} // end of if (map.get(REPLACE_SENDER_PROP_KEY) != null)
		tmp = (String)map.get(TASK_OWNER_PROP_KEY);
		if (tmp != null && tmp.length() > 0) {
			owner = tmp.trim();
			RosterItem ri = getRosterItem(owner);
			if (ri == null) {
				ri = addToRoster(owner);
			} // end of if (ri == null)
			setRosterItemOwner(ri, true);
			setRosterItemAdmin(ri, true);
			setRosterItemModerationAccepted(ri, true);
			props.put(TASK_OWNER_PROP_KEY,
				new PropertyItem(TASK_OWNER_PROP_KEY, TASK_OWNER_DISPL_NAME, owner));
		}
		if (props.get(TASK_OWNER_PROP_KEY) == null) {
			props.put(TASK_OWNER_PROP_KEY,
				new PropertyItem(TASK_OWNER_PROP_KEY, TASK_OWNER_DISPL_NAME, ""));
		}
		tmp = (String)map.get(TASK_ADMINS_PROP_KEY);
		if (tmp != null && tmp.length() > 0) {
			admins = tmp.split(",");
			for (String admin: admins) {
				RosterItem ri = getRosterItem(admin.trim());
				if (ri == null) {
					ri = addToRoster(admin.trim());
				} // end of if (ri == null)
				setRosterItemAdmin(ri, true);
				setRosterItemModerationAccepted(ri, true);
			} // end of for (String tmp_b: tmp_arr)
			props.put(TASK_ADMINS_PROP_KEY,
				new PropertyItem(TASK_ADMINS_PROP_KEY, TASK_ADMINS_DISPL_NAME, tmp));
		}
		if (props.get(TASK_ADMINS_PROP_KEY) == null) {
			props.put(TASK_ADMINS_PROP_KEY,
				new PropertyItem(TASK_ADMINS_PROP_KEY, TASK_ADMINS_DISPL_NAME, ""));
		}
	}

	/**
	 * Describe <code>getParams</code> method here.
	 *
	 * @return a <code>Map</code> value
	 */
	public Map<String, PropertyItem> getParams() {
		return props;
	}

	public Map<String, PropertyItem> getDefaultParams() {
		Map<String, PropertyItem> defs = new TreeMap<String, PropertyItem>();
		defs.put(SUBSCR_RESTRICTIONS_PROP_KEY,
			new PropertyItem(SUBSCR_RESTRICTIONS_PROP_KEY,
				SUBSCR_RESTRICTIONS_DISPL_NAME, SUBSCR_RESTRICTIONS_PROP_VAL));
		defs.put(MESSAGE_TYPE_PROP_KEY,
			new PropertyItem(MESSAGE_TYPE_PROP_KEY,
				MESSAGE_TYPE_DISPL_NAME, MESSAGE_TYPE_PROP_VAL));
		defs.put(ALLOWED_SENDERS_PROP_KEY,
			new PropertyItem(ALLOWED_SENDERS_PROP_KEY,
				ALLOWED_SENDERS_DISPL_NAME, ALLOWED_SENDERS_PROP_VAL));
		defs.put(SUBSCR_RESTR_REGEX_PROP_KEY,
			new PropertyItem(SUBSCR_RESTR_REGEX_PROP_KEY,
				SUBSCR_RESTR_REGEX_DISPL_NAME, SUBSCR_RESTR_REGEX_PROP_VAL));
		defs.put(ONLINE_ONLY_PROP_KEY,
			new PropertyItem(ONLINE_ONLY_PROP_KEY,
				ONLINE_ONLY_DISPL_NAME, ONLINE_ONLY_PROP_VAL));
		defs.put(REPLACE_SENDER_PROP_KEY,
			new PropertyItem(REPLACE_SENDER_PROP_KEY,
				REPLACE_SENDER_DISPL_NAME, REPLACE_SENDER_PROP_VAL));
		defs.put(ALLOWED_SENDERS_LIST_PROP_KEY,
			new PropertyItem(ALLOWED_SENDERS_LIST_PROP_KEY,
				ALLOWED_SENDERS_LIST_DISPL_NAME, ALLOWED_SENDERS_LIST_PROP_VAL));
		defs.put(DESCRIPTION_PROP_KEY,
			new PropertyItem(DESCRIPTION_PROP_KEY,
				DESCRIPTION_DISPL_NAME, DESCRIPTION_PROP_VAL));
		defs.put(TASK_ADMINS_PROP_KEY,
			new PropertyItem(TASK_ADMINS_PROP_KEY,
				TASK_ADMINS_DISPL_NAME, TASK_ADMINS_PROP_VAL));
		defs.put(TASK_OWNER_PROP_KEY,
			new PropertyItem(TASK_OWNER_PROP_KEY,
				TASK_OWNER_DISPL_NAME, TASK_OWNER_PROP_VAL));
		return defs;
	}

	public void init(final Queue<Packet> results) {
		for (RosterItem ri: roster.values()) {
			Packet presence = null;
			if (ri.isSubscribed()) {
				presence = getPresence(ri.getJid(), jid, StanzaType.available,
					null, getDescription());
			} else {
				presence = getPresence(ri.getJid(), jid, StanzaType.subscribe,
					JIDUtils.getNodeNick(jid), null);
			} // end of if (ri.isSubscribed()) else
			results.offer(presence);
		}
	}

	public void destroy(Queue<Packet> results) {
		for (RosterItem ri: roster.values()) {
			Packet presence = getPresence(ri.getJid(), jid, StanzaType.unsubscribe);
			results.offer(presence);
			presence = getPresence(ri.getJid(), jid, StanzaType.unsubscribed);
			results.offer(presence);
		}
	}

	/**
	 * Describe <code>processPacket</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param results a <code>Queue</code> value
	 */
	public void processPacket(final Packet packet, final Queue<Packet> results) {
		++packets_received;
		log.finest(getJID() + ": " + "Processing packet: " + packet.toString());
		if (packet.getType() == StanzaType.error) {
			log.fine("Ignoring error stanza: " + packet.toString());
			return;
		}
		if (packet.getElemName().equals("presence")) {
			processPresence(packet, results);
		} // end of if (packet.getElemName().equals("presence))
		if (packet.getElemName().equals("message")) {
			if (isAllowedToPost(JIDUtils.getNodeID(packet.getElemFrom()))) {
				processMessage(packet, results);
			} else {
				try {
					results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
							"You are not allowed to post a message.", true));
				} catch (PacketErrorTypeException e) {
					log.warning("Packet processing exception: " + e);
				}
			}
		} // end of if (packet.getElemName().equals("message"))
		packets_sent += results.size();
	}

	private void processPresence(Packet packet, Queue<Packet> results) {
		StanzaType presence_type = StanzaType.available;
		if (packet.getType() != null) {
			presence_type = packet.getType();
		}
		RosterItem ri = getRosterItem(packet.getElemFrom());
		switch (presence_type) {
		case available:
		case probe:
			if (ri != null) {
				setRosterItemOnline(ri, true);
				results.offer(getPresence(packet.getElemFrom(), jid,
						StanzaType.available, null, getDescription()));
			} // end of if (ri != null)
			break;
		case unavailable:
			if (ri != null) {
				setRosterItemOnline(ri, false);
			// This is really not necessary as the task is always on-line
			// It should only mark remote contact as off-line and that's it.
// 				results.offer(getPresence(packet.getElemFrom(), jid,
// 						StanzaType.unavailable));
			} // end of if (ri != null)
			break;
		case subscribe:
			addNewSubscribers(results, packet.getElemFrom());
			results.offer(getPresence(packet.getElemFrom(), jid,
					StanzaType.subscribed));
			break;
		case subscribed:
			if (ri != null) {
				setRosterItemSubscribed(ri, true);
				results.offer(getPresence(packet.getElemFrom(), jid,
						StanzaType.available, null, getDescription()));
				if (!ri.isModerationAccepted()) {
					results.offer(getMessage(packet.getElemFrom(), jid,
							StanzaType.headline,
							"You are now subscribed to " + getJID() + ".\n\n"
							+ "Your subscription, however awaits moderation.\n\n"
							+ "Once your subscription is approved next message\n"
							+ "will be sent confirming your membership."));
				}
			} // end of if (ri != null)
			break;
		case unsubscribe:
		case unsubscribed:
			removeSubscribers(results, packet.getElemFrom());
			break;
		default:
			break;
		} // end of switch (packet.getType())
	}

	protected void processMessage(Packet packet, Queue<Packet> results) {
		for (RosterItem ri : roster.values()) {
			if (ri.isSubscribed() && ri.isModerationAccepted() &&
							(!send_to_online_only || ri.isOnline()) &&
							(!JIDUtils.getNodeID(packet.getElemFrom()).equals(ri.getJid()))) {
				Element message = packet.getElement().clone();
				Element body = message.getChild("body");
				if (body == null) {
					return;
				} // end of if (body == null)
				message.setAttribute("to", ri.getJid());
				message.setAttribute("type", message_type.toString().toLowerCase());
				switch (replace_sender_address) {
					case REPLACE: {
						String old_from = message.getAttribute("from");
						message.setAttribute("from", jid);
						String cdata = body.getCData();
						body.setCData(old_from + " sends:\n\n" + cdata);
						break;
					}
					case REMOVE:
						message.setAttribute("from", jid);
						break;
					case REPLACE_SRECV: {
						String old_from = message.getAttribute("from");
						message.setAttribute("from", JIDUtils.getJID(srecv.getName(), 
										local_domain, name));
						String cdata = body.getCData();
						body.setCData(old_from + " sends for installation at " +
										srecv.getDefHostName() + ":\n\n" + cdata);
						break;
					}
					default:
						break;
				}
				results.offer(new Packet(message));
			} // end of if (ri.isSubscribed() && ri.isModerationAccepted())
		} // end of for (RosterItem ri: roster.values())
	}

	public List<StatRecord> getStats() {
    List<StatRecord> stats = new LinkedList<StatRecord>();
    stats.add(new StatRecord(getJID(), "Roster size", "int",
				roster.size(), Level.INFO));
    stats.add(new StatRecord(getJID(), "Packets received", "long",
				packets_received, Level.INFO));
    stats.add(new StatRecord(getJID(), "Packets sent", "long",
				packets_sent, Level.INFO));
		int moderation_needed = 0;
		for (RosterItem ri: roster.values()) {
			moderation_needed += (ri.isModerationAccepted() ? 0 : 1);
		} // end of for (RosterItem ri: roster)
    stats.add(new StatRecord(getJID(), "Awaiting moderation", "int",
				moderation_needed, Level.INFO));
		return stats;
	}

	public boolean isAdmin(String jid) {
		RosterItem ri = getRosterItem(jid);
		return ri != null && (ri.isAdmin() || ri.isOwner());
	}

	public Map<String, RosterItem> getRoster() {
		return roster;
	}

} // AbstractReceiverTask
