/*
 * DynamicRoster.java
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

import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;

//~--- JDK imports ------------------------------------------------------------

/**
 * Describe class DynamicRoster here.
 *
 *
 * Created: Tue Nov 6 11:28:10 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class DynamicRoster {
	private static final String DYNAMIC_ROSTERS         = "dynamic-rosters";
	private static final String DYNAMIC_ROSTERS_CLASSES = "dynamic-roster-classes";

	private static final EnumSet<SubscriptionType> subs = EnumSet.noneOf(SubscriptionType.class);

	/**
	 * Private logger for class instances.
	 */
	private static Logger log = Logger.getLogger("tigase.xmpp.impl.DynamicRoster");

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param settings
	 * @param buddies
	 *
	 *
	 *
	 *
	 * @return a value of <code>JID[]</code>
	 * @throws NotAuthorizedException
	 * @throws RepositoryAccessException
	 * @throws RosterRetrievingException
	 */
	public static JID[] addBuddies(final XMPPResourceConnection session, final Map<String,
			Object> settings, JID[] buddies)
					throws NotAuthorizedException, RosterRetrievingException,
							RepositoryAccessException {
		return addBuddies(session,settings,buddies, subs);
	}

	public static JID[] addBuddies(final XMPPResourceConnection session, final Map<String,
			Object> settings, JID[] buddies, final EnumSet<RosterAbstract.SubscriptionType> subscrs)
					throws NotAuthorizedException, RosterRetrievingException,
							RepositoryAccessException {
		List<JID> result = getBuddiesList(session, settings);

		if (result != null && subscrs != null && !subscrs.isEmpty()) {
			final Iterator<JID> iterator = result.iterator();
			while (iterator.hasNext()) {
				final JID jid = iterator.next();
				Element buddy = getBuddyItem(session, settings, jid);
				String sub = null;
				if (buddy != null && ((sub = buddy.getAttributeStaticStr("subscription")) != null)) {
					try {
						final SubscriptionType subType = SubscriptionType.valueOf(sub.toLowerCase());
						if (!subscrs.contains(subType)) {
							iterator.remove();
						}
					} catch (IllegalArgumentException e) {
						log.log(Level.FINEST, "Illegal dynamic roster element, skipping");
					}
				}
			}
		}

		if (buddies != null) {
			if (result == null) {
				result = new ArrayList<JID>();
			}
			addBuddiesToList(result, buddies);
		}
		if ((result != null) && (result.size() > 0)) {
			return result.toArray(new JID[result.size()]);
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 * @param buddies
	 */
	public static void addBuddiesToList(List<JID> list, JID[] buddies) {
		for (JID buddy : buddies) {
			if (!list.contains(buddy)) {
				list.add(buddy);
			}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param settings
	 */
	public static void init_settings(final Map<String, Object> settings) {
		DynamicRosterIfc[] dynr = (DynamicRosterIfc[]) settings.get(DYNAMIC_ROSTERS);

		if (dynr == null) {
			log.finer("Initializing dynamic rosters...");

			String dynclss = (String) settings.get(DYNAMIC_ROSTERS_CLASSES);

			if (dynclss != null) {
				String[]                    dyncls = dynclss.split(",");
				ArrayList<DynamicRosterIfc> al     = new ArrayList<DynamicRosterIfc>(50);

				for (String cls : dyncls) {
					try {
						DynamicRosterIfc dri = (DynamicRosterIfc) Class.forName(cls).newInstance();

						if (settings.get(cls + ".init") != null) {
							dri.init((String) settings.get(cls + ".init"));
						} else {
							dri.init(settings);
						}
						al.add(dri);
						log.log(Level.INFO, "Initialized dynamic roster: {0}", cls);
					} catch (Exception e) {
						log.log(Level.WARNING, "Problem initializing dynmic roster class: {0}, {1}",
								new Object[] { cls,
								e });
					}
				}
				if (al.size() > 0) {
					settings.put(DYNAMIC_ROSTERS, al.toArray(new DynamicRosterIfc[al.size()]));
				}
			}
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param settings
	 *
	 *
	 *
	 *
	 * @return a value of <code>JID[]</code>
	 * @throws NotAuthorizedException
	 * @throws RepositoryAccessException
	 * @throws RosterRetrievingException
	 */
	public static JID[] getBuddies(final XMPPResourceConnection session, final Map<String,
			Object> settings)
					throws NotAuthorizedException, RosterRetrievingException,
							RepositoryAccessException {
		List<JID> result = getBuddiesList(session, settings);

		if ((result != null) && (result.size() > 0)) {
			return result.toArray(new JID[result.size()]);
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param settings
	 *
	 *
	 *
	 *
	 * @return a value of {@code List<JID>}
	 * @throws NotAuthorizedException
	 * @throws RepositoryAccessException
	 * @throws RosterRetrievingException
	 */
	public static List<JID> getBuddiesList(final XMPPResourceConnection session,
			final Map<String, Object> settings)
					throws NotAuthorizedException, RosterRetrievingException,
							RepositoryAccessException {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			ArrayList<JID> result = new ArrayList<JID>();

			for (DynamicRosterIfc dri : dynr) {
				JID[] buddies = dri.getBuddies(session);

				if (buddies != null) {
					addBuddiesToList(result, buddies);

					// result.addAll(Arrays.asList(buddies));
				}
			}
			if (result.size() > 0) {
				return result;
			}
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param settings
	 * @param buddy
	 *
	 *
	 *
	 *
	 * @return a value of <code>Element</code>
	 * @throws NotAuthorizedException
	 * @throws RepositoryAccessException
	 * @throws RosterRetrievingException
	 */
	public static Element getBuddyItem(final XMPPResourceConnection session,
			final Map<String, Object> settings, JID buddy)
					throws NotAuthorizedException, RosterRetrievingException,
							RepositoryAccessException {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			for (DynamicRosterIfc dri : dynr) {
				Element item = dri.getBuddyItem(session, buddy);

				if (item != null) {
					return item;
				}
			}
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param settings
	 *
	 *
	 *
	 * @return a value of <code>DynamicRosterIfc[]</code>
	 */
	public static DynamicRosterIfc[] getDynamicRosters(final Map<String, Object> settings) {
		DynamicRosterIfc[] dynr = null;

		if (settings != null) {
			synchronized (settings) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Initializing settings.");
				}
				init_settings(settings);
			}
			dynr = (DynamicRosterIfc[]) settings.get(DYNAMIC_ROSTERS);
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Settings parameter is NULL");
			}
		}

		return dynr;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param settings
	 * @param item
	 *
	 *
	 *
	 * @return a value of <code>Element</code>
	 */
	public static Element getItemExtraData(XMPPResourceConnection session, Map<String,
			Object> settings, Element item) {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			Element result = null;

			for (DynamicRosterIfc dri : dynr) {
				if ((result = dri.getItemExtraData(item)) != null) {
					break;
				}
			}

			return result;
		} else {
			return null;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param settings
	 *
	 *
	 *
	 *
	 * @return a value of {@code List<Element>}
	 * @throws NotAuthorizedException
	 * @throws RepositoryAccessException
	 * @throws RosterRetrievingException
	 */
	public static List<Element> getRosterItems(final XMPPResourceConnection session,
			final Map<String, Object> settings)
					throws NotAuthorizedException, RosterRetrievingException,
							RepositoryAccessException {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			ArrayList<Element> result = new ArrayList<Element>();

			for (DynamicRosterIfc dri : dynr) {
				List<Element> items = dri.getRosterItems(session);

				if (items != null) {
					result.addAll(items);
				}
			}
			if (result.size() > 0) {
				return result;
			}
		}

		return null;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param settings
	 * @param item
	 */
	public static void setItemExtraData(XMPPResourceConnection session, Map<String,
			Object> settings, Element item) {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			for (DynamicRosterIfc dri : dynr) {
				dri.setItemExtraData(item);
			}
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/11/29
