/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
package tigase.xmpp.impl.roster;

import java.util.HashSet;
import java.util.Set;
import tigase.xml.Element;
import java.util.logging.Logger;
import tigase.util.JIDUtils;

import tigase.xmpp.XMPPResourceConnection;
import static tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;

/**
 * Describe class RosterElement here.
 *
 *
 * Created: Wed Oct 29 14:21:16 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RosterElement {

  private static final Logger log =
    Logger.getLogger("tigase.xmpp.impl.roster.RosterElement");

	private static final String ELEM_NAME = "contact";
	private static final String JID_ATT = "jid";
	private static final String NAME_ATT = "name";
	private static final String SUBS_ATT = "subs";
	private static final String GRP_ATT = "groups";

	private XMPPResourceConnection session = null;
	private SubscriptionType subscription = null;
	private String[] groups = null;
	private String name = null;
	private String jid = null;
	private boolean online = false;

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public boolean isPresence_sent() {
		return presence_sent;
	}

	public void setPresence_sent(boolean presence_sent) {
		this.presence_sent = presence_sent;
	}
	private boolean presence_sent = false;

	public RosterElement(String jid, String name, String[] groups,
					XMPPResourceConnection session) {
		this.session = session;
		setJid(jid);
		if (name == null) {
			this.name = JIDUtils.getNodeNick(this.jid);
			if (this.name == null || this.name.isEmpty()) {
				this.name = this.jid;
			}
		} else {
			this.name = name;
		}
		this.groups = groups;
		this.subscription = SubscriptionType.none;
	}

	/**
	 * Creates a new <code>RosterElement</code> instance.
	 *
	 *
	 * @param roster_el
	 */
	public RosterElement(Element roster_el, XMPPResourceConnection session) {
		this.session = session;
		if (roster_el.getName() == ELEM_NAME) {
			setJid(roster_el.getAttribute(JID_ATT));
			name = roster_el.getAttribute(NAME_ATT);
			if (name == null || name.isEmpty()) {
				name = JIDUtils.getNodeNick(jid);
				if (this.name == null || this.name.isEmpty()) {
					this.name = jid;
				}
			}
			if (roster_el.getAttribute(SUBS_ATT) == null) {
				subscription = SubscriptionType.none;
			} else {
				subscription = SubscriptionType.valueOf(roster_el.getAttribute(SUBS_ATT));
			}
		} else {
			log.warning("Incorrect roster data: " + roster_el.toString());
		}
		String grps = roster_el.getAttribute(GRP_ATT);
		if (grps != null && !grps.isEmpty()) {
			groups = grps.split(",");
		}
	}

	private void setJid(String jid) {
		this.jid = jid.toLowerCase();
		String buddy_domain = JIDUtils.getNodeHost(this.jid);
		if (session != null && session.isLocalDomain(buddy_domain, false)) {
			this.jid = this.jid.intern();
		}
	}

	public String getJid() {
		return jid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getGroups() {
		return groups;
	}

	public void setGroups(String[] groups) {
		this.groups = groups;
	}

	public SubscriptionType getSubscription() {
		return subscription;
	}

	public void setSubscription(SubscriptionType subscription) {
		if (subscription == null) {
			this.subscription = SubscriptionType.none;
		} else {
			this.subscription = subscription;
		}
	}

	public Element getRosterElement() {
		Element elem = new Element(ELEM_NAME,
			new String[] {JID_ATT, SUBS_ATT, NAME_ATT},
			new String[] {jid, subscription.toString(), name});
		if (groups != null && groups.length > 0) {
			String grps = "";
			for (String group: groups) {
				grps += group + ",";
			}
			grps = grps.substring(0, grps.length() - 1);
			elem.setAttribute(GRP_ATT, grps);
		}
		return elem;
	}

	void addGroups(String[] groups) {
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
	}

}
