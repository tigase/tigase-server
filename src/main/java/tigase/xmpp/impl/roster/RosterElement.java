/*
 * RosterElement.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */



package tigase.xmpp.impl.roster;

//~--- non-JDK imports --------------------------------------------------------

import tigase.util.TigaseStringprepException;
import tigase.util.XMPPStringPrepFactory;

import tigase.xml.Element;
import tigase.xml.XMLUtils;

import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;

import static tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;

//~--- JDK imports ------------------------------------------------------------

import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;

/**
 * Describe class RosterElement here.
 *
 *
 * Created: Wed Oct 29 14:21:16 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RosterElement
				implements RosterElementIfc {
	/** Field description */
	protected static final long INITIAL_LAST_SEEN_VAL = 1000l;
	private static final String ACTIVITY_ATT          = "activity";
	private static final String ELEM_NAME             = "contact";
	private static final String GRP_ATT               = "groups";
	private static final double INITIAL_ACTIVITY_VAL  = 1d;
	private static final double INITIAL_WEIGHT_VAL    = 1d;
	private static final String JID_ATT               = "jid";
	private static final String LAST_SEEN_ATT         = "last-seen";
	private static final Logger log                   =
		Logger.getLogger(RosterElement.class.getName());
	private static final String NAME_ATT              = "name";
	private static final String OTHER_ATT             = "other";
	private static final String STRINGPREP_ATT        = "preped";
	private static final String SUBS_ATT              = "subs";
	private static final String WEIGHT_ATT            = "weight";

	//~--- fields ---------------------------------------------------------------

	// ~--- fields ---------------------------------------------------------------
	private String[] groups                = null;
	private JID jid                        = null;
	private String name                    = null;
	private String otherData               = null;
	private long lastSeen                  = INITIAL_LAST_SEEN_VAL;
	private double activity                = INITIAL_ACTIVITY_VAL;
	private XMPPResourceConnection session = null;
	private String stringpreped            = null;
	private SubscriptionType subscription  = null;
	private double weight                  = INITIAL_WEIGHT_VAL;
	private boolean presence_sent          = false;
	private boolean persistent             = true;
	private Map<String, Boolean> onlineMap = new ConcurrentHashMap<String, Boolean>();

	// private Element item = null;
	// private boolean online = false;
	private boolean modified = false;

	//~--- constructors ---------------------------------------------------------

	// ~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>RosterElement</code> instance.
	 *
	 *
	 * @param roster_el
	 * @param session
	 * @throws TigaseStringprepException
	 */
	public RosterElement(Element roster_el, XMPPResourceConnection session)
					throws TigaseStringprepException {
		this.session = session;
		if (roster_el.getName() == ELEM_NAME) {
			this.stringpreped = roster_el.getAttributeStaticStr(STRINGPREP_ATT);
			setJid(roster_el.getAttributeStaticStr(JID_ATT));
			setName(roster_el.getAttributeStaticStr(NAME_ATT));
			if (roster_el.getAttributeStaticStr(SUBS_ATT) == null) {
				subscription = SubscriptionType.none;
			} else {
				subscription =
					SubscriptionType.valueOf(roster_el.getAttributeStaticStr(SUBS_ATT));
			}

			String grps = roster_el.getAttributeStaticStr(GRP_ATT);

			if ((grps != null) &&!grps.trim().isEmpty()) {
				groups = grps.split(",");
			}

			String other_data = roster_el.getAttributeStaticStr(OTHER_ATT);

			if ((other_data != null) &&!other_data.trim().isEmpty()) {
				otherData = other_data;
			}

			String num_str = roster_el.getAttributeStaticStr(ACTIVITY_ATT);

			if (num_str != null) {
				try {
					activity = Double.parseDouble(num_str);
				} catch (NumberFormatException nfe) {
					log.warning("Incorrect activity field: " + num_str);
					activity = INITIAL_ACTIVITY_VAL;
				}
			}
			num_str = roster_el.getAttributeStaticStr(WEIGHT_ATT);
			if (num_str != null) {
				try {
					weight = Double.parseDouble(num_str);
				} catch (NumberFormatException nfe) {
					log.warning("Incorrect weight field: " + num_str);
					weight = INITIAL_WEIGHT_VAL;
				}
			}
			num_str = roster_el.getAttributeStaticStr(LAST_SEEN_ATT);
			if (num_str != null) {
				try {
					lastSeen = Long.parseLong(num_str);
				} catch (NumberFormatException nfe) {
					log.warning("Incorrect last seen field: " + num_str);
					lastSeen = INITIAL_LAST_SEEN_VAL;
				}
			}
		} else {
			log.warning("Incorrect roster data: " + roster_el.toString());
		}
	}

	/**
	 * Constructs ...
	 *
	 *
	 * @param jid
	 * @param name
	 * @param groups
	 * @param session
	 */
	public RosterElement(JID jid, String name, String[] groups,
											 XMPPResourceConnection session) {
		this.stringpreped = XMPPStringPrepFactory.STRINGPREP_PROCESSOR;
		this.session      = session;
		setJid(jid);
		setName(name);
		this.groups       = groups;
		this.subscription = SubscriptionType.none;
	}

	//~--- methods --------------------------------------------------------------

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param groups
	 */
	public void addGroups(String[] groups) {
		if (groups != null) {
			if (this.groups == null) {
				this.groups = groups;
			} else {

				// Groups names must be unique
				Set<String> groupsSet = new HashSet<String>();

				for (String group : this.groups) {
					groupsSet.add(group);
				}
				for (String group : groups) {
					groupsSet.add(group);
				}
				this.groups = groupsSet.toArray(new String[groupsSet.size()]);
			}
		}

		// item = null;
	}

	//~--- get methods ----------------------------------------------------------

	// ~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String[] getGroups() {
		return groups;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public JID getJid() {
		return jid;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getOtherData() {
		return otherData;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public Element getRosterElement() {
		Element elem = new Element(ELEM_NAME, new String[] { JID_ATT, SUBS_ATT, NAME_ATT,
						STRINGPREP_ATT }, new String[] { jid.toString(), subscription.toString(),
						name, "" + stringpreped });

		if ((groups != null) && (groups.length > 0)) {
			String grps = "";

			for (String group : groups) {
				grps += group + ",";
			}
			grps = grps.substring(0, grps.length() - 1);
			elem.setAttribute(GRP_ATT, grps);
		}
		if (otherData != null) {
			elem.setAttribute(OTHER_ATT, otherData);
		}
		elem.setAttribute(ACTIVITY_ATT, Double.toString(activity));
		elem.setAttribute(WEIGHT_ATT, Double.toString(weight));
		elem.setAttribute(LAST_SEEN_ATT, Long.toString(lastSeen));
		modified = false;

		return elem;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public Element getRosterItem() {

		// This is actually not a good idea to cache the item element.
		// This causes a huge memory consumption and usually the item
		// is needed only once at the roster retrieving time.
		// if (item == null) {
		Element item = new Element("item");

		item.setAttribute("jid", jid.toString());
		item.addAttributes(subscription.getSubscriptionAttr());
		if (name != null) {
			item.setAttribute("name", XMLUtils.escape(name));
		}
		if (groups != null) {
			for (String gr : groups) {
				Element group = new Element("group");

				group.setCData(XMLUtils.escape(gr));
				item.addChild(group);
			}    // end of for ()
		}      // end of if-else

		// }
		return item;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String toString() {
		return getRosterItem().toString();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public SubscriptionType getSubscription() {
		return subscription;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public boolean isModified() {
		return modified;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public boolean isOnline() {
		return onlineMap.size() > 0;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public boolean isPresence_sent() {
		return presence_sent;
	}

	//~--- set methods ----------------------------------------------------------

	// ~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param groups
	 */
	public void setGroups(String[] groups) {
		this.groups = groups;
		modified    = true;

		// item = null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	public void setName(String name) {
		if (name == null) {
			this.name = this.jid.getLocalpart();
			if ((this.name == null) || this.name.trim().isEmpty()) {
				this.name = this.jid.getBareJID().toString();
			}
			modified = true;
		} else {
			this.name = name;
		}
	}

	/**
	 * Method description
	 *
	 *
	 *
	 * @param resource
	 * @param online
	 */
	public void setOnline(String resource, boolean online) {
		if ((onlineMap != null) && (resource != null)) {
			if (online) {
				onlineMap.put(resource, Boolean.TRUE);
			} else {
				onlineMap.remove(resource);
			}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param other_data
	 */
	public void setOtherData(String other_data) {
		otherData = other_data;
	}

	/**
	 * Method description
	 *
	 *
	 * @param presence_sent
	 */
	public void setPresence_sent(boolean presence_sent) {
		this.presence_sent = presence_sent;
	}

	/**
	 * Method description
	 *
	 *
	 * @param subscription
	 */
	public void setSubscription(SubscriptionType subscription) {
		if (subscription == null) {
			this.subscription = SubscriptionType.none;
		} else {
			this.subscription = subscription;
		}
		modified = true;

		// item = null;
	}

	private void setJid(JID jid) {
		this.jid = jid;
		modified = true;
	}

	private void setJid(String jid) throws TigaseStringprepException {
		if (XMPPStringPrepFactory.STRINGPREP_PROCESSOR.equals(stringpreped)) {
			this.jid = JID.jidInstanceNS(jid);
		} else {
			this.jid = JID.jidInstance(jid);
			modified = true;
		}
		stringpreped = XMPPStringPrepFactory.STRINGPREP_PROCESSOR;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * @return
	 */
	public boolean isPersistent() {
		return persistent;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param persistent
	 */
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * @return the activity
	 */
	public double getActivity() {
		return activity;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * @param activity
	 *          the activity to set
	 */
	public void setActivity(double activity) {
		this.activity = activity;
		if (activity != 0) {
			weight = 1 / activity;
		}
		modified = true;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * @return the weight
	 */
	public double getWeight() {
		return weight;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * @param weight
	 *          the weight to set
	 */
	public void setWeight(double weight) {
		this.weight = weight;
		modified    = true;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * @return the lastSeen
	 */
	public long getLastSeen() {
		return lastSeen;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * @param lastSeen the lastSeen to set
	 */
	public void setLastSeen(long lastSeen) {
		this.lastSeen = lastSeen;
		modified      = true;
	}
}


//~ Formatted in Tigase Code Convention on 13/02/28
