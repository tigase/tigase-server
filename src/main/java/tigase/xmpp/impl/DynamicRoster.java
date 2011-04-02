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

package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class DynamicRoster here.
 *
 *
 * Created: Tue Nov  6 11:28:10 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class DynamicRoster {
	private static final String DYNAMIC_ROSTERS = "dynamic-rosters";
	private static final String DYNAMIC_ROSTERS_CLASSES = "dynamic-roster-classes";

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
	 * @return
	 *
	 * @throws NotAuthorizedException
	 */
	public static JID[] addBuddies(final XMPPResourceConnection session,
			final Map<String, Object> settings, JID[] buddies)
			throws NotAuthorizedException {
		List<JID> result = getBuddiesList(session, settings);

		if (buddies != null) {
			if (result == null) {
				result = new ArrayList<JID>();
			}

			result.addAll(Arrays.asList(buddies));
		}

		if ((result != null) && (result.size() > 0)) {
			return result.toArray(new JID[result.size()]);
		}

		return null;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param settings
	 *
	 * @return
	 *
	 * @throws NotAuthorizedException
	 */
	public static JID[] getBuddies(final XMPPResourceConnection session,
			final Map<String, Object> settings)
			throws NotAuthorizedException {
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
	 * @return
	 *
	 * @throws NotAuthorizedException
	 */
	public static List<JID> getBuddiesList(final XMPPResourceConnection session,
			final Map<String, Object> settings)
			throws NotAuthorizedException {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			ArrayList<JID> result = new ArrayList<JID>();

			for (DynamicRosterIfc dri : dynr) {
				JID[] buddies = dri.getBuddies(session);

				if (buddies != null) {
					result.addAll(Arrays.asList(buddies));
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
	 * @return
	 *
	 * @throws NotAuthorizedException
	 */
	public static Element getBuddyItem(final XMPPResourceConnection session,
			final Map<String, Object> settings, JID buddy)
			throws NotAuthorizedException {
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
	 * @return
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
	 *
	 * @return
	 *
	 * @throws NotAuthorizedException
	 */
	public static List<Element> getRosterItems(final XMPPResourceConnection session,
			final Map<String, Object> settings)
			throws NotAuthorizedException {
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

	//~--- methods --------------------------------------------------------------

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
				String[] dyncls = dynclss.split(",");
				ArrayList<DynamicRosterIfc> al = new ArrayList<DynamicRosterIfc>(50);

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

	static Element getItemExtraData(XMPPResourceConnection session, Map<String, Object> settings,
			Element item) {
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

	//~--- set methods ----------------------------------------------------------

	static void setItemExtraData(XMPPResourceConnection session, Map<String, Object> settings,
			Element item) {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			for (DynamicRosterIfc dri : dynr) {
				dri.setItemExtraData(item);
			}
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
