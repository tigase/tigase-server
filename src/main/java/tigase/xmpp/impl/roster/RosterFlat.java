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
package tigase.xmpp.impl.roster;

import java.util.Queue;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import tigase.xml.Element;
import tigase.xml.DomBuilderHandler;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.db.TigaseDBException;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

/**
 * Describe class RosterFlat here.
 *
 *
 * Created: Tue Feb 21 18:05:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RosterFlat extends RosterAbstract {

	/**
   * Private logger for class instancess.
   */
	private static final Logger log = Logger.getLogger(
			"tigase.xmpp.impl.roster.RosterFlat");
	private static final SimpleParser parser = SingletonFactory.getParserInstance();
	private static int maxRosterSize =
					new Long(Runtime.getRuntime().maxMemory() / 250000L).intValue();

	public static void parseRoster(String roster_str, Map<BareJID, RosterElement> roster,
			XMPPResourceConnection session) {
		DomBuilderHandler domHandler = new DomBuilderHandler();
		parser.parse(domHandler, roster_str.toCharArray(), 0, roster_str.length());
		Queue<Element> elems = domHandler.getParsedElements();
		if (elems != null && elems.size() > 0) {
			for (Element elem : elems) {
				try {
					RosterElement relem = new RosterElement(elem, session);
					if (!addBuddy(relem, roster)) {
						break;
					}
				} catch (Exception e) {
					log.warning("Can't load roster element: " + elem.toString());
				}
			}
		}
	}

	private Map<BareJID, RosterElement> loadUserRoster(XMPPResourceConnection session)
    throws NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster =
						new LinkedHashMap<BareJID, RosterElement>();
		session.putCommonSessionData(ROSTER, roster);
		String roster_str = session.getData(null, ROSTER, null);
		updateRosterHash(roster_str, session);
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Loaded user roster: " + roster_str);
		}
		if (roster_str != null && !roster_str.isEmpty()) {
			parseRoster(roster_str, roster, session);
		} else {
			// Try to load a roster from the 'old' style roster storage and
			// convert it the the flat roster storage
			Roster oldRoster = new Roster();
			JID[] buddies = oldRoster.getBuddies(session);
			if (buddies != null && buddies.length > 0) {
				for (JID buddy: buddies) {
					String name = oldRoster.getBuddyName(session, buddy);
					SubscriptionType subscr = oldRoster.getBuddySubscription(session, buddy);
					String[] groups = oldRoster.getBuddyGroups(session, buddy);
					RosterElement relem = new RosterElement(buddy, name, groups, session);
					relem.setSubscription(subscr);
					if (!addBuddy(relem, roster)) {
						break;
					}
				}
				saveUserRoster(session);
			}
		}
		return roster;
	}

	private static boolean addBuddy(RosterElement relem,
					Map<BareJID, RosterElement> roster) {
		if (roster.size() < maxRosterSize) {
			roster.put(relem.getJid().getBareJID(), relem);
			return true;
		}
		return false;
	}

	private void saveUserRoster(XMPPResourceConnection session)
    throws NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster = getUserRoster(session);
		StringBuilder sb = new StringBuilder();
		for (RosterElement relem: roster.values()) {
			sb.append(relem.getRosterElement().toString());
		}
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Saving user roster: " + sb.toString());
		}
		session.setData(null, ROSTER, sb.toString());
	}

	@SuppressWarnings("unchecked")
	private Map<BareJID, RosterElement> getUserRoster(XMPPResourceConnection session)
    throws NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster =
      (Map<BareJID, RosterElement>)session.getCommonSessionData(ROSTER);
		if (roster == null) {
			roster = loadUserRoster(session);
		}
		return roster;
	}

	private RosterElement getRosterElement(XMPPResourceConnection session,
		JID buddy)
    throws NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster = getUserRoster(session);
		return roster.get(buddy.getBareJID());
	}

	@Override
//	public String[] getBuddies(XMPPResourceConnection session,
//					boolean onlineOnly)
	public JID[] getBuddies(XMPPResourceConnection session)
    throws NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster = getUserRoster(session);
		JID[] result = new JID[roster.size()];
		int idx = 0;
		for (RosterElement rosterElement : roster.values()) {
			result[idx++] = rosterElement.getJid();
		}
		return result;
