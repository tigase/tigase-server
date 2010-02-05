/*
 *   Tigase Jabber/XMPP Server
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

//~--- non-JDK imports --------------------------------------------------------

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;
import tigase.xml.XMLUtils;

import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;

import static tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

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
	private static final Logger log = Logger.getLogger("tigase.xmpp.impl.roster.RosterElement");
	private static final String ELEM_NAME = "contact";
	private static final String JID_ATT = "jid";
	private static final String NAME_ATT = "name";
	private static final String SUBS_ATT = "subs";
	private static final String GRP_ATT = "groups";
	private static final String STRINGPREP_ATT = "preped";

	//~--- fields ---------------------------------------------------------------

	private String[] groups = null;
	private JID jid = null;
	private String name = null;
	private XMPPResourceConnection session = null;
	private SubscriptionType subscription = null;
	private boolean stringpreped = false;

	// private boolean online = false;
	// private Element item = null;
	private boolean presence_sent = false;
	private boolean modified = false;

	//~--- constructors ---------------------------------------------------------

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
			this.stringpreped = Boolean.parseBoolean(roster_el.getAttribute(STRINGPREP_ATT));
			setJid(roster_el.getAttribute(JID_ATT));
			setName(roster_el.getAttribute(NAME_ATT));

			if (roster_el.getAttribute(SUBS_ATT) == null) {
				subscription = SubscriptionType.none;
			} else {
				subscription = SubscriptionType.valueOf(roster_el.getAttribute(SUBS_ATT));
			}
		} else {
			log.warning("Incorrect roster data: " + roster_el.toString());
		}

		String grps = roster_el.getAttribute(GRP_ATT);

		if ((grps != null) &&!grps.isEmpty()) {
			groups = grps.split(",");
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
	public RosterElement(JID jid, String name, String[] groups, XMPPResourceConnection session) {
		this.stringpreped = true;
		this.session = session;
		this.stringpreped = true;
		setJid(jid);
		setName(name);
		this.groups = groups;
		this.subscription = SubscriptionType.none;
	}

	//~--- get methods ----------------------------------------------------------

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
	public Element getRosterElement() {
		Element elem = new Element(ELEM_NAME, new String[] { JID_ATT, SUBS_ATT, NAME_ATT,
				STRINGPREP_ATT }, new String[] { jid.toString(), subscription.toString(), name,
				"" + stringpreped });

		if ((groups != null) && (groups.length > 0)) {
			String grps = "";

			for (String group : groups) {
				grps += group + ",";
			}

			grps = grps.substring(0, grps.length() - 1);
			elem.setAttribute(GRP_ATT, grps);
		}

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

//public boolean isOnline() {
//  return online;
//}
//
//public void setOnline(boolean online) {
//  this.online = online;
//}

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

	/**
	 * Method description
	 *
	 *
	 * @param groups
	 */
	public void setGroups(String[] groups) {
		this.groups = groups;
		modified = true;

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
		} else {
			this.name = name;
		}

		modified = true;
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

	//~--- methods --------------------------------------------------------------

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

		// item = null;
	}

	//~--- set methods ----------------------------------------------------------

	private void setJid(JID jid) {
		this.jid = jid;
		modified = true;
	}

	private void setJid(String jid) throws TigaseStringprepException {
		if (stringpreped) {
			this.jid = JID.jidInstanceNS(jid);
		} else {
			this.jid = JID.jidInstance(jid);
			modified = true;
		}

		stringpreped = true;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
