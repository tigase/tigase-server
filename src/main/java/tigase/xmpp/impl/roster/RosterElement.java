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

import tigase.xml.Element;
import java.util.logging.Logger;
import tigase.util.JIDUtils;

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


	private SubscriptionType subscription = null;
	private String[] groups = null;
	private String name = null;
	private String jid = null;

	public RosterElement(String jid, String name, String[] groups) {
		this.jid = jid.toLowerCase();
		if (name == null) {
			this.name = JIDUtils.getNodeNick(jid);
			if (this.name == null || this.name.isEmpty()) {
				this.name = jid;
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
	 */
	public RosterElement(Element roster_el) {
		if (roster_el.getName() == ELEM_NAME) {
			jid = roster_el.getAttribute(JID_ATT).toLowerCase();
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

}
