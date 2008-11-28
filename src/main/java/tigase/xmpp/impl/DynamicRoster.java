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
package tigase.xmpp.impl;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.logging.Logger;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

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
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.xmpp.impl.DynamicRoster");

	public static void init_settings(final Map<String, Object> settings) {
		DynamicRosterIfc[] dynr = (DynamicRosterIfc[])settings.get(DYNAMIC_ROSTERS);
		if (dynr == null) {
			log.info("Initializing dynamic rosters...");
			String dynclss = (String)settings.get(DYNAMIC_ROSTERS_CLASSES);
			if (dynclss != null) {
				String[] dyncls = dynclss.split(",");
				ArrayList<DynamicRosterIfc> al = new ArrayList<DynamicRosterIfc>();
				for (String cls: dyncls) {
					try {
						DynamicRosterIfc dri =
							(DynamicRosterIfc)Class.forName(cls).newInstance();
						if (settings.get(cls + ".init") != null) {
							dri.init((String)settings.get(cls + ".init"));
						} else {
							dri.init(settings);
						}
						al.add(dri);
						log.info("Initialized dynamic roster: " + cls);
					} catch (Exception e) {
						log.warning("Problem initializing dynmic roster class: "
							+ cls + ", " + e);
					}
				}
				if (al.size() > 0) {
					settings.put(DYNAMIC_ROSTERS,
						al.toArray(new DynamicRosterIfc[al.size()]));
				}
			}
		}
	}

	public static DynamicRosterIfc[] getDynamicRosters(final Map<String, Object> settings) {
		DynamicRosterIfc[] dynr = null;
		if (settings != null) {
			synchronized (settings) {
				log.finest("Initializing settings.");
				init_settings(settings);
			}
			dynr = (DynamicRosterIfc[])settings.get(DYNAMIC_ROSTERS);
		} else {
			log.finest("Settings parameter is NULL");
		}
		return dynr;
	}

	public static List<Element> getRosterItems(final XMPPResourceConnection session,
		final Map<String, Object> settings) throws NotAuthorizedException {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);
		if (dynr != null) {
			ArrayList<Element> result = new ArrayList<Element>();
			for (DynamicRosterIfc dri: dynr) {
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

	public static Element getBuddyItem(final XMPPResourceConnection session,
		final Map<String, Object> settings, String buddy)
		throws NotAuthorizedException {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);
		if (dynr != null) {
			for (DynamicRosterIfc dri: dynr) {
				Element item = dri.getBuddyItem(session, buddy);
				if (item != null) {
					return item;
				}
			}
		}
		return null;
	}

	public static List<String> getBuddiesList(final XMPPResourceConnection session,
		final Map<String, Object> settings) throws NotAuthorizedException {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);
		if (dynr != null) {
			ArrayList<String> result = new ArrayList<String>();
			for (DynamicRosterIfc dri: dynr) {
				String[] buddies = dri.getBuddies(session);
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

	public static String[] getBuddies(final XMPPResourceConnection session,
		final Map<String, Object> settings) throws NotAuthorizedException {
		List<String> result = getBuddiesList(session, settings);
		if (result != null && result.size() > 0) {
			return result.toArray(new String[result.size()]);
		}
		return null;
	}

	public static String[] addBuddies(final XMPPResourceConnection session,
		final Map<String, Object> settings, String[] buddies)
		throws NotAuthorizedException {
		List<String> result = getBuddiesList(session, settings);
		if (buddies != null) {
			if (result == null) {
				result = new ArrayList<String>();
			}
			result.addAll(Arrays.asList(buddies));
		}
		if (result != null && result.size() > 0) {
			return result.toArray(new String[result.size()]);
		}
		return null;
	}

	static Element getItemExtraData(XMPPResourceConnection session,
					Map<String, Object> settings, Element item) {
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

	static void setItemExtraData(XMPPResourceConnection session,
					Map<String, Object> settings, Element item) {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);
		if (dynr != null) {
			for (DynamicRosterIfc dri : dynr) {
				dri.setItemExtraData(item);
			}
		}
	}

}