//		if (onlineOnly) {
//			ArrayList<String> online = new ArrayList<String>();
//			for (Map.Entry<String, RosterElement> rosterEl : roster.entrySet()) {
//				if (rosterEl.getValue().isOnline()) {
//					online.add(rosterEl.getKey());
//				}
//			}
//			return online.toArray(new String[online.size()]);
//		} else {
//		}
  }

	@Override
//	public List<Element> getRosterItems(XMPPResourceConnection session, boolean onlineOnly)
//					throws NotAuthorizedException, TigaseDBException {
	public List<Element> getRosterItems(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		LinkedList<Element> items = new LinkedList<Element>();
		Map<BareJID, RosterElement> roster = getUserRoster(session);
		for (RosterElement relem : roster.values()) {
//			if (relem.isOnline() || !onlineOnly) {
				items.add(relem.getRosterItem());
//			}
		}
		return items;
	}


	@Override
  public String getBuddyName(XMPPResourceConnection session, JID buddy)
    throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem == null) {
			return null;
		} else {
			return relem.getName();
		}
  }

	@Override
  public void setBuddyName(XMPPResourceConnection session,
		JID buddy, String name)
    throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Setting name: '"+name+"' for buddy: " + buddy);
			}
			if (name != null && !name.isEmpty()) {
				relem.setName(name);
			}
			saveUserRoster(session);
		} else {
			log.warning("Setting buddy name for non-existen contact: " + buddy);
		}
  }

	@Override
  public void setBuddySubscription(XMPPResourceConnection session,
    SubscriptionType subscription, JID buddy)
		throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem != null) {
			relem.setSubscription(subscription);
			saveUserRoster(session);
		} else {
			log.warning("Missing roster contact for subscription set: " + buddy);
		}
  }

	@Override
  public SubscriptionType getBuddySubscription(XMPPResourceConnection session,
    JID buddy) throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem == null) {
			return null;
		} else {
			return relem.getSubscription();
		}
		//		return SubscriptionType.both;
  }

	@Override
	public boolean removeBuddy(XMPPResourceConnection session, JID jid) throws
			NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster = getUserRoster(session);
		roster.remove(jid.getBareJID());
		saveUserRoster(session);
		return true;
	}

	@Override
	public void addBuddy(XMPPResourceConnection session,
		JID buddy, String name, String[] groups)
    throws NotAuthorizedException, TigaseDBException {
		//String buddy = JIDUtils.getNodeID(jid);
		RosterElement relem = getRosterElement(session, buddy);
		if (relem == null) {
			Map<BareJID, RosterElement> roster = getUserRoster(session);
			relem = new RosterElement(buddy, name, groups, session);
			if (addBuddy(relem, roster)) {
				saveUserRoster(session);
			} else {
				throw new TigaseDBException("Too many elements in the user roster.");
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Added buddy to roster: " + buddy);
			}
		} else {
			if (name != null && !name.isEmpty()) {
				relem.setName(name);
			}
			if (groups != null && groups.length > 0) {
				relem.setGroups(groups);
			}
			saveUserRoster(session);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Updated buddy in roster: " + buddy);
			}
		}
	}

	@Override
  public String[] getBuddyGroups(XMPPResourceConnection session, JID buddy)
    throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem == null) {
			return null;
		} else {
			return relem.getGroups();
		}
  }

	@Override
	public boolean containsBuddy(XMPPResourceConnection session, JID buddy)
					throws NotAuthorizedException, TigaseDBException {
		return getRosterElement(session, buddy) != null;
	}

	@Override
	public boolean addBuddyGroup(XMPPResourceConnection session, JID buddy,
					String[] groups)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem != null) {
			relem.addGroups(groups);
			// Intentionally not saving the roster here.
			// At the moment it is only used to combine dynamic roster with the
			// static roster in case a contact exist in both but in a different
			// group.
			return true;
		} else {
			return false;
		}
	}

//	@Override
//	public void setBuddyOnline(XMPPResourceConnection session, String buddy,
//					boolean online)
//					throws NotAuthorizedException, TigaseDBException {
//		RosterElement relem = getRosterElement(session, buddy);
//		if (relem != null) {
//			relem.setOnline(online);
//		}
//	}
//
//	@Override
//	public boolean isBuddyOnline(XMPPResourceConnection session, String buddy)
//					throws NotAuthorizedException, TigaseDBException {
//		RosterElement relem = getRosterElement(session, buddy);
//		if (relem != null) {
//			return relem.isOnline();
//		}
//		return false;
//	}

} // RosterFlat
