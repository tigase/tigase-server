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

import java.util.ArrayList;
import java.util.Queue;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import tigase.xml.Element;
import tigase.xml.DomBuilderHandler;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.util.JIDUtils;
import tigase.db.TigaseDBException;

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
  private static final Logger log =
    Logger.getLogger("tigase.xmpp.impl.roster.RosterFlat");
	private static final SimpleParser parser = 
					SingletonFactory.getParserInstance();
	private static final int maxRosterSize =
					new Long(Runtime.getRuntime().maxMemory() / 250000L).intValue();

	public static void parseRoster(String roster_str,
					Map<String, RosterElement> roster, XMPPResourceConnection session) {
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

	private Map<String, RosterElement> loadUserRoster(XMPPResourceConnection session)
    throws NotAuthorizedException, TigaseDBException {
		Map<String, RosterElement> roster =
						new LinkedHashMap<String, RosterElement>();
		session.putCommonSessionData(ROSTER, roster);
		String roster_str = session.getData(null, ROSTER, null);
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Loaded user roster: " + roster_str);
		}
		if (roster_str != null && !roster_str.isEmpty()) {
			parseRoster(roster_str, roster, session);
		} else {
			// Try to load a roster from the 'old' style roster storage and
			// convert it the the flat roster storage
			Roster oldRoster = new Roster();
			String[] buddies = oldRoster.getBuddies(session);
			if (buddies != null && buddies.length > 0) {
				for (String buddy: buddies) {
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
					Map<String, RosterElement> roster) {
		if (roster.size() <  maxRosterSize) {
			roster.put(relem.getJid(), relem);
			return true;
		}
		return false;
	}

	private void saveUserRoster(XMPPResourceConnection session)
    throws NotAuthorizedException, TigaseDBException {
		Map<String, RosterElement> roster = getUserRoster(session);
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
	private Map<String, RosterElement> getUserRoster(XMPPResourceConnection session)
    throws NotAuthorizedException, TigaseDBException {
		Map<String, RosterElement> roster =
      (Map<String, RosterElement>)session.getCommonSessionData(ROSTER);
		if (roster == null) {
			roster = loadUserRoster(session);
		}
		return roster;
	}

	private RosterElement getRosterElement(XMPPResourceConnection session,
		String buddy)
    throws NotAuthorizedException, TigaseDBException {
		Map<String, RosterElement> roster = getUserRoster(session);
		return roster.get(JIDUtils.getNodeID(buddy));
	}

	@Override
	public String[] getBuddies(final XMPPResourceConnection session,
					boolean onlineOnly)
    throws NotAuthorizedException, TigaseDBException {
		Map<String, RosterElement> roster = getUserRoster(session);
		if (onlineOnly) {
			ArrayList<String> online = new ArrayList<String>();
			for (Map.Entry<String, RosterElement> rosterEl : roster.entrySet()) {
				if (rosterEl.getValue().isOnline()) {
					online.add(rosterEl.getKey());
				}
			}
			return online.toArray(new String[online.size()]);
		} else {
			return roster.keySet().toArray(new String[0]);
		}
  }

	@Override
  public String getBuddyName(final XMPPResourceConnection session,
		final String buddy)
    throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem == null) {
			return null;
		} else {
			return relem.getName();
		}
  }

	@Override
  public void setBuddyName(final XMPPResourceConnection session,
		final String buddy, final String name)
    throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Setting name: '"+name+"' for buddy: " + buddy);
			}
			relem.setName(name);
			saveUserRoster(session);
		} else {
			log.warning("Setting buddy name for non-existen contact: " + buddy);
		}
  }

	@Override
  public void setBuddySubscription(final XMPPResourceConnection session,
    final SubscriptionType subscription, final String buddy)
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
  public SubscriptionType getBuddySubscription(
		final XMPPResourceConnection session,
    final String buddy) throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem == null) {
			return null;
		} else {
			return relem.getSubscription();
		}
		//		return SubscriptionType.both;
  }

	@Override
	public boolean removeBuddy(final XMPPResourceConnection session,
		final String jid) throws NotAuthorizedException, TigaseDBException {
		Map<String, RosterElement> roster = getUserRoster(session);
		roster.remove(jid);
		saveUserRoster(session);
		return true;
	}

	@Override
	public void addBuddy(XMPPResourceConnection session,
		String jid, String name, String[] groups)
    throws NotAuthorizedException, TigaseDBException {
		String buddy = JIDUtils.getNodeID(jid);
		RosterElement relem = getRosterElement(session, buddy);
		if (relem == null) {
			Map<String, RosterElement> roster = getUserRoster(session);
			relem = new RosterElement(buddy, name, groups, session);
			if (addBuddy(relem, roster)) {
				saveUserRoster(session);
			} else {
				throw new TigaseDBException("Too many elements in the user roster.");
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Added buddy to roster: " + jid);
			}
		} else {
			relem.setName(name);
			relem.setGroups(groups);
			saveUserRoster(session);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Updated buddy in roster: " + jid);
			}
		}
	}

	@Override
  public String[] getBuddyGroups(final XMPPResourceConnection session,
		final String buddy)
    throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem == null) {
			return null;
		} else {
			return relem.getGroups();
		}
  }

	@Override
	public boolean containsBuddy(XMPPResourceConnection session, String buddy)
					throws NotAuthorizedException, TigaseDBException {
		return getRosterElement(session, buddy) != null;
	}

	@Override
	public boolean addBuddyGroup(XMPPResourceConnection session, String buddy,
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

	@Override
	public void setBuddyOnline(XMPPResourceConnection session, String buddy,
					boolean online)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem != null) {
			relem.setOnline(online);
		}
	}

	@Override
	public boolean isBuddyOnline(XMPPResourceConnection session, String buddy)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);
		if (relem != null) {
			return relem.isOnline();
		}
		return false;
	}

} // RosterFlat
