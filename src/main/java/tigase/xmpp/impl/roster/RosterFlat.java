/*
 * RosterFlat.java
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

import tigase.db.TigaseDBException;

import tigase.server.PolicyViolationException;

import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class RosterFlat here.
 *
 * Created: Tue Feb 21 18:05:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class RosterFlat
				extends RosterAbstract {
	/**
	 * Private logger for class instances.
	 */
	private static final Logger log          = Logger.getLogger(RosterFlat.class.getName());
	private static final SimpleParser parser = SingletonFactory.getParserInstance();

	private final SimpleDateFormat formatter;
	{
		this.formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
		this.formatter.setTimeZone( TimeZone.getTimeZone("UTC") );
	}

	public static boolean addBuddy(RosterElement relem,
																 Map<BareJID, RosterElement> roster) {
		if (roster.size() < maxRosterSize) {
			roster.put(relem.getJid().getBareJID(), relem);

			return true;
		}

		return false;
	}

	public RosterElement addTempBuddy(JID buddy, XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElementInstance(buddy.copyWithoutResource(), null, null, session);

		relem.setPersistent(false);
		relem.setSubscription( null );
		addBuddy(relem, getUserRoster(session));

		if ( log.isLoggable( Level.FINEST ) ){
			log.log( Level.FINEST, "Added temporary buddy to roster: {0}, name: {1}, item: {2}",
							 new Object[] { relem.getJid(), relem.getName(),
															relem.getRosterItem(), relem.toString() } );
		}
		return relem;
	}

	public static boolean parseRosterUtil(String roster_str,
					Map<BareJID, RosterElement> roster, XMPPResourceConnection session) {
		boolean result               = false;
		DomBuilderHandler domHandler = new DomBuilderHandler();

		parser.parse(domHandler, roster_str.toCharArray(), 0, roster_str.length());

		Queue<Element> elems = domHandler.getParsedElements();

		if ((elems != null) && (elems.size() > 0)) {
			for (Element elem : elems) {
				try {
					RosterElement relem = new RosterElement(elem, session);

					result |= relem.isModified();
					if (!addBuddy(relem, roster)) {
						break;
					}
				} catch (Exception e) {
					log.log(Level.WARNING, "Can't load roster element: {0}", elem);
				}
			}
		}

		return result;
	}

	@Override
	public void addBuddy(XMPPResourceConnection session, JID buddy, String name,
											 String[] groups, String otherData)
					throws NotAuthorizedException, TigaseDBException, PolicyViolationException {

		// String buddy = JIDUtils.getNodeID(jid);
		RosterElement relem = getRosterElement(session, buddy);

		if (relem == null) {
			Map<BareJID, RosterElement> roster = getUserRoster(session);

			relem = getRosterElementInstance(buddy, name, groups, session);
			if (emptyNameAllowed && (name == null || name.isEmpty())) {
				relem.setName(null);
			} else if (name == null || name.isEmpty()) {
				String n = buddy.getLocalpart();
				if ((n == null) || n.trim().isEmpty()) {
					n = buddy.getBareJID().toString();
				}
				relem.setName(n);
			} else {
				relem.setName(name);
			}
			relem.setOtherData(otherData);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "1. Added buddy to roster: {0}, name: {1}, item: {2}",
								new Object[] { relem.getJid(),
															 relem.getName(), relem.getRosterItem() });
			}
			if (addBuddy(relem, roster)) {
				saveUserRoster(session);
			} else {
				throw new PolicyViolationException("Too many elements in the user roster. Limit: " + maxRosterSize);
			}
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "2. Added buddy to roster: {0}, name: {1}, item: {2}",
								new Object[] { relem.getJid(),
															 relem.getName(), relem.getRosterItem() });
			}
		} else {
			if (emptyNameAllowed && (name == null || name.isEmpty())) {
				relem.setName(null);
			} else if (name == null || name.isEmpty()) {
				String n = buddy.getLocalpart();
				if ((n == null) || n.trim().isEmpty()) {
					n = buddy.getBareJID().toString();
				}
				relem.setName(n);
			} else {
				relem.setName(name);
			}
				
			// Hm, as one user reported this make it impossible to remove the user
			// from
			// all groups. Let's comments it out for now to see how it works.
			// Probably added this some time ago , before RosterFlat to prevent NPE.
			// if ((groups != null) && (groups.length > 0)) {
			relem.setGroups(groups);

			// }
			relem.setPersistent( true );
			saveUserRoster(session);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Updated buddy in roster: {0}", buddy);
			}
		}
	}

	@Override
	public boolean addBuddyGroup(XMPPResourceConnection session, JID buddy, String[] groups)
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
	public boolean containsBuddy(XMPPResourceConnection session, JID buddy)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);

		return (relem != null) && relem.isPersistent();
	}

	@Override
	public JID[] getBuddies(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster = getUserRoster(session);

		if (roster.size() == 0) {
			return null;
		}

		JID[] result = new JID[roster.size()];
		int idx      = 0;

		for (RosterElement rosterElement : roster.values()) {
			result[idx++] = rosterElement.getJid();
		}

		// TODO: this sorting should be optional as it may impact performance
		Arrays.sort(result, new RosterElemComparator(roster));

		return result;

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

	public Element getBuddyItem(RosterElement relem) {
		return relem.getRosterItem();
	}

	@Override
	public Element getBuddyItem(final XMPPResourceConnection session, JID buddy)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);

		if (relem == null) {
			return null;
		} else {
			return getBuddyItem(relem);
		}
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
	public SubscriptionType getBuddySubscription(XMPPResourceConnection session, JID buddy)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);

		// either we don't have such contact or it's not persistend in which case it shouldn't
		// have subscription -- this is simpler solution instead of reworking whole RosterElement
		// to allow sub==null
		if ( relem == null || ( !relem.isPersistent() ) ){
			return null;
		} else {
			return relem.getSubscription();
		}

		// return SubscriptionType.both;
	}

	public RosterElement getRosterElementInstance(JID buddy, String name, String[] groups,
					XMPPResourceConnection session) {
		return new RosterElement(buddy.copyWithoutResource(), name, groups, session);
	}

	@Override
	public List<Element> getRosterItems(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		LinkedList<Element> items          = new LinkedList<Element>();
		Map<BareJID, RosterElement> roster = getUserRoster(session);

		for (RosterElement relem : roster.values()) {

			// Skip temporary roster elements added only for online presence tracking
			// from dynamic roster
			if (relem.isPersistent() && !SubscriptionType.none_pending_in.equals(relem.getSubscription())) {
				items.add(getBuddyItem(relem));
			}
		}

		return items;
	}

	@Override
	public boolean isRosterLoaded(XMPPResourceConnection session) {
		return session.getCommonSessionData(ROSTER) != null;
	}
	
	@Override
	public boolean isOnline(XMPPResourceConnection session, JID buddy)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);

		return (relem != null) && relem.isOnline();
	}

	public boolean parseRoster(String roster_str, Map<BareJID, RosterElement> roster,
														 XMPPResourceConnection session) {
		return parseRosterUtil(roster_str, roster, session);
	}

	@Override
	public boolean presenceSent(XMPPResourceConnection session, JID buddy)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);

		return (relem != null) && relem.isPresence_sent();
	}

	@Override
	public boolean removeBuddy(XMPPResourceConnection session, JID jid)
					throws NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster = getUserRoster(session);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Removing roster buddy: {0}, before removal: {1}",
							new Object[] { jid,
														 roster });
		}
		roster.remove(jid.getBareJID());
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Removing roster buddy: {0}, after removal: {1}",
							new Object[] { jid,
														 roster });
		}
		saveUserRoster(session);

		return true;
	}

	@Override
	public void setBuddyName(XMPPResourceConnection session, JID buddy, String name)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);

		if (relem != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Setting name: ''{0}'' for buddy: {1}", new Object[] { name,
								buddy });
			}
			if (emptyNameAllowed && (name == null || name.isEmpty())) {
				relem.setName(null);
			} else if (name == null || name.isEmpty()) {
				String n = buddy.getLocalpart();
				if ((n == null) || n.trim().isEmpty()) {
					n = buddy.getBareJID().toString();
				}
				relem.setName(n);
			} else {
				relem.setName(name);
			}
			saveUserRoster(session);
		} else {
			log.log(Level.WARNING, "Setting buddy name for non-existen contact: {0}", buddy);
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
			log.log(Level.WARNING, "Missing roster contact for subscription set: {0}", buddy);
		}
	}

	@Override
	public void setOnline(XMPPResourceConnection session, JID buddy, boolean online)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);

		if (relem == null) {
			relem = addTempBuddy(buddy, session);
		}
		relem.setOnline(buddy.getResource(), online);
	}

	@Override
	public void setPresenceSent(XMPPResourceConnection session, JID buddy, boolean sent)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement relem = getRosterElement(session, buddy);

		if (relem == null) {
			relem = addTempBuddy(buddy, session);
		}
		relem.setPresence_sent(sent);
	}

	@Override
	public RosterElement getRosterElement(XMPPResourceConnection session, JID buddy)
					throws NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster = getUserRoster(session);

		return roster.get(buddy.getBareJID());
	}

	@SuppressWarnings({ "unchecked" })
	protected Map<BareJID, RosterElement> getUserRoster(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster = null;

		// The method can be called from different plugins concurrently.
		// If the roster is not yet loaded from DB this causes concurent
		// access problems
		synchronized (session) {
			roster = (Map<BareJID, RosterElement>) session.getCommonSessionData(ROSTER);
			if (roster == null) {
				roster = loadUserRoster(session);
			}
		}

		return roster;
	}

	protected void saveUserRoster(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster = getUserRoster(session);
		StringBuilder sb                   = new StringBuilder(5000);

		for (RosterElement relem : roster.values()) {
			if (relem.isPersistent()) {
				sb.append(relem.getRosterElement().toString());
			}
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0} | Saving user roster: {1}",
														new String [] {session.getBareJID().toString(), sb.toString()});
		}
		session.setData(null, ROSTER, sb.toString());
	}

	private Map<BareJID, RosterElement> loadUserRoster(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {

		// In most times we just read from this data structure
		// From time to time there might be some modification, posibly concurrent
		// very unlikely by more than one thread
		Map<BareJID, RosterElement> roster = new ConcurrentHashMap<BareJID,
																					 RosterElement>(100, 0.25f, 1);

		session.putCommonSessionData(ROSTER, roster);

		String roster_str = session.getData(null, ROSTER, null);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Loaded user {1} roster: {0}", new Object[] {roster_str, session.getjid()});
		}
		if ((roster_str != null) &&!roster_str.isEmpty()) {
			updateRosterHash(roster_str, session);

			boolean modified = parseRoster(roster_str, roster, session);

			if (modified) {
				saveUserRoster(session);
			}
		} else {

			// Try to load a roster from the 'old' style roster storage and
			// convert it the the flat roster storage
			Roster oldRoster = new Roster();
			JID[] buddies    = oldRoster.getBuddies(session);

			if ((buddies != null) && (buddies.length > 0)) {
				for (JID buddy : buddies) {
					String name             = oldRoster.getBuddyName(session, buddy);
					SubscriptionType subscr = oldRoster.getBuddySubscription(session, buddy);
					String[] groups         = oldRoster.getBuddyGroups(session, buddy);
					RosterElement relem     = getRosterElementInstance(buddy, name, groups,
																			session);

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

	@Override
	public Element getCustomChild(XMPPResourceConnection session, JID buddy)
					throws NotAuthorizedException, TigaseDBException {
		RosterElement rel = getRosterElement(session, buddy);

		if (rel != null && rel.getLastSeen() > RosterElement.INITIAL_LAST_SEEN_VAL) {
			String stamp;
			synchronized (formatter) {
				stamp = formatter.format(new Date(rel.getLastSeen()));
			}

			return new Element("delay", new String[]{
					"stamp", "xmlns"}, new String[]{stamp, "urn:xmpp:delay"});
		}

		return null;
	}

	@Override
	public void logout(XMPPResourceConnection session) {
		try {
			if (session.isAuthorized() && isModified(session)) {
				saveUserRoster(session);
			}
		} catch (NotAuthorizedException ex) {

			// TODO Auto-generated catch block
			ex.printStackTrace();
		} catch (TigaseDBException ex) {

			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
	}

	public boolean isModified(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		Map<BareJID, RosterElement> roster = getUserRoster(session);
		boolean result                     = false;

		if (roster != null) {
			for (RosterElement rel : roster.values()) {
				result |= rel.isModified();
			}
		}

		return result;
	}

	private class RosterElemComparator
					implements Comparator<JID> {
		private Map<BareJID, RosterElement> roster = null;

		private RosterElemComparator(Map<BareJID, RosterElement> roster) {
			this.roster = roster;
		}

		@Override
		public int compare(JID arg0, JID arg1) {
			double w0 = roster.get(arg0.getBareJID()).getWeight();
			double w1 = roster.get(arg1.getBareJID()).getWeight();

			return Double.compare(w0, w1);
		}
	}
}    // RosterFlat

